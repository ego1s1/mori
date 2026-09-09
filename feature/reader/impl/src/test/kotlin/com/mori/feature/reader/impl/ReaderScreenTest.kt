package com.mori.feature.reader.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.mori.core.designsystem.MoriTheme
import com.mori.core.model.PageFit
import com.mori.core.model.ReadingDirection
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
        volumeKeys = false,
        keepScreenOn = true,
        showTapZones = false,
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
    fun settingsSheetContentShowsOptions() {
        composeTestRule.setContent {
            MoriTheme {
                ReaderSettingsSheetContent(
                    direction = ReadingDirection.LEFT_TO_RIGHT,
                    pageFit = PageFit.WIDTH,
                    cropMargins = false,
                    volumeKeys = false,
                    keepScreenOn = true,
                    showTapZones = false,
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

    @Test
    fun errorStateShowsMessage() {
        composeTestRule.setContent {
            MoriTheme {
                ReaderScreen(
                    uiState = ReaderUiState.Error("Archive is corrupt"),
                    onAction = {},
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Archive is corrupt").assertIsDisplayed()
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

        composeTestRule.onNodeWithTag(ReaderTestTags.Slider).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ReaderTestTags.Prev).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderTestTags.Next).assertIsDisplayed()
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
}
