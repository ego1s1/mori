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
    fun chromeTogglesNeverRescanResume() = runTest {
        val repository = TestComicsRepository(
            listOf(TestComicsRepository.comic("a", title = "Apple")),
        )
        val viewModel = viewModel(repository)
        viewModel.resumeTarget.test {
            assertEquals("a", awaitItem()?.id)
            // Search/filter/refresh chrome churns uiState but must not
            // re-emit (or rescan for) the resume candidate.
            viewModel.onAction(LibraryAction.ToggleSearch)
            viewModel.onAction(LibraryAction.OpenFilter)
            viewModel.onAction(LibraryAction.CloseFilter)
            viewModel.onAction(LibraryAction.ToggleSearch)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
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
        repository.linkReport = com.mori.core.model.ImportReport(2, 0, 2, emptyList())
        val preferences = TestPreferencesDataSource()
        preferences.setSourceTreeUri("content://tree/linked")
        val viewModel = viewModel(repository, preferences = preferences)
        viewModel.messages.test {
            viewModel.onAction(LibraryAction.Refresh)
            assertEquals(LibraryMessage.IndexFailed(2), awaitItem())
        }
        // Init gate plus manual refresh, both idempotent.
        assertEquals(2, repository.linkedTrees.size)
    }

    @Test
    fun refreshExceptionMessageIsOneShot() = runTest {
        val repository = TestComicsRepository()
        repository.failLinkWith = IllegalStateException("disk gone")
        val preferences = TestPreferencesDataSource()
        preferences.setSourceTreeUri("content://tree/linked")
        val viewModel = viewModel(repository, preferences = preferences)
        viewModel.messages.test {
            viewModel.onAction(LibraryAction.Refresh)
            assertEquals(LibraryMessage.RescanFailed, awaitItem())
        }
    }

    @Test
    fun refreshWithoutTreeIsSilentNoOp() = runTest {
        val repository = TestComicsRepository()
        val viewModel = viewModel(repository)
        viewModel.messages.test {
            viewModel.onAction(LibraryAction.Refresh)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(repository.linkedTrees.isEmpty())
    }

    @Test
    fun refreshReindexesLinkedTree() = runTest {
        val repository = TestComicsRepository()
        val preferences = TestPreferencesDataSource()
        val viewModel = viewModel(
            repository = repository,
            preferences = preferences,
        )
        preferences.setSourceTreeUri("content://tree/linked")
        viewModel.uiState.test {
            awaitSuccess()
            viewModel.onAction(LibraryAction.Refresh)
            // Refresh completes synchronously in tests; assert the settled
            // value directly (emission-awaiting hangs with nested stateIn).
            val settled = viewModel.uiState.value
            assertTrue(settled is LibraryUiState.Success)
            assertEquals(false, (settled as LibraryUiState.Success).refreshing)
            cancelAndIgnoreRemainingEvents()
        }
        // Linked trees index in place: no copies, no app rescan. (The init
        // gate read prefs before the tree was set here, so only the manual
        // refresh fires — see emptyShelfAutoIndexesLinkedTree for the gate.)
        val tree = android.net.Uri.parse("content://tree/linked")
        assertEquals(
            listOf(tree),
            repository.linkedTrees,
        )
    }

    @Test
    fun folderSelectedLinksAndIndexes() = runTest {
        val repository = TestComicsRepository()
        val preferences = TestPreferencesDataSource()
        val viewModel = viewModel(
            repository = repository,
            preferences = preferences,
        )
        viewModel.uiState.test {
            awaitSuccess()
            viewModel.onAction(
                LibraryAction.FolderSelected(android.net.Uri.parse("content://tree/new")),
            )
            val settled = viewModel.uiState.value
            assertTrue(settled is LibraryUiState.Success)
            assertEquals(false, (settled as LibraryUiState.Success).refreshing)
            cancelAndIgnoreRemainingEvents()
        }
        preferences.sourceTreeUri.test {
            assertEquals("content://tree/new", awaitItem())
        }
        assertEquals(
            listOf(android.net.Uri.parse("content://tree/new")),
            repository.linkedTrees,
        )
    }

    @Test
    fun emptyShelfAutoIndexesLinkedTree() = runTest {
        val repository = TestComicsRepository()
        val preferences = TestPreferencesDataSource()
        preferences.setSourceTreeUri("content://tree/linked")
        viewModel(
            repository = repository,
            preferences = preferences,
        )
        // The init gate fires on an empty shelf with a linked tree.
        assertEquals(
            listOf(android.net.Uri.parse("content://tree/linked")),
            repository.linkedTrees,
        )
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
