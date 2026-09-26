package com.mori.feature.history.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.mori.core.designsystem.MoriTheme
import com.mori.core.model.Comic
import com.mori.core.model.ComicFormat
import com.mori.core.model.HistoryDay
import com.mori.core.model.dayStartMillis
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun comic(id: String, title: String, lastPageIndex: Int) = Comic(
        id = id,
        title = title,
        series = null,
        number = null,
        format = ComicFormat.CBZ,
        pageCount = 10,
        sourcePath = "/lib/$id.cbz",
        coverPath = null,
        lastPageIndex = lastPageIndex,
        sourceDisplayName = "$id.cbz",
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun emptyHistoryShowsEmptyState() {
        composeTestRule.setContent {
            MoriTheme {
                HistoryScreen(
                    uiState = HistoryUiState.Success(emptyList(), queryText = ""),
                    onAction = {},
                    onReadClick = { _, _ -> },
                    onComicLongClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(HistoryTestTags.EmptyState).assertIsDisplayed()
    }

    @Test
    fun rowsRenderAndTapResumesAtSavedPage() {
        val reads = mutableListOf<Pair<String, Int>>()
        composeTestRule.setContent {
            MoriTheme {
                HistoryScreen(
                    uiState = HistoryUiState.Success(
                        days = listOf(
                            HistoryDay(
                                dayStartMillis = dayStartMillis(System.currentTimeMillis()),
                                comics = listOf(comic("a", "Apple Saga", lastPageIndex = 4)),
                            ),
                        ),
                        queryText = "",
                    ),
                    onAction = {},
                    onReadClick = { id, index -> reads += id to index },
                    onComicLongClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Apple Saga").assertIsDisplayed()
        composeTestRule.onNodeWithTag(HistoryTestTags.rowFor("a")).performClick()

        assert(reads == listOf("a" to 4))
    }

    @Test
    fun successListRendersListTag() {
        composeTestRule.setContent {
            MoriTheme {
                HistoryScreen(
                    uiState = HistoryUiState.Success(
                        days = listOf(
                            HistoryDay(
                                dayStartMillis = dayStartMillis(System.currentTimeMillis()),
                                comics = listOf(comic("a", "Apple Saga", lastPageIndex = 4)),
                            ),
                        ),
                        queryText = "",
                    ),
                    onAction = {},
                    onReadClick = { _, _ -> },
                    onComicLongClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(HistoryTestTags.List).assertIsDisplayed()
        composeTestRule.onNodeWithText("Apple Saga").assertIsDisplayed()
    }

    @Test
    fun searchToggleDispatchesToggleSearch() {
        val actions = mutableListOf<HistoryAction>()
        composeTestRule.setContent {
            MoriTheme {
                HistoryScreen(
                    uiState = HistoryUiState.Success(emptyList(), queryText = ""),
                    onAction = actions::add,
                    onReadClick = { _, _ -> },
                    onComicLongClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(HistoryTestTags.SearchToggle).performClick()

        assert(actions.contains(HistoryAction.ToggleSearch))
    }

    @Test
    fun searchFieldDispatchesTextWhenOpen() {
        val actions = mutableListOf<HistoryAction>()
        composeTestRule.setContent {
            MoriTheme {
                HistoryScreen(
                    uiState = HistoryUiState.Success(
                        emptyList(),
                        queryText = "",
                        searchOpen = true,
                    ),
                    onAction = actions::add,
                    onReadClick = { _, _ -> },
                    onComicLongClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(HistoryTestTags.SearchField).assertIsDisplayed()
        composeTestRule.onNodeWithTag(HistoryTestTags.SearchField).performTextInput("app")

        assert(actions.any { it is HistoryAction.SearchTextChanged && it.text == "app" })
    }
}
