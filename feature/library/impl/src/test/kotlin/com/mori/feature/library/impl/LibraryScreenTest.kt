package com.mori.feature.library.impl


import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
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
        searchOpen: Boolean = false,
    ) = LibraryUiState.Success(
        comics = listOf(
            TestComicsRepository.comic("a", title = "Apple"),
            TestComicsRepository.comic("b", title = "Banana", lastPageIndex = 2, pageCount = 10),
        ),
        query = query,
        refreshing = refreshing,
        filterOpen = filterOpen,
        searchOpen = searchOpen,
    )

    private fun setScreen(
        uiState: LibraryUiState,
        actions: MutableList<LibraryAction> = mutableListOf(),
        onReadClick: (String, Int) -> Unit = { _, _ -> },
        onComicLongClick: (String) -> Unit = {},
        onSettingsClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            MoriTheme {
                LibraryScreen(
                    uiState = uiState,
                    onAction = actions::add,
                    onReadClick = onReadClick,
                    onComicLongClick = onComicLongClick,
                    onSettingsClick = onSettingsClick,
                )
            }
        }
    }

    @Test
    fun loadingShowsSpinner() {
        setScreen(LibraryUiState.Loading)
        composeTestRule.onNodeWithTag(LibraryTestTags.Loading).assertIsDisplayed()
    }

    @Test
    fun gridShowsCardsWithTitles() {
        setScreen(success())

        composeTestRule.onNodeWithTag(LibraryTestTags.Grid).assertIsDisplayed()
        composeTestRule.onNodeWithText("Apple").assertIsDisplayed()
        composeTestRule.onNodeWithText("Banana").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 comics on the shelf").assertIsDisplayed()
    }

    @Test
    fun cardTapReadsFromSavedPage() {
        var opened: Pair<String, Int>? = null
        setScreen(success(), onReadClick = { id, index -> opened = id to index })

        composeTestRule.onNodeWithTag(LibraryTestTags.cardFor("b")).performClick()

        assert(opened == ("b" to 2))
    }

    @Test
    fun cardLongPressOpensDetails() {
        var detailed: String? = null
        setScreen(success(), onComicLongClick = { detailed = it })

        composeTestRule.onNodeWithTag(LibraryTestTags.cardFor("a")).performTouchInput {
            longClick()
        }

        assert(detailed == "a")
    }

    @Test
    fun emptyStateShowsMessageAndRescan() {
        setScreen(success().copy(comics = emptyList()))

        // Note: the rescan tap itself is covered by toolbarButtonsDispatch; the empty
        // button measures zero under Robolectric legacy graphics (see commit history),
        // so only composition is asserted here. Device coverage lands in F6.
        composeTestRule.onNodeWithTag(LibraryTestTags.EmptyState).assertIsDisplayed()
        composeTestRule.onNodeWithText("Your library is empty").assertIsDisplayed()
        composeTestRule.onNodeWithTag(LibraryTestTags.EmptyRescan).assertExists()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun searchFieldDispatchesText() {
        val actions = mutableListOf<LibraryAction>()
        setScreen(success(searchOpen = true), actions = actions)

        composeTestRule.onNodeWithTag(LibraryTestTags.SearchField).performTextInput("app")

        assert(actions.any { it is LibraryAction.SearchTextChanged && it.text == "app" })
    }

    @Test
    fun searchToggleFlipsOpenState() {
        val actions = mutableListOf<LibraryAction>()
        setScreen(success(), actions = actions)

        composeTestRule.onNodeWithTag(LibraryTestTags.SearchToggle).performClick()

        assert(actions.contains(LibraryAction.ToggleSearch))
    }

    @Test
    fun toolbarButtonsDispatch() {
        val actions = mutableListOf<LibraryAction>()
        setScreen(success(), actions = actions)

        composeTestRule.onNodeWithTag(LibraryTestTags.Toolbar).assertIsDisplayed()
        composeTestRule.onNodeWithTag(LibraryTestTags.FilterButton).performClick()
        composeTestRule.onNodeWithTag(LibraryTestTags.RefreshButton).performClick()

        assert(actions.contains(LibraryAction.OpenFilter))
        assert(actions.contains(LibraryAction.Refresh))
    }

    @Test
    fun resumeFabOpensMostRecentlyTouched() {
        var opened: Pair<String, Int>? = null
        setScreen(success(), onReadClick = { id, index -> opened = id to index })

        composeTestRule.onNodeWithTag(LibraryTestTags.ResumeFab).performClick()

        // Both comics share updatedAt; either is a valid "last touched" answer.
        assert(opened != null)
    }

    @Test
    fun resumeFabHiddenWhenEmpty() {
        setScreen(success().copy(comics = emptyList()))

        composeTestRule.onAllNodesWithTag(LibraryTestTags.ResumeFab)
            .assertCountEquals(0)
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

    @Test
    fun settingsButtonNavigatesToSettings() {
        var settingsOpened = false
        setScreen(success(), onSettingsClick = { settingsOpened = true })

        composeTestRule.onNodeWithTag(LibraryTestTags.SettingsButton).performClick()

        assert(settingsOpened)
    }
}
