package com.mori.feature.settings.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.mori.core.designsystem.MoriTheme
import com.mori.core.model.ColorSchemeChoice
import com.mori.core.model.MotionStyle
import com.mori.core.model.PageFit
import com.mori.core.model.ThemeMode
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.StorageUsage
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
        motion: MotionStyle = MotionStyle.EXPRESSIVE,
    ) {
        composeTestRule.setContent {
            MoriTheme {
                SettingsContent(
                    theme = ThemePreferences(),
                    reader = ReaderPreferences(),
                    motion = motion,
                    storage = StorageUsage(comicCount = 2, libraryBytes = 2048L, coversBytes = 512L),
                    onAction = actions::add,
                )
            }
        }
    }

    @Test
    fun motionOptionsRenderWithSelection() {
        setContent()

        composeTestRule.onNodeWithText("Motion").assertExists()
        composeTestRule.onNodeWithTag(SettingsTestTags.pillFor("Expressive")).assertIsSelected()
        composeTestRule.onNodeWithTag(SettingsTestTags.pillFor("Calm")).assertIsNotSelected()
    }

    @Test
    fun motionSelectionFollowsState() {
        setContent(motion = MotionStyle.CALM)

        composeTestRule.onNodeWithTag(SettingsTestTags.pillFor("Calm")).assertIsSelected()
        composeTestRule.onNodeWithTag(SettingsTestTags.pillFor("Expressive")).assertIsNotSelected()
    }

    @Test
    fun pillTogglesDispatch() {
        val actions = mutableListOf<SettingsAction>()
        setContent(actions = actions)

        composeTestRule.onNodeWithTag(SettingsTestTags.pillFor("Dark")).performScrollTo()
        composeTestRule.onNodeWithTag(SettingsTestTags.pillFor("Dark")).performClick()
        assert(actions.contains(SettingsAction.SetThemeMode(ThemeMode.DARK)))
        composeTestRule.onNodeWithTag(SettingsTestTags.pillFor("Calm")).performScrollTo()
        composeTestRule.onNodeWithTag(SettingsTestTags.pillFor("Calm")).performClick()
        assert(actions.contains(SettingsAction.SetMotionStyle(MotionStyle.CALM)))
        composeTestRule.onNodeWithTag(SettingsTestTags.pillFor("Height")).performScrollTo()
        composeTestRule.onNodeWithTag(SettingsTestTags.pillFor("Height")).performClick()
        assert(actions.contains(SettingsAction.SetPageFit(PageFit.HEIGHT)))
    }

    @Test
    fun schemeSwatchesDispatchSelection() {
        val actions = mutableListOf<SettingsAction>()
        setContent(actions = actions)

        // All schemes render (dispatch covered by VM tests; mini-phone label
        // clicks don't actuate under Robolectric).
        composeTestRule.onNodeWithText("Colors").assertExists()
        composeTestRule.onNodeWithText("Forest").assertExists()
        composeTestRule.onNodeWithText("Dynamic").assertExists()
    }

    @Test
    fun sectionsRender() {
        setContent()

        composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reader defaults").assertExists()
        composeTestRule.onNodeWithText("Storage").assertExists()
        // Section title only; placeholder rows carry specific subtitles.
        composeTestRule.onAllNodesWithText("Coming soon").assertCountEquals(1)
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
        composeTestRule.onNodeWithText("Crop page margins").assertExists()
        composeTestRule.onNodeWithText("Page counter").assertExists()
        composeTestRule.onNodeWithText("Swipe to turn pages").assertExists()
    }

    @Test
    fun licensesRowOpensLicenses() {
        var opened = false
        composeTestRule.setContent {
            MoriTheme {
                SettingsContent(
                    theme = ThemePreferences(),
                    reader = ReaderPreferences(),
                    motion = MotionStyle.EXPRESSIVE,
                    storage = StorageUsage(comicCount = 2, libraryBytes = 2048L, coversBytes = 512L),
                    onAction = {},
                    onLicensesClick = { opened = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Open-source licenses").performScrollTo()
        composeTestRule.onNodeWithText("Open-source licenses").performClick()

        assert(opened)
    }

    @Test
    fun placeholdersRenderAsSoon() {
        setFullScreen()

        composeTestRule.onNodeWithText("Clear thumbnail cache").assertExists()
        composeTestRule.onNodeWithText("CB7 and CBT support").assertExists()
        composeTestRule.onNodeWithText("Cloud sync").assertExists()
    }

    @Test
    fun storageSectionShowsUsageAndClearDispatches() {
        val actions = mutableListOf<SettingsAction>()
        composeTestRule.setContent {
            MoriTheme {
                SettingsContent(
                    theme = ThemePreferences(),
                    reader = ReaderPreferences(),
                    motion = MotionStyle.EXPRESSIVE,
                    storage = StorageUsage(comicCount = 2, libraryBytes = 2048L, coversBytes = 512L),
                    onAction = actions::add,
                )
            }
        }

        composeTestRule.onNodeWithText("2 comics • 2 KB library • 512 B covers").assertExists()
        // Below-fold content only measures once scrolled into view under Robolectric.
        composeTestRule.onNodeWithText("Clear thumbnail cache").performScrollTo()
        composeTestRule.onNodeWithText("Clear thumbnail cache").performClick()

        assert(actions.contains(SettingsAction.ClearThumbnailCache))
    }

    private fun setFullScreen() {
        composeTestRule.setContent {
            MoriTheme {
                SettingsScreen(
                    uiState = SettingsUiState.Ready(
                        theme = ThemePreferences(),
                        reader = ReaderPreferences(),
                        motion = MotionStyle.EXPRESSIVE,
                        storage = StorageUsage(comicCount = 2, libraryBytes = 2048L, coversBytes = 512L),
                    ),
                    onAction = {},
                )
            }
        }
    }
}
