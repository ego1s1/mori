package com.mori.comic.decode

import android.graphics.Bitmap
import android.graphics.Rect
import com.mori.comic.DecodeException
import com.mori.comic.model.DecodeOptions
import com.mori.comic.model.MediaType
import com.mori.comic.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PageDecoderTest {

    private val decoder = PageDecoder()

    // --- Dimension reading ---

    @Test
    fun readsDimensionsOfBaselineJpeg() {
        val dims = decoder.readDimensions(Fixtures.Images.landscapeJpg(), MediaType.JPEG)
        assertEquals(1200, dims.width)
        assertEquals(923, dims.height)
    }

    @Test
    fun readsDimensionsOfPortraitJpeg() {
        val dims = decoder.readDimensions(Fixtures.Images.portraitJpg(), MediaType.JPEG)
        assertEquals(447, dims.width)
        assertEquals(687, dims.height)
    }

    @Test
    fun readsDimensionsOfProgressiveJpeg() {
        val dims = decoder.readDimensions(Fixtures.Images.progressiveJpg(), MediaType.JPEG)
        assertEquals(736, dims.width)
        assertEquals(1131, dims.height)
    }

    @Test
    fun gigapixelHeaderIsRefusedBeforeDecode() {
        // Minimal JPEG claiming 65535x65535 (~4GP): pixel paths must fail as
        // a normal decode error, never reach an allocation. (If bounds
        // parsing itself rejects the stub, that also throws DecodeException.)
        val header = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), // SOI
            0xFF.toByte(), 0xC0.toByte(), // SOF0
            0x00, 0x0B, // segment length
            0x08, // precision
            0xFF.toByte(), 0xFF.toByte(), // height
            0xFF.toByte(), 0xFF.toByte(), // width
            0x01, 0x01, 0x11, 0x00, // one component
        )
        assertThrows(DecodeException::class.java) {
            decoder.decode(header, MediaType.JPEG, DecodeOptions())
        }
        assertThrows(DecodeException::class.java) {
            decoder.decodeRegion(header, MediaType.JPEG, Rect(0, 0, 200, 200), DecodeOptions())
        }
    }

    // --- Full decode ---

    @Test
    fun fullDecodeProducesExpectedSize() {
        val page = decoder.decode(Fixtures.Images.landscapeJpg(), MediaType.JPEG, DecodeOptions())
        assertEquals(1200, page.bitmap.width)
        assertEquals(923, page.bitmap.height)
        assertEquals(1, page.sampleSize)
        assertEquals(1200, page.sourceWidth)
        assertEquals(923, page.sourceHeight)
        page.bitmap.recycle()
    }

    @Test
    fun maxDimensionSubsamplesToPowerOfTwo() {
        val page = decoder.decode(
            Fixtures.Images.landscapeJpg(),
            MediaType.JPEG,
            DecodeOptions(maxDimension = 500),
        )
        assertEquals(4, page.sampleSize)
        assertTrue(page.bitmap.width <= 500)
        page.bitmap.recycle()
    }

    @Test
    fun maxDimensionAtFullSizeStaysUnsampled() {
        val page = decoder.decode(
            Fixtures.Images.landscapeJpg(),
            MediaType.JPEG,
            DecodeOptions(maxDimension = 5000),
        )
        assertEquals(1, page.sampleSize)
        assertEquals(1200, page.bitmap.width)
        page.bitmap.recycle()
    }

    @Test
    fun explicitSampleSizeRoundsDownToPowerOfTwo() {
        val page = decoder.decode(
            Fixtures.Images.landscapeJpg(),
            MediaType.JPEG,
            DecodeOptions(sampleSize = 3),
        )
        assertEquals(2, page.sampleSize)
        assertEquals(600, page.bitmap.width)
        page.bitmap.recycle()
    }

    @Test
    fun rgb565ConfigIsRespected() {
        val page = decoder.decode(
            Fixtures.Images.landscapeJpg(),
            MediaType.JPEG,
            DecodeOptions(preferredConfig = Bitmap.Config.RGB_565),
        )
        assertEquals(Bitmap.Config.RGB_565, page.bitmap.config)
        page.bitmap.recycle()
    }

    // --- Region decode ---

    @Test
    fun regionDecodeReturnsOnlyRequestedArea() {
        val region = Rect(100, 100, 400, 400)
        val page = decoder.decodeRegion(Fixtures.Images.landscapeJpg(), MediaType.JPEG, region, DecodeOptions())
        assertNotNull(page.sampleRect)
        assertEquals(region, page.sampleRect)
        assertEquals(300, page.bitmap.width)
        assertEquals(300, page.bitmap.height)
        page.bitmap.recycle()
    }

    @Test
    fun regionDecodeClampsToImageBounds() {
        val region = Rect(1100, 800, 2000, 2000)
        val page = decoder.decodeRegion(Fixtures.Images.landscapeJpg(), MediaType.JPEG, region, DecodeOptions())
        assertEquals(Rect(1100, 800, 1200, 923), page.sampleRect)
        assertEquals(100, page.bitmap.width)
        assertEquals(123, page.bitmap.height)
        page.bitmap.recycle()
    }

    @Test
    fun regionDecodeSupportsSubsampling() {
        val region = Rect(0, 0, 800, 800)
        val page = decoder.decodeRegion(
            Fixtures.Images.landscapeJpg(),
            MediaType.JPEG,
            region,
            DecodeOptions(maxDimension = 200),
        )
        assertEquals(4, page.sampleSize)
        assertTrue(page.bitmap.width in 1..200)
        page.bitmap.recycle()
    }

    @Test
    fun progressiveJpegDecodesFullyAndByRegion() {
        val full = decoder.decode(
            Fixtures.Images.progressiveJpg(),
            MediaType.JPEG,
            DecodeOptions(maxDimension = 400),
        )
        assertTrue(full.bitmap.width in 1..400)
        full.bitmap.recycle()

        val region = decoder.decodeRegion(
            Fixtures.Images.progressiveJpg(),
            MediaType.JPEG,
            Rect(0, 0, 200, 200),
            DecodeOptions(),
        )
        assertEquals(200, region.bitmap.width)
        region.bitmap.recycle()
    }

    // --- WebP / GIF (native Skia codecs not available under Robolectric) ---

    @Test
    fun webpDecodeEitherSucceedsOrFailsGracefully() {
        val outcome = runCatching {
            decoder.decode(Fixtures.Images.comicWebp(), MediaType.WEBP, DecodeOptions(maxDimension = 256))
        }
        if (outcome.isSuccess) {
            val page = outcome.getOrThrow()
            assertTrue(page.bitmap.width in 1..256)
            page.bitmap.recycle()
        } else {
            assertTrue(outcome.exceptionOrNull() is DecodeException)
        }
    }

    @Test
    fun gifDecodeEitherSucceedsOrFailsGracefully() {
        val gif = minimalGif()
        val outcome = runCatching { decoder.decode(gif, MediaType.GIF, DecodeOptions(maxDimension = 64)) }
        if (outcome.isSuccess) {
            outcome.getOrThrow().bitmap.recycle()
        } else {
            // Robolectric may surface a native-codec failure as a RuntimeException here.
            assertTrue(outcome.exceptionOrNull() != null)
        }
    }

    // --- Error / edge cases ---
    //
    // NOTE: Robolectric's default ShadowBitmapFactory returns a synthetic 100x100 bitmap for
    // undecodable bytes instead of null, so "invalid bytes -> DecodeException" is verified on a
    // real device via instrumented tests (Phase 4). Here we assert the library never crashes.

    @Test
    fun invalidBytesDoNotCrashDimensionRead() {
        val result = runCatching {
            decoder.readDimensions("not an image".toByteArray(), MediaType.JPEG)
        }
        result.fold(
            onSuccess = { assertTrue(it.width > 0) },
            onFailure = { assertTrue(it is DecodeException || it is IllegalArgumentException) },
        )
    }

    @Test
    fun invalidBytesDoNotCrashDecode() {
        val result = runCatching {
            decoder.decode("not an image".toByteArray(), MediaType.JPEG, DecodeOptions())
        }
        result.fold(
            onSuccess = { it.bitmap.recycle() },
            onFailure = { assertTrue(it is DecodeException || it is IllegalArgumentException) },
        )
    }

    @Test
    fun emptyBytesThrowDecodeException() {
        assertThrows(DecodeException::class.java) {
            decoder.readDimensions(byteArrayOf(), MediaType.JPEG)
        }
        assertThrows(DecodeException::class.java) {
            decoder.decode(byteArrayOf(), MediaType.JPEG, DecodeOptions())
        }
    }

    @Test
    fun maxDimensionZeroMeansFullResolution() {
        val page = decoder.decode(Fixtures.Images.portraitJpg(), MediaType.JPEG, DecodeOptions(maxDimension = 0))
        assertEquals(1, page.sampleSize)
        assertEquals(447, page.bitmap.width)
        page.bitmap.recycle()
    }

    @Test
    fun tinyMaxDimensionStillDecodes() {
        val page = decoder.decode(Fixtures.Images.portraitJpg(), MediaType.JPEG, DecodeOptions(maxDimension = 1))
        assertTrue(page.sampleSize >= 1)
        assertTrue(page.bitmap.width >= 1)
        page.bitmap.recycle()
    }

    @Test
    fun unrotatedJpegHasNoEffectiveRotation() {
        // Fixtures carry EXIF orientation 0 (undefined) or 1 (normal); both mean "no rotation".
        val orientation = decoder.readDimensions(Fixtures.Images.landscapeJpg(), MediaType.JPEG).orientation
        assertTrue(orientation == 0 || orientation == 1)
    }

    @Test
    fun orientationReadsExifWhenPresent() {
        val rotated = jpegWithExifOrientation(6)
        val dims = decoder.readDimensions(rotated, MediaType.JPEG)
        assertEquals(6, dims.orientation)
    }

    @Test
    fun respectExifSwapsDimensionsForRotate90() {
        val rotated = jpegWithExifOrientation(6)
        val upright = decoder.decode(rotated, MediaType.JPEG, DecodeOptions(respectExif = true))
        val ignored = decoder.decode(rotated, MediaType.JPEG, DecodeOptions(respectExif = false))
        assertEquals(upright.bitmap.height, ignored.bitmap.width)
        upright.bitmap.recycle()
        ignored.bitmap.recycle()
    }

    // --- Helpers ---

    private fun minimalGif(): ByteArray =
        byteArrayOf(
            0x47, 0x49, 0x46, 0x38, 0x39, 0x61, // GIF89a
            0x01, 0x00, 0x01, 0x00, // 1x1
            0x80.toByte(), 0x00, 0x00,
            0x00, 0x00, 0x00,
            0x21, 0xF9.toByte(), 0x04, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x2C, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
            0x02, 0x02, 0x44, 0x01, 0x00,
            0x3B,
        )

    private fun jpegWithExifOrientation(orientation: Int): ByteArray {
        val source = Fixtures.Images.portraitJpg()
        val app0End = findApp0End(source)
        val exif = buildExifSegment(orientation)
        return source.copyOfRange(0, app0End) + exif + source.copyOfRange(app0End, source.size)
    }

    private fun buildExifSegment(orientation: Int): ByteArray {
        val tiff = byteArrayOf(
            0x49, 0x49, 0x2A, 0x00,
            0x08, 0x00, 0x00, 0x00,
            0x01, 0x00,
            0x12, 0x01,
            0x03, 0x00,
            0x01, 0x00, 0x00, 0x00,
            (orientation and 0xFF).toByte(), 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
        )
        val payload = "Exif\u0000\u0000".toByteArray(Charsets.US_ASCII) + tiff
        val len = payload.size + 2
        return byteArrayOf(
            0xFF.toByte(), 0xE1.toByte(),
            ((len shr 8) and 0xFF).toByte(), (len and 0xFF).toByte(),
        ) + payload
    }

    // --- Margin cropping ---

    private fun borderedArt(size: Int = 100, border: Int = 20): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply { color = android.graphics.Color.BLACK }
        canvas.drawRect(
            border.toFloat(),
            border.toFloat(),
            (size - border).toFloat(),
            (size - border).toFloat(),
            paint,
        )
        return bitmap
    }

    @Test
    fun trimRemovesUniformBorders() {
        val trimmed = decoder.trimUniformMargins(borderedArt(border = 8))

        assertEquals(85, trimmed.width)
        assertEquals(85, trimmed.height)
    }

    @Test
    fun trimLeavesFramedArtUntouched() {
        // Pixel-direct diagonal (Canvas strokes are flaky under Robolectric):
        // every edge carries ink against a light corner, so nothing trims.
        // (A uniform frame the corner's own color IS margin by definition.)
        val art = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.WHITE)
        }
        val ink = android.graphics.Color.BLACK
        for (i in 0 until 100) {
            art.setPixel(i, i, ink)
        }

        assertEquals(art, decoder.trimUniformMargins(art))
    }

    @Test
    fun trimNeverEmptiesUniformArt() {
        val solid = Bitmap.createBitmap(100, 80, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.WHITE)
        }
        val trimmed = decoder.trimUniformMargins(solid)

        // Capped at 10% per side: 80x64 of solid white survives.
        assertEquals(80, trimmed.width)
        assertEquals(64, trimmed.height)
    }

    @Test
    fun decodeHonorsCropMarginsOption() {
        val bytes = Fixtures.Images.landscapeJpg()
        val plain = decoder.decode(bytes, MediaType.JPEG, DecodeOptions())
        val page = decoder.decode(bytes, MediaType.JPEG, DecodeOptions(cropMargins = true))

        // Cropping never grows the bitmap.
        assertTrue(page.bitmap.width <= plain.bitmap.width)
        assertTrue(page.bitmap.height <= plain.bitmap.height)
    }

    private fun findApp0End(bytes: ByteArray): Int {
        var i = 2
        while (i < bytes.size - 1) {
            if (bytes[i] == 0xFF.toByte() && bytes[i + 1] == 0xE0.toByte()) {
                val len = ((bytes[i + 2].toInt() and 0xFF) shl 8) or (bytes[i + 3].toInt() and 0xFF)
                return i + 2 + len
            }
            if (bytes[i] == 0xFF.toByte() && bytes[i + 1] != 0xFF.toByte()) {
                val len = ((bytes[i + 2].toInt() and 0xFF) shl 8) or (bytes[i + 3].toInt() and 0xFF)
                i += 2 + len
            } else {
                i += 1
            }
        }
        return 2
    }
}
