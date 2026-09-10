package com.mori.core.data

import android.graphics.Bitmap
import com.mori.comic.ComicFactory
import com.mori.comic.ComicSource
import com.mori.comic.decode.PageDecoder
import com.mori.comic.model.ComicPage
import com.mori.comic.model.DecodeOptions
import com.mori.comic.model.MediaType
import com.mori.comic.model.PageDimensions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MoriComicBackendDataSource @Inject constructor(
    private val decoder: PageDecoder,
) : ComicBackendDataSource {

    override suspend fun inspect(file: File): InspectedComic = withContext(Dispatchers.IO) {
        ComicFactory.open(ComicSource.File(file)).use { archive ->
            InspectedComic(archive.metadata, archive.pages.toList())
        }
    }

    override suspend fun readPageBytes(file: File, page: ComicPage): ByteArray =
        withContext(Dispatchers.IO) {
            ComicFactory.open(ComicSource.File(file)).use { archive ->
                archive.readPage(page)
            }
        }

    override suspend fun readDimensions(bytes: ByteArray, mediaType: MediaType): PageDimensions =
        withContext(Dispatchers.IO) {
            decoder.readDimensions(bytes, mediaType)
        }

    override suspend fun decodeCover(bytes: ByteArray, mediaType: MediaType, maxDimension: Int): Bitmap =
        withContext(Dispatchers.IO) {
            decoder.decode(bytes, mediaType, DecodeOptions(maxDimension = maxDimension)).bitmap
        }
}
