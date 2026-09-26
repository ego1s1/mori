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
        // Framework decoders may throw (not just return empty bounds) on
        // hostile input; the decoder contract is DecodeException either way.
        val dimensions = runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            PageDimensions(options.outWidth, options.outHeight, mediaType)
        }.getOrNull()?.takeIf { it.width > 0 && it.height > 0 }
            ?: throw DecodeException("Unable to decode image dimensions for $mediaType")
        val orientation = readOrientation(bytes, mediaType)
        return dimensions.copy(orientation = orientation)
    }

    /** Decodes the full page into a [DecodedPage], honoring [options]. */
    fun decode(bytes: ByteArray, mediaType: MediaType, options: DecodeOptions): DecodedPage {
        val dimensions = readDimensions(bytes, mediaType)
        checkPixelBudget(dimensions)
        val sampleSize = chooseSampleSize(dimensions, options)
        val bitmap = decodeSubsampled(bytes, sampleSize, options, dimensions)
        // trimUniformMargins returns either the source or a new cropped bitmap;
        // recycle the source only when a distinct copy was produced.
        val final = if (options.cropMargins) {
            val cropped = trimUniformMargins(bitmap)
            if (cropped !== bitmap) bitmap.recycle()
            cropped
        } else {
            bitmap
        }
        return DecodedPage(
            bitmap = final,
            sourceWidth = dimensions.width,
            sourceHeight = dimensions.height,
            sampleSize = sampleSize,
            mediaType = mediaType,
        )
    }

    /**
     * Trims uniform border margins: edge rows/columns within tolerance of the
     * top-left corner color are removed, capped per side so light content
     * (skies, paper texture) survives. Never returns an empty bitmap.
     *
     * The scan itself lives in [MarginScan]: trimming four sides in one
     * function trips complexity budgets, so each side gets its own pass.
     */
    internal fun trimUniformMargins(bitmap: Bitmap): Bitmap {
        val frame = MarginScan(bitmap).frame()
        return if (frame.isWhole(bitmap.width, bitmap.height)) {
            bitmap
        } else {
            Bitmap.createBitmap(bitmap, frame.left, frame.top, frame.width, frame.height)
        }
    }

    /**
     * Decodes a rectangular [region] (in full-resolution coordinates) of the page.
     *
     * @return the decoded region with [DecodedPage.sampleRect] set to the clamped,
     *   sample-size-aligned region that was actually decoded.
     */
    fun decodeRegion(        bytes: ByteArray,
        mediaType: MediaType,
        region: Rect,
        options: DecodeOptions,
    ): DecodedPage {
        val dimensions = readDimensions(bytes, mediaType)
        checkPixelBudget(dimensions)
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
        try {
            val sampledRect = scaleRectForSample(region, sampleSize)
            val bitmap = if (decoder != null) {
                val cropped = sampledRect.clampTo(dimensions.width, dimensions.height)
                decoder.decodeRegion(cropped, decodeOptions)
            } else {
                decodeRegionByCrop(bytes, mediaType, region, options, dimensions).bitmap
            }
            requireNotNull(bitmap) { "Failed to decode region" }
            return DecodedPage(bitmap, dimensions.width, dimensions.height, sampleSize, region, mediaType)
        } finally {
            decoder?.recycle()
        }
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
        val bitmap = runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
        }.getOrNull()
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

    /**
     * Refuses absurd dimensions before any pixel buffer is allocated. Bounded
     * callers (reader, covers, thumbs) never approach the cap; only a crafted
     * header claiming gigapixels trips it, turning a potential OOM into the
     * normal decode-error path the UI already renders.
     */
    private fun checkPixelBudget(dimensions: PageDimensions) {
        if (dimensions.width.toLong() * dimensions.height > MAX_PIXELS) {
            throw DecodeException(
                "Refusing ${dimensions.width}x${dimensions.height} image " +
                    "(${dimensions.mediaType}): exceeds $MAX_PIXELS pixel budget",
            )
        }
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

    /**
     * Stream-based region decoder. Deliberately kept on the deprecated
     * `newInstance(InputStream, …)` overload: it is the only region path
     * that works back to minSdk 24 (ImageDecoder needs 28+ and offers no
     * region primitive). Failure here is non-fatal — [decodeRegionByCrop]
     * covers it with full-decode+crop.
     */
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

    private companion object {
        /**
         * Source images beyond this pixel count are refused before decoding.
         * Generous against real content (a 1600px-bounded reader page is
         * ~2MP; phone photos top out near 50MP) while keeping a crafted
         * gigapixel header from ever reaching a pixel allocation.
         */
        private const val MAX_PIXELS = 64L * 1024 * 1024
    }
}

