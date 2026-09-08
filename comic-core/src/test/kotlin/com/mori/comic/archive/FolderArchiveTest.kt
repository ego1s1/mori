package com.mori.comic.archive

import com.mori.comic.ArchiveClosedException
import com.mori.comic.ComicFactory
import com.mori.comic.ComicSource
import com.mori.comic.EmptyArchiveException
import com.mori.comic.testutil.Archives
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderArchiveTest {

    @Test
    fun listsNaturallySortedFilesRecursively() {
        val root = Archives.newTempDir()
        Archives.writeFile(root, "sub/2.png", Archives.PNG_1X1)
        Archives.writeFile(root, "1.png", Archives.PNG_1X1)
        Archives.writeFile(root, "sub/10.png", Archives.PNG_1X1)
        Archives.writeFile(root, "notes.txt", "x".toByteArray())

        ComicFactory.open(ComicSource.Directory(root)).use { archive ->
            assertEquals(3, archive.pageCount)
            assertEquals(listOf("1.png", "sub/2.png", "sub/10.png"), archive.pages.map { it.name })
        }
    }

    @Test
    fun readsPageBytes() = runTest {
        val root = Archives.newTempDir()
        val content = "page-content".toByteArray()
        Archives.writeFile(root, "1.png", content)

        ComicFactory.open(ComicSource.Directory(root)).use { archive ->
            assertEquals(content.toList(), archive.readPage(archive.pages[0]).toList())
        }
    }

    @Test
    fun parsesComicInfoAtRoot() {
        val root = Archives.newTempDir()
        Archives.writeFile(root, "page.png", Archives.PNG_1X1)
        Archives.writeFile(root, "ComicInfo.xml", Archives.sampleComicInfo)

        ComicFactory.open(ComicSource.Directory(root)).use { archive ->
            assertEquals("Test Comic", archive.metadata.title)
        }
    }

    @Test
    fun emptyDirectoryThrows() {
        val root = Archives.newTempDir()
        assertThrows(EmptyArchiveException::class.java) {
            ComicFactory.open(ComicSource.Directory(root))
        }
    }

    @Test
    fun hiddenFilesAreSkipped() {
        val root = Archives.newTempDir()
        Archives.writeFile(root, ".hidden.png", Archives.PNG_1X1)
        Archives.writeFile(root, "real.png", Archives.PNG_1X1)

        ComicFactory.open(ComicSource.Directory(root)).use { archive ->
            assertEquals(1, archive.pageCount)
            assertEquals("real.png", archive.pages[0].name)
        }
    }

    @Test
    fun readsAfterCloseThrows() {
        val root = Archives.newTempDir()
        Archives.writeFile(root, "1.png", Archives.PNG_1X1)
        val archive = ComicFactory.open(ComicSource.Directory(root))
        archive.close()
        assertThrows(ArchiveClosedException::class.java) {
            runBlocking { archive.readPage(archive.pages[0]) }
        }
    }
}
