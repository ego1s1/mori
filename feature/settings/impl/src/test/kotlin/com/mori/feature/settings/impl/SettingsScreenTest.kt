package com.mori.feature.settings.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mori.core.designsystem.MoriTheme
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.ThemePreferences
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        actions: MutableList<SettingsAction> = mutableListOf(),
    ) {
        composeTestRule.setContent {
            MoriTheme {
                SettingsContent(
                    theme = ThemePreferences(),
                    reader = ReaderPreferences(),
                    onAction = actions::add,
                )
            }
        }
    }

    @Test
    fun sectionsRender() {
        setContent()

        composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reader defaults").assertExists()
        composeTestRule.onNodeWithText("Storage").assertExists()
        // Section title plus two placeholder subtitles.
        composeTestRule.onAllNodesWithText("Coming soon").assertCountEquals(3)
        composeTestRule.onNodeWithText("About").assertExists()
    }

    @Test
    fun appearanceOptionsRender() {
        setContent()

        composeTestRule.onNodeWithText("Theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("System").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dynamic color").assertExists()
        composeTestRule.onNodeWithText("AMOLED black").assertExists()
    }

    @Test
    fun readerOptionsRender() {
        setContent()

        composeTestRule.onNodeWithText("Reading direction").assertExists()
        composeTestRule.onNodeWithText("Page fit").assertExists()
        composeTestRule.onNodeWithText("Volume keys turn pages").assertExists()
        composeTestRule.onNodeWithText("Keep screen on").assertExists()
    }

    @Test
    fun placeholdersRenderAsSoon() {
        setFullScreen()

        composeTestRule.onNodeWithText("Clear thumbnail cache").assertExists()
        composeTestRule.onNodeWithText("CB7 and CBT support").assertExists()
        composeTestRule.onNodeWithText("Cloud sync").assertExists()
        composeTestRule.onNodeWithText("OCR text layer").assertExists()
    }

    private fun setFullScreen() {
        composeTestRule.setContent {
            MoriTheme {
                SettingsScreen(
                    uiState = SettingsUiState.Ready(
                        theme = ThemePreferences(),
                        reader = ReaderPreferences(),
                    ),
                    onAction = {},
                    onBackClick = {},
                )
            }
        }
    }
}
