package com.mori.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.mori.core.designsystem.MoriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainNavigatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setPill(
        selectedTab: Int = 0,
        onSelectTab: (Int) -> Unit = {},
    ) {
        composeTestRule.setContent {
            MoriTheme {
                MainNavigator(
                    selectedTab = selectedTab,
                    onSelectTab = onSelectTab,
                    scrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
                        FloatingToolbarExitDirection.Bottom,
                    ),
                )
            }
        }
    }

    @Test
    fun selectedTabHighlighted() {
        setPill(selectedTab = 0)

        composeTestRule.onNodeWithTag(MainTestTags.Navigator).assertExists()
        composeTestRule.onNodeWithTag(MainTestTags.LibraryTab).assertIsSelected()
        composeTestRule.onNodeWithTag(MainTestTags.StatsTab).assertIsNotSelected()
        composeTestRule.onNodeWithTag(MainTestTags.SettingsTab).assertIsNotSelected()
    }

    @Test
    fun statsSitsBetweenLibraryAndSettings() {
        val order = mutableListOf<Int>()
        setPill(selectedTab = 1, onSelectTab = order::add)

        composeTestRule.onNodeWithTag(MainTestTags.StatsTab).assertIsSelected()
        composeTestRule.onNodeWithTag(MainTestTags.SettingsTab).performClick()

        assertEquals(listOf(2), order)
    }

    @Test
    fun tappingSettingsSelectsIt() {
        var selected = 0
        setPill(selectedTab = 0, onSelectTab = { selected = it })

        composeTestRule.onNodeWithTag(MainTestTags.SettingsTab).performClick()

        assertEquals(2, selected)
    }

    @Test
    fun resumeButtonDispatchesWithoutLabel() {
        var resumed = false
        composeTestRule.setContent {
            MoriTheme {
                ResumeButton(title = "Saga", onClick = { resumed = true })
            }
        }

        // Icon-only circle: no text, but the action dispatches.
        composeTestRule.onNodeWithTag(MainTestTags.ResumeAction).assertExists()
        composeTestRule.onNodeWithTag(MainTestTags.ResumeAction).performClick()

        assertEquals(true, resumed)
    }
}
