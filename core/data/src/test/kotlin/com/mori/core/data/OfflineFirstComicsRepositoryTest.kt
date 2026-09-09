package com.mori.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.mori.core.database.ComicDao
import com.mori.core.database.MoriDatabase
import com.mori.core.model.ComicError
import com.mori.core.model.LibraryQuery
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
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
class OfflineFirstComicsRepositoryTest {

    private lateinit var database: MoriDatabase
    private lateinit var dao: ComicDao
    private lateinit var libraryDir: File

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(context, MoriDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.comicDao()
        libraryDir = File(context.filesDir, "comics").apply { mkdirs() }
        libraryDir.listFiles()?.forEach { it.delete() }
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun repository(backend: ComicBackendDataSource) = OfflineFirstComicsRepository(
        dao = dao,
        backend = backend,
        covers = CoverGenerator(context, backend),
        context = context,
    )

    private fun resourceBytes(name: String): ByteArray =
        javaClass.classLoader!!.getResourceAsStream("com/mori/core/data/$name")!!.use { it.readBytes() }

    private fun writeCbz(name: String, entries: Map<String, ByteArray>): File {
        val file = File(libraryDir, name)
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            entries.forEach { (entryName, content) ->
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return file
    }

    @Test
    fun refreshLibraryIndexesNewArchivesWithCovers() = runTest {
        writeCbz(
            "alpha.cbz",
            mapOf("001.jpg" to resourceBytes("landscape.jpg")),
        )

        val backend = FakeComicBackendDataSource(
            inspected = mapOf(
                "alpha.cbz" to FakeComicBackendDataSource.inspected("001.jpg"),
            ),
        )
        val report = repository(backend).refreshLibrary()

        assertEquals(1, report.indexed)
        assertEquals(0, report.failed)
        repository(backend).observeLibrary(LibraryQuery()).test {
            val comics = awaitItem()
            assertEquals(1, comics.size)
            assertEquals("alpha.cbz", comics.single().id)
            assertEquals(1, comics.single().pageCount)
            assertTrue(comics.single().coverPath?.let { File(it).isFile } == true)
        }
    }

    @Test
    fun refreshLibrarySkipsUnchangedFiles() = runTest {
        writeCbz("alpha.cbz", mapOf("001.jpg" to resourceBytes("landscape.jpg")))
        var inspects = 0
        val inspected = mutableMapOf(
            "alpha.cbz" to FakeComicBackendDataSource.inspected("001.jpg"),
        )
        val inner = FakeComicBackendDataSource(
            inspected = inspected,
            pageBytes = mapOf("001.jpg" to resourceBytes("landscape.jpg")),
        )
        val backend = object : ComicBackendDataSource by inner {
            override suspend fun inspect(file: File): InspectedComic {
                inspects += 1
                return inner.inspect(file)
            }
        }

        val repository = repository(backend)
        repository.refreshLibrary()
        // Second added file forces a rescan; alpha.cbz must be skipped via fast path,
        // so exactly one more inspect happens (for beta.cbz).
        writeCbz("beta.cbz", mapOf("001.jpg" to resourceBytes("portrait.jpg")))
        inspected["beta.cbz"] = FakeComicBackendDataSource.inspected("001.jpg")
        repository.refreshLibrary()
        assertEquals(2, inspects)
        repository.observeLibrary(LibraryQuery()).test {
            assertEquals(2, awaitItem().size)
        }
    }

    @Test
    fun refreshLibraryRemovesMissingFiles() = runTest {
        val file = writeCbz("alpha.cbz", mapOf("001.jpg" to resourceBytes("landscape.jpg")))
        val backend = FakeComicBackendDataSource(
            inspected = mapOf("alpha.cbz" to FakeComicBackendDataSource.inspected("001.jpg")),
        )
        val repository = repository(backend)
        repository.refreshLibrary()
        file.delete()

        val report = repository.refreshLibrary()

        assertEquals(1, report.removed)
        repository.observeLibrary(LibraryQuery()).test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun corruptArchivesBecomeErrorRows() = runTest {
        writeCbz("broken.cbz", mapOf("001.jpg" to resourceBytes("landscape.jpg")))
        val backend = FakeComicBackendDataSource(
            failWith = com.mori.comic.CorruptArchiveException("bad"),
        )
        val repository = repository(backend)
        val report = repository.refreshLibrary()

        assertEquals(0, report.indexed)
        assertEquals(1, report.failed)
        repository.observeLibrary(LibraryQuery()).test {
            val comics = awaitItem()
            assertEquals(ComicError.CORRUPT, comics.single().error)
        }
    }

    @Test
    fun removeComicDeletesFilesAndRow() = runTest {
        writeCbz("alpha.cbz", mapOf("001.jpg" to resourceBytes("landscape.jpg")))
        val backend = FakeComicBackendDataSource(
            inspected = mapOf("alpha.cbz" to FakeComicBackendDataSource.inspected("001.jpg")),
        )
        val repository = repository(backend)
        repository.refreshLibrary()
        val cover = repository.getComic("alpha.cbz")?.coverPath
        assertTrue(cover != null)

        repository.removeComic("alpha.cbz")

        assertNull(repository.getComic("alpha.cbz"))
        assertTrue(cover?.let { File(it).exists() } == false)
    }

    @Test
    fun saveProgressClampsToPageRange() = runTest {
        writeCbz("alpha.cbz", mapOf("001.jpg" to resourceBytes("landscape.jpg")))
        val backend = FakeComicBackendDataSource(
            inspected = mapOf("alpha.cbz" to FakeComicBackendDataSource.inspected("001.jpg")),
        )
        val repository = repository(backend)
        repository.refreshLibrary()

        repository.saveProgress("alpha.cbz", 99)

        // Single-page comic clamps to index 0.
        assertEquals(0, repository.getComic("alpha.cbz")?.lastPageIndex)
    }

    @Test
    fun toggleBookmarkFlipsPersistedFlag() = runTest {
        writeCbz("alpha.cbz", mapOf("001.jpg" to resourceBytes("landscape.jpg")))
        val backend = FakeComicBackendDataSource(
            inspected = mapOf("alpha.cbz" to FakeComicBackendDataSource.inspected("001.jpg")),
        )
        val repository = repository(backend)
        repository.refreshLibrary()

        assertEquals(false, repository.getComic("alpha.cbz")?.bookmarked)
        repository.toggleBookmark("alpha.cbz")
        assertEquals(true, repository.getComic("alpha.cbz")?.bookmarked)
        repository.toggleBookmark("alpha.cbz")
        assertEquals(false, repository.getComic("alpha.cbz")?.bookmarked)
    }

    @Test
    fun refreshComicReturnsNullForUnknownId() = runTest {
        val backend = FakeComicBackendDataSource()
        assertNull(repository(backend).refreshComic("nope"))
    }
}
