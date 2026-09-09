package com.mori.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComicDaoTest {

    private lateinit var database: MoriDatabase
    private lateinit var dao: ComicDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MoriDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.comicDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun entity(id: String) = ComicEntity(
        id = id,
        title = "Title $id",
        series = null,
        number = null,
        format = "CBZ",
        pageCount = 10,
        sourcePath = "/lib/$id.cbz",
        coverPath = null,
        lastPageIndex = 0,
        sourceDisplayName = "$id.cbz",
        sourceModified = 1L,
        error = null,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun upsertAndObserveAll() = runTest {
        dao.upsertAll(listOf(entity("a"), entity("b")))
        dao.observeAll().test {
            val all = awaitItem()
            assertEquals(2, all.size)
            assertEquals(setOf("a", "b"), all.map { it.id }.toSet())
        }
    }

    @Test
    fun upsertReplacesRow() = runTest {
        dao.upsert(entity("a"))
        dao.upsert(entity("a").copy(title = "Renamed", lastPageIndex = 4))
        assertEquals("Renamed", dao.getById("a")?.title)
        assertEquals(4, dao.getById("a")?.lastPageIndex)
    }

    @Test
    fun observeByIdEmitsNullForMissing() = runTest {
        dao.observeById("missing").test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun updateProgressChangesIndexAndTimestamp() = runTest {
        dao.upsert(entity("a"))
        dao.updateProgress("a", 7, 99L)
        val row = dao.getById("a")
        assertEquals(7, row?.lastPageIndex)
        assertEquals(99L, row?.updatedAt)
    }

    @Test
    fun deleteByIdRemovesRow() = runTest {
        dao.upsertAll(listOf(entity("a"), entity("b")))
        dao.deleteById("a")
        assertNull(dao.getById("a"))
        dao.observeAll().test {
            assertEquals(listOf("b"), awaitItem().map { it.id })
        }
    }

    @Test
    fun deleteMissingKeepsListedIds() = runTest {
        dao.upsertAll(listOf(entity("a"), entity("b"), entity("c")))
        dao.deleteMissing(listOf("a", "c"))
        dao.observeAll().test {
            assertEquals(setOf("a", "c"), awaitItem().map { it.id }.toSet())
        }
    }
}
