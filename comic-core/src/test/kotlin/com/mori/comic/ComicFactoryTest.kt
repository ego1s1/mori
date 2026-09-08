package com.mori.comic

import com.mori.comic.model.MediaType
import com.mori.comic.testutil.Archives
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ComicFactoryTest {

    @Test
    fun opensCbz() {
        val dir = Archives.newTempDir()
        val file = Archives.writeCbz(dir, "a.cbz", mapOf("1.jpg" to Archives.PNG_1X1))
        ComicFactory.open(ComicSource.File(file)).use { archive ->
            assertEquals(1, archive.pageCount)
        }
    }

    @Test
    fun opensDirectory() {
        val root = Archives.newTempDir()
        Archives.writeFile(root, "1.jpg", Archives.PNG_1X1)
        ComicFactory.open(ComicSource.Directory(root)).use { archive ->
            assertEquals(1, archive.pageCount)
        }
    }

    @Test
    fun missingFileThrowsCorrupt() {
        val file = File(Archives.newTempDir(), "missing.cbz")
        assertThrows(CorruptArchiveException::class.java) {
            ComicFactory.open(ComicSource.File(file))
        }
    }

    @Test
    fun missingDirectoryThrowsCorrupt() {
        val dir = File(Archives.newTempDir(), "nope")
        assertThrows(CorruptArchiveException::class.java) {
            ComicFactory.open(ComicSource.Directory(dir))
        }
    }

    @Test
    fun fileAsDirectoryThrowsCorrupt() {
        val root = Archives.newTempDir()
        val file = Archives.writeFile(root, "notadir", byteArrayOf(1))
        assertThrows(CorruptArchiveException::class.java) {
            ComicFactory.open(ComicSource.Directory(file))
        }
    }

    @Test
    fun unsupportedFileThrows() {
        val file = Archives.writeFile(Archives.newTempDir(), "comic.pdf", "%PDF-1.4".toByteArray())
        assertThrows(UnsupportedFormatException::class.java) {
            ComicFactory.open(ComicSource.File(file))
        }
    }

    @Test
    fun pageCountConvenienceAccessor() {
        val dir = Archives.newTempDir()
        val file = Archives.writeCbz(
            dir,
            "a.cbz",
            mapOf("1.jpg" to Archives.PNG_1X1, "2.jpg" to Archives.PNG_1X1, "3.jpg" to Archives.PNG_1X1),
        )
        ComicFactory.open(ComicSource.File(file)).use { archive ->
            assertEquals(3, archive.pageCount)
            assertEquals(MediaType.JPEG, archive.pages.last().mediaType)
            assertTrue(archive.pages[1].sizeBytes > 0)
        }
    }
}
