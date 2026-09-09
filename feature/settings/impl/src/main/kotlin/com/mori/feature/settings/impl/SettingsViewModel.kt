package com.mori.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.core.datastore.MoriPreferencesDataSource
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: MoriPreferencesDataSource,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.themePreferences,
        preferences.readerPreferences,
        ::toUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState.Loading,
    )

    private fun toUiState(
        theme: ThemePreferences,
        reader: ReaderPreferences,
    ): SettingsUiState = SettingsUiState.Ready(theme = theme, reader = reader)

    fun onAction(action: SettingsAction) {
        viewModelScope.launch {
            when (action) {
                is SettingsAction.SetThemeMode -> preferences.updateThemePreferences {
                    it.copy(mode = action.mode)
                }
                is SettingsAction.SetDynamicColor -> preferences.updateThemePreferences {
                    it.copy(dynamicColor = action.enabled)
                }
                is SettingsAction.SetAmoled -> preferences.updateThemePreferences {
                    it.copy(amoled = action.enabled)
                }
                is SettingsAction.SetDirection -> preferences.updateReaderPreferences {
                    it.copy(direction = action.direction)
                }
                is SettingsAction.SetPageFit -> preferences.updateReaderPreferences {
                    it.copy(pageFit = action.fit)
                }
                SettingsAction.ToggleVolumeKeys -> preferences.updateReaderPreferences {
                    it.copy(volumeKeys = !it.volumeKeys)
                }
                SettingsAction.ToggleKeepScreenOn -> preferences.updateReaderPreferences {
                    it.copy(keepScreenOn = !it.keepScreenOn)
                }
            }
        }
    }
}
