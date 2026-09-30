package com.mori.feature.detail.impl

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.mori.core.testing.FakeComicsRepository
import com.mori.core.testing.TestDispatcherRule
import com.mori.core.testing.awaitAs
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
class DetailViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private fun viewModel(
        comicId: String = "a",
        repository: FakeComicsRepository = FakeComicsRepository(
            mapOf("a" to FakeComicsRepository.comic("a")),
        ),
    ) = DetailHost(
        DetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("comicId" to comicId)),
            repository = repository,
        ),
        repository,
    )

    private data class DetailHost(
        val viewModel: DetailViewModel,
        val repository: FakeComicsRepository,
    )

    @Test
    fun shelvesDialogTogglesMembershipAndCreates() = runTest {
        val (viewModel, repository) = viewModel()
        val shelfId = repository.createCollection("Picks")
        viewModel.uiState.test {
            awaitAs<DetailUiState.Ready>()
            viewModel.onAction(DetailAction.OpenShelves)
            val open = awaitAs<DetailUiState.Ready> { it.shelves != null }
            assertEquals(listOf(shelfId), open.shelves!!.collections.map { it.id })
            assertTrue(shelfId !in open.shelves!!.memberIds)

            viewModel.onAction(DetailAction.ToggleShelfMember(shelfId))
            val member = awaitAs<DetailUiState.Ready> { shelfId in (it.shelves?.memberIds.orEmpty()) }
            assertTrue(shelfId in member.shelves!!.memberIds)

            viewModel.onAction(DetailAction.CreateShelf("New"))
            awaitAs<DetailUiState.Ready> { shelves ->
                shelves.shelves?.collections?.any { it.name == "New" } == true &&
                    shelves.shelves?.memberIds?.size == 2
            }
            viewModel.onAction(DetailAction.CloseShelves)
            awaitAs<DetailUiState.Ready> { it.shelves == null }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadsComicFromRepository() = runTest {
        val (viewModel, _) = viewModel()
        viewModel.uiState.test {
            val state = awaitAs<DetailUiState.Ready>()
            assertEquals("a", state.comic.id)
        }
    }

    @Test
    fun missingComicBecomesMissing() = runTest {
        val (viewModel, _) = viewModel(
            comicId = "ghost",
            repository = FakeComicsRepository(emptyMap()),
        )
        viewModel.uiState.test {
            assertTrue(awaitItem() is DetailUiState.Missing)
        }
    }

    @Test
    fun shareEmitsFileMessage() = runTest {
        val (viewModel, _) = viewModel()
        viewModel.uiState.test { awaitAs<DetailUiState.Ready>() }
        viewModel.messages.test {
            viewModel.onAction(DetailAction.Share)
            val message = awaitItem()
            assertTrue(message is DetailMessage.ShareFile)
            message as DetailMessage.ShareFile
            assertEquals("/lib/a.cbz", message.uri)
            assertEquals("a.cbz", message.displayName)
            assertEquals("application/zip", message.mimeType)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleBookmarkFlipsPersistedFlag() = runTest {
        val (viewModel, _) = viewModel()
        viewModel.uiState.test {
            assertEquals(false, awaitAs<DetailUiState.Ready>().comic.bookmarked)
            viewModel.onAction(DetailAction.ToggleBookmark)
            assertEquals(true, awaitAs<DetailUiState.Ready>().comic.bookmarked)
            viewModel.onAction(DetailAction.ToggleBookmark)
            assertEquals(false, awaitAs<DetailUiState.Ready>().comic.bookmarked)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refreshDelegatesToRepository() = runTest {
        val (viewModel, repository) = viewModel()
        viewModel.uiState.test {
            awaitAs<DetailUiState.Ready>()
            viewModel.onAction(DetailAction.Refresh)
            // The refresh settles synchronously and StateFlow conflation may swallow
            // the transient; assert the settled value directly plus delegation.
            val settled = viewModel.uiState.value
            assertTrue(settled is DetailUiState.Ready)
            assertEquals(false, (settled as DetailUiState.Ready).refreshing)
        }
        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun refreshFailureMessageIsOneShot() = runTest {
        val repository = FakeComicsRepository(mapOf("a" to FakeComicsRepository.comic("a")))
        repository.failRefreshWith = IllegalStateException("boom")
        val (viewModel, _) = viewModel(repository = repository)
        viewModel.messages.test {
            viewModel.onAction(DetailAction.Refresh)
            assertEquals(DetailMessage.RescanFailed, awaitItem())
        }
    }

    @Test
    fun refreshVanishedDocumentSendsRescanFailed() = runTest {
        // Empty repository: refreshComic returns null instead of throwing.
        val (viewModel, _) = viewModel(repository = FakeComicsRepository(emptyMap()))
        viewModel.messages.test {
            viewModel.onAction(DetailAction.Refresh)
            assertEquals(DetailMessage.RescanFailed, awaitItem())
        }
    }

    @Test
    fun rapidRefreshTapsQueueInsteadOfDropping() = runTest {
        // Park the first refresh inside a gate; the second tap must queue
        // behind the mutex instead of being dropped.
        val repository = FakeComicsRepository(mapOf("a" to FakeComicsRepository.comic("a")))
        repository.refreshGate = kotlinx.coroutines.CompletableDeferred()
        val (viewModel, _) = viewModel(repository = repository)
        viewModel.uiState.test {
            awaitAs<DetailUiState.Ready>()
            viewModel.onAction(DetailAction.Refresh)
            viewModel.onAction(DetailAction.Refresh)
            dispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
            repository.refreshGate?.complete(Unit)
            dispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(2, repository.refreshCalls)
    }

    @Test
    fun bookmarkAfterRemovalIsIgnored() = runTest {
        val (viewModel, repository) = viewModel()
        viewModel.uiState.test {
            awaitAs<DetailUiState.Ready>()
            viewModel.onAction(DetailAction.AskRemove)
            awaitItem() // confirmRemove = true
            viewModel.onAction(DetailAction.ConfirmRemove)
            assertTrue(awaitItem() is DetailUiState.Missing)
            viewModel.onAction(DetailAction.ToggleBookmark)
            dispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        // The row is gone: no phantom bookmark write for the removed id.
        assertTrue(repository.bookmarkToggles.isEmpty())
    }

    @Test
    fun doubleConfirmRemovesOnce() = runTest {
        val (viewModel, repository) = viewModel()
        viewModel.uiState.test {
            awaitAs<DetailUiState.Ready>()
            viewModel.onAction(DetailAction.AskRemove)
            awaitItem() // confirmRemove = true
            viewModel.onAction(DetailAction.ConfirmRemove)
            viewModel.onAction(DetailAction.ConfirmRemove)
            dispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("a"), repository.removedIds)
    }

    @Test
    fun removeFlowAsksConfirmsAndMarksRemoved() = runTest {
        val (viewModel, repository) = viewModel()
        viewModel.uiState.test {
            awaitAs<DetailUiState.Ready>()
            viewModel.onAction(DetailAction.AskRemove)
            assertTrue((awaitItem() as DetailUiState.Ready).confirmRemove)
            viewModel.onAction(DetailAction.CancelRemove)
            assertTrue(!(awaitItem() as DetailUiState.Ready).confirmRemove)
            viewModel.onAction(DetailAction.AskRemove)
            awaitItem() // confirmRemove = true again
            viewModel.onAction(DetailAction.ConfirmRemove)
            // Removal deletes the row, so the state becomes Missing.
            assertTrue(awaitItem() is DetailUiState.Missing)
        }
        assertEquals(listOf("a"), repository.removedIds)
    }
}
