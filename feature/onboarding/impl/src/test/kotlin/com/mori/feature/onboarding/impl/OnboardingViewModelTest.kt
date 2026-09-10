package com.mori.feature.onboarding.impl

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.mori.core.data.ComicImporter
import com.mori.core.model.ColorSchemeChoice
import com.mori.core.model.StorageLocation
import com.mori.core.model.ThemeMode
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
        repository: FakeComicsRepository = FakeComicsRepository(),
    ) = Triple(OnboardingViewModel(importer, repository, preferences), importer, preferences)

    private fun treeUri(): Uri = Uri.parse("content://com.example/tree/1")

    private suspend fun app.cash.turbine.ReceiveTurbine<OnboardingUiState>.awaitImporting(): OnboardingUiState {
        while (true) {
            val next = awaitItem()
            if (next is OnboardingUiState.Importing) return next
        }
    }

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
            assertTrue(awaitImporting() is OnboardingUiState.Importing)
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
            assertTrue(awaitImporting() is OnboardingUiState.Importing)
            assertTrue(awaitItem() is OnboardingUiState.Done)
        }
        assertEquals(listOf(uris), (importer as FakeComicImporter).seenDocuments)
    }

    @Test
    fun cancelImportReturnsToImport() = runTest {
        val (viewModel, _, _) = viewModel(importer = FakeComicImporter(hangImport = true))
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
            viewModel.onAction(OnboardingAction.FolderSelected(treeUri()))
            assertTrue(awaitImporting() is OnboardingUiState.Importing)
            viewModel.onAction(OnboardingAction.CancelImport)
            assertTrue(awaitItem() is OnboardingUiState.Import)
        }
    }

    @Test
    fun successfulImportRefreshesLibraryIndex() = runTest {
        val repository = FakeComicsRepository()
        val (viewModel, _, _) = viewModel(
            importer = FakeComicImporter(treeReport = FakeComicImporter.success(3)),
            repository = repository,
        )
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
            viewModel.onAction(OnboardingAction.FolderSelected(treeUri()))
            assertTrue(awaitImporting() is OnboardingUiState.Importing)
            assertTrue(awaitItem() is OnboardingUiState.Done)
            cancelAndIgnoreRemainingEvents()
        }
        // Final tick refresh plus the post-success guarantee.
        assertEquals(2, repository.refreshCalls)
    }

    @Test
    fun emptyImportSkipsLibraryRefresh() = runTest {
        val repository = FakeComicsRepository()
        val (viewModel, _, _) = viewModel(
            importer = FakeComicImporter(treeReport = FakeComicImporter.success(0)),
            repository = repository,
        )
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
            viewModel.onAction(OnboardingAction.FolderSelected(treeUri()))
            assertTrue(awaitImporting() is OnboardingUiState.Importing)
            assertTrue(awaitItem() is OnboardingUiState.Done)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(0, repository.refreshCalls)
    }

    @Test
    fun folderImportRecordsLinkedSource() = runTest {
        val preferences = FakePreferencesDataSource()
        val (viewModel, _, _) = viewModel(
            importer = FakeComicImporter(treeReport = FakeComicImporter.success(2)),
            preferences = preferences,
        )
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
            viewModel.onAction(OnboardingAction.FolderSelected(treeUri()))
            assertTrue(awaitImporting() is OnboardingUiState.Importing)
            assertTrue(awaitItem() is OnboardingUiState.Done)
            cancelAndIgnoreRemainingEvents()
        }
        preferences.sourceTreeUri.test {
            assertEquals(treeUri().toString(), awaitItem())
        }
    }

    @Test
    fun largeImportIndexesIncrementally() = runTest {
        val repository = FakeComicsRepository()
        val (viewModel, _, _) = viewModel(
            importer = FakeComicImporter(treeReport = FakeComicImporter.success(25)),
            repository = repository,
        )
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
            viewModel.onAction(OnboardingAction.FolderSelected(treeUri()))
            assertTrue(awaitImporting() is OnboardingUiState.Importing)
            assertTrue(awaitItem() is OnboardingUiState.Done)
            cancelAndIgnoreRemainingEvents()
        }
        // Throttled ticks (10, 20, 25) plus the final guarantee refresh.
        assertEquals(4, repository.refreshCalls)
    }

    @Test
    fun finishMarksOnboardingComplete() = runTest {
        val (viewModel, _, preferences) = viewModel(
            importer = FakeComicImporter(treeReport = FakeComicImporter.success(2)),
        )
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
            viewModel.onAction(OnboardingAction.FolderSelected(treeUri()))
            assertTrue(awaitImporting() is OnboardingUiState.Importing)
            assertTrue(awaitItem() is OnboardingUiState.Done)
        }
        viewModel.onAction(OnboardingAction.Finish)
        preferences.onboardingCompleted.test {
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun importMoreReturnsToImport() = runTest {
        val (viewModel, _, _) = viewModel(
            importer = FakeComicImporter(treeReport = FakeComicImporter.success(1)),
        )
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
            viewModel.onAction(OnboardingAction.FolderSelected(treeUri()))
            assertTrue(awaitImporting() is OnboardingUiState.Importing)
            assertTrue(awaitItem() is OnboardingUiState.Done)
            viewModel.onAction(OnboardingAction.ImportMore)
            val back = awaitItem()
            assertTrue(back is OnboardingUiState.Import)
        }
    }

    @Test
    fun wizardWalksWelcomeToImport() = runTest {
        val (viewModel, _, _) = viewModel()
        viewModel.uiState.test {
            assertEquals(OnboardingUiState.Welcome, awaitItem())
            viewModel.onAction(OnboardingAction.GetStarted)
            val storage = awaitItem()
            assertTrue(storage is OnboardingUiState.Storage)
            assertEquals(StorageLocation.APP, (storage as OnboardingUiState.Storage).location)
            viewModel.onAction(OnboardingAction.ContinueStep)
            val appearance = awaitItem()
            assertTrue(appearance is OnboardingUiState.Appearance)
            viewModel.onAction(OnboardingAction.ContinueStep)
            val import = awaitItem()
            assertTrue(import is OnboardingUiState.Import)
            viewModel.onAction(OnboardingAction.BackStep)
            assertTrue(awaitItem() is OnboardingUiState.Appearance)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun storageAndThemeChoicesPersist() = runTest {
        val preferences = FakePreferencesDataSource()
        val (viewModel, _, _) = viewModel(preferences = preferences)
        viewModel.uiState.test {
            awaitItem() // Welcome
            viewModel.onAction(OnboardingAction.GetStarted)
            awaitItem() // Storage
            viewModel.onAction(OnboardingAction.SelectStorage(StorageLocation.CUSTOM))
            val storage = awaitItem()
            assertTrue(storage is OnboardingUiState.Storage)
            storage as OnboardingUiState.Storage
            assertEquals(StorageLocation.CUSTOM, storage.location)
            viewModel.onAction(OnboardingAction.ContinueStep)
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
        preferences.storageLocation.test {
            assertEquals(StorageLocation.CUSTOM, awaitItem())
        }
    }

    @Test
    fun indexRefreshThrottlesAcrossImport() {
        assertEquals(false, shouldRefreshIndex(done = 3, total = 100, lastRefreshDone = 0))
        assertEquals(true, shouldRefreshIndex(done = 10, total = 100, lastRefreshDone = 0))
        assertEquals(true, shouldRefreshIndex(done = 100, total = 100, lastRefreshDone = 95))
        assertEquals(false, shouldRefreshIndex(done = 0, total = 0, lastRefreshDone = 0))
    }

    @Test
    fun contextAvailableForUriParsing() {        // Sanity: Robolectric provides a working application context for Uri handling.
        val context: android.content.Context = ApplicationProvider.getApplicationContext()
        assertTrue(context.packageName.isNotEmpty())
    }
}
