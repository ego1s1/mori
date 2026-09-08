package com.mori.comic.archive

import com.mori.comic.ArchiveClosedException
import com.mori.comic.ComicFactory
import com.mori.comic.ComicSource
import com.mori.comic.EmptyArchiveException
import com.mori.comic.PasswordRequiredException
import com.mori.comic.testutil.Fixtures
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CbrArchiveTest {

    @Test
    fun listsSortedPages() {
        ComicFactory.open(ComicSource.File(Fixtures.sortedCbr())).use { archive ->
            assertEquals(3, archive.pageCount)
            assertEquals(listOf("001.jpg", "002.jpg", "003.jpg"), archive.pages.map { it.name })
            assertEquals(listOf(0, 1, 2), archive.pages.map { it.index })
        }
    }

    @Test
    fun readsPageBytes() = runTest {
        ComicFactory.open(ComicSource.File(Fixtures.sortedCbr())).use { archive ->
            val bytes = archive.readPage(archive.pages[1])
            assertTrue(bytes.isNotEmpty())
        }
    }

    @Test
    fun readsEveryPageOfSolidArchive() = runTest {
        ComicFactory.open(ComicSource.File(Fixtures.solidCbr())).use { archive ->
            assertEquals(3, archive.pageCount)
            val first = archive.readPage(archive.pages[0])
            assertEquals(first.toList(), archive.readPage(archive.pages[0]).toList())
            assertTrue(archive.readPage(archive.pages[2]).isNotEmpty())
        }
    }

    @Test
    fun encryptedArchiveThrowsPasswordRequired() {
        assertThrows(PasswordRequiredException::class.java) {
            ComicFactory.open(ComicSource.File(Fixtures.lockedCbr()))
        }
    }

    @Test
    fun readsAfterCloseThrows() {
        val archive = ComicFactory.open(ComicSource.File(Fixtures.sortedCbr()))
        archive.close()
        assertThrows(ArchiveClosedException::class.java) {
            runBlocking { archive.readPage(archive.pages[0]) }
        }
    }
}
