package com.mori.feature.settings.impl

import app.cash.turbine.test
import com.mori.core.model.PageFit
import com.mori.core.model.ReadingDirection
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
class SettingsViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    @Test
    fun emitsCurrentPreferences() = runTest {
        val viewModel = SettingsViewModel(TestPreferencesDataSource())
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is SettingsUiState.Ready)
        }
    }

    @Test
    fun themeActionsPersist() = runTest {
        val preferences = TestPreferencesDataSource()
        val viewModel = SettingsViewModel(preferences)
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
    fun readerActionsPersist() = runTest {
        val preferences = TestPreferencesDataSource()
        val viewModel = SettingsViewModel(preferences)
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
