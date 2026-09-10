package com.mori.feature.onboarding.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mori.core.designsystem.MoriTheme
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
        actions: MutableList<OnboardingAction> = mutableListOf(),
        onFinish: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            MoriTheme {
                OnboardingScreen(
                    uiState = uiState,
                    onPickFolder = onPickFolder,
                    onPickFiles = onPickFiles,
                    onAction = actions::add,
                    onOnboardingComplete = onFinish,
                )
            }
        }
    }

    @Test
    fun welcomeShowsPickerActions() {
        var folderPicks = 0
        var filePicks = 0
        setScreen(
            OnboardingUiState.Welcome,
            onPickFolder = { folderPicks += 1 },
            onPickFiles = { filePicks += 1 },
        )

        // Welcome staggers in; let the cascade finish.
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.onNodeWithText("Where are your comics?").assertIsDisplayed()
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

        composeTestRule.onNodeWithText("Imported 3 of 4").assertIsDisplayed()
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
