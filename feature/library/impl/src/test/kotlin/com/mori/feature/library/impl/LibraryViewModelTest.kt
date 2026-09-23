package com.mori.feature.library.impl

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.mori.core.model.LibraryDisplay
import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibrarySortOrder
import com.mori.core.testing.FakeComicsRepository
import com.mori.core.testing.FakePreferencesDataSource
import com.mori.core.testing.TestDispatcherRule
import com.mori.core.testing.awaitWhere
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
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
        repository: FakeComicsRepository = FakeComicsRepository(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        preferences: FakePreferencesDataSource = FakePreferencesDataSource(),
    ) = LibraryViewModel(savedStateHandle, repository, preferences)

    @Test
    fun collectionsFilterCreateAndDelete() = runTest {
        val repository = FakeComicsRepository(
            listOf(
                FakeComicsRepository.comic("a", title = "Apple"),
                FakeComicsRepository.comic("b", title = "Banana"),
            ),
        )
        val viewModel = viewModel(repository)
        viewModel.uiState.test {
            awaitSuccess()
            viewModel.onAction(LibraryAction.OpenCreateCollection)
            awaitSuccessWhere { it.collectionDialog is CollectionDialog.Create }
            viewModel.onAction(LibraryAction.CreateCollection("Favorites"))
            val created = awaitSuccessWhere { it.collections.size == 1 }
            assertEquals("Favorites", created.collections.single().name)
            val id = created.collections.single().id
            // Dialog closes and the new shelf selects itself.
            assertEquals(null, created.collectionDialog)
            assertEquals(id, created.selectedCollectionId)
            // Nothing is a member yet: the grid filters to empty.
            awaitSuccessWhere { it.selectedCollectionId == id && it.comics.isEmpty() }

            viewModel.onAction(LibraryAction.SelectCollection(null))
            awaitSuccessWhere { it.selectedCollectionId == null && it.comics.size == 2 }

            viewModel.onAction(
                LibraryAction.OpenDeleteCollection(id, "Favorites"),
            )
            awaitSuccessWhere {
                it.collectionDialog is CollectionDialog.Delete
            }
            viewModel.onAction(LibraryAction.ConfirmDeleteCollection(id))
            val deleted = awaitSuccessWhere { it.collections.isEmpty() }
            assertEquals(null, deleted.collectionDialog)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emitsComicsFromRepository() = runTest {
        val repository = FakeComicsRepository(
            listOf(
                FakeComicsRepository.comic("b", title = "Banana"),
                FakeComicsRepository.comic("a", title = "Apple"),
            ),
        )
        viewModel(repository).uiState.test {
            val success = awaitSuccess()
            assertEquals(2, success.comics.size)
        }
    }

    @Test
    fun chromeTogglesNeverRescanResume() = runTest {
        val repository = FakeComicsRepository(
            listOf(FakeComicsRepository.comic("a", title = "Apple", lastPageIndex = 3)),
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
        val repository = FakeComicsRepository(
            listOf(
                FakeComicsRepository.comic("b", title = "Banana"),
                FakeComicsRepository.comic("a", title = "Apple"),
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
        val repository = FakeComicsRepository()
        repository.linkReport = com.mori.core.model.ImportReport(2, 0, 2, emptyList())
        val preferences = FakePreferencesDataSource()
        preferences.setSourceTreeUri("content://tree/linked")
        val viewModel = viewModel(repository, preferences = preferences)
        viewModel.messages.test {
            viewModel.onAction(LibraryAction.Refresh)
            // Launch rescan and manual refresh each report once.
            assertEquals(LibraryMessage.IndexFailed(2), awaitItem())
            assertEquals(LibraryMessage.IndexFailed(2), awaitItem())
        }
        // Launch rescan plus manual refresh, both idempotent.
        assertEquals(2, repository.linkedTrees.size)
    }

    @Test
    fun refreshExceptionMessageIsOneShot() = runTest {
        val repository = FakeComicsRepository()
        repository.failLinkWith = IllegalStateException("disk gone")
        val preferences = FakePreferencesDataSource()
        preferences.setSourceTreeUri("content://tree/linked")
        val viewModel = viewModel(repository, preferences = preferences)
        viewModel.messages.test {
            viewModel.onAction(LibraryAction.Refresh)
            // Launch rescan and manual refresh each report once.
            assertEquals(LibraryMessage.RescanFailed, awaitItem())
            assertEquals(LibraryMessage.RescanFailed, awaitItem())
        }
    }

    @Test
    fun refreshWithoutTreeIsSilentNoOp() = runTest {
        val repository = FakeComicsRepository()
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
        val repository = FakeComicsRepository()
        val preferences = FakePreferencesDataSource()
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
        // Linked trees index in place: no copies, no app rescan. (The launch
        // rescan read prefs before the tree was set here, so only the manual
        // refresh fires — see emptyShelfAutoIndexesLinkedTree for the gate.)
        val tree = android.net.Uri.parse("content://tree/linked")
        assertEquals(
            listOf(tree),
            repository.linkedTrees,
        )
    }

    @Test
    fun rescanProgressReachesUiState() = runTest {
        // The index callback forwards done/total instead of being dropped,
        // so large rescans show a determinate bar, not a stuck spinner.
        val repository = FakeComicsRepository().apply {
            linkReport = com.mori.core.model.ImportReport(2, 2, 0, emptyList())
            indexGate = CompletableDeferred()
        }
        val preferences = FakePreferencesDataSource()
        preferences.setSourceTreeUri("content://tree/old")
        val viewModel = viewModel(repository, preferences = preferences)
        viewModel.uiState.test {
            // The launch rescan parks inside the gate with its opening
            // progress tick already forwarded — no race with the clear.
            // (The first Success may already carry it, so await the
            // predicate directly instead of awaiting twice.)
            val progress = awaitSuccessWhere { it.indexProgress != null }
            assertEquals(0, progress.indexProgress?.done)
            assertEquals(2, progress.indexProgress?.total)
            repository.indexGate?.complete(Unit)
            dispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rapidRefreshTapsCoalesceIntoOneFollowUp() = runTest {
        // Three taps behind a parked run collapse into a single follow-up
        // pass instead of queueing three full reindexes.
        val repository = FakeComicsRepository()
        repository.indexGate = CompletableDeferred()
        val preferences = FakePreferencesDataSource()
        preferences.setSourceTreeUri("content://tree/old")
        val viewModel = viewModel(repository, preferences = preferences)
        viewModel.uiState.test {
            awaitSuccess()
            viewModel.onAction(LibraryAction.Refresh)
            viewModel.onAction(LibraryAction.Refresh)
            viewModel.onAction(LibraryAction.Refresh)
            repository.indexGate?.complete(Unit)
            dispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(2, repository.linkedTrees.size)
    }

    @Test
    fun folderSelectedLinksAndIndexes() = runTest {
        val repository = FakeComicsRepository()
        val preferences = FakePreferencesDataSource()
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
    fun folderPickDuringRescanIsQueuedNotDropped() = runTest {
        val repository = FakeComicsRepository()
        repository.indexGate = CompletableDeferred()
        val preferences = FakePreferencesDataSource()
        preferences.setSourceTreeUri("content://tree/old")
        // init holds the launch rescan inside the gate...
        val viewModel = viewModel(repository, preferences = preferences)
        // ...so this pick queues behind the lock instead of returning early.
        viewModel.onAction(LibraryAction.FolderSelected(android.net.Uri.parse("content://tree/new")))
        // The new URI persists immediately even while indexing runs.
        assertEquals("content://tree/new", preferences.sourceTreeUri.first())
        repository.indexGate?.complete(Unit)
        dispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            listOf(
                android.net.Uri.parse("content://tree/old"),
                android.net.Uri.parse("content://tree/new"),
            ),
            repository.linkedTrees,
        )
    }
    @Test
    fun emptyShelfAutoIndexesLinkedTree() = runTest {
        val repository = FakeComicsRepository()
        val preferences = FakePreferencesDataSource()
        preferences.setSourceTreeUri("content://tree/linked")
        viewModel(
            repository = repository,
            preferences = preferences,
        )
        // The launch rescan fires on an empty shelf with a linked tree.
        assertEquals(
            listOf(android.net.Uri.parse("content://tree/linked")),
            repository.linkedTrees,
        )
    }

    @Test
    fun populatedShelfRescansOnLaunch() = runTest {
        val repository = FakeComicsRepository(
            listOf(FakeComicsRepository.comic("a", title = "Apple")),
        )
        val preferences = FakePreferencesDataSource()
        preferences.setSourceTreeUri("content://tree/linked")
        viewModel(
            repository = repository,
            preferences = preferences,
        )
        // Every launch rescans: files added outside the app get picked up
        // even when the shelf is already populated.
        assertEquals(
            listOf(android.net.Uri.parse("content://tree/linked")),
            repository.linkedTrees,
        )
    }

    @Test
    fun continueShelfOrdersInProgressByRecency() = runTest {
        val repository = FakeComicsRepository(
            listOf(
                FakeComicsRepository.comic("old", title = "Old", pageCount = 10, lastPageIndex = 3, updatedAt = 10L),
                FakeComicsRepository.comic("new", title = "New", pageCount = 10, lastPageIndex = 3, updatedAt = 30L),
                FakeComicsRepository.comic("fresh", title = "Fresh"),
                FakeComicsRepository.comic("done", title = "Done", pageCount = 10, lastPageIndex = 9, updatedAt = 50L),
            ),
        )
        viewModel(repository).uiState.test {
            val shelf = awaitSuccess().continueReading.map { it.id }
            // In-progress only, newest first; untouched and finished excluded.
            assertEquals(listOf("new", "old"), shelf)
        }
    }

    @Test
    fun queryRestoresFromSavedState() = runTest {
        val handle = SavedStateHandle(
            mapOf(
                "mori_query_text" to "app",
            ),
        )
        val preferences = FakePreferencesDataSource(
            initialLibrary = LibraryDisplay(
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
        val preferences = FakePreferencesDataSource()
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
private suspend fun ReceiveTurbine<LibraryUiState>.awaitSuccess(): LibraryUiState.Success =
    awaitWhere { it is LibraryUiState.Success } as LibraryUiState.Success

/**
 * Consumes until a Success satisfying [predicate]. Necessary because `combine` can emit
 * transient states where the query already changed but the re-queried list has not
 * propagated yet; conflation guarantees the settled state arrives.
 */
private suspend fun ReceiveTurbine<LibraryUiState>.awaitSuccessWhere(
    predicate: (LibraryUiState.Success) -> Boolean,
): LibraryUiState.Success =
    awaitWhere { it is LibraryUiState.Success && predicate(it) } as LibraryUiState.Success
