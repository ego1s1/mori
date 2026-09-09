package com.mori.core.data

import android.graphics.Bitmap
import com.mori.comic.model.ComicMetadata
import com.mori.comic.model.ComicPage
import com.mori.comic.model.MediaType
import com.mori.comic.model.PageDimensions
import java.io.File

/** Scripted backend double: no archives touched. */
internal class FakeComicBackendDataSource(
    var inspected: Map<String, InspectedComic> = emptyMap(),
    var pageBytes: Map<String, ByteArray> = emptyMap(),
    var failWith: Exception? = null,
) : ComicBackendDataSource {

    private fun fail(): Nothing = throw failWith ?: IllegalStateException("not stubbed")

    override suspend fun inspect(file: File): InspectedComic =
        failWith?.let { throw it } ?: inspected[file.name] ?: fail()

    override suspend fun readPageBytes(file: File, page: ComicPage): ByteArray =
        failWith?.let { throw it } ?: pageBytes[page.name] ?: ByteArray(0)

    override suspend fun readDimensions(bytes: ByteArray, mediaType: MediaType): PageDimensions =
        failWith?.let { throw it } ?: PageDimensions(100, 100, mediaType, 1)

    override suspend fun decodeCover(bytes: ByteArray, mediaType: MediaType, maxDimension: Int): Bitmap =
        failWith?.let { throw it } ?: Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

    companion object {
        fun pages(vararg names: String): List<ComicPage> =
            names.mapIndexed { index, name ->
                ComicPage(index, name, MediaType.fromName(name), 10L)
            }

        fun inspected(vararg names: String): InspectedComic =
            InspectedComic(ComicMetadata(title = "T"), pages(*names))
    }
}
