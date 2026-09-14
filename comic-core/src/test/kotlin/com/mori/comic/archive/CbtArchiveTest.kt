package com.mori.comic.archive

import com.mori.comic.ComicArchive
import com.mori.comic.ComicFactory
import com.mori.comic.ComicSource
import com.mori.comic.CorruptArchiveException
import com.mori.comic.EmptyArchiveException
import com.mori.comic.model.MediaType
import com.mori.comic.testutil.Archives
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CbtArchiveTest {

    private fun openCbt(entries: Map<String, ByteArray>): ComicArchive {
        val dir = Archives.newTempDir()
        val file = Archives.writeCbt(dir, "comic.cbt", entries)
        return ComicFactory.open(ComicSource.File(file))
    }

    @Test
    fun listsNaturallySortedPages() {
        openCbt(
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
        openCbt(mapOf("a.jpg" to content)).use { archive ->
            assertTrue(archive.readPage(archive.pages[0]).contentEquals(content))
        }
    }

    @Test
    fun findsPagesInSubdirectories() {
        openCbt(mapOf("ch1/page1.jpg" to Archives.PNG_1X1)).use { archive ->
            assertEquals(listOf("ch1/page1.jpg"), archive.pages.map { it.name })
        }
    }

    @Test
    fun readsComicInfoMetadata() {
        openCbt(
            mapOf("page1.jpg" to Archives.PNG_1X1, "ComicInfo.xml" to Archives.sampleComicInfo),
        ).use { archive ->
            assertEquals("Test Comic", archive.metadata.title)
            assertEquals("Sample Series", archive.metadata.series)
        }
    }

    @Test
    fun emptyArchiveThrows() {
        val dir = Archives.newTempDir()
        val file = Archives.writeCbt(dir, "empty.cbt", mapOf("note.txt" to "no pictures".toByteArray()))
        assertThrows(EmptyArchiveException::class.java) {
            ComicFactory.open(ComicSource.File(file))
        }
    }

    @Test
    fun corruptFileThrows() {
        // Truncated tar header: block too short to be a valid archive.
        val file = Archives.writeFile(Archives.newTempDir(), "broken.cbt", "not a tar file".toByteArray())
        assertThrows(CorruptArchiveException::class.java) {
            ComicFactory.open(ComicSource.File(file))
        }
    }

    @Test
    fun missingFileThrows() {
        assertThrows(CorruptArchiveException::class.java) {
            ComicFactory.open(ComicSource.File(File(Archives.newTempDir(), "ghost.cbt")))
        }
    }
}
