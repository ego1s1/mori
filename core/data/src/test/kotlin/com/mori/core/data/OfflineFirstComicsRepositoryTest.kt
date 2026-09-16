package com.mori.core.data

import android.content.Context
import android.net.Uri
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfflineFirstComicsRepositoryTest {

    private lateinit var database: MoriDatabase
    private lateinit var dao: ComicDao

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(context, MoriDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.comicDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun repository(
        backend: ComicBackendDataSource,
        lister: LinkedTreeLister = FakeLinkedTreeLister(),
    ) = OfflineFirstComicsRepository(
        dao = dao,
        backend = backend,
        covers = CoverGenerator(context, backend),
        linkedCache = LinkedArchiveCache(context),
        treeLister = lister,
        context = context,
    )

    private fun resourceBytes(name: String): ByteArray =
        javaClass.classLoader!!.getResourceAsStream("com/mori/core/data/$name")!!.use { it.readBytes() }

    private fun zipBytes(entries: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (entryName, content) ->
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun docUri(name: String): Uri =
        Uri.parse("content://com.example/tree/" + Uri.encode(name))

    /** Serves archive bytes for a document URI through the content resolver. */
    private fun registerDoc(name: String, entries: Map<String, ByteArray> = mapOf("001.jpg" to resourceBytes("landscape.jpg"))) {
        shadowOf(context.contentResolver)
            .registerInputStream(docUri(name), ByteArrayInputStream(zipBytes(entries)))
    }

    private fun linkedDoc(name: String, modified: Long = 1000L) =
        LinkedDocument(docUri(name), name, modified)

    private fun backendFor(): FakeComicBackendDataSource =
        FakeComicBackendDataSource(
            defaultInspected = FakeComicBackendDataSource.inspected("001.jpg"),
            pageBytes = mapOf("001.jpg" to resourceBytes("landscape.jpg")),
        )

    @Test
    fun indexLinkedTreeIndexesDocumentsWithCovers() = runTest {
        registerDoc("alpha.cbz")
        registerDoc("beta.cbz")
        val lister = FakeLinkedTreeLister(
            LinkedTreeListResult(
                listOf(linkedDoc("alpha.cbz"), linkedDoc("beta.cbz")),
                walkFailed = false,
            ),
        )
        val report = repository(backendFor(), lister)
            .indexLinkedTree(Uri.parse("content://com.example/tree")) { _, _ -> }

        assertEquals(2, report.total)
        assertEquals(2, report.succeeded)
        assertEquals(0, report.failed)
        repository(backendFor(), lister).observeLibrary(LibraryQuery()).test {
            val comics = awaitItem()
            assertEquals(2, comics.size)
            comics.forEach {
                assertTrue(it.coverPath?.let { path -> File(path).isFile } == true)
            }
        }
    }

    @Test
    fun indexLinkedTreeBatchesWritesIntoOneEmission() = runTest {
        listOf("alpha.cbz", "beta.cbz", "gamma.cbz").forEach { registerDoc(it) }
        val lister = FakeLinkedTreeLister(
            LinkedTreeListResult(
                listOf(linkedDoc("alpha.cbz"), linkedDoc("beta.cbz"), linkedDoc("gamma.cbz")),
                walkFailed = false,
            ),
        )
        val repository = repository(backendFor(), lister)
        // Batched upsert emits once with the final state, not once per file.
        repository.observeLibrary(LibraryQuery()).test {
            assertEquals(0, awaitItem().size)
            repository.indexLinkedTree(Uri.parse("content://com.example/tree")) { _, _ -> }
            assertEquals(3, awaitItem().size)
            expectNoEvents()
        }
    }

    @Test
    fun indexLinkedTreeSkipsUnchangedDocuments() = runTest {
        registerDoc("alpha.cbz")
        var inspects = 0
        val inner = FakeComicBackendDataSource(
            defaultInspected = FakeComicBackendDataSource.inspected("001.jpg"),
            pageBytes = mapOf("001.jpg" to resourceBytes("landscape.jpg")),
        )
        val backend = object : ComicBackendDataSource by inner {
            override suspend fun inspect(file: File): InspectedComic {
                inspects += 1
                return inner.inspect(file)
            }
        }
        val lister = FakeLinkedTreeLister(
            LinkedTreeListResult(listOf(linkedDoc("alpha.cbz", modified = 1000L)), walkFailed = false),
        )
        val repository = repository(backend, lister)
        repository.indexLinkedTree(Uri.parse("content://com.example/tree")) { _, _ -> }
        repository.indexLinkedTree(Uri.parse("content://com.example/tree")) { _, _ -> }

        // Second pass hits the fast path: same modified + cover on disk.
        assertEquals(1, inspects)
    }

    @Test
    fun indexLinkedTreePrunesRemovedDocumentsAndCovers() = runTest {
        registerDoc("alpha.cbz")
        registerDoc("beta.cbz")
        val lister = FakeLinkedTreeLister(
            LinkedTreeListResult(
                listOf(linkedDoc("alpha.cbz"), linkedDoc("beta.cbz")),
                walkFailed = false,
            ),
        )
        val repository = repository(backendFor(), lister)
        repository.indexLinkedTree(Uri.parse("content://com.example/tree")) { _, _ -> }
        val betaCover = repository.getComic(docUri("beta.cbz").toString())?.coverPath
        assertTrue(betaCover != null)

        lister.result = LinkedTreeListResult(listOf(linkedDoc("alpha.cbz")), walkFailed = false)
        repository.indexLinkedTree(Uri.parse("content://com.example/tree")) { _, _ -> }

        repository.observeLibrary(LibraryQuery()).test {
            val comics = awaitItem()
            assertEquals(listOf(docUri("alpha.cbz").toString()), comics.map { it.id })
        }
        assertTrue(betaCover?.let { File(it).exists() } == false)
    }

    @Test
    fun indexLinkedTreeSkipsPruningOnWalkFailure() = runTest {
        registerDoc("alpha.cbz")
        val lister = FakeLinkedTreeLister(
            LinkedTreeListResult(listOf(linkedDoc("alpha.cbz")), walkFailed = false),
        )
        val repository = repository(backendFor(), lister)
        repository.indexLinkedTree(Uri.parse("content://com.example/tree")) { _, _ -> }

        // A failed walk (e.g. revoked permission) must never read as an
        // empty folder: rows the user still owns survive.
        lister.result = LinkedTreeListResult(emptyList(), walkFailed = true)
        val report = repository.indexLinkedTree(Uri.parse("content://com.example/tree")) { _, _ -> }

        assertEquals(0, report.total)
        repository.observeLibrary(LibraryQuery()).test {
            assertEquals(1, awaitItem().size)
        }
    }

    @Test
    fun linkedCorruptArchivesBecomeErrorRows() = runTest {
        registerDoc("broken.cbz")
        val backend = FakeComicBackendDataSource(
            failWith = com.mori.comic.CorruptArchiveException("bad"),
        )
        val lister = FakeLinkedTreeLister(
            LinkedTreeListResult(listOf(linkedDoc("broken.cbz")), walkFailed = false),
        )
        val repository = repository(backend, lister)
        val report = repository.indexLinkedTree(Uri.parse("content://com.example/tree")) { _, _ -> }

        assertEquals(0, report.succeeded)
        assertEquals(1, report.failed)
        repository.observeLibrary(LibraryQuery()).test {
            val comics = awaitItem()
            assertEquals(ComicError.CORRUPT, comics.single().error)
        }
    }

    @Test
    fun removeComicUnlinksWithoutTouchingOriginal() = runTest {
        registerDoc("alpha.cbz")
        val lister = FakeLinkedTreeLister(
            LinkedTreeListResult(listOf(linkedDoc("alpha.cbz")), walkFailed = false),
        )
        val repository = repository(backendFor(), lister)
        repository.indexLinkedTree(Uri.parse("content://com.example/tree")) { _, _ -> }
        val cover = repository.getComic(docUri("alpha.cbz").toString())?.coverPath
        assertTrue(cover != null)

        repository.removeComic(docUri("alpha.cbz").toString())

        assertNull(repository.getComic(docUri("alpha.cbz").toString()))
        assertTrue(cover?.let { File(it).exists() } == false)
    }

    @Test
    fun saveProgressClampsToPageRange() = runTest {
        dao.upsert(
            com.mori.core.database.ComicEntity(
                id = "content://com.example/tree/alpha.cbz",
                title = "Alpha",
                series = null,
                number = null,
                format = "CBZ",
                pageCount = 1,
                sourcePath = "content://com.example/tree/alpha.cbz",
                coverPath = null,
                lastPageIndex = 0,
                sourceDisplayName = "alpha.cbz",
                sourceModified = 1000L,
                error = null,
                createdAt = 1L,
                updatedAt = 2L,
            ),
        )
        val repository = repository(FakeComicBackendDataSource())

        repository.saveProgress("content://com.example/tree/alpha.cbz", 99)

        // Single-page comic clamps to index 0.
        assertEquals(0, repository.getComic("content://com.example/tree/alpha.cbz")?.lastPageIndex)
    }

    @Test
    fun toggleBookmarkFlipsPersistedFlag() = runTest {
        dao.upsert(
            com.mori.core.database.ComicEntity(
                id = "content://com.example/tree/alpha.cbz",
                title = "Alpha",
                series = null,
                number = null,
                format = "CBZ",
                pageCount = 10,
                sourcePath = "content://com.example/tree/alpha.cbz",
                coverPath = null,
                lastPageIndex = 0,
                sourceDisplayName = "alpha.cbz",
                sourceModified = 1000L,
                error = null,
                createdAt = 1L,
                updatedAt = 2L,
            ),
        )
        val repository = repository(FakeComicBackendDataSource())

        assertEquals(false, repository.getComic("content://com.example/tree/alpha.cbz")?.bookmarked)
        repository.toggleBookmark("content://com.example/tree/alpha.cbz")
        assertEquals(true, repository.getComic("content://com.example/tree/alpha.cbz")?.bookmarked)
        repository.toggleBookmark("content://com.example/tree/alpha.cbz")
        assertEquals(false, repository.getComic("content://com.example/tree/alpha.cbz")?.bookmarked)
    }

    @Test
    fun clearThumbnailCacheDeletesCoversAndNullsReferences() = runTest {
        registerDoc("alpha.cbz")
        val lister = FakeLinkedTreeLister(
            LinkedTreeListResult(listOf(linkedDoc("alpha.cbz")), walkFailed = false),
        )
        val repository = repository(backendFor(), lister)
        repository.indexLinkedTree(Uri.parse("content://com.example/tree")) { _, _ -> }
        val cover = repository.getComic(docUri("alpha.cbz").toString())?.coverPath
        assertTrue(cover != null && File(cover).isFile)

        repository.clearThumbnailCache()

        assertTrue(File(cover!!).exists().not())
        assertNull(repository.getComic(docUri("alpha.cbz").toString())?.coverPath)
    }

    @Test
    fun storageUsageCountsCoversAndCache() = runTest {
        registerDoc("alpha.cbz")
        val lister = FakeLinkedTreeLister(
            LinkedTreeListResult(listOf(linkedDoc("alpha.cbz")), walkFailed = false),
        )
        val repository = repository(backendFor(), lister)
        repository.indexLinkedTree(Uri.parse("content://com.example/tree")) { _, _ -> }
        val coverBytes = repository.getComic(docUri("alpha.cbz").toString())?.coverPath
            ?.let { File(it).length() } ?: 0L

        val usage = repository.storageUsage()

        // No app-private library exists: bytes come from covers plus the
        // transient read cache holding the materialized archive.
        assertEquals(1, usage.comicCount)
        assertEquals(0L, usage.libraryBytes)
        assertTrue(usage.coversBytes >= coverBytes)
    }

    @Test
    fun refreshComicReturnsNullForUnknownId() = runTest {
        val backend = FakeComicBackendDataSource()
        assertNull(repository(backend).refreshComic("nope"))
    }

    @Test
    fun refreshComicReindexesChangedDocument() = runTest {
        registerDoc("alpha.cbz")
        val lister = FakeLinkedTreeLister(
            LinkedTreeListResult(listOf(linkedDoc("alpha.cbz", modified = 1000L)), walkFailed = false),
        )
        val repository = repository(backendFor(), lister)
        repository.indexLinkedTree(Uri.parse("content://com.example/tree")) { _, _ -> }

        lister.result = LinkedTreeListResult(
            listOf(linkedDoc("alpha.cbz", modified = 2000L)),
            walkFailed = false,
        )
        val updated = repository.refreshComic(docUri("alpha.cbz").toString())

        assertEquals(2000L, updated?.let { dao.getById(it.id)?.sourceModified })
    }

    @Test
    fun refreshComicStampsTypedErrorInsteadOfNull() = runTest {
        registerDoc("alpha.cbz")
        val lister = FakeLinkedTreeLister(
            LinkedTreeListResult(listOf(linkedDoc("alpha.cbz", modified = 1000L)), walkFailed = false),
        )
        val backend = backendFor()
        val repository = repository(backend, lister)
        repository.indexLinkedTree(Uri.parse("content://com.example/tree")) { _, _ -> }
        val id = docUri("alpha.cbz").toString()

        // Corrupt on re-read with a bumped modified (skips the fast path).
        backend.failWith = com.mori.comic.CorruptArchiveException("torn")
        lister.result = LinkedTreeListResult(
            listOf(linkedDoc("alpha.cbz", modified = 2000L)),
            walkFailed = false,
        )
        val failed = repository.refreshComic(id)

        // Typed failure surfaces; null stays reserved for vanished docs.
        assertEquals(com.mori.core.model.ComicError.CORRUPT, failed?.error)
        assertEquals(ComicError.CORRUPT.name, dao.getById(id)?.error)
    }
}

/** Scripted tree lister: no SAF provider needed. */
internal class FakeLinkedTreeLister(
    var result: LinkedTreeListResult = LinkedTreeListResult(emptyList(), walkFailed = false),
) : LinkedTreeLister {
    override suspend fun listArchives(treeUri: Uri): LinkedTreeListResult = result

    override suspend fun resolve(documentUri: Uri): LinkedDocument? =
        result.documents.firstOrNull { it.uri == documentUri }
}
