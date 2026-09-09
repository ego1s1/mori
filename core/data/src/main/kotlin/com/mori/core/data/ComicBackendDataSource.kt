package com.mori.core.data

import android.graphics.Bitmap
import com.mori.comic.model.ComicMetadata
import com.mori.comic.model.ComicPage
import com.mori.comic.model.MediaType
import com.mori.comic.model.PageDimensions
import java.io.File

/**
 * Thin suspend wrapper over the comic backend (`comic-core`).
 *
 * Kept as an interface so repositories and fetchers run against fakes in tests while
 * production delegates to [ComicFactory][com.mori.comic.ComicFactory] and
 * [PageDecoder][com.mori.comic.decode.PageDecoder] on Dispatchers.IO.
 */
interface ComicBackendDataSource {
    /** Lists metadata + pages without decoding pixels. The archive is closed before return. */
    suspend fun inspect(file: File): InspectedComic

    suspend fun readPageBytes(file: File, page: ComicPage): ByteArray

    suspend fun readDimensions(bytes: ByteArray, mediaType: MediaType): PageDimensions

    suspend fun decodeCover(bytes: ByteArray, mediaType: MediaType, maxDimension: Int): Bitmap
}

/** Metadata plus page list for one archive file. */
data class InspectedComic(
    val metadata: ComicMetadata,
    val pages: List<ComicPage>,
)
