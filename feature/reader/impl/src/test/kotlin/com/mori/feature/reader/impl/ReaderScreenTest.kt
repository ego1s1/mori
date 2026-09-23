package com.mori.feature.reader.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import com.mori.core.designsystem.MoriTheme
import com.mori.core.model.DisplayFilter
import com.mori.core.model.PageFit
import com.mori.core.model.PageHalf
import com.mori.core.model.ReadingDirection
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun ready(
        pageIndex: Int = 12,
        settingsOpen: Boolean = false,
    ) = ReaderUiState.Ready(
        comicId = "batman",
        title = "Batman",
        subtitle = "Court of Owls (2012)",
        bookmarked = false,
        pageIndex = pageIndex,
        pageCount = 173,
        chromeVisible = true,
        direction = ReadingDirection.LEFT_TO_RIGHT,
        pageFit = PageFit.WIDTH,
        cropMargins = false,
        settingsOpen = settingsOpen,
        overviewOpen = false,
        volumeKeys = false,
        volumeKeysInverted = false,
        keepScreenOn = true,
        showTapZones = false,
        showPageCounter = true,
        swipeToTurn = true,
    )

    @Test
    fun readyStateShowsTitleAndPageNumbers() {
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(),
                    onAction = {},
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderTestTags.TopBar).assertIsDisplayed()
        composeTestRule.onNodeWithText("Batman").assertIsDisplayed()
        composeTestRule.onNodeWithText("Court of Owls (2012)").assertIsDisplayed()
        // "13" is also rendered by the pager's placeholder art, so assert the total (unique)
        // and the slider container instead of the bare current-page text.
        composeTestRule.onNodeWithText("173").assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderTestTags.Prev).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderTestTags.Next).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderTestTags.Slider).assertIsDisplayed()
    }

    @Test
    fun nextButtonDispatchesNextPage() {
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderTestTags.Next).performClick()

        assert(actions.contains(ReaderAction.NextPage))
    }

    @Test
    fun prevButtonDispatchesPrevPage() {
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderTestTags.Prev).performClick()

        assert(actions.contains(ReaderAction.PrevPage))
    }

    @Test
    fun bookmarkButtonDispatchesToggle() {
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderTestTags.Bookmark).performClick()

        assert(actions.contains(ReaderAction.ToggleBookmark))
    }

    @Test
    fun settingsButtonDispatchesOpenSettings() {
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderTestTags.SettingsButton).performClick()

        assert(actions.contains(ReaderAction.OpenSettings))
    }

    @Test
    fun overviewButtonDispatchesOpenOverview() {
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderTestTags.OverviewButton).performClick()

        assert(actions.contains(ReaderAction.OpenOverview))
    }

    @Test
    fun overviewSheetContentSeeksAndCloses() {
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderOverviewSheetContent(
                    comicId = "batman",
                    currentPage = 2,
                    expandedCount = 4,
                    currentArchiveIndex = 1,
                    archivePageCount = 4,
                    expandedForArchive = listOf(0, 1, 2, 3),
                    cropMargins = false,
                    onAction = actions::add,
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderTestTags.OverviewGrid).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderTestTags.thumbFor(2)).performClick()

        assert(actions.contains(ReaderAction.SeekPage(2)))
        assert(actions.contains(ReaderAction.CloseOverview))
    }

    @Test
    fun overviewSeekMapsArchiveToExpandedPosition() {
        // Split book: archive page 1 (wide) occupies expanded 1..2, so its
        // thumb seeks to 1, not to its archive index.
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderOverviewSheetContent(
                    comicId = "batman",
                    currentPage = 1,
                    expandedCount = 5,
                    currentArchiveIndex = 0,
                    archivePageCount = 4,
                    expandedForArchive = listOf(0, 1, 3, 4),
                    cropMargins = false,
                    onAction = actions::add,
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderTestTags.thumbFor(1)).performClick()

        assert(actions.contains(ReaderAction.SeekPage(1)))
        assert(actions.contains(ReaderAction.CloseOverview))
    }

    @Test
    fun settingsSheetContentShowsOptions() {
        composeTestRule.setContent {
            MoriTheme {
                ReaderSettingsSheetContent(
                    direction = ReadingDirection.LEFT_TO_RIGHT,
                    pageFit = PageFit.WIDTH,
                    cropMargins = false,
                    volumeKeys = false,
                    volumeKeysInverted = false,
                    keepScreenOn = true,
                    showTapZones = false,
                    showPageCounter = true,
                    swipeToTurn = true,
                    dualPageSplit = false,
                    dualPageInvert = false,
                    displayFilter = DisplayFilter.Neutral,
                    hasFilterOverride = false,
                    onAction = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Reading settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reading direction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Page fit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Crop margins").assertIsDisplayed()
        composeTestRule.onNodeWithText("Left to right").assertIsDisplayed()
        composeTestRule.onNodeWithText("Width").assertIsDisplayed()
        // Lower sheet content may sit below the test viewport fold; assert composition.
        composeTestRule.onNodeWithText("Tap zones").assertExists()
        composeTestRule.onNodeWithText("Volume keys turn pages").assertExists()
        composeTestRule.onNodeWithText("Keep screen on").assertExists()
        composeTestRule.onNodeWithText("Page counter").assertExists()
        composeTestRule.onNodeWithText("Split wide pages").assertExists()
    }

    @Test
    fun invertSwitchHiddenWhenSplitOff() {
        composeTestRule.setContent {
            MoriTheme {
                ReaderSettingsSheetContent(
                    direction = ReadingDirection.LEFT_TO_RIGHT,
                    pageFit = PageFit.WIDTH,
                    cropMargins = false,
                    volumeKeys = false,
                    volumeKeysInverted = false,
                    keepScreenOn = true,
                    showTapZones = false,
                    showPageCounter = true,
                    swipeToTurn = true,
                    dualPageSplit = false,
                    dualPageInvert = false,
                    displayFilter = DisplayFilter.Neutral,
                    hasFilterOverride = false,
                    onAction = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Invert split halves").assertDoesNotExist()
    }

    @Test
    fun invertSwitchShownWhenSplitOn() {
        composeTestRule.setContent {
            MoriTheme {
                ReaderSettingsSheetContent(
                    direction = ReadingDirection.LEFT_TO_RIGHT,
                    pageFit = PageFit.WIDTH,
                    cropMargins = false,
                    volumeKeys = false,
                    volumeKeysInverted = false,
                    keepScreenOn = true,
                    showTapZones = false,
                    showPageCounter = true,
                    swipeToTurn = true,
                    dualPageSplit = true,
                    dualPageInvert = false,
                    displayFilter = DisplayFilter.Neutral,
                    hasFilterOverride = false,
                    onAction = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Invert split halves").assertExists()
    }

    @Test
    fun displaySectionShowsResetOnlyWithOverride() {
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderSettingsSheetContent(
                    direction = ReadingDirection.LEFT_TO_RIGHT,
                    pageFit = PageFit.WIDTH,
                    cropMargins = false,
                    volumeKeys = false,
                    volumeKeysInverted = false,
                    keepScreenOn = true,
                    showTapZones = false,
                    showPageCounter = true,
                    swipeToTurn = true,
                    dualPageSplit = false,
                    dualPageInvert = false,
                    displayFilter = DisplayFilter.Neutral,
                    hasFilterOverride = true,
                    onAction = actions::add,
                )
            }
        }

        composeTestRule.onNodeWithText("Display filters").assertExists()
        composeTestRule.onNodeWithText("Brightness").assertExists()
        composeTestRule.onNodeWithText("Night warmth").assertExists()
        composeTestRule.onNodeWithText("Grayscale").assertExists()
        composeTestRule.onNodeWithText("Invert colors").assertExists()
        // Switch/button dispatch is covered by ViewModel tests; clicks on
        // sheet rows don't actuate under Robolectric.
        composeTestRule.onNodeWithText("Reset to defaults").assertExists()
    }

    @Test
    fun displayResetHiddenWithoutOverride() {
        composeTestRule.setContent {
            MoriTheme {
                ReaderSettingsSheetContent(
                    direction = ReadingDirection.LEFT_TO_RIGHT,
                    pageFit = PageFit.WIDTH,
                    cropMargins = false,
                    volumeKeys = false,
                    volumeKeysInverted = false,
                    keepScreenOn = true,
                    showTapZones = false,
                    showPageCounter = true,
                    swipeToTurn = true,
                    dualPageSplit = false,
                    dualPageInvert = false,
                    displayFilter = DisplayFilter.Neutral,
                    hasFilterOverride = false,
                    onAction = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Reset to defaults").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tappingLeftZoneDispatchesPrevPage() {
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        val bounds = composeTestRule.onNodeWithTag(ReaderTestTags.Pager)
            .fetchSemanticsNode().boundsInRoot
        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
            down(0, Offset(bounds.width * 0.1f, bounds.height * 0.5f))
            up(0)
        }
        // Single taps wait out the double-tap timeout before dispatching.
        composeTestRule.mainClock.advanceTimeBy(1_000)

        assert(actions.contains(ReaderAction.PrevPage))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tappingRightZoneDispatchesNextPage() {
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        val bounds = composeTestRule.onNodeWithTag(ReaderTestTags.Pager)
            .fetchSemanticsNode().boundsInRoot
        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
            down(0, Offset(bounds.width * 0.9f, bounds.height * 0.5f))
            up(0)
        }
        // Single taps wait out the double-tap timeout before dispatching.
        composeTestRule.mainClock.advanceTimeBy(1_000)

        assert(actions.contains(ReaderAction.NextPage))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun pageTapDispatchesExactlyOnce() {
        // The page detector and the container fallback both observe the tap;
        // the page consumes its tap-up so the fallback stands down.
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        val bounds = composeTestRule.onNodeWithTag(ReaderTestTags.Pager)
            .fetchSemanticsNode().boundsInRoot
        repeat(3) {
            composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
                down(0, Offset(bounds.width * 0.9f, bounds.height * 0.5f))
                up(0)
            }
            composeTestRule.mainClock.advanceTimeBy(1_000)
        }

        assertEquals(3, actions.filterIsInstance<ReaderAction.NextPage>().size)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rapidSameSpotPairZoomsWithoutTurning() {
        // Three taps back-to-back with a frozen clock: the first pair is a
        // double-tap (zoom, no dispatch) and the third is still held. Neither
        // the page nor the container fallback may turn or toggle. Auto-advance
        // stays off so the frozen pair can't be split by clock pumping; the
        // held tap firing is covered by the spaced-tap tests.
        composeTestRule.mainClock.autoAdvance = false
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        val bounds = composeTestRule.onNodeWithTag(ReaderTestTags.Pager)
            .fetchSemanticsNode().boundsInRoot
        repeat(3) {
            composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
                down(0, Offset(bounds.width * 0.9f, bounds.height * 0.5f))
                up(0)
            }
        }

        assertEquals(0, actions.filterIsInstance<ReaderAction.NextPage>().size)
        assertEquals(0, actions.filterIsInstance<ReaderAction.ToggleChrome>().size)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun overlappingTwoFingerTapsBothRegister() {
        // Fast skippers alternate fingers: the second finger lands before the
        // first lifts. Overlapping downs never pair, so each finger-up still
        // dispatches its own tap (the second after its confirmation window).
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        val bounds = composeTestRule.onNodeWithTag(ReaderTestTags.Pager)
            .fetchSemanticsNode().boundsInRoot
        val spot = Offset(bounds.width * 0.9f, bounds.height * 0.5f)
        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
            down(0, spot)
            down(1, spot)
            up(0)
            up(1)
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)

        assertEquals(2, actions.filterIsInstance<ReaderAction.NextPage>().size)
    }

    @Test
    fun pageCounterShowsWhenChromeHidden() {
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready().copy(chromeVisible = false),
                    onAction = {},
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderTestTags.PageCounter).assertIsDisplayed()
        composeTestRule.onNodeWithText("13 / 173").assertIsDisplayed()
    }

    @Test
    fun pageCounterHiddenWithChrome() {
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready().copy(chromeVisible = true),
                    onAction = {},
                    onBackClick = {},
                )
            }
        }
        composeTestRule.onNodeWithTag(ReaderTestTags.PageCounter).assertDoesNotExist()
    }

    @Test
    fun pageCounterHiddenWhenDisabled() {
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready().copy(chromeVisible = false, showPageCounter = false),
                    onAction = {},
                    onBackClick = {},
                )
            }
        }
        composeTestRule.onNodeWithTag(ReaderTestTags.PageCounter).assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun shrinkingPageCountClampsPagerTarget() {
        // A split/direction rebuild mid-glide must never retarget out of
        // range: the stale index is clamped, no out-of-bounds glide runs,
        // and the pager stays usable at the clamped page.
        var pageCount by mutableStateOf(10)
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(pageIndex = 9).copy(
                        pageCount = pageCount,
                        viewerPages = List(pageCount) { ReaderViewerPage(it, PageHalf.FULL) },
                        archivePageCount = pageCount,
                        expandedForArchive = List(pageCount) { it },
                    ),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        actions.clear()

        pageCount = 2
        composeTestRule.mainClock.advanceTimeBy(1_000)

        assert(actions.none { it is ReaderAction.PageChanged })

        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
            swipeRight()
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)

        assert(actions.contains(ReaderAction.PageChanged(0)))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun splitToggleWhileZoomedReleasesPager() {
        // Zoomed, then the viewer list rebuilds (split on): zoom belongs to
        // the old layout, so the page remounts at fit with the gate released
        // instead of stranding swipe-turns behind a stale zoomed flag.
        composeTestRule.mainClock.autoAdvance = false
        var split by mutableStateOf(false)
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                val pages = buildViewerPages(
                    10,
                    if (split) setOf(2) else emptySet(),
                    ReadingDirection.LEFT_TO_RIGHT,
                    false,
                )
                ReaderScreen(
                    uiState = ready(pageIndex = 0).copy(
                        pageCount = pages.size,
                        viewerPages = pages,
                        archivePageCount = 10,
                        expandedForArchive = archiveToExpandedPositions(pages, 10),
                        dualPageSplit = split,
                    ),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        val bounds = composeTestRule.onNodeWithTag(ReaderTestTags.Pager)
            .fetchSemanticsNode().boundsInRoot
        val spot = Offset(bounds.width * 0.9f, bounds.height * 0.5f)
        repeat(2) {
            composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
                down(0, spot)
                up(0)
            }
        }
        // Let the double-tap zoom glide finish (frozen clock otherwise).
        composeTestRule.mainClock.advanceTimeBy(1_000)
        actions.clear()

        // Zoomed: the pager stands down, swipes never turn.
        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
            swipeLeft()
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        assert(actions.none { it is ReaderAction.PageChanged })

        // The viewer list rebuilds: the gate releases, swipes turn again.
        split = true
        composeTestRule.mainClock.advanceTimeBy(1_000)
        actions.clear()
        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
            swipeLeft()
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)

        assert(actions.any { it is ReaderAction.PageChanged })
    }

    @Test
    fun errorStateShowsMessage() {
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ReaderUiState.Error(ReaderErrorCause.Removed),
                    onAction = {},
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("This comic was removed from your library.").assertIsDisplayed()
    }

    @Test
    fun prevDisabledOnFirstPage() {
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(pageIndex = 0),
                    onAction = {},
                    onBackClick = {},
                )
            }
        }
        composeTestRule.onNodeWithTag(ReaderTestTags.Prev).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(ReaderTestTags.Next).assertIsEnabled()
    }

    @Test
    fun nextDisabledOnLastPage() {
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(pageIndex = 172),
                    onAction = {},
                    onBackClick = {},
                )
            }
        }
        composeTestRule.onNodeWithTag(ReaderTestTags.Prev).assertIsEnabled()
        composeTestRule.onNodeWithTag(ReaderTestTags.Next).assertIsNotEnabled()
    }

    @Test
    fun singlePageComicHidesSliderPill() {
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(pageIndex = 0).copy(pageCount = 1),
                    onAction = {},
                    onBackClick = {},
                )
            }
        }

        // Nowhere to turn: the whole nav row (slider and both buttons)
        // hides instead of rendering disabled.
        composeTestRule.onNodeWithTag(ReaderTestTags.Slider).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ReaderTestTags.Prev).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ReaderTestTags.Next).assertDoesNotExist()
    }

    @Test
    fun rtlLeadingButtonAdvances() {
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready().copy(direction = ReadingDirection.RIGHT_TO_LEFT),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        // In RTL the leading control moves forward.
        composeTestRule.onNodeWithTag(ReaderTestTags.Prev).performClick()

        assert(actions.contains(ReaderAction.NextPage))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun longPressDoesNotTurnPage() {
        // A press held past the long-press timeout is a long press, not a
        // tap: nothing may dispatch on release, however still the finger.
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        val bounds = composeTestRule.onNodeWithTag(ReaderTestTags.Pager)
            .fetchSemanticsNode().boundsInRoot
        val spot = Offset(bounds.width * 0.9f, bounds.height * 0.5f)
        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
            down(0, spot)
            repeat(6) { move(delayMillis = 100) }
            up(0)
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)

        assertEquals(0, actions.filterIsInstance<ReaderAction.NextPage>().size)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun swipeLeftTurnsToNextPage() {
        // Single-finger drags at fit belong to the pager, not the pan/zoom
        // tracker: a left swipe must settle on the next page.
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
            swipeLeft()
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)

        assert(actions.contains(ReaderAction.PageChanged(13)))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun swipeWhileZoomedPansInsteadOfTurning() {
        // Double-tap zooms in; a following swipe must pan inside the zoomed
        // page (edge handoff), never turn the page outright. Zooming
        // back out restores turning, proving gestures still flow.
        composeTestRule.mainClock.autoAdvance = false
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        val bounds = composeTestRule.onNodeWithTag(ReaderTestTags.Pager)
            .fetchSemanticsNode().boundsInRoot
        val spot = Offset(bounds.width * 0.9f, bounds.height * 0.5f)
        repeat(2) {
            composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
                down(0, spot)
                up(0)
            }
        }
        // Let the double-tap zoom glide finish (frozen clock otherwise).
        composeTestRule.mainClock.advanceTimeBy(1_000)
        actions.clear()

        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
            swipeLeft()
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)

        assert(actions.none { it is ReaderAction.PageChanged })

        // Zoom back out: turning works again, so gestures were panning, not eaten.
        repeat(2) {
            composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
                down(0, spot)
                up(0)
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        actions.clear()
        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
            swipeLeft()
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)

        assert(actions.any { it is ReaderAction.PageChanged })
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun secondSwipeAtEdgeTurnsWhileZoomed() {
        // Hard-stop then turn: the first swipe pans to the clamp without
        // turning by itself; a fresh second swipe pushing further outward
        // turns the page even while still zoomed. The turn arrives as an
        // explicit NextPage edge dispatch, never as a pager PageChanged, and
        // the swipe may start anywhere — only the outward direction matters.
        composeTestRule.mainClock.autoAdvance = false
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        val bounds = composeTestRule.onNodeWithTag(ReaderTestTags.Pager)
            .fetchSemanticsNode().boundsInRoot
        val spot = Offset(bounds.width * 0.9f, bounds.height * 0.5f)
        repeat(2) {
            composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
                down(0, spot)
                up(0)
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        actions.clear()

        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
            swipeLeft()
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)

        assert(actions.none { it is ReaderAction.PageChanged })
        actions.clear()

        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
            swipeLeft()
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)

        assert(actions.any { it is ReaderAction.NextPage })
        assert(actions.none { it is ReaderAction.PageChanged })
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun swipeAfterPinchZoomPansInsteadOfTurning() {
        // Pinch out to zoom, then swipe: the pager must ignore the whole
        // multi-touch gesture (only the initial settle may show), and the
        // follow-up swipe pans instead of turning. Zooming back out restores
        // turning, proving gestures still flow.
        composeTestRule.mainClock.autoAdvance = false
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        val bounds = composeTestRule.onNodeWithTag(ReaderTestTags.Pager)
            .fetchSemanticsNode().boundsInRoot
        val left = Offset(bounds.width * 0.4f, bounds.height * 0.5f)
        val right = Offset(bounds.width * 0.6f, bounds.height * 0.5f)
        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
            down(0, left)
            down(1, right)
            // Gradual spread like real fingers (not teleport jumps): six
            // small steps with frame delays between them.
            repeat(6) { step ->
                val f = (step + 1) / 6f
                moveTo(0, left + Offset(-80f * f, 0f), delayMillis = 16)
                moveTo(1, right + Offset(80f * f, 0f), delayMillis = 16)
            }
            up(0)
            up(1)
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        // The pinch itself must never turn: any pager move here stays on 12
        // (the initial settle may or may not emit, hence `all` not equality).
        assert(actions.filterIsInstance<ReaderAction.PageChanged>().all { it.index == 12 })
        actions.clear()

        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
            swipeLeft()
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)

        assert(actions.none { it is ReaderAction.PageChanged })

        // Zoom back out: turning works again, so gestures were panning, not eaten.
        val spot = Offset(bounds.width * 0.9f, bounds.height * 0.5f)
        repeat(2) {
            composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
                down(0, spot)
                up(0)
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        actions.clear()
        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
            swipeLeft()
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)

        assert(actions.any { it is ReaderAction.PageChanged })
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun swipePastPanEdgeTurnsExplicitly() {
        // Zoomed, repeated full swipes: panning absorbs what fits, and pushing
        // past the clamp dispatches an explicit NextPage — never a pager
        // PageChanged, since the pager stands down while zoomed.
        composeTestRule.mainClock.autoAdvance = false
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready(),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        val edgeBounds = composeTestRule.onNodeWithTag(ReaderTestTags.Pager)
            .fetchSemanticsNode().boundsInRoot
        val edgeSpot = Offset(edgeBounds.width * 0.9f, edgeBounds.height * 0.5f)
        repeat(2) {
            composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
                down(0, edgeSpot)
                up(0)
            }
        }
        // Let the double-tap zoom glide finish (frozen clock otherwise).
        composeTestRule.mainClock.advanceTimeBy(1_000)
        actions.clear()

        repeat(4) {
            composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performTouchInput {
                swipeLeft()
            }
            composeTestRule.mainClock.advanceTimeBy(1_000)
        }

        assert(actions.contains(ReaderAction.NextPage))
        assert(actions.none { it is ReaderAction.PageChanged })
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun composeIgnoresVolumeKeys() {
        // Volume paging is owned by the activity interceptor, not
        // composition: key presses here must never navigate, so a press can
        // never double-fire through both paths.
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready().copy(volumeKeys = true),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performKeyInput {
            pressKey(Key.VolumeDown)
        }
        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performKeyInput {
            pressKey(Key.VolumeUp)
        }
        assert(
            actions.none {
                it == ReaderAction.NextPage || it == ReaderAction.PrevPage
            },
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun volumeKeysIgnoredWhenDisabled() {
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ready().copy(volumeKeys = false),
                    onAction = actions::add,
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderTestTags.Pager).performKeyInput {
            pressKey(Key.VolumeDown)
        }

        // The initial pager settle may dispatch PageChanged; volume keys must
        // not navigate.
        assert(
            actions.none {
                it == ReaderAction.NextPage || it == ReaderAction.PrevPage
            },
        )
    }
}
