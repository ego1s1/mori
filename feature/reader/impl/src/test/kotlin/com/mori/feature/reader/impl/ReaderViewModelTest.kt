package com.mori.feature.reader.impl

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.mori.core.model.ComicError
import com.mori.core.model.PageFit
import com.mori.core.model.PageHalf
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.ReadingDirection
import com.mori.core.testing.FakeComicsRepository
import com.mori.core.testing.FakePreferencesDataSource
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
class ReaderViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private fun viewModel(
        comicId: String = "c",
        pageIndex: Int = 0,
        repository: FakeComicsRepository = FakeComicsRepository(
            mapOf("c" to FakeComicsRepository.comic("c", series = "Series", number = "1")),
        ),
        preferences: FakePreferencesDataSource = FakePreferencesDataSource(),
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
            repository = FakeComicsRepository(emptyMap()),
        )
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is ReaderUiState.Error)
        }
    }

    @Test
    fun errorComicBecomesErrorState() = runTest {
        val viewModel = viewModel(
            repository = FakeComicsRepository(
                mapOf("c" to FakeComicsRepository.comic("c", error = ComicError.CORRUPT)),
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
        val repository = FakeComicsRepository(mapOf("c" to FakeComicsRepository.comic("c")))
        val viewModel = viewModel(repository = repository)
        viewModel.uiState.test {
            awaitReady()
            viewModel.onAction(ReaderAction.NextPage)
            assertEquals(1, (awaitItem() as ReaderUiState.Ready).pageIndex)
            // External repo emission (e.g. our own progress save) must not reset position.
            repository.send(FakeComicsRepository.comic("c", lastPageIndex = 0))
            viewModel.onAction(ReaderAction.SeekPage(50))
            val clamped = awaitReadyWhere { it.pageIndex == 9 }
            assertEquals(9, clamped.pageIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun pageChangedSavesProgressDebounced() = runTest {
        val repository = FakeComicsRepository(mapOf("c" to FakeComicsRepository.comic("c")))
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
        val repository = FakeComicsRepository(mapOf("c" to FakeComicsRepository.comic("c")))
        val viewModel = viewModel(repository = repository)
        viewModel.uiState.test {
            assertEquals(false, awaitReady().bookmarked)
            viewModel.onAction(ReaderAction.ToggleBookmark)
            assertEquals(true, (awaitItem() as ReaderUiState.Ready).bookmarked)
        }
    }

    @Test
    fun settingsChangesPersistToPreferences() = runTest {
        val preferences = FakePreferencesDataSource()
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
            viewModel.onAction(ReaderAction.ToggleVolumeKeysInverted)
            viewModel.onAction(ReaderAction.ToggleKeepScreenOn)
            val settled = awaitReadyWhere {
                it.pageFit == PageFit.HEIGHT && it.cropMargins && it.volumeKeys &&
                    it.volumeKeysInverted && !it.keepScreenOn
            }
            assertEquals(PageFit.HEIGHT, settled.pageFit)
            assertEquals(true, settled.cropMargins)
            assertEquals(true, settled.volumeKeys)
            assertEquals(true, settled.volumeKeysInverted)
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
        val preferences = FakePreferencesDataSource()
        val viewModel = viewModel(preferences = preferences)
        viewModel.uiState.test {
            assertEquals(true, awaitReady().showPageCounter)
            viewModel.onAction(ReaderAction.TogglePageCounter)
            assertEquals(false, awaitReadyWhere { !it.showPageCounter }.showPageCounter)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun swipeToTurnDefaultsOnAndToggles() = runTest {
        val preferences = FakePreferencesDataSource()
        val viewModel = viewModel(preferences = preferences)
        viewModel.uiState.test {
            assertEquals(true, awaitReady().swipeToTurn)
            viewModel.onAction(ReaderAction.ToggleSwipeToTurn)
            assertEquals(false, awaitReadyWhere { !it.swipeToTurn }.swipeToTurn)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun tapZonePreviewPersistsToPreferences() = runTest {
        val preferences = FakePreferencesDataSource()
        val viewModel = viewModel(preferences = preferences)
        viewModel.uiState.test {
            assertEquals(false, awaitReady().showTapZones)
            viewModel.onAction(ReaderAction.ToggleTapZones)
            assertEquals(true, awaitReadyWhere { it.showTapZones }.showTapZones)
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
    fun overviewOpensClosesAndKeepsChrome() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(false, awaitReady().overviewOpen)
            viewModel.onAction(ReaderAction.OpenOverview)
            val open = awaitReadyWhere { it.overviewOpen }
            assertEquals(true, open.overviewOpen)
            assertEquals(true, open.chromeVisible)
            viewModel.onAction(ReaderAction.PageChanged(3))
            val settled = awaitReadyWhere { it.pageIndex == 3 }
            assertEquals(true, settled.chromeVisible)
            assertEquals(true, settled.overviewOpen)
            viewModel.onAction(ReaderAction.CloseOverview)
            assertEquals(false, awaitReadyWhere { !it.overviewOpen }.overviewOpen)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun splitExpandsWidePagesIntoHalves() = runTest {
        val repository = FakeComicsRepository(
            mapOf("c" to FakeComicsRepository.comic("c")),
        ).apply { widePages = setOf(2) }
        val viewModel = viewModel(
            repository = repository,
            preferences = FakePreferencesDataSource(ReaderPreferences(dualPageSplit = true)),
        )
        viewModel.uiState.test {
            val settled = awaitReadyWhere { it.pageCount == 11 }
            assertEquals(11, settled.pageCount)
            assertEquals(10, settled.archivePageCount)
            assertEquals(PageHalf.FULL, settled.viewerPages[0].half)
            assertEquals(2, settled.viewerPages[2].archiveIndex)
            assertEquals(PageHalf.LEFT, settled.viewerPages[2].half)
            assertEquals(PageHalf.RIGHT, settled.viewerPages[3].half)
            assertEquals(listOf(0, 1, 2, 4, 5, 6, 7, 8, 9, 10), settled.expandedForArchive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleSplitKeepsArchivePage() = runTest {
        val repository = FakeComicsRepository(
            mapOf("c" to FakeComicsRepository.comic("c")),
        ).apply { widePages = setOf(2) }
        val viewModel = viewModel(pageIndex = 2, repository = repository)
        viewModel.uiState.test {
            assertEquals(10, awaitReady().pageCount)
            viewModel.onAction(ReaderAction.ToggleDualSplit)
            val split = awaitReadyWhere { it.dualPageSplit && it.pageCount == 11 }
            assertEquals(2, split.pageIndex)
            assertEquals(2, split.currentArchiveIndex)
            assertEquals(PageHalf.LEFT, split.viewerPages[2].half)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun invertSwapsSplitHalvesWithoutLeavingPage() = runTest {
        val repository = FakeComicsRepository(
            mapOf("c" to FakeComicsRepository.comic("c")),
        ).apply { widePages = setOf(2) }
        val viewModel = viewModel(
            repository = repository,
            preferences = FakePreferencesDataSource(ReaderPreferences(dualPageSplit = true)),
        )
        viewModel.uiState.test {
            awaitReadyWhere { it.pageCount == 11 }
            viewModel.onAction(ReaderAction.SeekPage(2))
            awaitReadyWhere { it.pageIndex == 2 }
            viewModel.onAction(ReaderAction.ToggleDualInvert)
            val inverted = awaitReadyWhere { it.dualPageInvert }
            assertEquals(2, inverted.currentArchiveIndex)
            assertEquals(PageHalf.RIGHT, inverted.viewerPages[2].half)
            assertEquals(PageHalf.LEFT, inverted.viewerPages[3].half)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleSplitOffKeepsArchivePage() = runTest {
        val repository = FakeComicsRepository(
            mapOf("c" to FakeComicsRepository.comic("c")),
        ).apply { widePages = setOf(2) }
        val viewModel = viewModel(
            repository = repository,
            preferences = FakePreferencesDataSource(ReaderPreferences(dualPageSplit = true)),
        )
        viewModel.uiState.test {
            awaitReadyWhere { it.pageCount == 11 }
            // Second half of archive 2, then split off: the anchor lands on
            // archive 2's (only) position in the identity list.
            viewModel.onAction(ReaderAction.SeekPage(3))
            awaitReadyWhere { it.pageIndex == 3 }
            viewModel.onAction(ReaderAction.ToggleDualSplit)
            // The anchor follows the prefs write: settle on archive 2's
            // position in the identity list, not the stale expanded index.
            val unsplit = awaitReadyWhere { !it.dualPageSplit && it.pageIndex == 2 }
            assertEquals(10, unsplit.pageCount)
            assertEquals(2, unsplit.currentArchiveIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun progressSavesArchiveIndexWhenSplit() = runTest {
        val repository = FakeComicsRepository(
            mapOf("c" to FakeComicsRepository.comic("c")),
        ).apply { widePages = setOf(2) }
        val viewModel = viewModel(
            repository = repository,
            preferences = FakePreferencesDataSource(ReaderPreferences(dualPageSplit = true)),
        )
        viewModel.uiState.test {
            awaitReadyWhere { it.pageCount == 11 }
            // Expanded 3 is the RIGHT half of archive 2; the library resumes
            // by archive page, so it must record 2, not 3.
            viewModel.onAction(ReaderAction.PageChanged(3))
            dispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("c" to 2), repository.progressSaves)
    }

    @Test
    fun wideScanRunsOnceAndSurvivesProgressSaves() = runTest {
        // Progress saves re-emit the comic; the id de-dup must keep the
        // bounds decode to a single emission per session.
        val repository = FakeComicsRepository(
            mapOf("c" to FakeComicsRepository.comic("c")),
        ).apply { widePages = setOf(2) }
        val viewModel = viewModel(
            repository = repository,
            preferences = FakePreferencesDataSource(ReaderPreferences(dualPageSplit = true)),
        )
        viewModel.uiState.test {
            awaitReadyWhere { it.pageCount == 11 }
            viewModel.onAction(ReaderAction.PageChanged(3))
            dispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("c"), repository.wideScanCalls)
    }

    @Test
    fun wideScanFailureStaysWholePagesAndRetries() = runTest {
        // A transient decode failure must not poison the session: whole
        // pages render, and the next toggle retries the scan.
        val repository = FakeComicsRepository(
            mapOf("c" to FakeComicsRepository.comic("c")),
        ).apply {
            widePages = setOf(2)
            failWideWith = IllegalStateException("decode boom")
        }
        val viewModel = viewModel(
            repository = repository,
            preferences = FakePreferencesDataSource(ReaderPreferences(dualPageSplit = true)),
        )
        viewModel.uiState.test {
            val whole = awaitReadyWhere { !it.dualPageSplit || it.pageCount == 10 }
            assertEquals(10, whole.pageCount)
            repository.failWideWith = null
            viewModel.onAction(ReaderAction.ToggleDualSplit)
            awaitReadyWhere { !it.dualPageSplit }
            viewModel.onAction(ReaderAction.ToggleDualSplit)
            assertEquals(11, awaitReadyWhere { it.dualPageSplit && it.pageCount == 11 }.pageCount)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("c", "c"), repository.wideScanCalls)
    }

    @Test
    fun concurrentToggleDuringScanDecodesOnce() = runTest {
        // Rapid off/on while a decode is parked must not launch a second
        // full-book decode; the in-flight guard collapses it.
        val repository = FakeComicsRepository(
            mapOf("c" to FakeComicsRepository.comic("c")),
        ).apply {
            widePages = setOf(2)
            wideGate = kotlinx.coroutines.CompletableDeferred()
        }
        val viewModel = viewModel(
            repository = repository,
            preferences = FakePreferencesDataSource(ReaderPreferences(dualPageSplit = true)),
        )
        viewModel.uiState.test {
            awaitReady()
            viewModel.onAction(ReaderAction.ToggleDualSplit)
            awaitReadyWhere { !it.dualPageSplit }
            viewModel.onAction(ReaderAction.ToggleDualSplit)
            repository.wideGate?.complete(Unit)
            assertEquals(11, awaitReadyWhere { it.dualPageSplit && it.pageCount == 11 }.pageCount)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("c"), repository.wideScanCalls)
    }

    @Test
    fun setDirectionKeepsArchivePageAfterPrefsLand() = runTest {
        val repository = FakeComicsRepository(
            mapOf("c" to FakeComicsRepository.comic("c")),
        ).apply { widePages = setOf(2) }
        val viewModel = viewModel(
            repository = repository,
            preferences = FakePreferencesDataSource(ReaderPreferences(dualPageSplit = true)),
        )
        viewModel.uiState.test {
            awaitReadyWhere { it.pageCount == 11 }
            viewModel.onAction(ReaderAction.SeekPage(2))
            awaitReadyWhere { it.pageIndex == 2 }
            viewModel.onAction(ReaderAction.SetDirection(ReadingDirection.RIGHT_TO_LEFT))
            // The anchor follows the prefs write and stays on archive 2.
            val flipped = awaitReadyWhere {
                it.direction == ReadingDirection.RIGHT_TO_LEFT && it.currentArchiveIndex == 2
            }
            assertEquals(2, flipped.currentArchiveIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyPageCountIsTypedErrorNotPhantomPage() = runTest {
        val viewModel = viewModel(
            repository = FakeComicsRepository(
                mapOf("c" to FakeComicsRepository.comic("c").copy(pageCount = 0)),
            ),
        )
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is ReaderUiState.Error)
            assertEquals(
                ReaderErrorCause.Failed(ComicError.EMPTY),
                (state as ReaderUiState.Error).cause,
            )
        }
    }

    @Test
    fun pageChangedAfterRemovalSavesNothing() = runTest {
        val repository = FakeComicsRepository(
            mapOf("c" to FakeComicsRepository.comic("c")),
        )
        val viewModel = viewModel(repository = repository)
        viewModel.uiState.test {
            awaitReady()
            repository.send(emptyList())
            awaitItem() // Error(Removed)
            // Stale pager event for the deleted book: no navigation write,
            // no progress save — and nothing to flush on close either.
            viewModel.onAction(ReaderAction.PageChanged(3))
            dispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(repository.progressSaves.isEmpty())
    }

    @Test
    fun turnDuringSplitToggleWinsOverStaleAnchor() = runTest {
        // The toggle captures archive 2, but a seek landing before the
        // prefs round-trip completes must win: the stale anchor drops
        // instead of yanking the pager back.
        val repository = FakeComicsRepository(
            mapOf("c" to FakeComicsRepository.comic("c")),
        ).apply { widePages = setOf(2) }
        val viewModel = viewModel(
            repository = repository,
            preferences = FakePreferencesDataSource(ReaderPreferences(dualPageSplit = true)),
        )
        viewModel.uiState.test {
            awaitReadyWhere { it.pageCount == 11 }
            viewModel.onAction(ReaderAction.SeekPage(2))
            awaitReadyWhere { it.pageIndex == 2 }
            viewModel.onAction(ReaderAction.ToggleDualSplit)
            // Synchronous turn before the async prefs write lands.
            viewModel.onAction(ReaderAction.SeekPage(5))
            val settled = awaitReadyWhere { !it.dualPageSplit && it.pageIndex == 5 }
            assertEquals(5, settled.currentArchiveIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun savedPageRestoresBeforeRepositoryEmits() = runTest {
        val viewModel = ReaderViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf("comicId" to "c", "pageIndex" to 0, "mori_saved_page_index" to 7),
            ),
            repository = FakeComicsRepository(mapOf("c" to FakeComicsRepository.comic("c"))),
            preferences = FakePreferencesDataSource(),
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
        val preferences = FakePreferencesDataSource(
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
        val preferences = FakePreferencesDataSource(overviewSeen = false)
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
        val preferences = FakePreferencesDataSource(overviewSeen = true)
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
        val preferences = FakePreferencesDataSource(overviewSeen = false)
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

    private suspend fun ReceiveTurbine<ReaderUiState>.awaitReady(): ReaderUiState.Ready =
        awaitWhere { it is ReaderUiState.Ready } as ReaderUiState.Ready

    private suspend fun ReceiveTurbine<ReaderUiState>.awaitReadyWhere(
        predicate: (ReaderUiState.Ready) -> Boolean,
    ): ReaderUiState.Ready =
        awaitWhere { it is ReaderUiState.Ready && predicate(it) } as ReaderUiState.Ready
}
