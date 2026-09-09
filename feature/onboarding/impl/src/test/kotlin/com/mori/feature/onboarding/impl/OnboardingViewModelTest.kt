package com.mori.feature.onboarding.impl

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
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
        importer: ComicImporter = FakeComicImporter(),
        preferences: FakePreferencesDataSource = FakePreferencesDataSource(),
    ) = Triple(OnboardingViewModel(importer, preferences), importer, preferences)

    private fun treeUri(): Uri = Uri.parse("content://com.example/tree/1")

    @Test
    fun startsAtWelcome() = runTest {
        val (viewModel, _, _) = viewModel()
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
        }
    }

    @Test
    fun folderImportFlowsToDone() = runTest {
        val (viewModel, importer, _) = viewModel(
            importer = FakeComicImporter(treeReport = FakeComicImporter.success(3)),
        )
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
            viewModel.onAction(OnboardingAction.FolderSelected(treeUri()))
            assertTrue(awaitItem() is OnboardingUiState.Importing)
            val done = awaitItem()
            assertTrue(done is OnboardingUiState.Done)
            done as OnboardingUiState.Done
            assertEquals(3, done.report.succeeded)
        }
        assertEquals(listOf(treeUri()), (importer as FakeComicImporter).seenTrees)
    }

    @Test
    fun filesImportFlowsToDone() = runTest {
        val uris = listOf(Uri.parse("content://com.example/doc/1"))
        val (viewModel, importer, _) = viewModel(
            importer = FakeComicImporter(documentsReport = FakeComicImporter.success(1)),
        )
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
            viewModel.onAction(OnboardingAction.FilesSelected(uris))
            assertTrue(awaitItem() is OnboardingUiState.Importing)
            assertTrue(awaitItem() is OnboardingUiState.Done)
        }
        assertEquals(listOf(uris), (importer as FakeComicImporter).seenDocuments)
    }

    @Test
    fun cancelImportReturnsToWelcome() = runTest {
        val (viewModel, _, _) = viewModel(importer = FakeComicImporter(hangImport = true))
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
            viewModel.onAction(OnboardingAction.FolderSelected(treeUri()))
            assertTrue(awaitItem() is OnboardingUiState.Importing)
            viewModel.onAction(OnboardingAction.CancelImport)
            assertEquals(OnboardingUiState.Welcome, awaitItem())
        }
    }

    @Test
    fun finishMarksOnboardingComplete() = runTest {
        val (viewModel, _, preferences) = viewModel(
            importer = FakeComicImporter(treeReport = FakeComicImporter.success(2)),
        )
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
            viewModel.onAction(OnboardingAction.FolderSelected(treeUri()))
            assertTrue(awaitItem() is OnboardingUiState.Importing)
            assertTrue(awaitItem() is OnboardingUiState.Done)
        }
        viewModel.onAction(OnboardingAction.Finish)
        preferences.onboardingCompleted.test {
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun importMoreReturnsToWelcome() = runTest {
        val (viewModel, _, _) = viewModel(
            importer = FakeComicImporter(treeReport = FakeComicImporter.success(1)),
        )
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
            viewModel.onAction(OnboardingAction.FolderSelected(treeUri()))
            assertTrue(awaitItem() is OnboardingUiState.Importing)
            assertTrue(awaitItem() is OnboardingUiState.Done)
            viewModel.onAction(OnboardingAction.ImportMore)
            assertEquals(OnboardingUiState.Welcome, awaitItem())
        }
    }

    @Test
    fun contextAvailableForUriParsing() {
        // Sanity: Robolectric provides a working application context for Uri handling.
        val context: android.content.Context = ApplicationProvider.getApplicationContext()
        assertTrue(context.packageName.isNotEmpty())
    }
}
