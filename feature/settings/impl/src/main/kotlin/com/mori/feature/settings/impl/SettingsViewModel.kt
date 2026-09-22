package com.mori.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.core.data.ComicsRepository
import com.mori.core.datastore.MoriPreferencesDataSource
import com.mori.core.model.MotionStyle
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.StorageUsage
import com.mori.core.model.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val preferences: MoriPreferencesDataSource,
    private val repository: ComicsRepository,
) : ViewModel() {

    private val storageRefresh = MutableStateFlow(0)
    private val storageInfo = MutableStateFlow<StorageUsage?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.themePreferences,
        preferences.readerPreferences,
        preferences.motionStyle,
        storageInfo,
        ::toUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState.Loading,
    )

    init {
        viewModelScope.launch {
            storageRefresh.collect {
                storageInfo.value = repository.storageUsage()
            }
        }
    }

    private fun toUiState(
        theme: ThemePreferences,
        reader: ReaderPreferences,
        motion: MotionStyle,
        storage: StorageUsage?,
    ): SettingsUiState = SettingsUiState.Ready(
        theme = theme,
        reader = reader,
        motion = motion,
        storage = storage,
    )

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetThemeMode -> updateTheme { it.copy(mode = action.mode) }
            is SettingsAction.SetDynamicColor -> updateTheme { it.copy(dynamicColor = action.enabled) }
            is SettingsAction.SetAmoled -> updateTheme { it.copy(amoled = action.enabled) }
            is SettingsAction.SetMotionStyle -> updateMotion(action.style)
            is SettingsAction.SetColorScheme -> updateTheme {
                it.copy(colorScheme = action.scheme, dynamicColor = false)
            }
            is SettingsAction.SetDirection -> updateReader { it.copy(direction = action.direction) }
            is SettingsAction.SetPageFit -> updateReader { it.copy(pageFit = action.fit) }
            SettingsAction.ToggleVolumeKeys -> updateReader { it.copy(volumeKeys = !it.volumeKeys) }
            SettingsAction.ToggleVolumeKeysInverted -> updateReader { it.copy(volumeKeysInverted = !it.volumeKeysInverted) }
            SettingsAction.ToggleKeepScreenOn -> updateReader { it.copy(keepScreenOn = !it.keepScreenOn) }
            SettingsAction.ToggleCropMargins -> updateReader { it.copy(cropMargins = !it.cropMargins) }
            SettingsAction.TogglePageCounter -> updateReader { it.copy(showPageCounter = !it.showPageCounter) }
            SettingsAction.ToggleSwipeToTurn -> updateReader { it.copy(swipeToTurn = !it.swipeToTurn) }
            SettingsAction.ClearThumbnailCache -> clearCache()
        }
    }

    private fun updateTheme(transform: (ThemePreferences) -> ThemePreferences) {
        viewModelScope.launch {
            preferences.updateThemePreferences(transform)
        }
    }

    private fun updateReader(transform: (ReaderPreferences) -> ReaderPreferences) {
        viewModelScope.launch {
            preferences.updateReaderPreferences(transform)
        }
    }

    private fun updateMotion(style: MotionStyle) {
        viewModelScope.launch {
            preferences.updateMotionStyle(style)
        }
    }

    private fun clearCache() {
        viewModelScope.launch {
            repository.clearThumbnailCache()
            storageRefresh.update { it + 1 }
        }
    }
}
