package com.mori.feature.settings.impl

import com.mori.core.model.PageFit
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.ReadingDirection
import com.mori.core.model.MotionStyle
import com.mori.core.model.StorageUsage
import com.mori.core.model.ThemeMode
import com.mori.core.model.ThemePreferences

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Ready(
        val theme: ThemePreferences,
        val reader: ReaderPreferences,
        val motion: MotionStyle,
        val storage: StorageUsage?,
    ) : SettingsUiState
}

sealed interface SettingsAction {
    data class SetThemeMode(val mode: ThemeMode) : SettingsAction

    data class SetDynamicColor(val enabled: Boolean) : SettingsAction

    data class SetAmoled(val enabled: Boolean) : SettingsAction

    data class SetMotionStyle(val style: MotionStyle) : SettingsAction

    data class SetDirection(val direction: ReadingDirection) : SettingsAction

    data class SetPageFit(val fit: PageFit) : SettingsAction

    data object ToggleVolumeKeys : SettingsAction

    data object ToggleKeepScreenOn : SettingsAction

    data object ClearThumbnailCache : SettingsAction
}
