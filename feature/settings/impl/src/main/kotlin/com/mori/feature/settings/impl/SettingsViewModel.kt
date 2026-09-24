package com.mori.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.core.data.ComicsRepository
import com.mori.core.datastore.MoriPreferencesDataSource
import com.mori.core.model.MotionStyle
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.ReadingStats
import com.mori.core.model.StorageUsage
import com.mori.core.model.ThemePreferences
import com.mori.core.model.UserCollection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    private val groupDialog = MutableStateFlow<GroupDialog?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            preferences.themePreferences,
            preferences.readerPreferences,
            preferences.motionStyle,
            storageInfo,
            repository.observeReadingStats(),
        ) { theme, reader, motion, storage, stats ->
            ThemeReaderState(theme, reader, motion, storage, stats)
        },
        preferences.appLockEnabled,
        repository.observeCollections(),
        groupDialog,
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
        combined: ThemeReaderState,
        appLock: Boolean,
        groups: List<UserCollection>,
        groupDialog: GroupDialog?,
    ): SettingsUiState = SettingsUiState.Ready(
        theme = combined.theme,
        reader = combined.reader,
        motion = combined.motion,
        storage = combined.storage,
        stats = combined.stats,
        appLock = appLock,
        groups = groups,
        groupDialog = groupDialog,
    )

    /** Five-flow combine carrier (fixed-arity combine caps at five). */
    private data class ThemeReaderState(
        val theme: ThemePreferences,
        val reader: ReaderPreferences,
        val motion: MotionStyle,
        val storage: StorageUsage?,
        val stats: ReadingStats,
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
            SettingsAction.ToggleIncognito -> updateReader { it.copy(incognito = !it.incognito) }
            SettingsAction.ToggleAppLock -> toggleAppLock()
            SettingsAction.ToggleCropMargins -> updateReader { it.copy(cropMargins = !it.cropMargins) }
            SettingsAction.TogglePageCounter -> updateReader { it.copy(showPageCounter = !it.showPageCounter) }
            SettingsAction.ToggleSwipeToTurn -> updateReader { it.copy(swipeToTurn = !it.swipeToTurn) }
            is SettingsAction.SetDisplayBrightness -> updateReader {
                it.copy(displayFilter = it.displayFilter.copy(brightness = action.brightness).coerce())
            }
            is SettingsAction.SetDisplayNightTint -> updateReader {
                it.copy(displayFilter = it.displayFilter.copy(nightTint = action.nightTint).coerce())
            }
            SettingsAction.ToggleDisplayGrayscale -> updateReader {
                it.copy(displayFilter = it.displayFilter.copy(grayscale = !it.displayFilter.grayscale))
            }
            SettingsAction.ToggleDisplayInvert -> updateReader {
                it.copy(displayFilter = it.displayFilter.copy(invert = !it.displayFilter.invert))
            }
            SettingsAction.ResetDisplayFilter -> updateReader {
                it.copy(displayFilter = com.mori.core.model.DisplayFilter.Neutral)
            }
            SettingsAction.ClearThumbnailCache -> clearCache()
            SettingsAction.OpenCreateGroup -> groupDialog.value = GroupDialog.Create
            SettingsAction.CloseGroupDialog -> groupDialog.value = null
            is SettingsAction.CreateGroup -> createGroup(action.name)
            is SettingsAction.OpenRenameGroup -> groupDialog.value = GroupDialog.Rename(action.groupId, action.name)
            is SettingsAction.RenameGroup -> renameGroup(action.groupId, action.name)
            is SettingsAction.OpenDeleteGroup -> groupDialog.value = GroupDialog.Delete(action.groupId, action.name)
            is SettingsAction.ConfirmDeleteGroup -> deleteGroup(action.groupId)
        }
    }

    /**
     * Shelf management lives here (the library only views). Blank names are
     * dropped; the dialog closes on success so the new shelf is visible.
     */
    private fun createGroup(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.createCollection(name.trim()) }
            groupDialog.value = null
        }
    }

    private fun renameGroup(groupId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.renameCollection(groupId, name.trim()) }
            groupDialog.value = null
        }
    }

    private fun deleteGroup(groupId: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteCollection(groupId) }
            groupDialog.value = null
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
            val cleared = runCatching { repository.clearThumbnailCache() }.isSuccess
            if (cleared) {
                storageRefresh.update { it + 1 }
                _events.emit(SettingsEvent.CacheCleared)
            } else {
                _events.emit(SettingsEvent.CacheClearFailed)
            }
        }
    }

    private fun toggleAppLock() {
        viewModelScope.launch {
            val current = preferences.appLockEnabled.first()
            preferences.setAppLockEnabled(!current)
        }
    }
}
