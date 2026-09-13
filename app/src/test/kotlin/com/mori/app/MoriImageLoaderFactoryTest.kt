package com.mori.app

import androidx.test.core.app.ApplicationProvider
import com.mori.core.data.ComicPageFetcher
import com.mori.core.testing.FakeComicsRepository
import com.mori.core.data.LinkedArchiveCache
import com.mori.comic.model.ComicPage
import com.mori.comic.model.MediaType
import com.mori.comic.model.PageDimensions
import com.mori.core.data.ComicBackendDataSource
import com.mori.core.data.InspectedComic
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
                FakeComicsRepository(),
                FakeBackend(),
                com.mori.comic.decode.PageDecoder(),
                LinkedArchiveCache(context),
            ),
        )

        val loader = factory.newImageLoader(context)

        assertNotNull(loader)
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
