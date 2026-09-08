package com.mori.comic.model

/**
 * The decoded dimensions of a page without materializing pixel data.
 *
 * @property width the full-resolution width in pixels.
 * @property height the full-resolution height in pixels.
 * @property mediaType the image container format.
 * @property orientation the EXIF orientation value (1..8), or `1` when unknown/absent.
 */
data class PageDimensions(
    val width: Int,
    val height: Int,
    val mediaType: MediaType,
    val orientation: Int = 1,
)
