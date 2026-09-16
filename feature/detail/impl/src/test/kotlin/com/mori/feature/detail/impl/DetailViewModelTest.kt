package com.mori.feature.detail.impl

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
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
    fun loadsComicFromRepository() = runTest {
        val (viewModel, _) = viewModel()
        viewModel.uiState.test {
            val state = awaitReady()
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
        viewModel.uiState.test { awaitReady() }
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
            assertEquals(false, awaitReady().comic.bookmarked)
            viewModel.onAction(DetailAction.ToggleBookmark)
            assertEquals(true, awaitReady().comic.bookmarked)
            viewModel.onAction(DetailAction.ToggleBookmark)
            assertEquals(false, awaitReady().comic.bookmarked)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refreshDelegatesToRepository() = runTest {
        val (viewModel, repository) = viewModel()
        viewModel.uiState.test {
            awaitReady()
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
    fun removeFlowAsksConfirmsAndMarksRemoved() = runTest {
        val (viewModel, repository) = viewModel()
        viewModel.uiState.test {
            awaitReady()
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

    private suspend fun app.cash.turbine.ReceiveTurbine<DetailUiState>.awaitReady(): DetailUiState.Ready {
        return when (val next = awaitWhere { it !is DetailUiState.Loading }) {
            is DetailUiState.Ready -> next
            is DetailUiState.Missing -> throw AssertionError("Expected Ready, got Missing")
            DetailUiState.Loading -> error("unreachable")
        }
    }
}
