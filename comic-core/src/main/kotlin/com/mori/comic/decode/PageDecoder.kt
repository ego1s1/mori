package com.mori.comic.decode

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import androidx.exifinterface.media.ExifInterface
import com.mori.comic.DecodeException
import com.mori.comic.model.DecodeOptions
import com.mori.comic.model.DecodedPage
import com.mori.comic.model.MediaType
import com.mori.comic.model.PageDimensions

/**
 * Decodes page image bytes into [Bitmap]s, bounding memory via subsampling.
 *
 * The decoder never decodes a bitmap larger than requested: bounds are probed first, then a
 * power-of-two [inSampleSize][BitmapFactory.Options.inSampleSize] is chosen to fit the
 * result within [DecodeOptions.maxDimension]. Region decoding uses
 * [android.graphics.BitmapRegionDecoder] where the format supports it (JPEG, PNG, WebP),
 * falling back to a full decode + crop for formats it does not (GIF, BMP).
 */
class PageDecoder {

    /** Reads dimensions (and EXIF orientation) without decoding pixels. */
    fun readDimensions(bytes: ByteArray, mediaType: MediaType): PageDimensions {
        if (bytes.isEmpty()) throw DecodeException("Cannot decode dimensions of empty image data")
        val options = boundsOptions()
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        val width = options.outWidth
        val height = options.outHeight
        if (width <= 0 || height <= 0) {
            throw DecodeException("Unable to decode image dimensions for $mediaType")
        }
        val orientation = readOrientation(bytes, mediaType)
        return PageDimensions(width, height, mediaType, orientation)
    }

    /** Decodes the full page into a [DecodedPage], honoring [options]. */
    fun decode(bytes: ByteArray, mediaType: MediaType, options: DecodeOptions): DecodedPage {
        val dimensions = readDimensions(bytes, mediaType)
        val sampleSize = chooseSampleSize(dimensions, options)
        val bitmap = decodeSubsampled(bytes, sampleSize, options, dimensions)
        return DecodedPage(
            bitmap = bitmap,
            sourceWidth = dimensions.width,
            sourceHeight = dimensions.height,
            sampleSize = sampleSize,
            mediaType = mediaType,
        )
    }

    /**
     * Decodes a rectangular [region] (in full-resolution coordinates) of the page.
     *
     * @return the decoded region with [DecodedPage.sampleRect] set to the clamped,
     *   sample-size-aligned region that was actually decoded.
     */
    fun decodeRegion(
        bytes: ByteArray,
        mediaType: MediaType,
        region: Rect,
        options: DecodeOptions,
    ): DecodedPage {
        val dimensions = readDimensions(bytes, mediaType)
        val clamped = region.intersectFull(dimensions)
        return if (decoderSupportsRegions(mediaType)) {
            decodeRegionFast(bytes, mediaType, clamped, options, dimensions)
        } else {
            decodeRegionByCrop(bytes, mediaType, clamped, options, dimensions)
        }
    }

    private fun decodeRegionFast(
        bytes: ByteArray,
        mediaType: MediaType,
        region: Rect,
        options: DecodeOptions,
        dimensions: PageDimensions,
    ): DecodedPage {
        val sampleSize = chooseRegionSampleSize(region, options)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = options.preferredConfig
        }
        val decoder = regionDecoder(bytes)
        val sampledRect = scaleRectForSample(region, sampleSize)
        val bitmap = if (decoder != null) {
            val cropped = sampledRect.clampTo(dimensions.width, dimensions.height)
            decoder.decodeRegion(cropped, decodeOptions)
        } else {
            decodeRegionByCrop(bytes, mediaType, region, options, dimensions).bitmap
        }
        requireNotNull(bitmap) { "Failed to decode region" }
        return DecodedPage(bitmap, dimensions.width, dimensions.height, sampleSize, region, mediaType)
    }
    private fun decodeRegionByCrop(
        bytes: ByteArray,
        mediaType: MediaType,
        region: Rect,
        options: DecodeOptions,
        dimensions: PageDimensions,
    ): DecodedPage {
        val sampleSize = chooseRegionSampleSize(region, options)
        val full = decodeSubsampled(bytes, sampleSize, options, dimensions)
        return try {
            val left = region.left / sampleSize
            val top = region.top / sampleSize
            val right = (region.right + sampleSize - 1) / sampleSize
            val bottom = (region.bottom + sampleSize - 1) / sampleSize
            val cropped = Bitmap.createBitmap(
                full,
                left,
                top,
                (right - left).coerceAtMost(full.width - left),
                (bottom - top).coerceAtMost(full.height - top),
            )
            if (cropped !== full) full.recycle()
            DecodedPage(cropped, dimensions.width, dimensions.height, sampleSize, region, mediaType)
        } catch (e: IllegalArgumentException) {
            full.recycle()
            throw e
        }
    }

    private fun decodeSubsampled(
        bytes: ByteArray,
        sampleSize: Int,
        options: DecodeOptions,
        dimensions: PageDimensions,
    ): Bitmap {
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = options.preferredConfig
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            ?: throw DecodeException("Failed to decode image as ${dimensions.mediaType}")
        return if (options.respectExif && dimensions.orientation != 1) {
            rotateForOrientation(bitmap, dimensions.orientation)
        } else {
            bitmap
        }
    }

    private fun chooseSampleSize(dimensions: PageDimensions, options: DecodeOptions): Int {
        if (options.sampleSize > 1) return SampleSizeCalculator.roundToPowerOfTwo(options.sampleSize)
        return SampleSizeCalculator.forDimension(dimensions.width, dimensions.height, options.maxDimension)
    }

    private fun chooseRegionSampleSize(region: Rect, options: DecodeOptions): Int {
        if (options.sampleSize > 1) return SampleSizeCalculator.roundToPowerOfTwo(options.sampleSize)
        return SampleSizeCalculator.forDimension(region.width(), region.height(), options.maxDimension)
    }

    private fun boundsOptions(): BitmapFactory.Options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    private fun readOrientation(bytes: ByteArray, mediaType: MediaType): Int {
        if (mediaType != MediaType.JPEG) return 1
        return runCatching {
            bytes.inputStream().use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrDefault(1)
    }

    private fun rotateForOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = android.graphics.Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    @Suppress("DEPRECATION")
    private fun regionDecoder(bytes: ByteArray): android.graphics.BitmapRegionDecoder? {
        return runCatching {
            bytes.inputStream().use {
                android.graphics.BitmapRegionDecoder.newInstance(it, false)
            }
        }.getOrNull()
    }

    private fun decoderSupportsRegions(mediaType: MediaType): Boolean =
        mediaType == MediaType.JPEG || mediaType == MediaType.PNG || mediaType == MediaType.WEBP

    private fun scaleRectForSample(rect: Rect, sampleSize: Int): Rect =
        Rect(
            rect.left,
            rect.top,
            rect.left + rect.width() / sampleSize,
            rect.top + rect.height() / sampleSize,
        )

    private fun Rect.intersectFull(dimensions: PageDimensions): Rect =
        clampTo(dimensions.width, dimensions.height)

    private fun Rect.clampTo(width: Int, height: Int): Rect = Rect(
        left.coerceIn(0, width),
        top.coerceIn(0, height),
        right.coerceIn(0, width),
        bottom.coerceIn(0, height),
    )
}
