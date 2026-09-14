package com.mori.comic.archive

import com.mori.comic.ComicArchive
import com.mori.comic.ComicFactory
import com.mori.comic.ComicSource
import com.mori.comic.CorruptArchiveException
import com.mori.comic.EmptyArchiveException
import com.mori.comic.PasswordRequiredException
import com.mori.comic.model.MediaType
import com.mori.comic.testutil.Archives
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Cb7ArchiveTest {

    private fun openCb7(entries: Map<String, ByteArray>): ComicArchive {
        val dir = Archives.newTempDir()
        val file = Archives.writeCb7(dir, "comic.cb7", entries)
        return ComicFactory.open(ComicSource.File(file))
    }

    @Test
    fun listsNaturallySortedPages() {
        openCb7(
            mapOf(
                "page10.jpg" to Archives.PNG_1X1,
                "page2.jpg" to Archives.PNG_1X1,
                "page1.jpg" to Archives.PNG_1X1,
            ),
        ).use { archive ->
            assertEquals(listOf("page1.jpg", "page2.jpg", "page10.jpg"), archive.pages.map { it.name })
            assertEquals(3, archive.pageCount)
            assertEquals(listOf(0, 1, 2), archive.pages.map { it.index })
            assertEquals(MediaType.JPEG, archive.pages[0].mediaType)
        }
    }

    @Test
    fun readsPageBytesByIndex() = runTest {
        val content = Archives.PNG_1X1
        openCb7(mapOf("a.jpg" to content)).use { archive ->
            assertTrue(archive.readPage(archive.pages[0]).contentEquals(content))
        }
    }

    @Test
    fun findsPagesInSubdirectories() {
        openCb7(mapOf("ch1/page1.jpg" to Archives.PNG_1X1)).use { archive ->
            assertEquals(listOf("ch1/page1.jpg"), archive.pages.map { it.name })
        }
    }

    @Test
    fun readsComicInfoMetadata() {
        val dir = Archives.newTempDir()
        val file = Archives.writeCb7(
            dir,
            "comic.cb7",
            mapOf("page1.jpg" to Archives.PNG_1X1, "ComicInfo.xml" to Archives.sampleComicInfo),
        )
        ComicFactory.open(ComicSource.File(file)).use { archive ->
            assertEquals("Test Comic", archive.metadata.title)
            assertEquals("Sample Series", archive.metadata.series)
        }
    }

    @Test
    fun emptyArchiveThrows() {
        val dir = Archives.newTempDir()
        val file = Archives.writeCb7(dir, "empty.cb7", mapOf("note.txt" to "no pictures".toByteArray()))
        assertThrows(EmptyArchiveException::class.java) {
            ComicFactory.open(ComicSource.File(file))
        }
    }

    @Test
    fun corruptFileThrows() {
        val file = Archives.writeFile(Archives.newTempDir(), "broken.cb7", "not a 7z file".toByteArray())
        assertThrows(CorruptArchiveException::class.java) {
            ComicFactory.open(ComicSource.File(file))
        }
    }

    @Test
    fun encryptedArchiveThrowsPasswordRequired() {
        val dir = Archives.newTempDir()
        val file = Archives.writeCb7(
            dir,
            "locked.cb7",
            mapOf("page1.jpg" to Archives.PNG_1X1),
            password = "secret".toCharArray(),
        )
        // Data-only encryption lists headers fine; the password gate trips on read.
        ComicFactory.open(ComicSource.File(file)).use { archive ->
            assertThrows(PasswordRequiredException::class.java) {
                runBlocking { archive.readPage(archive.pages[0]) }
            }
        }
    }

    @Test
    fun missingFileThrows() {
        assertThrows(CorruptArchiveException::class.java) {
            ComicFactory.open(ComicSource.File(File(Archives.newTempDir(), "ghost.cb7")))
        }
    }
}
