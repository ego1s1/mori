package com.mori.feature.library.impl

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.mori.core.model.LibraryDisplay
import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibrarySortOrder
import com.mori.core.testing.TestDispatcherRule
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
class LibraryViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private fun viewModel(
        repository: TestComicsRepository = TestComicsRepository(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        preferences: TestPreferencesDataSource = TestPreferencesDataSource(),
    ) = LibraryViewModel(savedStateHandle, repository, preferences)

    @Test
    fun emitsComicsFromRepository() = runTest {
        val repository = TestComicsRepository(
            listOf(
                TestComicsRepository.comic("b", title = "Banana"),
                TestComicsRepository.comic("a", title = "Apple"),
            ),
        )
        viewModel(repository).uiState.test {
            val success = awaitSuccess()
            assertEquals(2, success.comics.size)
        }
    }

    @Test
    fun searchNarrowsResults() = runTest {
        val repository = TestComicsRepository(
            listOf(
                TestComicsRepository.comic("b", title = "Banana"),
                TestComicsRepository.comic("a", title = "Apple"),
            ),
        )
        val viewModel = viewModel(repository)
        viewModel.uiState.test {
            awaitSuccess()
            viewModel.onAction(LibraryAction.SearchTextChanged("app"))
            val filtered = awaitSuccessWhere { it.comics.map { comic -> comic.id } == listOf("a") }
            assertEquals(listOf("a"), filtered.comics.map { it.id })
        }
    }

    @Test
    fun sortAndFilterUpdateQuery() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitSuccess()
            viewModel.onAction(LibraryAction.SortSelected(LibrarySortOrder.TITLE))
            viewModel.onAction(LibraryAction.FilterSelected(LibraryFilter.FINISHED))
            viewModel.onAction(LibraryAction.ToggleHideErrors(true))
            val state = awaitSuccessWhere {
                it.query.sortOrder == LibrarySortOrder.TITLE &&
                    it.query.filter == LibraryFilter.FINISHED &&
                    it.query.hideErrors
            }
            assertEquals(LibrarySortOrder.TITLE, state.query.sortOrder)
            assertEquals(LibraryFilter.FINISHED, state.query.filter)
            assertTrue(state.query.hideErrors)
        }
    }

    @Test
    fun refreshFailureMessageIsOneShot() = runTest {
        val repository = TestComicsRepository()
        repository.refreshReport = com.mori.core.model.IndexReport(1, 2, 0)
        val viewModel = viewModel(repository)
        viewModel.messages.test {
            viewModel.onAction(LibraryAction.Refresh)
            assertEquals(
                "Couldn't index 2 file(s). Check the files and rescan.",
                awaitItem(),
            )
        }
        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun refreshExceptionMessageIsOneShot() = runTest {
        val repository = TestComicsRepository()
        repository.failRefreshWith = IllegalStateException("disk gone")
        val viewModel = viewModel(repository)
        viewModel.messages.test {
            viewModel.onAction(LibraryAction.Refresh)
            assertEquals("Rescan failed. Try again.", awaitItem())
        }
    }

    @Test
    fun queryRestoresFromSavedState() = runTest {
        val handle = SavedStateHandle(
            mapOf(
                "mori_query_text" to "app",
            ),
        )
        val preferences = TestPreferencesDataSource(
            initialDisplay = LibraryDisplay(
                sortOrder = LibrarySortOrder.TITLE,
                filter = LibraryFilter.FINISHED,
                hideErrors = true,
            ),
        )
        val viewModel = viewModel(savedStateHandle = handle, preferences = preferences)
        viewModel.uiState.test {
            val state = awaitSuccess()
            assertEquals("app", state.query.text)
            assertEquals(LibrarySortOrder.TITLE, state.query.sortOrder)
            assertEquals(LibraryFilter.FINISHED, state.query.filter)
            assertTrue(state.query.hideErrors)
        }
    }

    @Test
    fun displayActionsPersistAcrossViewModels() = runTest {
        val preferences = TestPreferencesDataSource()
        val first = viewModel(preferences = preferences)
        first.uiState.test {
            awaitSuccess()
            first.onAction(LibraryAction.SortSelected(LibrarySortOrder.TITLE))
            awaitSuccessWhere { it.query.sortOrder == LibrarySortOrder.TITLE }
            cancelAndIgnoreRemainingEvents()
        }
        // A fresh ViewModel (full restart) restores the persisted display.
        val second = viewModel(preferences = preferences)
        second.uiState.test {
            assertEquals(LibrarySortOrder.TITLE, awaitSuccess().query.sortOrder)
        }
    }

    @Test
    fun searchToggleFlipsState() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(false, awaitSuccess().searchOpen)
            viewModel.onAction(LibraryAction.ToggleSearch)
            assertEquals(true, awaitSuccessWhere { it.searchOpen }.searchOpen)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

/** Consumes until the first Success (tolerating an optional leading Loading). */
private suspend fun ReceiveTurbine<LibraryUiState>.awaitSuccess(): LibraryUiState.Success {
    var next = awaitItem()
    while (next is LibraryUiState.Loading) {
        next = awaitItem()
    }
    return next as LibraryUiState.Success
}

/**
 * Consumes until a Success satisfying [predicate]. Necessary because `combine` can emit
 * transient states where the query already changed but the re-queried list has not
 * propagated yet; conflation guarantees the settled state arrives.
 */
private suspend fun ReceiveTurbine<LibraryUiState>.awaitSuccessWhere(
    predicate: (LibraryUiState.Success) -> Boolean,
): LibraryUiState.Success {
    while (true) {
        val next = awaitItem()
        if (next is LibraryUiState.Success && predicate(next)) return next
    }
}
