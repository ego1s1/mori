package com.mori.comic.model

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * A decoded comic page, ready for display.
 *
 * @property bitmap the decoded (possibly subsampled) bitmap.
 * @property sourceWidth the full-resolution width of the page.
 * @property sourceHeight the full-resolution height of the page.
 * @property sampleSize the inSampleSize applied when decoding [bitmap]; `1` means full size.
 * @property sampleRect the region of the full page represented by [bitmap], or empty for a full-page decode.
 * @property mediaType the image container format.
 */
data class DecodedPage(
    val bitmap: Bitmap,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val sampleSize: Int,
    val sampleRect: Rect? = null,
    val mediaType: MediaType,
)
