package com.mori.feature.reader.impl

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.mori.core.model.PageFit
import com.mori.core.model.ReadingDirection
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
class ReaderViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private fun viewModel(pageIndex: Int = 12): ReaderViewModel = ReaderViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                "comicId" to "batman",
                "pageIndex" to pageIndex,
            ),
        ),
    )

    @Test
    fun startsAtRequestedPage() = runTest {
        val viewModel = viewModel(pageIndex = 12)
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is ReaderUiState.Ready)
            state as ReaderUiState.Ready
            assertEquals(12, state.pageIndex)
            assertEquals(13, state.currentPage)
            assertEquals(173, state.pageCount)
        }
    }

    @Test
    fun nextPageAdvancesAndClampsAtEnd() = runTest {
        val viewModel = viewModel(pageIndex = 172)
        viewModel.onAction(ReaderAction.NextPage)
        viewModel.uiState.test {
            val state = awaitItem() as ReaderUiState.Ready
            assertEquals(172, state.pageIndex)
        }
    }

    @Test
    fun prevPageClampsAtStart() = runTest {
        val viewModel = viewModel(pageIndex = 0)
        viewModel.onAction(ReaderAction.PrevPage)
        viewModel.uiState.test {
            val state = awaitItem() as ReaderUiState.Ready
            assertEquals(0, state.pageIndex)
        }
    }

    @Test
    fun seekPageClampsAndShowsChrome() = runTest {
        val viewModel = viewModel(pageIndex = 5)
        viewModel.onAction(ReaderAction.ToggleChrome)
        viewModel.onAction(ReaderAction.SeekPage(500))
        viewModel.uiState.test {
            val state = awaitItem() as ReaderUiState.Ready
            assertEquals(172, state.pageIndex)
            assertTrue(state.chromeVisible)
        }
    }

    @Test
    fun pageChangedUpdatesIndexWithoutForcingChrome() = runTest {
        val viewModel = viewModel(pageIndex = 5)
        viewModel.onAction(ReaderAction.ToggleChrome)
        viewModel.onAction(ReaderAction.PageChanged(9))
        viewModel.uiState.test {
            val state = awaitItem() as ReaderUiState.Ready
            assertEquals(9, state.pageIndex)
        }
    }

    @Test
    fun bookmarkToggles() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(ReaderAction.ToggleBookmark)
        viewModel.uiState.test {
            val state = awaitItem() as ReaderUiState.Ready
            assertTrue(state.bookmarked)
        }
    }

    @Test
    fun settingsOpenCloseAndOptions() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(ReaderAction.OpenSettings)
        viewModel.onAction(ReaderAction.SetDirection(ReadingDirection.RIGHT_TO_LEFT))
        viewModel.onAction(ReaderAction.SetPageFit(PageFit.HEIGHT))
        viewModel.onAction(ReaderAction.ToggleCrop)
        viewModel.uiState.test {
            val state = awaitItem() as ReaderUiState.Ready
            assertTrue(state.settingsOpen)
            assertEquals(ReadingDirection.RIGHT_TO_LEFT, state.direction)
            assertEquals(PageFit.HEIGHT, state.pageFit)
            assertTrue(state.cropMargins)
        }
        viewModel.onAction(ReaderAction.CloseSettings)
        viewModel.uiState.test {
            val state = awaitItem() as ReaderUiState.Ready
            assertTrue(!state.settingsOpen)
        }
    }
}
