package com.mori.comic

import com.mori.comic.decode.PageDecoder
import com.mori.comic.model.ComicPage
import com.mori.comic.model.DecodeOptions
import com.mori.comic.model.DecodedPage
import com.mori.comic.model.PageDimensions

/**
 * A [ComicArchive] augmented with image decoding helpers.
 *
 * Wraps a [ComicArchive] and adds [decode], [decodeRegion], and [pageDimensions] that decode
 * the raw page bytes through a [PageDecoder] on the IO dispatcher.
 */
class DecodingComicArchive internal constructor(
    private val delegate: ComicArchive,
    private val decoder: PageDecoder = PageDecoder(),
) : ComicArchive by delegate {

    /** Decodes [ComicPage] at a bounded resolution. */
    suspend fun decode(page: ComicPage, options: DecodeOptions = DecodeOptions()): DecodedPage =
        decoder.decode(readPage(page), page.mediaType, options)

    /** Decodes a region of [ComicPage]. */
    suspend fun decodeRegion(
        page: ComicPage,
        region: android.graphics.Rect,
        options: DecodeOptions = DecodeOptions(),
    ): DecodedPage = decoder.decodeRegion(readPage(page), page.mediaType, region, options)

    /** Reads [page] dimensions without decoding pixels. */
    suspend fun pageDimensions(page: ComicPage): PageDimensions =
        decoder.readDimensions(readPage(page), page.mediaType)
}

/** Wraps [this] archive with decoding helpers. */
fun ComicArchive.withDecode(decoder: PageDecoder = PageDecoder()): DecodingComicArchive =
    DecodingComicArchive(this, decoder)
