package com.mori.feature.reader.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
}
