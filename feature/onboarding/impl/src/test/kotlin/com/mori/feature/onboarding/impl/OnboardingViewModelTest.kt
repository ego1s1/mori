package com.mori.feature.onboarding.impl

import android.net.Uri
import app.cash.turbine.test
import com.mori.core.model.ColorSchemeChoice
import com.mori.core.model.ThemeMode
import com.mori.core.testing.FakePreferencesDataSource
import com.mori.core.testing.TestDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OnboardingViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private fun viewModel(
        preferences: FakePreferencesDataSource = FakePreferencesDataSource(),
    ) = Pair(OnboardingViewModel(preferences), preferences)

    private fun treeUri(): Uri = Uri.parse("content://com.example/tree/1")

    @Test
    fun startsAtWelcome() = runTest {
        val (viewModel, _) = viewModel()
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
        }
    }

    @Test
    fun getStartedAdvancesToFolder() = runTest {
        val (viewModel, _) = viewModel()
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
            viewModel.onAction(OnboardingAction.GetStarted)
            assertEquals(OnboardingUiState.Folder, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun folderSelectedPersistsTreeAndAdvances() = runTest {
        val (viewModel, preferences) = viewModel()
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
            viewModel.onAction(OnboardingAction.GetStarted)
            assertEquals(OnboardingUiState.Folder, awaitItem())
            viewModel.onAction(OnboardingAction.FolderSelected(treeUri()))
            assertTrue(awaitItem() is OnboardingUiState.Appearance)
            cancelAndIgnoreRemainingEvents()
        }
        preferences.sourceTreeUri.test {
            assertEquals(treeUri().toString(), awaitItem())
        }
    }

    @Test
    fun backStepWalksBackwards() = runTest {
        val (viewModel, _) = viewModel()
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
            viewModel.onAction(OnboardingAction.GetStarted)
            assertEquals(OnboardingUiState.Folder, awaitItem())
            viewModel.onAction(OnboardingAction.FolderSelected(treeUri()))
            assertTrue(awaitItem() is OnboardingUiState.Appearance)
            viewModel.onAction(OnboardingAction.BackStep)
            assertEquals(OnboardingUiState.Folder, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun themeChoicesPersist() = runTest {
        val (viewModel, preferences) = viewModel()
        viewModel.uiState.test {
            awaitItem() // Welcome
            viewModel.onAction(OnboardingAction.GetStarted)
            awaitItem() // Folder
            viewModel.onAction(OnboardingAction.FolderSelected(treeUri()))
            awaitItem() // Appearance
            viewModel.onAction(OnboardingAction.SetThemeMode(ThemeMode.DARK))
            viewModel.onAction(OnboardingAction.SetColorScheme(ColorSchemeChoice.OCEAN))
            val appearance = awaitItem()
            assertTrue(appearance is OnboardingUiState.Appearance)
            cancelAndIgnoreRemainingEvents()
        }
        preferences.themePreferences.test {
            val theme = awaitItem()
            assertEquals(ThemeMode.DARK, theme.mode)
            assertEquals(ColorSchemeChoice.OCEAN, theme.colorScheme)
            assertEquals(false, theme.dynamicColor)
        }
    }

    @Test
    fun skipMarksOnboardingComplete() = runTest {
        val (viewModel, preferences) = viewModel()
        viewModel.onAction(OnboardingAction.Skip)
        preferences.onboardingCompleted.test {
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun finishMarksOnboardingComplete() = runTest {
        val (viewModel, preferences) = viewModel()
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
            viewModel.onAction(OnboardingAction.GetStarted)
            assertEquals(OnboardingUiState.Folder, awaitItem())
            viewModel.onAction(OnboardingAction.FolderSelected(treeUri()))
            assertTrue(awaitItem() is OnboardingUiState.Appearance)
        }
        viewModel.onAction(OnboardingAction.Finish)
        preferences.onboardingCompleted.test {
            assertEquals(true, awaitItem())
        }
    }
}
