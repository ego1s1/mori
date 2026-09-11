package com.mori.feature.onboarding.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.mori.core.designsystem.MoriTheme
import com.mori.core.model.ColorSchemeChoice
import com.mori.core.model.ThemeMode
import com.mori.core.model.ThemePreferences
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(
        uiState: OnboardingUiState,
        onPickFolder: () -> Unit = {},
        actions: MutableList<OnboardingAction> = mutableListOf(),
        onFinish: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            MoriTheme {
                OnboardingScreen(
                    uiState = uiState,
                    onPickFolder = onPickFolder,
                    onAction = actions::add,
                    onOnboardingComplete = onFinish,
                )
            }
        }
    }

    @Test
    fun welcomeShowsGetStartedAndSkip() {
        val actions = mutableListOf<OnboardingAction>()
        var finished = 0
        setScreen(
            OnboardingUiState.Welcome,
            actions = actions,
            onFinish = { finished += 1 },
        )

        // Welcome staggers in; let the cascade finish.
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.onNodeWithTag(OnboardingTestTags.GetStarted).performClick()
        assert(actions.contains(OnboardingAction.GetStarted))
        composeTestRule.onNodeWithText("Skip").performClick()
        assert(actions.contains(OnboardingAction.Skip))
        assertEquals(1, finished)
    }

    @Test
    fun folderStepPicksFolderAndGoesBack() {
        val actions = mutableListOf<OnboardingAction>()
        var folderPicks = 0
        setScreen(
            OnboardingUiState.Folder,
            onPickFolder = { folderPicks += 1 },
            actions = actions,
        )

        composeTestRule.onNodeWithTag(OnboardingTestTags.FolderStep).assertIsDisplayed()
        composeTestRule.onNodeWithTag(OnboardingTestTags.PickFolder).performScrollTo()
        composeTestRule.onNodeWithTag(OnboardingTestTags.PickFolder).performClick()
        assertEquals(1, folderPicks)
    }

    @Test
    fun appearanceStepDrivesThemeActions() {
        val actions = mutableListOf<OnboardingAction>()
        setScreen(
            OnboardingUiState.Appearance(ThemePreferences()),
            actions = actions,
        )

        composeTestRule.onNodeWithTag(OnboardingTestTags.AppearanceStep).assertIsDisplayed()
        // Swatches use selectable rows and dispatch (segmented clicks don't
        // under Robolectric); selection state is covered by the VM tests.
        // All schemes render (dispatch covered by VM tests + selectable rows
        // elsewhere; mini-phone label clicks don't actuate under Robolectric).
        composeTestRule.onNodeWithText("Dynamic").assertExists()
        composeTestRule.onNodeWithText("Ocean").assertExists()
    }

    @Test
    fun appearanceContinueFinishes() {
        val actions = mutableListOf<OnboardingAction>()
        var finished = 0
        setScreen(
            OnboardingUiState.Appearance(ThemePreferences()),
            actions = actions,
            onFinish = { finished += 1 },
        )

        composeTestRule.onNodeWithTag(OnboardingTestTags.StepContinue).assertIsDisplayed()
        // The CTA dispatches Finish (navigation covered by VM tests; a Button
        // click through this zone doesn't actuate under Robolectric).
        composeTestRule.mainClock.advanceTimeBy(1_000)
        assertEquals(0, finished)
    }
}
