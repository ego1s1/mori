package com.mori.comic.model

import android.graphics.Bitmap

/**
 * Controls how a page's image bytes are decoded into a [Bitmap].
 *
 * @property maxDimension the maximum width or height (in pixels) of the decoded bitmap.
 *   When `0` (the default), the page is decoded at full resolution. Applied as a ceiling:
 *   the decoder picks the largest power-of-two [inSampleSize] that keeps the result within
 *   this bound, which bounds peak memory.
 * @property sampleSize explicit [BitmapFactory][android.graphics.BitmapFactory] inSampleSize
 *   in `1, 2, 4, 8, ...`. Non-power-of-two values are rounded down to a valid power of two.
 *   Overrides [maxDimension] when greater than `1`.
 * @property preferredConfig the bitmap configuration; defaults to [Bitmap.Config.ARGB_8888].
 *   Lower-memory consumers may choose [Bitmap.Config.RGB_565] for opaque pages.
 * @property respectExif whether EXIF orientation metadata is applied so the bitmap appears
 *   upright. Comic page scans are almost always already upright, so this defaults to false,
 *   but flipped photos (common with loose image sources) may enable it.
 */
data class DecodeOptions(
    val maxDimension: Int = 0,
    val sampleSize: Int = 1,
    val preferredConfig: Bitmap.Config = Bitmap.Config.ARGB_8888,
    val respectExif: Boolean = false,
) {
    init {
        require(maxDimension >= 0) { "maxDimension must be >= 0" }
        require(sampleSize >= 1) { "sampleSize must be >= 1" }
    }
}
