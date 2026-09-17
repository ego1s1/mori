package com.mori.feature.history.impl

import app.cash.turbine.test
import com.mori.core.model.Comic
import com.mori.core.model.ComicFormat
import com.mori.core.testing.FakeComicsRepository
import com.mori.core.testing.TestDispatcherRule
import com.mori.core.testing.awaitWhere
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HistoryViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private fun comic(id: String, lastPageIndex: Int, updatedAt: Long) = Comic(
        id = id,
        title = "Title $id",
        series = null,
        number = null,
        format = ComicFormat.CBZ,
        pageCount = 10,
        sourcePath = "/lib/$id.cbz",
        coverPath = null,
        lastPageIndex = lastPageIndex,
        sourceDisplayName = "$id.cbz",
        createdAt = 1L,
        updatedAt = updatedAt,
    )

    private fun viewModel(comics: List<Comic> = emptyList()) = HistoryViewModel(
        FakeComicsRepository(comics.associateBy { it.id }),
    )

    @Test
    fun untouchedLibraryIsEmptyHistory() = runTest {
        val viewModel = viewModel(
            listOf(comic("a", lastPageIndex = 0, updatedAt = 1_000L)),
        )
        viewModel.uiState.test {
            val state = awaitWhere { it is HistoryUiState.Success } as HistoryUiState.Success
            assertTrue(state.isEmpty)
        }
    }

    @Test
    fun touchedComicsGroupByRecency() = runTest {
        val now = System.currentTimeMillis()
        val viewModel = viewModel(
            listOf(
                comic("old", lastPageIndex = 2, updatedAt = now - 90_000_000L),
                comic("new", lastPageIndex = 4, updatedAt = now - 1_000L),
                comic("fresh", lastPageIndex = 0, updatedAt = now),
            ),
        )
        viewModel.uiState.test {
            val state = awaitWhere {
                (it as? HistoryUiState.Success)?.days?.isNotEmpty() == true
            } as HistoryUiState.Success
            val ids = state.days.flatMap { day -> day.comics.map { it.id } }
            assertEquals(listOf("new", "old"), ids)
        }
    }

    @Test
    fun searchNarrowsByTitle() = runTest {
        val now = System.currentTimeMillis()
        val viewModel = viewModel(
            listOf(
                comic("a", lastPageIndex = 2, updatedAt = now).copy(title = "Apple Saga"),
                comic("b", lastPageIndex = 3, updatedAt = now - 1_000L).copy(title = "Banana Tale"),
            ),
        )
        viewModel.uiState.test {
            awaitWhere { (it as? HistoryUiState.Success)?.days?.isNotEmpty() == true }
            viewModel.onAction(HistoryAction.SearchTextChanged("app"))
            val filtered = awaitWhere {
                val success = it as? HistoryUiState.Success
                success != null && success.queryText == "app" &&
                    success.days.flatMap { day -> day.comics }.size == 1
            } as HistoryUiState.Success
            assertEquals("a", filtered.days.single().comics.single().id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
