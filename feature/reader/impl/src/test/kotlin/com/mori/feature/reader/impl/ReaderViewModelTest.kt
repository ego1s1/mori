package com.mori.feature.reader.impl

import androidx.compose.ui.geometry.Offset
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
            assertEquals(
                ReaderErrorCause.Failed(ComicError.CORRUPT),
                (state as ReaderUiState.Error).cause,
            )
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
    fun zoomOffsetKeepsTapPointUnderFinger() {
        // Tapping the center needs no compensation.
        zoomOffsetForTap(
            tap = Offset(500f, 800f),
            center = Offset(500f, 800f),
            targetScale = 2.5f,
        ).assertOffset(0f, 0f)
        // Tapping right/below center shifts content left/up so the art stays put.
        zoomOffsetForTap(
            tap = Offset(700f, 1100f),
            center = Offset(500f, 800f),
            targetScale = 2.5f,
        ).assertOffset(-300f, -450f)
        // Zooming out always returns to fit, wherever the tap landed.
        zoomOffsetForTap(
            tap = Offset(700f, 1100f),
            center = Offset(500f, 800f),
            targetScale = 1f,
        ).assertOffset(0f, 0f)
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
    fun rapidPageTurnsAccumulateWithoutWaitingForState() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(0, awaitReady().pageIndex)
            // Two taps in the same frame: the second must build on the first,
            // not on the still-stale combined state.
            viewModel.onAction(ReaderAction.NextPage)
            viewModel.onAction(ReaderAction.NextPage)
            assertEquals(2, awaitReadyWhere { it.pageIndex == 2 }.pageIndex)
            viewModel.onAction(ReaderAction.PrevPage)
            viewModel.onAction(ReaderAction.PrevPage)
            assertEquals(0, awaitReadyWhere { it.pageIndex == 0 }.pageIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun pageCounterTogglePersistsToPreferences() = runTest {
        val preferences = TestPreferencesDataSource()
        val viewModel = viewModel(preferences = preferences)
        viewModel.uiState.test {
            assertEquals(true, awaitReady().showPageCounter)
            viewModel.onAction(ReaderAction.TogglePageCounter)
            assertEquals(false, awaitReadyWhere { !it.showPageCounter }.showPageCounter)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun hideChromeNeverTogglesBackOn() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(true, awaitReady().chromeVisible)
            viewModel.onAction(ReaderAction.HideChrome)
            assertEquals(false, awaitReadyWhere { !it.chromeVisible }.chromeVisible)
            viewModel.onAction(ReaderAction.HideChrome)
            dispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun pageChangedKeepsChromeOverOpenSettings() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitReady()
            viewModel.onAction(ReaderAction.OpenSettings)
            awaitReadyWhere { it.settingsOpen }
            viewModel.onAction(ReaderAction.PageChanged(3))
            val settled = awaitReadyWhere { it.pageIndex == 3 }
            assertEquals(true, settled.chromeVisible)
            assertEquals(true, settled.settingsOpen)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun savedPageRestoresBeforeRepositoryEmits() = runTest {
        val viewModel = ReaderViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf("comicId" to "c", "pageIndex" to 0, "mori_saved_page_index" to 7),
            ),
            repository = TestComicsRepository(mapOf("c" to TestComicsRepository.comic("c"))),
            preferences = TestPreferencesDataSource(),
        )
        viewModel.uiState.test {
            assertEquals(7, awaitReady().pageIndex)
        }
    }

    @Test
    fun bumpingIntoBookEndRevealsChrome() = runTest {
        val viewModel = viewModel(pageIndex = 9)
        viewModel.uiState.test {
            awaitReady()
            viewModel.onAction(ReaderAction.PageChanged(9))
            assertEquals(false, awaitReadyWhere { it.pageIndex == 9 && !it.chromeVisible }.chromeVisible)
            viewModel.onAction(ReaderAction.NextPage)
            val end = awaitReadyWhere { it.chromeVisible }
            assertEquals(true, end.chromeVisible)
            assertEquals(9, end.pageIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun turnStyleFollowsMoveSource() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(true, awaitReady().turnAnimated)
            viewModel.onAction(ReaderAction.NextPage)
            assertEquals(true, awaitReadyWhere { it.pageIndex == 1 }.turnAnimated)
            viewModel.onAction(ReaderAction.SeekPage(5))
            val sought = awaitReadyWhere { it.pageIndex == 5 }
            assertEquals(false, sought.turnAnimated)
            cancelAndIgnoreRemainingEvents()
        }
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

    @Test
    fun firstLaunchShowsOverviewThenFades() = runTest {
        // The overview beat itself is pinned: chrome stays up 2s, then fades.
        assertEquals(2_000L, ReaderViewModel.READER_OVERVIEW_MS)
        val preferences = TestPreferencesDataSource(overviewSeen = false)
        val viewModel = viewModel(preferences = preferences)
        viewModel.uiState.test {
            assertEquals(true, awaitReady().chromeVisible)
            dispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitReadyWhere { !it.chromeVisible }.chromeVisible)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(true, preferences.isOverviewSeen())
    }

    @Test
    fun laterLaunchesKeepChromeUp() = runTest {
        val preferences = TestPreferencesDataSource(overviewSeen = true)
        val viewModel = viewModel(preferences = preferences)
        viewModel.uiState.test {
            assertEquals(true, awaitReady().chromeVisible)
            dispatcherRule.testDispatcher.scheduler.advanceTimeBy(10_000L)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(true, preferences.isOverviewSeen())
    }

    @Test
    fun overviewDoesNotHideChromeOverOpenSettings() = runTest {
        val preferences = TestPreferencesDataSource(overviewSeen = false)
        val viewModel = viewModel(preferences = preferences)
        viewModel.uiState.test {
            awaitReady()
            viewModel.onAction(ReaderAction.OpenSettings)
            val open = awaitReadyWhere { it.settingsOpen }
            assertEquals(true, open.chromeVisible)
            dispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(true, preferences.isOverviewSeen())
    }

    private fun Offset.assertOffset(x: Float, y: Float) {
        // Delta comparison: zoom math can yield -0.0f, which boxed-equals rejects
        // against 0.0f despite rendering identically.
        assertEquals(x, this.x, 0.001f)
        assertEquals(y, this.y, 0.001f)
    }

    private suspend fun ReceiveTurbine<ReaderUiState>.awaitReady(): ReaderUiState.Ready {        while (true) {
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
