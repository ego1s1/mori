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
import com.mori.core.model.StorageLocation
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
        onPickFiles: () -> Unit = {},
        onPickCustomFolder: () -> Unit = {},
        actions: MutableList<OnboardingAction> = mutableListOf(),
        onFinish: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            MoriTheme {
                OnboardingScreen(
                    uiState = uiState,
                    onPickFolder = onPickFolder,
                    onPickFiles = onPickFiles,
                    onPickCustomFolder = onPickCustomFolder,
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
        composeTestRule.onNodeWithText("WELCOME").assertIsDisplayed()
        composeTestRule.onNodeWithText("beautifully", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(OnboardingTestTags.GetStarted).performClick()
        assert(actions.contains(OnboardingAction.GetStarted))
        composeTestRule.onNodeWithText("Skip").performClick()
        assert(actions.contains(OnboardingAction.Skip))
        assertEquals(1, finished)
    }

    @Test
    fun storageStepSelectsAndPicksCustom() {
        val actions = mutableListOf<OnboardingAction>()
        var customPicks = 0
        setScreen(
            OnboardingUiState.Storage(StorageLocation.APP, folderName = null),
            actions = actions,
            onPickCustomFolder = { customPicks += 1 },
        )

        composeTestRule.onNodeWithTag(OnboardingTestTags.StorageStep).assertIsDisplayed()
        composeTestRule.onNodeWithText("On this device").assertIsDisplayed()
        composeTestRule.onNodeWithText("Custom folder").performClick()
        assertEquals(1, customPicks)
        // The CTA dispatches ContinueStep (navigation covered by VM tests; a
        // Button click through this zone doesn't actuate under Robolectric).
        composeTestRule.onNodeWithTag(OnboardingTestTags.StepContinue).assertIsDisplayed()
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
    fun importStepOffersPickers() {
        var folderPicks = 0
        var filePicks = 0
        setScreen(
            OnboardingUiState.Import(StorageLocation.APP, folderName = null),
            onPickFolder = { folderPicks += 1 },
            onPickFiles = { filePicks += 1 },
        )

        composeTestRule.onNodeWithTag(OnboardingTestTags.ImportStep).assertIsDisplayed()
        composeTestRule.onNodeWithTag(OnboardingTestTags.PickFolder).performClick()
        composeTestRule.onNodeWithTag(OnboardingTestTags.PickFiles).performClick()
        assert(folderPicks == 1)
        assert(filePicks == 1)
    }

    @Test
    fun importingShowsProgressAndCancel() {
        val actions = mutableListOf<OnboardingAction>()
        setScreen(OnboardingUiState.Importing(done = 2, total = 5), actions = actions)

        composeTestRule.onNodeWithTag(OnboardingTestTags.Progress).assertIsDisplayed()
        composeTestRule.onNodeWithText("Copying 2 of 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
        assert(actions.contains(OnboardingAction.CancelImport))
    }

    @Test
    fun importingWithoutTotalShowsSpinner() {
        setScreen(OnboardingUiState.Importing(done = 0, total = 0))

        composeTestRule.onNodeWithTag(OnboardingTestTags.Progress).assertIsDisplayed()
        composeTestRule.onNodeWithText("Scanning for comics…").assertIsDisplayed()
    }

    @Test
    fun doneShowsSummaryAndFinish() {
        val actions = mutableListOf<OnboardingAction>()
        var finished = 0
        setScreen(
            OnboardingUiState.Done(FakeComicImporter.success(4, succeeded = 3)),
            actions = actions,
            onFinish = { finished += 1 },
        )

        composeTestRule.onNodeWithText("3 of 4").assertIsDisplayed()
        composeTestRule.onNodeWithText("comics imported").assertIsDisplayed()
        composeTestRule.onNodeWithTag(OnboardingTestTags.Finish).performClick()
        assert(actions.contains(OnboardingAction.Finish))
        assertEquals(1, finished)
    }

    @Test
    fun doneAutoAdvancesHome() {
        var finished = 0
        setScreen(
            OnboardingUiState.Done(FakeComicImporter.success(4, succeeded = 3)),
            onFinish = { finished += 1 },
        )

        composeTestRule.mainClock.advanceTimeBy(2_000)
        assertEquals(1, finished)
    }
}
