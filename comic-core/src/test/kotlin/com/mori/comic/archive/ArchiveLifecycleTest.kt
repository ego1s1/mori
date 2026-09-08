package com.mori.comic.archive

import com.mori.comic.ArchiveClosedException
import com.mori.comic.ComicArchive
import com.mori.comic.ComicFactory
import com.mori.comic.ComicSource
import com.mori.comic.testutil.Archives
import com.mori.comic.testutil.Fixtures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveLifecycleTest {

    private fun openAll(): List<Pair<String, ComicArchive>> {
        val dir = Archives.newTempDir()
        val cbz = Archives.writeCbz(
            dir,
            "a.cbz",
            mapOf(
                "001.jpg" to Archives.PNG_1X1,
                "002.jpg" to Archives.PNG_1X1,
                "003.jpg" to Archives.PNG_1X1,
            ),
        )
        val folder = Archives.writeDir(dir, "folder")
        Archives.writeFile(folder, "001.jpg", Archives.PNG_1X1)
        Archives.writeFile(folder, "002.jpg", Archives.PNG_1X1)
        return listOf(
            "cbz" to ComicFactory.open(ComicSource.File(cbz)),
            "cbr" to ComicFactory.open(ComicSource.File(Fixtures.sortedCbr())),
            "folder" to ComicFactory.open(ComicSource.Directory(folder)),
        )
    }

    @Test
    fun doubleCloseIsSafeForEveryBackend() {
        openAll().forEach { (name, archive) ->
            archive.close()
            archive.close()
            assertThrows("read after close must fail for $name", ArchiveClosedException::class.java) {
                runBlocking { archive.readPage(archive.pages[0]) }
            }
        }
    }

    @Test
    fun useBlockClosesArchive() {
        val dir = Archives.newTempDir()
        val file = Archives.writeCbz(dir, "a.cbz", mapOf("1.jpg" to Archives.PNG_1X1))
        val archive = ComicFactory.open(ComicSource.File(file))
        archive.use { assertEquals(1, it.pageCount) }
        assertThrows(ArchiveClosedException::class.java) {
            runBlocking { archive.readPage(archive.pages[0]) }
        }
    }

    @Test
    fun concurrentReadsReturnCorrectBytesForEveryBackend() = runTest {
        openAll().forEach { (name, archive) ->
            archive.use { open ->
                val expected = open.pages.associate { page ->
                    page.index to runBlocking { open.readPage(page).toList() }
                }
                val results = (1..16).map { i ->
                    async(Dispatchers.IO) {
                        val page = open.pages[i % open.pageCount]
                        page.index to runBlocking { open.readPage(page).toList() }
                    }
                }.awaitAll()
                results.forEach { (index, bytes) ->
                    assertEquals("concurrent read mismatch on $name page $index", expected[index], bytes)
                }
            }
        }
    }

    @Test
    fun cancelledScopeDoesNotHangArchive() = runTest {
        val dir = Archives.newTempDir()
        val file = Archives.writeCbz(
            dir,
            "a.cbz",
            (1..20).associate { "p%03d.jpg".format(it) to Archives.PNG_1X1 },
        )
        ComicFactory.open(ComicSource.File(file)).use { archive ->
            val jobs = archive.pages.map { page ->
                async(Dispatchers.IO) { archive.readPage(page) }
            }
            jobs.forEach { it.cancel() }
            jobs.forEach {
                runCatching { it.await() }
            }
            // Archive must still be usable after mass cancellation.
            assertTrue(archive.readPage(archive.pages[0]).isNotEmpty())
        }
    }
}