/**
 * Pixel scan behind [PageDecoder.trimUniformMargins].
 *
 * Columns scan the full height first; rows then scan within the surviving
 * left/right edges — the same order as the original single-pass version,
 * just one side per function so complexity budgets hold. Trims keep at
 * least one pixel per side.
 */
private class MarginScan(bitmap: Bitmap) {
    private val width = bitmap.width
    private val height = bitmap.height
    private val pixels = IntArray(width * height).also {
        bitmap.getPixels(it, 0, width, 0, 0, width, height)
    }
    private val background = pixels[0]

    fun frame(): MarginFrame {
        if (width <= 2 || height <= 2) {
            return MarginFrame(0, 0, width - 1, height - 1)
        }
        // Per-side trim cap (10%): light content must survive aggressive
        // gutters. Local literal (not const) to stay out of the ABI dump.
        val maxX = (width * 0.10f).toInt().coerceAtLeast(1)
        val maxY = (height * 0.10f).toInt().coerceAtLeast(1)
        val left = trimLeft(maxX)
        val right = trimRight(left, maxX)
        val top = trimTop(left, right, maxY)
        val bottom = trimBottom(left, right, top, maxY)
        return MarginFrame(left, top, right, bottom)
    }

    private fun trimLeft(max: Int): Int {
        var edge = 0
        while (edge < max && edge + 1 < width && isMarginColumn(edge)) edge++
        return edge
    }

    private fun trimRight(left: Int, max: Int): Int {
        var edge = width - 1
        while (edge > left && width - 1 - edge < max && isMarginColumn(edge)) edge--
        return edge
    }

    private fun trimTop(left: Int, right: Int, max: Int): Int {
        var edge = 0
        while (edge < max && edge + 1 < height && isMarginRow(edge, left, right)) edge++
        return edge
    }

    private fun trimBottom(left: Int, right: Int, top: Int, max: Int): Int {
        var edge = height - 1
        while (edge > top && height - 1 - edge < max && isMarginRow(edge, left, right)) edge--
        return edge
    }

    private fun isMarginColumn(x: Int): Boolean {
        for (y in 0 until height) {
            if (!nearColor(pixels[y * width + x])) return false
        }
        return true
    }

    private fun isMarginRow(y: Int, left: Int, right: Int): Boolean {
        val row = y * width
        for (x in left..right) {
            if (!nearColor(pixels[row + x])) return false
        }
        return true
    }

    private fun nearColor(pixel: Int): Boolean {
        val dr = (pixel shr 16 and 0xFF) - (background shr 16 and 0xFF)
        val dg = (pixel shr 8 and 0xFF) - (background shr 8 and 0xFF)
        val db = (pixel and 0xFF) - (background and 0xFF)
        // Squared RGB tolerance (~14 levels per channel). Local literals (not
        // consts) so they stay out of the module's public ABI dump.
        return dr * dr + dg * dg + db * db <= 600
    }
}

/** Surviving rect after margin trimming. Plain class: identity is positional. */
private class MarginFrame(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left + 1
    val height: Int get() = bottom - top + 1

    /**
     * Whole-image check via dimensions only: with edges pinned inside the
     * image, unchanged dimensions imply untouched edges.
     */
    fun isWhole(imageWidth: Int, imageHeight: Int): Boolean =
        width == imageWidth && height == imageHeight
}
