package com.mori.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.fetch.FetchResult
import coil3.fetch.ImageFetchResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComicPageFetcherTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun resourceBytes(name: String): ByteArray =
        javaClass.classLoader!!.getResourceAsStream("com/mori/core/data/$name")!!.use { it.readBytes() }

    private fun writeCbz(name: String, entries: Map<String, ByteArray>): File {
        val dir = File(context.filesDir, "fetcher-test").apply { mkdirs() }
        val file = File(dir, name)
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            entries.forEach { (entryName, content) ->
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return file
    }

    private fun repositoryFor(file: File): FakeComicsRepository {
        val comic = FakeComicsRepository.comic(file.name, pageCount = 1).copy(
            sourcePath = file.absolutePath,
        )
        return FakeComicsRepository(listOf(comic))
    }

    @Test
    fun factoryCreatesFetcherForPageKeys() {
        val factory = ComicPageFetcher.Factory(
            FakeComicsRepository(),
            FakeComicBackendDataSource(),
            com.mori.comic.decode.PageDecoder(),
        )
        val loader = ImageLoader.Builder(context).build()
        val options = coil3.request.Options(context)

        assertNotNull(factory.create(ComicPageKey("a", 0, 256), options, loader))
    }

    @Test
    fun fetchDecodesBoundedBitmap() = runTest {
        val file = writeCbz("a.cbz", mapOf("001.jpg" to resourceBytes("landscape.jpg")))
        val fetcher = ComicPageFetcher(
            data = ComicPageKey("a.cbz", 0, 256),
            repository = repositoryFor(file),
            backend = MoriComicBackendDataSource(com.mori.comic.decode.PageDecoder()),
            decoder = com.mori.comic.decode.PageDecoder(),
        )

        val result = fetcher.fetch()

        assertTrue(result is ImageFetchResult)
        val image = (result as ImageFetchResult).image
        assertTrue(image is coil3.BitmapImage)
        val bitmap = (image as coil3.BitmapImage).bitmap
        assertTrue(bitmap.width in 1..256)
        assertTrue(bitmap.height in 1..256)
    }

    @Test
    fun fetchUnknownComicThrows() = runTest {
        val fetcher = ComicPageFetcher(
            data = ComicPageKey("missing", 0, 256),
            repository = FakeComicsRepository(),
            backend = FakeComicBackendDataSource(),
            decoder = com.mori.comic.decode.PageDecoder(),
        )

        var failed = false
        try {
            fetcher.fetch()
        } catch (e: IllegalArgumentException) {
            failed = true
        }
        assertTrue(failed)
    }

    @Test
    fun fetchOutOfRangePageThrows() = runTest {
        val file = writeCbz("b.cbz", mapOf("001.jpg" to resourceBytes("portrait.jpg")))
        val fetcher = ComicPageFetcher(
            data = ComicPageKey("b.cbz", 9, 256),
            repository = repositoryFor(file),
            backend = MoriComicBackendDataSource(com.mori.comic.decode.PageDecoder()),
            decoder = com.mori.comic.decode.PageDecoder(),
        )

        var failed = false
        try {
            fetcher.fetch()
        } catch (e: IndexOutOfBoundsException) {
            failed = true
        }
        assertTrue(failed)
    }

    @Test
    fun pageKeyEqualityDrivesCacheIdentity() {
        assertEquals(ComicPageKey("a", 0, 256), ComicPageKey("a", 0, 256))
        assertTrue(ComicPageKey("a", 0, 256) != ComicPageKey("a", 1, 256))
        assertTrue(ComicPageKey("a", 0, 256) != ComicPageKey("a", 0, 512))
    }
}
