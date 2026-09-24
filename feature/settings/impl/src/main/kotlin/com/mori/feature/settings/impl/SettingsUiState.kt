package com.mori.feature.settings.impl

import com.mori.core.model.PageFit
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.ReadingDirection
import com.mori.core.model.ReadingStats
import com.mori.core.model.UserCollection
import com.mori.core.model.ColorSchemeChoice
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
        val stats: ReadingStats = ReadingStats(),
        val appLock: Boolean = false,
        val groups: List<UserCollection> = emptyList(),
        val groupDialog: GroupDialog? = null,
    ) : SettingsUiState
}

sealed interface SettingsAction {
    data class SetThemeMode(val mode: ThemeMode) : SettingsAction

    data class SetDynamicColor(val enabled: Boolean) : SettingsAction

    data class SetAmoled(val enabled: Boolean) : SettingsAction

    data class SetMotionStyle(val style: MotionStyle) : SettingsAction

    data class SetColorScheme(val scheme: ColorSchemeChoice) : SettingsAction

    data class SetDirection(val direction: ReadingDirection) : SettingsAction

    data class SetPageFit(val fit: PageFit) : SettingsAction

    data object ToggleVolumeKeys : SettingsAction

    data object ToggleVolumeKeysInverted : SettingsAction

    data object ToggleKeepScreenOn : SettingsAction

    /** Pause progress, history, and stats recording while on. */
    data object ToggleIncognito : SettingsAction

    data object ToggleAppLock : SettingsAction

    data object ToggleCropMargins : SettingsAction

    data object TogglePageCounter : SettingsAction

    data object ToggleSwipeToTurn : SettingsAction

    data class SetDisplayBrightness(val brightness: Float) : SettingsAction

    data class SetDisplayNightTint(val nightTint: Float) : SettingsAction

    data object ToggleDisplayGrayscale : SettingsAction

    data object ToggleDisplayInvert : SettingsAction

    data object ResetDisplayFilter : SettingsAction

    data object ClearThumbnailCache : SettingsAction

    data object OpenCreateGroup : SettingsAction

    data object CloseGroupDialog : SettingsAction

    data class CreateGroup(val name: String) : SettingsAction

    data class OpenRenameGroup(val groupId: Long, val name: String) : SettingsAction

    data class RenameGroup(val groupId: Long, val name: String) : SettingsAction

    data class OpenDeleteGroup(val groupId: Long, val name: String) : SettingsAction

    data class ConfirmDeleteGroup(val groupId: Long) : SettingsAction
}

/** Group management dialog: create, rename (with current name), or delete-confirm. */
sealed interface GroupDialog {
    data object Create : GroupDialog

    data class Rename(val groupId: Long, val name: String) : GroupDialog

    data class Delete(val groupId: Long, val name: String) : GroupDialog
}

/** One-shot settings events (snackbar feedback, not state). */
sealed interface SettingsEvent {
    data object CacheCleared : SettingsEvent

    data object CacheClearFailed : SettingsEvent
}
