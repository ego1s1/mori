package com.mori.core.data

import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import com.mori.comic.model.DecodeOptions
import com.mori.comic.decode.PageDecoder
import java.io.File
import javax.inject.Inject

/**
 * Cacheable key for one decoded comic page. Data class equality gives Coil correct
 * memory/disk cache semantics for free; [maxDimension] is part of the key so different
 * resolutions never collide.
 */
data class ComicPageKey(
    val comicId: String,
    val pageIndex: Int,
    val maxDimension: Int,
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
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val comic = repository.getComic(data.comicId)
            ?: throw IllegalArgumentException("Unknown comic: ${data.comicId}")
        val file = File(comic.sourcePath)
        val inspected = backend.inspect(file)
        val page = inspected.pages.getOrNull(data.pageIndex)
            ?: throw IndexOutOfBoundsException("Page ${data.pageIndex} of ${data.comicId}")
        val bytes = backend.readPageBytes(file, page)
        val decoded = decoder.decode(bytes, page.mediaType, DecodeOptions(maxDimension = data.maxDimension))
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
    ) : Fetcher.Factory<ComicPageKey> {
        override fun create(
            data: ComicPageKey,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = ComicPageFetcher(data, repository, backend, decoder)
    }
}
