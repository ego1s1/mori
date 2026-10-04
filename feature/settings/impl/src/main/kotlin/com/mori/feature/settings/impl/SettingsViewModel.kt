package com.mori.feature.settings.impl

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.core.data.ComicsRepository
import com.mori.core.datastore.MoriPreferencesDataSource
import com.mori.core.model.LibraryQuery
import com.mori.core.model.MotionStyle
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.StorageUsage
import com.mori.core.model.ThemePreferences
import com.mori.core.model.UserCollection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
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
    private val removeFolderUri = MutableStateFlow<String?>(null)

    private val storageFoldersState: Flow<StorageFoldersState> = combine(
        preferences.sourceTreeUris,
        repository.observeLibrary(LibraryQuery()),
        removeFolderUri,
    ) { uris, comics, removeUri ->
        val folders = uris.map { uri ->
            val count = comics.count { it.sourcePath.startsWith(uri) || it.id.startsWith(uri) }
            SourceFolder(uri = uri, bookCount = count)
        }
        StorageFoldersState(folders = folders, removeFolderUri = removeUri)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            preferences.themePreferences,
            preferences.readerPreferences,
            preferences.motionStyle,
            storageInfo,
        ) { theme, reader, motion, storage ->
            ThemeReaderState(theme, reader, motion, storage)
        },
        preferences.appLockEnabled,
        repository.observeCollections(),
        groupDialog,
        storageFoldersState,
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
        storageFolders: StorageFoldersState,
    ): SettingsUiState = SettingsUiState.Ready(
        theme = combined.theme,
        reader = combined.reader,
        motion = combined.motion,
        storage = combined.storage,
        appLock = appLock,
        groups = groups,
        groupDialog = groupDialog,
        sourceFolders = storageFolders.folders,
        removeFolderUri = storageFolders.removeFolderUri,
    )

    /** Four-flow combine carrier (fixed-arity combine caps at five). */
    private data class ThemeReaderState(
        val theme: ThemePreferences,
        val reader: ReaderPreferences,
        val motion: MotionStyle,
        val storage: StorageUsage?,
    )

    private data class StorageFoldersState(
        val folders: List<SourceFolder>,
        val removeFolderUri: String?,
    )

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetThemeMode -> updateTheme { it.copy(mode = action.mode) }
            is SettingsAction.SetDynamicColor -> updateTheme { it.copy(dynamicColor = action.enabled) }
            is SettingsAction.SetAmoled -> updateTheme { it.copy(amoled = action.enabled) }
            is SettingsAction.SetHapticsEnabled -> updateTheme { it.copy(hapticsEnabled = action.enabled) }
            is SettingsAction.SetMotionStyle -> updateMotion(action.style)
            is SettingsAction.SetColorScheme -> updateTheme {
                it.copy(colorScheme = action.scheme, dynamicColor = false)
            }
            is SettingsAction.SetDirection -> updateReader { it.copy(direction = action.direction) }
            is SettingsAction.SetPageFit -> updateReader { it.copy(pageFit = action.fit) }
            is SettingsAction.SetReaderNavMode -> updateReader { it.copy(navMode = action.navMode) }
            is SettingsAction.SetTapInvertMode -> updateReader { it.copy(invertTaps = action.invertMode) }
            SettingsAction.ToggleVolumeKeys -> updateReader { it.copy(volumeKeys = !it.volumeKeys) }
            SettingsAction.ToggleVolumeKeysInverted -> updateReader { it.copy(volumeKeysInverted = !it.volumeKeysInverted) }
            SettingsAction.ToggleKeepScreenOn -> updateReader { it.copy(keepScreenOn = !it.keepScreenOn) }
            SettingsAction.ToggleIncognito -> updateReader { it.copy(incognito = !it.incognito) }
            SettingsAction.ToggleAppLock -> toggleAppLock()
            SettingsAction.ToggleCropMargins -> updateReader { it.copy(cropMargins = !it.cropMargins) }
            SettingsAction.TogglePageCounter -> updateReader { it.copy(showPageCounter = !it.showPageCounter) }
            SettingsAction.ToggleSwipeToTurn -> updateReader { it.copy(swipeToTurn = !it.swipeToTurn) }
            SettingsAction.ToggleDisplayFilterEnabled -> updateReader {
                it.copy(displayFilter = it.displayFilter.copy(enabled = !it.displayFilter.enabled))
            }
            is SettingsAction.SetDisplayBrightness -> updateReader {
                it.copy(displayFilter = it.displayFilter.copy(brightness = action.brightness).coerce())
            }
            is SettingsAction.SetDisplayContrast -> updateReader {
                it.copy(displayFilter = it.displayFilter.copy(contrast = action.contrast).coerce())
            }
            is SettingsAction.SetDisplayNightTint -> updateReader {
                it.copy(displayFilter = it.displayFilter.copy(nightTint = action.nightTint).coerce())
            }
            is SettingsAction.SetDisplayColorTone -> updateReader {
                it.copy(displayFilter = it.displayFilter.copy(colorTone = action.colorTone))
            }
            is SettingsAction.SetDisplayBlendMode -> updateReader {
                it.copy(displayFilter = it.displayFilter.copy(blendMode = action.blendMode))
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
            is SettingsAction.AddSourceTree -> addSourceTree(action.uri)
            is SettingsAction.AskRemoveSource -> removeFolderUri.value = action.uri
            SettingsAction.DismissRemoveSource -> removeFolderUri.value = null
            SettingsAction.ConfirmRemoveSource -> confirmRemoveSource()
            is SettingsAction.RelinkSource -> relinkSourceTree(action.oldUri, action.newUri)
            SettingsAction.OpenCreateGroup -> groupDialog.value = GroupDialog.Create
            SettingsAction.CloseGroupDialog -> groupDialog.value = null
            is SettingsAction.CreateGroup -> createGroup(action.name)
            is SettingsAction.OpenRenameGroup -> groupDialog.value = GroupDialog.Rename(action.groupId, action.name)
            is SettingsAction.RenameGroup -> renameGroup(action.groupId, action.name)
            is SettingsAction.OpenDeleteGroup -> groupDialog.value = GroupDialog.Delete(action.groupId, action.name)
            is SettingsAction.ConfirmDeleteGroup -> deleteGroup(action.groupId)
        }
    }

    private fun addSourceTree(uri: Uri) {
        viewModelScope.launch {
            preferences.addSourceTreeUri(uri.toString())
            val allUris = preferences.sourceTreeUris.first().map { Uri.parse(it) }
            runCatching {
                repository.indexLinkedTrees(allUris) { _, _ -> }
            }
            storageRefresh.update { it + 1 }
        }
    }

    private fun confirmRemoveSource() {
        val uri = removeFolderUri.value ?: return
        removeFolderUri.value = null
        viewModelScope.launch {
            preferences.removeSourceTreeUri(uri)
            repository.removeSourceTree(uri)
            storageRefresh.update { it + 1 }
        }
    }

    private fun relinkSourceTree(oldUri: String, newUri: Uri) {
        viewModelScope.launch {
            preferences.removeSourceTreeUri(oldUri)
            repository.removeSourceTree(oldUri)
            preferences.addSourceTreeUri(newUri.toString())
            val allUris = preferences.sourceTreeUris.first().map { Uri.parse(it) }
            runCatching {
                repository.indexLinkedTrees(allUris) { _, _ -> }
            }
            storageRefresh.update { it + 1 }
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

    private var prefsJob: Job? = null

    private fun launchPrefs(block: suspend () -> Unit) {
        prefsJob?.cancel()
        prefsJob = viewModelScope.launch { block() }
    }

    private fun updateTheme(transform: (ThemePreferences) -> ThemePreferences) {
        launchPrefs {
            preferences.updateThemePreferences(transform)
        }
    }

    private fun updateReader(transform: (ReaderPreferences) -> ReaderPreferences) {
        launchPrefs {
            preferences.updateReaderPreferences(transform)
        }
    }

    private fun updateMotion(style: MotionStyle) {
        launchPrefs {
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
