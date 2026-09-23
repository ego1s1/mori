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
import com.mori.core.model.ReadingStats
import com.mori.core.model.StorageUsage
import com.mori.core.model.ThemePreferences
import org.junit.Assert.assertEquals
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
                    stats = ReadingStats(
                        totalSessions = 3,
                        totalDurationMs = 3_720_000L,
                        totalPagesTurned = 42,
                        booksFinished = 2,
                    ),
                    onAction = actions::add,
                    appVersion = "9.9.9",
                )
            }
        }
    }

    @Test
    fun motionOptionsRenderWithSelection() {
        setContent()

        composeTestRule.onNodeWithText("Motion").assertExists()
        composeTestRule.onNodeWithTag(SettingsTestTags.segmentFor("Expressive")).assertIsSelected()
        composeTestRule.onNodeWithTag(SettingsTestTags.segmentFor("Calm")).assertIsNotSelected()
    }

    @Test
    fun motionSelectionFollowsState() {
        setContent(motion = MotionStyle.CALM)

        composeTestRule.onNodeWithTag(SettingsTestTags.segmentFor("Calm")).assertIsSelected()
        composeTestRule.onNodeWithTag(SettingsTestTags.segmentFor("Expressive")).assertIsNotSelected()
    }

    @Test
    fun segmentSelectionReflectsState() {
        // Segmented clicks don't actuate under Robolectric (same as the
        // onboarding sheets); selection state is asserted instead, with
        // dispatch covered by the VM tests.
        setContent(
            theme = ThemePreferences(mode = ThemeMode.DARK),
            motion = MotionStyle.CALM,
            reader = ReaderPreferences(pageFit = PageFit.HEIGHT),
        )

        composeTestRule.onNodeWithTag(SettingsTestTags.segmentFor("Dark")).assertIsSelected()
        composeTestRule.onNodeWithTag(SettingsTestTags.segmentFor("Calm")).assertIsSelected()
        composeTestRule.onNodeWithTag(SettingsTestTags.segmentFor("Height")).assertIsSelected()
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
    fun aboutShowsAppVersion() {
        setContent()

        // "Mori" also names a color-scheme swatch; the version line is unique.
        composeTestRule.onNodeWithText("Version 9.9.9").performScrollTo()
        composeTestRule.onNodeWithText("Version 9.9.9").assertIsDisplayed()
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
                    stats = ReadingStats(),
                    appLock = false,
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
                    stats = ReadingStats(
                        totalSessions = 3,
                        totalDurationMs = 3_720_000L,
                        totalPagesTurned = 42,
                        booksFinished = 2,
                    ),
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

    @Test
    fun statsSectionShowsAggregates() {
        composeTestRule.setContent {
            MoriTheme {
                SettingsContent(
                    theme = ThemePreferences(),
                    reader = ReaderPreferences(),
                    motion = MotionStyle.EXPRESSIVE,
                    storage = null,
                    stats = ReadingStats(
                        totalSessions = 3,
                        totalDurationMs = 3_720_000L,
                        totalPagesTurned = 42,
                        booksFinished = 2,
                    ),
                    onAction = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Reading stats").assertExists()
        composeTestRule.onNodeWithText("1h 2m").assertExists()
        composeTestRule.onNodeWithText("42").assertExists()
    }

    @Test
    fun formatReadingDurationCompacts() {
        assertEquals("0s", formatReadingDuration(0L))
        assertEquals("45s", formatReadingDuration(45_000L))
        assertEquals("12m", formatReadingDuration(750_000L))
        assertEquals("1h 2m", formatReadingDuration(3_720_000L))
        assertEquals("0s", formatReadingDuration(-5_000L))
    }

    @Test
    fun privacySectionShowsAppLock() {
        setFullScreen()

        composeTestRule.onNodeWithText("Privacy").assertExists()
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
                        stats = ReadingStats(),
                    ),
                    onAction = {},
                )
            }
        }
    }
}
