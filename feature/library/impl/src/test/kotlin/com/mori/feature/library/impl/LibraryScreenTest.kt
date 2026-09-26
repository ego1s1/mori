package com.mori.feature.library.impl


import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.mori.core.designsystem.MoriTheme
import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibraryQuery
import com.mori.core.model.UserCollection
import com.mori.core.model.ComicError
import com.mori.core.testing.FakeComicsRepository
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
        linked: Boolean = true,
    ): LibraryUiState.Success {
        val comics = listOf(
            FakeComicsRepository.comic("a", title = "Apple"),
            FakeComicsRepository.comic("b", title = "Banana", lastPageIndex = 2, pageCount = 10),
        )
        return LibraryUiState.Success(
            comics = comics,
            query = query,
            refreshing = refreshing,
            filterOpen = filterOpen,
            searchOpen = searchOpen,
            linked = linked,
        )
    }

    private fun setScreen(
        uiState: LibraryUiState,
        actions: MutableList<LibraryAction> = mutableListOf(),
        onReadClick: (String, Int) -> Unit = { _, _ -> },
        onComicLongClick: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            MoriTheme {
                LibraryScreen(
                    uiState = uiState,
                    onAction = actions::add,
                    onReadClick = onReadClick,
                    onComicLongClick = onComicLongClick,
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
        // The continue shelf pushes cards below the short test viewport fold.
        composeTestRule.onNodeWithTag(LibraryTestTags.Grid).performTouchInput {
            swipeUp()
        }
        composeTestRule.onNodeWithTag(LibraryTestTags.cardFor("a")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(LibraryTestTags.cardFor("b")).assertIsDisplayed()
        composeTestRule.onNodeWithText("Library").assertIsDisplayed()
    }

    @Test
    fun inProgressCardShowsPagesLeftBadge() {
        setScreen(success())

        // Banana: lastPageIndex 2 of 10 -> 7 pages left. The grid copy
        // needs a swipe (short test viewport fold).
        composeTestRule.onNodeWithTag(LibraryTestTags.Grid).performTouchInput {
            swipeUp()
        }
        composeTestRule.onNodeWithTag(LibraryTestTags.cardFor("b")).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("7 left").assertCountEquals(1)
    }


    @Test
    fun shelfFilterRendersInSheet() {
        setScreen(
            success(filterOpen = true).copy(
                collections = listOf(
                    UserCollection(id = 7L, name = "Picks", bookCount = 1, createdAt = 1L),
                ),
            ),
        )

        // Full-screen wiring: the sheet carries the shelf chips now that
        // the grid row is gone.
        composeTestRule.onNodeWithTag(LibraryTestTags.CollectionRow).assertIsDisplayed()
        composeTestRule.onNodeWithText("Picks (1)").assertIsDisplayed()
    }

    @Test
    fun shelfChipInSheetDispatchesSelection() {
        // Dispatch is covered at content level: modal clicks measure zero
        // under Robolectric legacy graphics (see emptyStateShowsMessage),
        // so full-screen only asserts composition above.
        val actions = mutableListOf<LibraryAction>()
        composeTestRule.setContent {
            MoriTheme {
                LibrarySortFilterContent(
                    query = LibraryQuery(),
                    onAction = actions::add,
                    collections = listOf(
                        UserCollection(id = 7L, name = "Picks", bookCount = 1, createdAt = 1L),
                    ),
                )
            }
        }

        composeTestRule.onNodeWithTag(LibraryTestTags.collectionChip(7L)).performClick()
        assert(actions.contains(LibraryAction.SelectCollection(7L)))
    }

    @Test
    fun bookmarkedCardShowsFavoriteBadge() {
        val actions = mutableListOf<LibraryAction>()
        setScreen(
            success().copy(
                comics = listOf(
                    FakeComicsRepository.comic("a", title = "Apple").copy(bookmarked = true),
                ),
            ),
            actions = actions,
        )

        composeTestRule.onNodeWithTag(LibraryTestTags.Grid).performTouchInput {
            swipeUp()
        }
        composeTestRule.onNodeWithTag(LibraryTestTags.cardFor("a")).assertIsDisplayed()
        // Unmerged: the clickable card merges badge semantics upward.
        composeTestRule.onNodeWithTag(
            LibraryTestTags.bookmarkBadgeFor("a"),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun cardTapReadsFromSavedPage() {
        var opened: Pair<String, Int>? = null
        setScreen(success(), onReadClick = { id, index -> opened = id to index })

        composeTestRule.onNodeWithTag(LibraryTestTags.Grid).performTouchInput {
            swipeUp()
        }
        composeTestRule.onNodeWithTag(LibraryTestTags.cardFor("b")).performClick()

        assert(opened == ("b" to 2))
    }

    @Test
    fun finishedBookRestartsAtZero() {
        var opened: Pair<String, Int>? = null
        val done = FakeComicsRepository.comic("d", title = "Done", pageCount = 10, lastPageIndex = 9)
        setScreen(success().copy(comics = listOf(done)), onReadClick = { id, index -> opened = id to index })

        composeTestRule.onNodeWithTag(LibraryTestTags.Grid).performTouchInput {
            swipeUp()
        }
        composeTestRule.onNodeWithTag(LibraryTestTags.cardFor("d")).performClick()

        assert(opened == ("d" to 0))
    }

    @Test
    fun errorCardTapOpensDetailsInsteadOfReading() {
        var opened: Pair<String, Int>? = null
        var detailed: String? = null
        val broken = FakeComicsRepository.comic("e", title = "Broken", error = ComicError.CORRUPT)
        setScreen(
            success().copy(comics = listOf(broken)),
            onReadClick = { id, index -> opened = id to index },
            onComicLongClick = { detailed = it },
        )

        composeTestRule.onNodeWithTag(LibraryTestTags.Grid).performTouchInput {
            swipeUp()
        }
        composeTestRule.onNodeWithTag(LibraryTestTags.cardFor("e")).performClick()

        assert(opened == null)
        assert(detailed == "e")
    }

    @Test
    fun cardLongPressOpensDetails() {
        var detailed: String? = null
        setScreen(success(), onComicLongClick = { detailed = it })

        composeTestRule.onNodeWithTag(LibraryTestTags.Grid).performTouchInput {
            swipeUp()
        }
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

    @Test
    fun emptyStateUnlinkedOffersChooseFolder() {
        setScreen(success(linked = false).copy(comics = emptyList()))

        composeTestRule.onNodeWithTag(LibraryTestTags.EmptyState).assertIsDisplayed()
        composeTestRule.onNodeWithTag(LibraryTestTags.EmptyChooseFolder).assertExists()
        composeTestRule.onNodeWithTag(LibraryTestTags.EmptyRescan).assertDoesNotExist()
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
    fun filterButtonOpensFilterSheet() {
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
                    collections = listOf(
                        UserCollection(id = 7L, name = "Picks", bookCount = 1, createdAt = 1L),
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("Shelf").assertIsDisplayed()
        // Both the shelf section and the status filter offer "All".
        composeTestRule.onAllNodesWithText("All").assertCountEquals(2)
        composeTestRule.onNodeWithText("Picks (1)").assertIsDisplayed()
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
