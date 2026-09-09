package com.mori.feature.detail.impl

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
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
class DetailViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private fun viewModel(
        comicId: String = "a",
        repository: TestComicsRepository = TestComicsRepository(
            mapOf("a" to TestComicsRepository.comic("a")),
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
        val repository: TestComicsRepository,
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
            repository = TestComicsRepository(emptyMap()),
        )
        viewModel.uiState.test {
            assertTrue(awaitItem() is DetailUiState.Missing)
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
    fun refreshFailureSurfacesSnackbar() = runTest {
        val repository = TestComicsRepository(mapOf("a" to TestComicsRepository.comic("a")))
        repository.failRefreshWith = IllegalStateException("boom")
        val (viewModel, _) = viewModel(repository = repository)
        viewModel.uiState.test {
            awaitReady()
            viewModel.onAction(DetailAction.Refresh)
            val settled = awaitSettled()
            assertEquals("boom", settled.snackbar)
            viewModel.onAction(DetailAction.DismissSnackbar)
            assertEquals(null, (awaitItem() as DetailUiState.Ready).snackbar)
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
        while (true) {
            when (val next = awaitItem()) {
                is DetailUiState.Ready -> return next
                DetailUiState.Loading -> Unit // keep waiting
                is DetailUiState.Missing -> throw AssertionError("Expected Ready, got Missing")
            }
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<DetailUiState>.awaitSettled(): DetailUiState.Ready {
        while (true) {
            when (val next = awaitItem()) {
                is DetailUiState.Ready -> if (!next.refreshing) return next
                DetailUiState.Loading -> Unit
                is DetailUiState.Missing -> throw AssertionError("Expected Ready, got Missing")
            }
        }
    }
}
