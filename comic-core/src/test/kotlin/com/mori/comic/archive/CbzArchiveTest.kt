package com.mori.comic.archive

import com.mori.comic.ArchiveClosedException
import com.mori.comic.ComicArchive
import com.mori.comic.ComicFactory
import com.mori.comic.ComicSource
import com.mori.comic.CorruptArchiveException
import com.mori.comic.EmptyArchiveException
import com.mori.comic.model.MediaType
import com.mori.comic.testutil.Archives
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CbzArchiveTest {

    private fun openCbz(entries: Map<String, ByteArray>): ComicArchive {
        val dir = Archives.newTempDir()
        val file = Archives.writeCbz(dir, "comic.cbz", entries)
        return ComicFactory.open(ComicSource.File(file))
    }

    @Test
    fun listsNaturallySortedPages() {
        openCbz(
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
    fun discoversPagesInSubdirectories() {
        openCbz(
            mapOf(
                "chapter1/001.jpg" to Archives.PNG_1X1,
                "chapter2/001.jpg" to Archives.PNG_1X1,
            ),
        ).use { archive ->
            assertEquals(2, archive.pageCount)
            assertTrue(archive.pages.all { it.name.contains('/') })
        }
    }

    @Test
    fun skipsNonImageAndHiddenEntries() {
        openCbz(
            mapOf(
                "001.jpg" to Archives.PNG_1X1,
                "notes.txt" to "hello".toByteArray(),
                "ComicInfo.xml" to Archives.sampleComicInfo,
                "readme.nfo" to "nfo".toByteArray(),
                ".DS_Store" to byteArrayOf(1, 2, 3),
                "__MACOSX/._001.jpg" to byteArrayOf(1),
            ),
        ).use { archive ->
            assertEquals(1, archive.pageCount)
            assertEquals("001.jpg", archive.pages[0].name)
        }
    }

    @Test
    fun emptyArchiveThrows() {
        assertThrows(EmptyArchiveException::class.java) { openCbz(emptyMap()) }
    }

    @Test
    fun archiveWithOnlyNonImagesThrows() {
        assertThrows(EmptyArchiveException::class.java) {
            openCbz(mapOf("readme.txt" to "hi".toByteArray()))
        }
    }

    @Test
    fun readsPageBytesVerbatim() = runTest {
        val content = "image-bytes-for-page-seven".toByteArray()
        openCbz(mapOf("007.png" to content)).use { archive ->
            val page = archive.pages.single()
            assertEquals(content.size.toLong(), page.sizeBytes)
            assertEquals(content.toList(), archive.readPage(page).toList())
        }
    }

    @Test
    fun readsAfterCloseThrows() {
        val archive = openCbz(mapOf("1.jpg" to Archives.PNG_1X1))
        archive.close()
        assertThrows(ArchiveClosedException::class.java) {
            runBlocking { archive.readPage(archive.pages[0]) }
        }
    }

    @Test
    fun closeIsIdempotent() {
        openCbz(mapOf("1.jpg" to Archives.PNG_1X1)).use { archive ->
            archive.close()
            archive.close()
        }
    }

    @Test
    fun metadataParsesComicInfo() {
        openCbz(mapOf("ComicInfo.xml" to Archives.sampleComicInfo, "1.jpg" to Archives.PNG_1X1)).use { archive ->
            val meta = archive.metadata
            assertEquals("Test Comic", meta.title)
            assertEquals("Sample Series", meta.series)
            assertEquals("42", meta.number)
            assertEquals(1999, meta.volume)
            assertEquals(2001, meta.year)
            assertEquals(3, meta.pageCount)
            assertEquals("en", meta.language)
            assertEquals(false, meta.manga)
            assertEquals("Mori Press", meta.publisher)
        }
    }

    @Test
    fun metadataEmptyWhenNoComicInfo() {
        openCbz(mapOf("1.jpg" to Archives.PNG_1X1)).use { archive ->
            assertEquals(com.mori.comic.model.ComicMetadata(), archive.metadata)
        }
    }

    @Test
    fun corruptZipThrowsCorruptArchive() {
        val dir = Archives.newTempDir()
        val file = File(dir, "corrupt.cbz").apply { writeText("this is not a zip") }
        assertThrows(CorruptArchiveException::class.java) {
            ComicFactory.open(ComicSource.File(file))
        }
    }
}
