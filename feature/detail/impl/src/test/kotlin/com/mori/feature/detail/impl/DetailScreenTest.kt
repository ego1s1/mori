package com.mori.feature.detail.impl

import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.mori.core.designsystem.MoriTheme
import com.mori.core.model.ComicError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun ready(
        pageCount: Int = 10,
        lastPageIndex: Int = 2,
        error: ComicError? = null,
        confirmRemove: Boolean = false,
    ) = DetailUiState.Ready(
        comic = TestComicsRepository.comic(
            "a",
            title = "Apple",
            pageCount = pageCount,
            lastPageIndex = lastPageIndex,
            error = error,
        ),
        refreshing = false,
        confirmRemove = confirmRemove,
        removed = false,
    )

    private fun setScreen(
        uiState: DetailUiState,
        actions: MutableList<DetailAction> = mutableListOf(),
        onReadClick: (String, Int) -> Unit = { _, _ -> },
    ) {
        composeTestRule.setContent {
            MoriTheme {
                DetailScreen(
                    uiState = uiState,
                    onAction = actions::add,
                    onBackClick = {},
                    onReadClick = onReadClick,
                )
            }
        }
    }

    @Test
    fun bookmarkButtonTogglesFavorite() {
        val actions = mutableListOf<DetailAction>()
        setScreen(ready(), actions = actions)

        composeTestRule.onNodeWithTag(DetailTestTags.BookmarkButton).performClick()

        assert(actions.contains(DetailAction.ToggleBookmark))
    }

    @Test
    fun shareButtonDispatchesShare() {
        val actions = mutableListOf<DetailAction>()
        setScreen(ready(), actions = actions)

        composeTestRule.onNodeWithTag(DetailTestTags.ShareButton).performClick()

        assert(actions.contains(DetailAction.Share))
    }

    @Test
    fun heroShowsTitleAndMetadata() {
        setScreen(ready())

        composeTestRule.onNodeWithTag(DetailTestTags.Hero).assertIsDisplayed()
        // Title lives in the medium app bar and the hero alike.
        composeTestRule.onAllNodesWithText("Apple").assertCountEquals(2)
        composeTestRule.onNodeWithText("10 pages • CBZ").assertIsDisplayed()
        composeTestRule.onNodeWithText("Page 3 of 10").assertIsDisplayed()
    }

    @Test
    fun resumeButtonOpensAtSavedPage() {
        var opened: Pair<String, Int>? = null
        setScreen(ready(), onReadClick = { id, index -> opened = id to index })

        composeTestRule.onNodeWithTag(DetailTestTags.ReadButton).performClick()

        assert(opened == ("a" to 2))
    }

    @Test
    fun freshComicShowsStartReading() {
        setScreen(ready(lastPageIndex = 0))

        composeTestRule.onNodeWithText("Start reading").assertIsDisplayed()
    }

    @Test
    fun pageStripJumpOpensPage() {
        var opened: Pair<String, Int>? = null
        setScreen(ready(), onReadClick = { id, index -> opened = id to index })

        // The strip sits below the fold; scroll it into view first (Robolectric only
        // measures never-visible lazy content once scrolled to).
        composeTestRule.onNodeWithTag(DetailTestTags.PageStrip).performScrollTo()
        composeTestRule.onNodeWithTag(DetailTestTags.pageChip(0)).performClick()

        assert(opened == ("a" to 0))
    }

    @Test
    fun errorCardOffersRetryAndRemove() {
        val actions = mutableListOf<DetailAction>()
        setScreen(ready(error = ComicError.CORRUPT), actions = actions)

        composeTestRule.onNodeWithTag(DetailTestTags.ErrorCard).assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()
        composeTestRule.onNodeWithText("Remove").performClick()

        assert(actions.contains(DetailAction.Refresh))
        assert(actions.contains(DetailAction.AskRemove))
    }

    @Test
    fun removeFlowShowsDialogAndConfirms() {
        val actions = mutableListOf<DetailAction>()
        setScreen(ready(confirmRemove = true), actions = actions)

        composeTestRule.onNodeWithTag(DetailTestTags.RemoveDialog).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DetailTestTags.ConfirmRemove).performClick()

        assert(actions.contains(DetailAction.ConfirmRemove))
    }

    @Test
    fun missingStateShowsMessageAndBack() {
        val backs = mutableListOf<Unit>()
        composeTestRule.setContent {
            MoriTheme {
                DetailScreen(
                    uiState = DetailUiState.Missing,
                    onAction = {},
                    onBackClick = { backs += Unit },
                    onReadClick = { _, _ -> },
                )
            }
        }

        composeTestRule.onNodeWithText("This comic was removed.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back to library").performClick()
        assertEquals(1, backs.size)
    }
}
