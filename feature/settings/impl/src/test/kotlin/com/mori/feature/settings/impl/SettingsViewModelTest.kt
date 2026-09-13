package com.mori.feature.settings.impl

import app.cash.turbine.test
import com.mori.core.model.ColorSchemeChoice
import com.mori.core.model.MotionStyle
import com.mori.core.model.PageFit
import com.mori.core.model.ReadingDirection
import com.mori.core.model.StorageUsage
import com.mori.core.model.ThemeMode
import com.mori.core.testing.FakeComicsRepository
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
            val settled = awaitReadyWhere {
                it.theme.mode == ThemeMode.DARK && !it.theme.dynamicColor && it.theme.amoled
            }
            assertEquals(ThemeMode.DARK, settled.theme.mode)
        }
    }

    @Test
    fun readerActionsPersist() = runTest {        val preferences = FakePreferencesDataSource()
        val viewModel = SettingsViewModel(preferences, FakeComicsRepository())
        viewModel.uiState.test {
            awaitReady()
            viewModel.onAction(SettingsAction.SetDirection(ReadingDirection.RIGHT_TO_LEFT))
            viewModel.onAction(SettingsAction.SetPageFit(PageFit.HEIGHT))
            viewModel.onAction(SettingsAction.ToggleVolumeKeys)
            viewModel.onAction(SettingsAction.ToggleKeepScreenOn)
            val settled = awaitReadyWhere {
                it.reader.direction == ReadingDirection.RIGHT_TO_LEFT &&
                    it.reader.pageFit == PageFit.HEIGHT &&
                    it.reader.volumeKeys && !it.reader.keepScreenOn
            }
            assertEquals(ReadingDirection.RIGHT_TO_LEFT, settled.reader.direction)
        }
    }

    @Test
    fun readerDisplayTogglesPersist() = runTest {
        val preferences = FakePreferencesDataSource()
        val viewModel = SettingsViewModel(preferences, FakeComicsRepository())
        viewModel.uiState.test {
            awaitReady()
            viewModel.onAction(SettingsAction.ToggleCropMargins)
            viewModel.onAction(SettingsAction.TogglePageCounter)
            viewModel.onAction(SettingsAction.ToggleSwipeToTurn)
            val settled = awaitReadyWhere {
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
            assertEquals(MotionStyle.EXPRESSIVE, awaitReady().motion)
            viewModel.onAction(SettingsAction.SetMotionStyle(MotionStyle.CALM))
            assertEquals(MotionStyle.CALM, awaitReadyWhere { it.motion == MotionStyle.CALM }.motion)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun colorSchemeActionPersistsAndDisablesDynamic() = runTest {
        val preferences = FakePreferencesDataSource()
        val viewModel = SettingsViewModel(preferences, FakeComicsRepository())
        viewModel.uiState.test {
            val initial = awaitReady()
            assertEquals(true, initial.theme.dynamicColor)
            viewModel.onAction(SettingsAction.SetColorScheme(ColorSchemeChoice.FOREST))
            val settled = awaitReadyWhere {
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
            val settled = awaitReadyWhere { it.storage != null }
            assertEquals(3, settled.storage?.comicCount)
        }
    }

    @Test
    fun clearThumbnailCacheRefreshesUsage() = runTest {
        val repository = FakeComicsRepository()
        repository.usage = StorageUsage(comicCount = 3, libraryBytes = 1_000_000L, coversBytes = 50_000L)
        val viewModel = SettingsViewModel(FakePreferencesDataSource(), repository)
        viewModel.uiState.test {
            awaitReadyWhere { it.storage != null }
            viewModel.onAction(SettingsAction.ClearThumbnailCache)
            val cleared = awaitReadyWhere { it.storage?.coversBytes == 0L }
            assertEquals(0L, cleared.storage?.coversBytes)
        }
        assertEquals(1, repository.clearCacheCalls)
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<SettingsUiState>.awaitReady(): SettingsUiState.Ready {
        while (true) {
            when (val next = awaitItem()) {
                is SettingsUiState.Ready -> return next
                else -> Unit
            }
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<SettingsUiState>.awaitReadyWhere(
        predicate: (SettingsUiState.Ready) -> Boolean,
    ): SettingsUiState.Ready {
        while (true) {
            when (val next = awaitItem()) {
                is SettingsUiState.Ready -> if (predicate(next)) return next
                else -> Unit
            }
        }
    }
}
