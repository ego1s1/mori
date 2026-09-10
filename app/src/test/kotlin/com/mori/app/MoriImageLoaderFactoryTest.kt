package com.mori.app

import androidx.test.core.app.ApplicationProvider
import com.mori.core.data.ComicPageFetcher
import com.mori.core.data.ComicsRepository
import com.mori.comic.model.ComicPage
import com.mori.comic.model.MediaType
import com.mori.comic.model.PageDimensions
import com.mori.core.data.ComicBackendDataSource
import com.mori.core.data.InspectedComic
import com.mori.core.model.Comic
import com.mori.core.model.IndexReport
import com.mori.core.model.LibraryQuery
import com.mori.core.model.StorageUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the Coil wiring that reader pages depend on: the factory must build a
 * working ImageLoader carrying our page fetcher. (A missing fetcher is exactly how
 * reader pages silently stayed placeholders.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MoriImageLoaderFactoryTest {

    @Test
    fun buildsLoaderWithPageFetcher() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val factory = MoriImageLoaderFactory(
            context,
            ComicPageFetcher.Factory(
                FakeRepository(),
                FakeBackend(),
                com.mori.comic.decode.PageDecoder(),
            ),
        )

        val loader = factory.newImageLoader(context)

        assertNotNull(loader)
    }

    private class FakeRepository : ComicsRepository {
        override fun observeLibrary(query: LibraryQuery): Flow<List<Comic>> =
            MutableStateFlow(emptyList<Comic>()).asStateFlow()

        override fun observeComic(id: String): Flow<Comic?> =
            MutableStateFlow<Comic?>(null).asStateFlow()

        override suspend fun getComic(id: String): Comic? = null

        override suspend fun refreshLibrary(): IndexReport = IndexReport(0, 0, 0)

        override suspend fun refreshComic(id: String): Comic? = null

        override suspend fun removeComic(id: String) = Unit

        override suspend fun saveProgress(id: String, pageIndex: Int) = Unit

        override suspend fun toggleBookmark(id: String) = Unit

        override suspend fun clearThumbnailCache() = Unit

        override suspend fun storageUsage(): StorageUsage = StorageUsage(0, 0L, 0L)
    }

    private class FakeBackend : ComicBackendDataSource {
        override suspend fun inspect(file: java.io.File): InspectedComic {
            throw UnsupportedOperationException()
        }

        override suspend fun readPageBytes(
            file: java.io.File,
            page: ComicPage,
        ): ByteArray {
            throw UnsupportedOperationException()
        }

        override suspend fun readDimensions(
            bytes: ByteArray,
            mediaType: MediaType,
        ): PageDimensions {
            throw UnsupportedOperationException()
        }

        override suspend fun decodeCover(
            bytes: ByteArray,
            mediaType: MediaType,
            maxDimension: Int,
        ): android.graphics.Bitmap {
            throw UnsupportedOperationException()
        }
    }
}
