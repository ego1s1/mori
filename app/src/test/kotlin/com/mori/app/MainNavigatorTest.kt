package com.mori.app

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.mori.core.designsystem.MoriTheme
import com.mori.core.model.ResumeTarget
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainNavigatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setPill(
        selectedTab: Int = 0,
        onSelectTab: (Int) -> Unit = {},
        resume: ResumeTarget? = null,
        onResumeClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            MoriTheme {
                MainNavigator(
                    selectedTab = selectedTab,
                    onSelectTab = onSelectTab,
                    resume = resume,
                    onResumeClick = onResumeClick,
                )
            }
        }
    }

    @Test
    fun selectedTabHighlighted() {
        setPill(selectedTab = 0)

        composeTestRule.onNodeWithTag(MainTestTags.Navigator).assertExists()
        composeTestRule.onNodeWithTag(MainTestTags.LibraryTab).assertIsSelected()
        composeTestRule.onNodeWithTag(MainTestTags.SettingsTab).assertIsNotSelected()
    }

    @Test
    fun tappingSettingsSelectsIt() {
        var selected = 0
        setPill(selectedTab = 0, onSelectTab = { selected = it })

        composeTestRule.onNodeWithTag(MainTestTags.SettingsTab).performClick()

        assertEquals(1, selected)
    }

    @Test
    fun resumeActionShowsAndDispatches() {
        var resumed = false
        setPill(
            resume = ResumeTarget("c", 4, "Saga"),
            onResumeClick = { resumed = true },
        )

        composeTestRule.onNodeWithTag(MainTestTags.ResumeAction).assertExists()
        composeTestRule.onNodeWithTag(MainTestTags.ResumeAction).performClick()

        assertEquals(true, resumed)
    }

    @Test
    fun resumeHiddenWithoutTarget() {
        setPill(resume = null)

        composeTestRule.onNodeWithTag(MainTestTags.ResumeAction).assertDoesNotExist()
    }
}
