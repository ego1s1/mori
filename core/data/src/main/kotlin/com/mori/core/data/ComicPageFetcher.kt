package com.mori.core.data

import android.graphics.Rect
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import com.mori.comic.decode.PageDecoder
import com.mori.comic.model.DecodeOptions
import com.mori.core.model.PageHalf
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

/**
 * Cacheable key for one decoded comic page. Data class equality gives Coil correct
 * memory/disk cache semantics for free; [maxDimension] is part of the key so different
 * resolutions never collide.
 *
 * [half] rides the key so split halves cache independently: toggling the
 * dual-page split can never serve a full page where a half belongs.
 */
data class ComicPageKey(
    val comicId: String,
    val pageIndex: Int,
    val maxDimension: Int,
    val cropMargins: Boolean = false,
    val half: PageHalf = PageHalf.FULL,
)

/**
 * Coil [Fetcher] that decodes comic pages through the backend ([ComicBackendDataSource]).
 *
 * Resolution is bounded by [ComicPageKey.maxDimension], keeping grid thumbnails and
 * reader pages inside fixed memory budgets.
 */
class ComicPageFetcher internal constructor(
    private val data: ComicPageKey,
    private val repository: ComicsRepository,
    private val backend: ComicBackendDataSource,
    private val decoder: PageDecoder,
    private val linkedCache: LinkedArchiveCache,
) : Fetcher {

    /**
     * All fetch failures surface as [IOException] (never
     * [IllegalArgumentException]/[IndexOutOfBoundsException]): Coil treats
     * IO errors as source failures with the error placeholder, while
     * programming-error exceptions would read as unrecoverable crashes.
     * Mapping: unknown comic -> [FileNotFoundException] (row removed);
     * bad index -> [IOException] with the valid range (stale split list).
     */
    override suspend fun fetch(): FetchResult {
        val comic = repository.getComic(data.comicId)
            ?: throw FileNotFoundException("Unknown comic: ${data.comicId}")
        // Linked rows materialize through the bounded read cache; the user
        // original is never copied into the library.
        val file = linkedCache.fileFor(comic)
        val inspected = backend.inspect(file)
        val page = inspected.pages.getOrNull(data.pageIndex)
            ?: throw IOException(
                "Page ${data.pageIndex} of ${data.comicId} (count ${inspected.pages.size})",
            )
        val bytes = backend.readPageBytes(file, page)
        // Crop rides the decode (and the key above), so toggling it can never
        // serve a stale cached bitmap.
        val options = DecodeOptions(maxDimension = data.maxDimension, cropMargins = data.cropMargins)
        val decoded = if (data.half == PageHalf.FULL) {
            decoder.decode(bytes, page.mediaType, options)
        } else {
            // Dual-page split: region-decode one side so
            // the full wide bitmap is never materialized. Halves meet at the
            // middle column with no overlap and no gap.
            val dimensions = decoder.readDimensions(bytes, page.mediaType)
            val mid = dimensions.width / 2
            val region = when (data.half) {
                PageHalf.LEFT -> Rect(0, 0, mid, dimensions.height)
                PageHalf.RIGHT -> Rect(mid, 0, dimensions.width, dimensions.height)
                PageHalf.FULL -> error("unreachable")
            }
            decoder.decodeRegion(bytes, page.mediaType, region, options)
        }
        return ImageFetchResult(
            image = decoded.bitmap.asImage(),
            isSampled = data.maxDimension > 0,
            dataSource = DataSource.DISK,
        )
    }

    class Factory @Inject constructor(
        private val repository: ComicsRepository,
        private val backend: ComicBackendDataSource,
        private val decoder: PageDecoder,
        private val linkedCache: LinkedArchiveCache,
    ) : Fetcher.Factory<ComicPageKey> {
        override fun create(
            data: ComicPageKey,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = ComicPageFetcher(data, repository, backend, decoder, linkedCache)
    }
}
