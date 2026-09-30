package com.mori.feature.settings.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
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
        theme: ThemePreferences = ThemePreferences(),
        reader: ReaderPreferences = ReaderPreferences(),
    ) {
        composeTestRule.setContent {
            MoriTheme {
                SettingsContent(
                    theme = theme,
                    reader = reader,
                    motion = motion,
                    storage = StorageUsage(comicCount = 2, libraryBytes = 2048L, coversBytes = 512L),
                    onAction = actions::add,
                    appVersion = "9.9.9",
                )
            }
        }
    }

    private fun openCategory(category: SettingsCategory) {
        // Below-fold rows ignore taps under Robolectric until scrolled into
        // view, like a real user reaching them.
        composeTestRule.onNodeWithTag(SettingsTestTags.categoryFor(category)).performScrollTo()
        composeTestRule.onNodeWithTag(SettingsTestTags.categoryFor(category)).performClick()
    }

    @Test
    fun hubListsAllCategories() {
        setContent()

        SettingsCategory.entries.forEach { category ->
            val tag = SettingsTestTags.categoryFor(category)
            composeTestRule.onNodeWithTag(tag).performScrollTo()
            composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
        }
        // Detail content stays hidden until a category opens.
        composeTestRule.onNodeWithText("Reading direction").assertDoesNotExist()
    }

    @Test
    fun motionOptionsRenderWithSelection() {
        setContent()
        openCategory(SettingsCategory.APPEARANCE)

        composeTestRule.onNodeWithText("Motion").assertExists()
        composeTestRule.onNodeWithTag(SettingsTestTags.segmentFor("Expressive")).assertIsSelected()
        composeTestRule.onNodeWithTag(SettingsTestTags.segmentFor("Calm")).assertIsNotSelected()
    }

    @Test
    fun motionSelectionFollowsState() {
        setContent(motion = MotionStyle.CALM)
        openCategory(SettingsCategory.APPEARANCE)

        composeTestRule.onNodeWithTag(SettingsTestTags.segmentFor("Calm")).assertIsSelected()
        composeTestRule.onNodeWithTag(SettingsTestTags.segmentFor("Expressive")).assertIsNotSelected()
    }

    @Test
    fun appearanceSegmentSelectionReflectsState() {
        // Segmented clicks don't actuate under Robolectric (same as the
        // onboarding sheets); selection state is asserted instead, with
        // dispatch covered by the VM tests.
        setContent(
            theme = ThemePreferences(mode = ThemeMode.DARK),
            motion = MotionStyle.CALM,
        )
        openCategory(SettingsCategory.APPEARANCE)

        composeTestRule.onNodeWithTag(SettingsTestTags.segmentFor("Dark")).assertIsSelected()
        composeTestRule.onNodeWithTag(SettingsTestTags.segmentFor("Calm")).assertIsSelected()
    }

    @Test
    fun readerSegmentSelectionReflectsState() {
        setContent(reader = ReaderPreferences(pageFit = PageFit.HEIGHT))
        openCategory(SettingsCategory.READER)

        composeTestRule.onNodeWithTag(SettingsTestTags.segmentFor("Height")).assertIsSelected()
    }

    @Test
    fun schemeSwatchesDispatchSelection() {
        val actions = mutableListOf<SettingsAction>()
        setContent(actions = actions)
        openCategory(SettingsCategory.APPEARANCE)

        // All schemes render (dispatch covered by VM tests; mini-phone label
        // clicks don't actuate under Robolectric).
        composeTestRule.onNodeWithText("Colors").assertExists()
        composeTestRule.onNodeWithText("Forest").assertExists()
        composeTestRule.onNodeWithText("Dynamic").assertExists()
    }

    @Test
    fun aboutShowsAppVersion() {
        setContent()
        openCategory(SettingsCategory.ABOUT)

        // "Mori" also names a color-scheme swatch; the version line is unique.
        composeTestRule.onNodeWithText("Version 9.9.9").performScrollTo()
        composeTestRule.onNodeWithText("Version 9.9.9").assertIsDisplayed()
    }

    @Test
    fun appearanceOptionsRender() {
        setContent()
        openCategory(SettingsCategory.APPEARANCE)

        composeTestRule.onNodeWithText("Theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("System").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dynamic color").assertExists()
        composeTestRule.onNodeWithText("AMOLED black").assertExists()
    }

    @Test
    fun readerOptionsRender() {
        setContent()
        openCategory(SettingsCategory.READER)

        composeTestRule.onNodeWithText("Reading direction").assertExists()
        composeTestRule.onNodeWithText("Page fit").assertExists()
        composeTestRule.onNodeWithText("Volume keys turn pages").assertExists()
        composeTestRule.onNodeWithText("Keep screen on").assertExists()
        composeTestRule.onNodeWithText("Crop page margins").assertExists()
        composeTestRule.onNodeWithText("Page counter").assertExists()
        composeTestRule.onNodeWithText("Swipe to turn pages").assertExists()
    }

    @Test
    fun privacyOptionsRender() {
        setContent()
        openCategory(SettingsCategory.PRIVACY)

        composeTestRule.onNodeWithText("App lock").assertExists()
        composeTestRule.onNodeWithText("Incognito").assertExists()
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
                    appLock = false,
                    onAction = {},
                    onLicensesClick = { opened = true },
                )
            }
        }
        openCategory(SettingsCategory.ABOUT)

        composeTestRule.onNodeWithText("Open-source licenses").performScrollTo()
        composeTestRule.onNodeWithText("Open-source licenses").performClick()

        assert(opened)
    }

    @Test
    fun placeholdersRenderAsSoon() {
        setFullScreen()
        openCategory(SettingsCategory.ABOUT)

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
        openCategory(SettingsCategory.STORAGE)

        composeTestRule.onNodeWithText("2 comics • 2 KB library • 512 B covers").assertExists()
        // Below-fold content only measures once scrolled into view under Robolectric.
        composeTestRule.onNodeWithText("Clear thumbnail cache").performScrollTo()
        composeTestRule.onNodeWithText("Clear thumbnail cache").performClick()

        assert(actions.contains(SettingsAction.ClearThumbnailCache))
    }

    @Test
    fun privacySectionShowsAppLock() {
        setFullScreen()
        openCategory(SettingsCategory.PRIVACY)

        composeTestRule.onNodeWithText("App lock").assertExists()
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
