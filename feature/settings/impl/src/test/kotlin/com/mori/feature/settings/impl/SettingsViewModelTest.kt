package com.mori.feature.settings.impl

import android.net.Uri
import app.cash.turbine.test
import com.mori.core.model.ColorSchemeChoice
import com.mori.core.model.DisplayFilter
import com.mori.core.model.MotionStyle
import com.mori.core.model.PageFit
import com.mori.core.model.ReadingDirection
import com.mori.core.model.StorageUsage
import com.mori.core.model.ThemeMode
import com.mori.core.testing.FakeComicsRepository
import com.mori.core.testing.FakePreferencesDataSource
import com.mori.core.testing.TestDispatcherRule
import com.mori.core.testing.awaitAs
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
class SettingsViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    @Test
    fun emitsCurrentPreferences() = runTest {
        val viewModel = SettingsViewModel(FakePreferencesDataSource(), FakeComicsRepository())
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is SettingsUiState.Ready)
        }
    }

    @Test
    fun themeActionsPersist() = runTest {
        val preferences = FakePreferencesDataSource()
        val viewModel = SettingsViewModel(preferences, FakeComicsRepository())
        viewModel.uiState.test {
            awaitItem() // initial (Loading or first Ready)
            viewModel.onAction(SettingsAction.SetThemeMode(ThemeMode.DARK))
            viewModel.onAction(SettingsAction.SetDynamicColor(false))
            viewModel.onAction(SettingsAction.SetAmoled(true))
            val settled = awaitAs<SettingsUiState.Ready> {
                it.theme.mode == ThemeMode.DARK && !it.theme.dynamicColor && it.theme.amoled
            }
            assertEquals(ThemeMode.DARK, settled.theme.mode)
        }
    }

    @Test
    fun readerActionsPersist() = runTest {        val preferences = FakePreferencesDataSource()
        val viewModel = SettingsViewModel(preferences, FakeComicsRepository())
        viewModel.uiState.test {
            awaitAs<SettingsUiState.Ready>()
            viewModel.onAction(SettingsAction.SetDirection(ReadingDirection.RIGHT_TO_LEFT))
            viewModel.onAction(SettingsAction.SetPageFit(PageFit.HEIGHT))
            viewModel.onAction(SettingsAction.ToggleVolumeKeys)
            viewModel.onAction(SettingsAction.ToggleVolumeKeysInverted)
            viewModel.onAction(SettingsAction.ToggleKeepScreenOn)
            val settled = awaitAs<SettingsUiState.Ready> {
                it.reader.direction == ReadingDirection.RIGHT_TO_LEFT &&
                    it.reader.pageFit == PageFit.HEIGHT &&
                    it.reader.volumeKeys && it.reader.volumeKeysInverted &&
                    !it.reader.keepScreenOn
            }
            assertEquals(ReadingDirection.RIGHT_TO_LEFT, settled.reader.direction)
        }
    }

    @Test
    fun toggleIncognitoPersists() = runTest {
        val preferences = FakePreferencesDataSource()
        val viewModel = SettingsViewModel(preferences, FakeComicsRepository())
        viewModel.uiState.test {
            awaitAs<SettingsUiState.Ready>()
            viewModel.onAction(SettingsAction.ToggleIncognito)
            val settled = awaitAs<SettingsUiState.Ready> { it.reader.incognito }
            assertEquals(true, settled.reader.incognito)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleAppLockPersists() = runTest {
        val preferences = FakePreferencesDataSource()
        val viewModel = SettingsViewModel(preferences, FakeComicsRepository())
        viewModel.uiState.test {
            val initial = awaitAs<SettingsUiState.Ready>()
            assertEquals(false, initial.appLock)
            viewModel.onAction(SettingsAction.ToggleAppLock)
            val settled = awaitAs<SettingsUiState.Ready> { it.appLock }
            assertEquals(true, settled.appLock)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun displayFilterActionsPersist() = runTest {
        val preferences = FakePreferencesDataSource()
        val viewModel = SettingsViewModel(preferences, FakeComicsRepository())
        viewModel.uiState.test {
            awaitAs<SettingsUiState.Ready>()
            viewModel.onAction(SettingsAction.SetDisplayBrightness(-0.5f))
            viewModel.onAction(SettingsAction.SetDisplayNightTint(0.5f))
            viewModel.onAction(SettingsAction.ToggleDisplayGrayscale)
            viewModel.onAction(SettingsAction.ToggleDisplayInvert)
            val settled = awaitAs<SettingsUiState.Ready> {
                it.reader.displayFilter == DisplayFilter(
                    brightness = -0.5f,
                    grayscale = true,
                    invert = true,
                    nightTint = 0.5f,
                )
            }
            assertEquals(-0.5f, settled.reader.displayFilter.brightness)
            viewModel.onAction(SettingsAction.ResetDisplayFilter)
            val reset = awaitAs<SettingsUiState.Ready> { it.reader.displayFilter.isNeutral }
            assertEquals(DisplayFilter.Neutral, reset.reader.displayFilter)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun readerDisplayTogglesPersist() = runTest {
        val preferences = FakePreferencesDataSource()
        val viewModel = SettingsViewModel(preferences, FakeComicsRepository())
        viewModel.uiState.test {
            awaitAs<SettingsUiState.Ready>()
            viewModel.onAction(SettingsAction.ToggleCropMargins)
            viewModel.onAction(SettingsAction.TogglePageCounter)
            viewModel.onAction(SettingsAction.ToggleSwipeToTurn)
            val settled = awaitAs<SettingsUiState.Ready> {
                it.reader.cropMargins && !it.reader.showPageCounter && !it.reader.swipeToTurn
            }
            assertEquals(true, settled.reader.cropMargins)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun motionActionPersists() = runTest {
        val preferences = FakePreferencesDataSource()
        val viewModel = SettingsViewModel(preferences, FakeComicsRepository())
        viewModel.uiState.test {
            assertEquals(MotionStyle.EXPRESSIVE, awaitAs<SettingsUiState.Ready>().motion)
            viewModel.onAction(SettingsAction.SetMotionStyle(MotionStyle.CALM))
            assertEquals(MotionStyle.CALM, awaitAs<SettingsUiState.Ready> { it.motion == MotionStyle.CALM }.motion)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun colorSchemeActionPersistsAndDisablesDynamic() = runTest {
        val preferences = FakePreferencesDataSource()
        val viewModel = SettingsViewModel(preferences, FakeComicsRepository())
        viewModel.uiState.test {
            val initial = awaitAs<SettingsUiState.Ready>()
            assertEquals(true, initial.theme.dynamicColor)
            viewModel.onAction(SettingsAction.SetColorScheme(ColorSchemeChoice.FOREST))
            val settled = awaitAs<SettingsUiState.Ready> {
                it.theme.colorScheme == ColorSchemeChoice.FOREST && !it.theme.dynamicColor
            }
            assertEquals(ColorSchemeChoice.FOREST, settled.theme.colorScheme)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun storageUsageLoadsOnStart() = runTest {
        val repository = FakeComicsRepository()
        repository.usage = StorageUsage(comicCount = 3, libraryBytes = 1_000_000L, coversBytes = 50_000L)
        val viewModel = SettingsViewModel(FakePreferencesDataSource(), repository)
        viewModel.uiState.test {
            val settled = awaitAs<SettingsUiState.Ready> { it.storage != null }
            assertEquals(3, settled.storage?.comicCount)
        }
    }

    @Test
    fun clearThumbnailCacheRefreshesUsage() = runTest {
        val repository = FakeComicsRepository()
        repository.usage = StorageUsage(comicCount = 3, libraryBytes = 1_000_000L, coversBytes = 50_000L)
        val viewModel = SettingsViewModel(FakePreferencesDataSource(), repository)
        viewModel.uiState.test {
            awaitAs<SettingsUiState.Ready> { it.storage != null }
            viewModel.onAction(SettingsAction.ClearThumbnailCache)
            val cleared = awaitAs<SettingsUiState.Ready> { it.storage?.coversBytes == 0L }
            assertEquals(0L, cleared.storage?.coversBytes)
        }
        assertEquals(1, repository.clearCacheCalls)
    }

    @Test
    fun clearThumbnailCacheEmitsSnackbarEvent() = runTest {
        val viewModel = SettingsViewModel(FakePreferencesDataSource(), FakeComicsRepository())
        viewModel.events.test {
            viewModel.onAction(SettingsAction.ClearThumbnailCache)
            assertEquals(SettingsEvent.CacheCleared, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun groupsCreateRenameAndDelete() = runTest {
        val repository = FakeComicsRepository()
        val viewModel = SettingsViewModel(FakePreferencesDataSource(), repository)
        viewModel.uiState.test {
            assertEquals(true, awaitAs<SettingsUiState.Ready>().groups.isEmpty())
            viewModel.onAction(SettingsAction.OpenCreateGroup)
            assertEquals(GroupDialog.Create, awaitAs<SettingsUiState.Ready> { it.groupDialog != null }.groupDialog)
            viewModel.onAction(SettingsAction.CreateGroup("Favorites"))
            val created = awaitAs<SettingsUiState.Ready> { it.groups.size == 1 && it.groupDialog == null }
            assertEquals("Favorites", created.groups.single().name)
            val id = created.groups.single().id

            viewModel.onAction(SettingsAction.OpenRenameGroup(id, "Favorites"))
            assertEquals(
                GroupDialog.Rename(id, "Favorites"),
                awaitAs<SettingsUiState.Ready> { it.groupDialog != null }.groupDialog,
            )
            viewModel.onAction(SettingsAction.RenameGroup(id, "Picks"))
            assertEquals("Picks", awaitAs<SettingsUiState.Ready> { it.groups.single().name == "Picks" }.groups.single().name)

            viewModel.onAction(SettingsAction.OpenDeleteGroup(id, "Picks"))
            assertEquals(
                GroupDialog.Delete(id, "Picks"),
                awaitAs<SettingsUiState.Ready> { it.groupDialog != null }.groupDialog,
            )
            viewModel.onAction(SettingsAction.ConfirmDeleteGroup(id))
            assertEquals(true, awaitAs<SettingsUiState.Ready> { it.groups.isEmpty() }.groups.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sourceFoldersEmitWithBookCounts() = runTest {
        val preferences = FakePreferencesDataSource()
        preferences.addSourceTreeUri("content://tree/comics")
        val comic1 = FakeComicsRepository.comic("c1").copy(sourcePath = "content://tree/comics/book1.cbz")
        val comic2 = FakeComicsRepository.comic("c2").copy(sourcePath = "content://tree/comics/book2.cbz")
        val repository = FakeComicsRepository(listOf(comic1, comic2))
        val viewModel = SettingsViewModel(preferences, repository)

        viewModel.uiState.test {
            val ready = awaitAs<SettingsUiState.Ready> { it.sourceFolders.isNotEmpty() }
            assertEquals(1, ready.sourceFolders.size)
            assertEquals("content://tree/comics", ready.sourceFolders.first().uri)
            assertEquals(2, ready.sourceFolders.first().bookCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addSourceTreePersistsAndReindexes() = runTest {
        val preferences = FakePreferencesDataSource()
        val repository = FakeComicsRepository()
        val viewModel = SettingsViewModel(preferences, repository)

        viewModel.uiState.test {
            awaitItem()
            val uri = Uri.parse("content://tree/manga")
            viewModel.onAction(SettingsAction.AddSourceTree(uri))

            val ready = awaitAs<SettingsUiState.Ready> { it.sourceFolders.any { f -> f.uri == uri.toString() } }
            assertEquals(1, ready.sourceFolders.size)
            assertEquals(uri.toString(), ready.sourceFolders.first().uri)
            assertTrue(repository.linkedTrees.contains(uri))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun removeSourceTreeFlow() = runTest {
        val preferences = FakePreferencesDataSource()
        val folderUri = "content://tree/comics"
        preferences.addSourceTreeUri(folderUri)
        val comic = FakeComicsRepository.comic("c1").copy(id = "$folderUri/book1.cbz", sourcePath = "$folderUri/book1.cbz")
        val repository = FakeComicsRepository(listOf(comic))
        val viewModel = SettingsViewModel(preferences, repository)

        viewModel.uiState.test {
            awaitAs<SettingsUiState.Ready> { it.sourceFolders.isNotEmpty() }

            // Ask to remove
            viewModel.onAction(SettingsAction.AskRemoveSource(folderUri))
            val askReady = awaitAs<SettingsUiState.Ready> { it.removeFolderUri == folderUri }
            assertEquals(folderUri, askReady.removeFolderUri)

            // Dismiss
            viewModel.onAction(SettingsAction.DismissRemoveSource)
            val dismissReady = awaitAs<SettingsUiState.Ready> { it.removeFolderUri == null }
            assertEquals(null, dismissReady.removeFolderUri)
            assertEquals(1, dismissReady.sourceFolders.size)

            // Ask again and confirm
            viewModel.onAction(SettingsAction.AskRemoveSource(folderUri))
            awaitAs<SettingsUiState.Ready> { it.removeFolderUri == folderUri }
            viewModel.onAction(SettingsAction.ConfirmRemoveSource)

            val confirmedReady = awaitAs<SettingsUiState.Ready> { it.sourceFolders.isEmpty() }
            assertEquals(null, confirmedReady.removeFolderUri)
            assertEquals(0, confirmedReady.sourceFolders.size)
            assertTrue(repository.removedIds.contains(comic.id))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun relinkSourceTreeReplacesAndReindexes() = runTest {
        val preferences = FakePreferencesDataSource()
        val oldUri = "content://tree/old"
        val newUri = Uri.parse("content://tree/new")
        preferences.addSourceTreeUri(oldUri)
        val comic = FakeComicsRepository.comic("c1").copy(id = "$oldUri/book1.cbz", sourcePath = "$oldUri/book1.cbz")
        val repository = FakeComicsRepository(listOf(comic))
        val viewModel = SettingsViewModel(preferences, repository)

        viewModel.uiState.test {
            awaitAs<SettingsUiState.Ready> { it.sourceFolders.isNotEmpty() }

            viewModel.onAction(SettingsAction.RelinkSource(oldUri = oldUri, newUri = newUri))

            val ready = awaitAs<SettingsUiState.Ready> { it.sourceFolders.any { f -> f.uri == newUri.toString() } }
            assertEquals(1, ready.sourceFolders.size)
            assertEquals(newUri.toString(), ready.sourceFolders.first().uri)
            assertTrue(repository.linkedTrees.contains(newUri))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
