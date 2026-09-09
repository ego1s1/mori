package com.mori.feature.reader.impl

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.mori.core.model.ComicError
import com.mori.core.model.PageFit
import com.mori.core.model.ReaderPreferences
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

    private fun viewModel(
        comicId: String = "c",
        pageIndex: Int = 0,
        repository: TestComicsRepository = TestComicsRepository(
            mapOf("c" to TestComicsRepository.comic("c")),
        ),
        preferences: TestPreferencesDataSource = TestPreferencesDataSource(),
    ) = ReaderViewModel(
        savedStateHandle = SavedStateHandle(mapOf("comicId" to comicId, "pageIndex" to pageIndex)),
        repository = repository,
        preferences = preferences,
    )

    @Test
    fun startsAtRoutePageWithComicMetadata() = runTest {
        val viewModel = viewModel(pageIndex = 3)
        viewModel.uiState.test {
            val state = awaitReady()
            assertEquals("c", state.comicId)
            assertEquals("Title c", state.title)
            assertEquals("Series • 1", state.subtitle)
            assertEquals(3, state.pageIndex)
            assertEquals(10, state.pageCount)
        }
    }

    @Test
    fun missingComicBecomesError() = runTest {
        val viewModel = viewModel(
            comicId = "ghost",
            repository = TestComicsRepository(emptyMap()),
        )
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is ReaderUiState.Error)
        }
    }

    @Test
    fun errorComicBecomesErrorState() = runTest {
        val viewModel = viewModel(
            repository = TestComicsRepository(
                mapOf("c" to TestComicsRepository.comic("c", error = ComicError.CORRUPT)),
            ),
        )
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is ReaderUiState.Error)
            assertTrue((state as ReaderUiState.Error).message.isNotBlank())
        }
    }

    @Test
    fun pageTurnsHideChromeWhileSliderSeekKeepsIt() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(true, awaitReady().chromeVisible)
            viewModel.onAction(ReaderAction.NextPage)
            // Both fields settle together; the predicate must cover both.
            assertEquals(false, awaitReadyWhere { it.pageIndex == 1 && !it.chromeVisible }.chromeVisible)
            viewModel.onAction(ReaderAction.SeekPage(5))
            val afterSeek = awaitReadyWhere { it.chromeVisible && it.pageIndex == 5 }
            assertEquals(5, afterSeek.pageIndex)
            viewModel.onAction(ReaderAction.PageChanged(6))
            assertEquals(false, awaitReadyWhere { it.pageIndex == 6 && !it.chromeVisible }.chromeVisible)
        }
    }

    @Test
    fun zoomTargetTogglesBetweenFitAndLevel() {
        assertEquals(1f, zoomTargetForTap(2.5f))
        assertEquals(1f, zoomTargetForTap(1.5f))
        assertEquals(2.5f, zoomTargetForTap(1f))
    }

    @Test
    fun navigationClampsAndSurvivesRepoEmissions() = runTest {
        val repository = TestComicsRepository(mapOf("c" to TestComicsRepository.comic("c")))
        val viewModel = viewModel(repository = repository)
        viewModel.uiState.test {
            awaitReady()
            viewModel.onAction(ReaderAction.NextPage)
            assertEquals(1, (awaitItem() as ReaderUiState.Ready).pageIndex)
            // External repo emission (e.g. our own progress save) must not reset position.
            repository.send(TestComicsRepository.comic("c", lastPageIndex = 0))
            viewModel.onAction(ReaderAction.SeekPage(50))
            val clamped = awaitReadyWhere { it.pageIndex == 9 }
            assertEquals(9, clamped.pageIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun pageChangedSavesProgressDebounced() = runTest {
        val repository = TestComicsRepository(mapOf("c" to TestComicsRepository.comic("c")))
        val viewModel = viewModel(repository = repository)
        viewModel.uiState.test {
            awaitReady()
            viewModel.onAction(ReaderAction.PageChanged(4))
            viewModel.onAction(ReaderAction.PageChanged(5))
            dispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("c" to 5), repository.progressSaves)
    }

    @Test
    fun bookmarkTogglePersistsThroughRepository() = runTest {
        val repository = TestComicsRepository(mapOf("c" to TestComicsRepository.comic("c")))
        val viewModel = viewModel(repository = repository)
        viewModel.uiState.test {
            assertEquals(false, awaitReady().bookmarked)
            viewModel.onAction(ReaderAction.ToggleBookmark)
            assertEquals(true, (awaitItem() as ReaderUiState.Ready).bookmarked)
        }
    }

    @Test
    fun settingsChangesPersistToPreferences() = runTest {
        val preferences = TestPreferencesDataSource()
        val viewModel = viewModel(preferences = preferences)
        viewModel.uiState.test {
            val initial = awaitReady()
            assertEquals(ReadingDirection.LEFT_TO_RIGHT, initial.direction)
            viewModel.onAction(ReaderAction.SetDirection(ReadingDirection.RIGHT_TO_LEFT))
            assertEquals(
                ReadingDirection.RIGHT_TO_LEFT,
                (awaitItem() as ReaderUiState.Ready).direction,
            )
            viewModel.onAction(ReaderAction.SetPageFit(PageFit.HEIGHT))
            viewModel.onAction(ReaderAction.ToggleCrop)
            viewModel.onAction(ReaderAction.ToggleVolumeKeys)
            viewModel.onAction(ReaderAction.ToggleKeepScreenOn)
            val settled = awaitReadyWhere {
                it.pageFit == PageFit.HEIGHT && it.cropMargins && it.volumeKeys && !it.keepScreenOn
            }
            assertEquals(PageFit.HEIGHT, settled.pageFit)
            assertEquals(true, settled.cropMargins)
            assertEquals(true, settled.volumeKeys)
            assertEquals(false, settled.keepScreenOn)
        }
        val last = preferences.updates.last()
        assertEquals(ReadingDirection.RIGHT_TO_LEFT, last.direction)
        assertEquals(PageFit.HEIGHT, last.pageFit)
        assertEquals(true, last.cropMargins)
        assertEquals(true, last.volumeKeys)
        assertEquals(false, last.keepScreenOn)
    }

    @Test
    fun persistedPreferencesDriveInitialState() = runTest {
        val preferences = TestPreferencesDataSource(
            ReaderPreferences(direction = ReadingDirection.RIGHT_TO_LEFT),
        )
        val viewModel = viewModel(preferences = preferences)
        viewModel.uiState.test {
            assertEquals(ReadingDirection.RIGHT_TO_LEFT, awaitReady().direction)
        }
    }

    private suspend fun ReceiveTurbine<ReaderUiState>.awaitReady(): ReaderUiState.Ready {
        while (true) {
            when (val next = awaitItem()) {
                is ReaderUiState.Ready -> return next
                else -> Unit
            }
        }
    }

    private suspend fun ReceiveTurbine<ReaderUiState>.awaitReadyWhere(
        predicate: (ReaderUiState.Ready) -> Boolean,
    ): ReaderUiState.Ready {
        while (true) {
            when (val next = awaitItem()) {
                is ReaderUiState.Ready -> if (predicate(next)) return next
                else -> Unit
            }
        }
    }
}
