package com.mori.feature.library.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.mori.core.designsystem.MoriTheme
import com.mori.core.model.LibraryQuery
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibraryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun success(
        query: LibraryQuery = LibraryQuery(),
        refreshing: Boolean = false,
        filterOpen: Boolean = false,
        snackbar: String? = null,
    ) = LibraryUiState.Success(
        comics = listOf(
            TestComicsRepository.comic("a", title = "Apple"),
            TestComicsRepository.comic("b", title = "Banana", lastPageIndex = 2, pageCount = 10),
        ),
        query = query,
        refreshing = refreshing,
        filterOpen = filterOpen,
        snackbar = snackbar,
    )

    private fun setScreen(
        uiState: LibraryUiState,
        actions: MutableList<LibraryAction> = mutableListOf(),
        onComicClick: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            MoriTheme {
                LibraryScreen(
                    uiState = uiState,
                    onAction = actions::add,
                    onComicClick = onComicClick,
                )
            }
        }
    }

    @Test
    fun loadingShowsSpinner() {
        setScreen(LibraryUiState.Loading)
        composeTestRule.onNodeWithText("Library").assertIsDisplayed()
    }

    @Test
    fun gridShowsCardsWithTitles() {
        setScreen(success())

        composeTestRule.onNodeWithTag(LibraryTestTags.Grid).assertIsDisplayed()
        composeTestRule.onNodeWithText("Apple").assertIsDisplayed()
        composeTestRule.onNodeWithText("Banana").assertIsDisplayed()
        composeTestRule.onNodeWithText("2").assertIsDisplayed() // count pill
    }

    @Test
    fun cardClickOpensComic() {
        var opened: String? = null
        setScreen(success(), onComicClick = { opened = it })

        composeTestRule.onNodeWithTag(LibraryTestTags.cardFor("b")).performClick()

        assert(opened == "b")
    }

    @Test
    fun emptyStateShowsMessageAndRescan() {
        val actions = mutableListOf<LibraryAction>()
        setScreen(success().copy(comics = emptyList()), actions = actions)

        composeTestRule.onNodeWithTag(LibraryTestTags.EmptyState).assertIsDisplayed()
        composeTestRule.onNodeWithText("Your library is empty").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rescan library").performClick()

        assert(actions.contains(LibraryAction.Refresh))
    }

    @Test
    fun searchFieldDispatchesText() {
        val actions = mutableListOf<LibraryAction>()
        setScreen(success(), actions = actions)

        composeTestRule.onNodeWithTag(LibraryTestTags.SearchToggle).performClick()
        composeTestRule.onNodeWithTag(LibraryTestTags.SearchField).performTextInput("app")

        assert(actions.any { it is LibraryAction.SearchTextChanged && it.text == "app" })
    }

    @Test
    fun filterButtonDispatchesOpenFilter() {
        val actions = mutableListOf<LibraryAction>()
        setScreen(success(), actions = actions)

        composeTestRule.onNodeWithTag(LibraryTestTags.FilterButton).performClick()

        assert(actions.contains(LibraryAction.OpenFilter))
    }

    @Test
    fun filterSheetContentRendersOptions() {
        composeTestRule.setContent {
            MoriTheme {
                LibrarySortFilterContent(
                    query = LibraryQuery(),
                    onAction = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Filter").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sort by").assertIsDisplayed()
        composeTestRule.onNodeWithText("In progress").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recently added").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hide unreadable comics").assertIsDisplayed()
    }

    @Test
    fun filterChipDispatchesSelection() {
        val actions = mutableListOf<LibraryAction>()
        composeTestRule.setContent {
            MoriTheme {
                LibrarySortFilterContent(
                    query = LibraryQuery(),
                    onAction = actions::add,
                )
            }
        }

        composeTestRule.onNodeWithText("Finished").performClick()

        assert(actions.any { it is LibraryAction.FilterSelected })
    }
}
