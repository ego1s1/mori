package com.mori.feature.reader.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mori.core.data.ComicsRepository
import com.mori.core.datastore.MoriPreferencesDataSource
import com.mori.core.model.Comic
import com.mori.core.model.ComicError
import com.mori.core.model.ReaderPreferences
import com.mori.feature.reader.api.ReaderRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reader state holder backed by the real repository and persisted preferences.
 *
 * Navigation position lives in [navigation] (route argument first, then user movement)
 * so repository re-emissions — including our own progress saves — never yank the pager
 * back. Progress saves debounce 500ms after the page settles.
 *
 * On the very first reader open the chrome (top bar, controls) stays up for a brief
 * overview beat before fading, then the moment is persisted so later opens fall back
 * to the stillness auto-hide in [ReaderContent].
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ComicsRepository,
    private val preferences: MoriPreferencesDataSource,
) : ViewModel() {

    private val args: ReaderRoute = savedStateHandle.toRoute<ReaderRoute>()

    /** Ephemeral chrome state; resets are harmless, so it is not persisted. */
    private val chrome = MutableStateFlow(ChromeState())

    /**
     * User-driven position, restored from [SavedStateHandle] before the
     * repository emits so process death never rewinds the page. Written
     * synchronously on every move, so rapid turns accumulate instead of
     * racing the combined [uiState].
     */
    private val navigation = MutableStateFlow<Int?>(
        savedStateHandle.get<Int>(SAVED_PAGE_INDEX),
    )

    private var saveJob: Job? = null
    private var pendingSave: Int? = null

    /** Outlives [viewModelScope] to flush the last progress write on exit. */
    private val flushScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCleared() {
        saveJob?.cancel()
        pendingSave?.let { index ->
            flushScope.launch { repository.saveProgress(args.comicId, index) }
        }
    }

    init {
        viewModelScope.launch {
            if (!preferences.readerOverviewSeen.first()) {
                delay(READER_OVERVIEW_MS)
                // Never yank chrome out from under the open settings sheet.
                if (!chrome.value.settingsOpen) {
                    chrome.value = chrome.value.copy(visible = false)
                }
                preferences.setReaderOverviewSeen()
            }
        }
    }

    val uiState: StateFlow<ReaderUiState> = combine(
        repository.observeComic(args.comicId),
        preferences.readerPreferences,
        chrome,
        navigation,
        ::toUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReaderUiState.Loading,
    )

    private fun toUiState(
        comic: Comic?,
        prefs: ReaderPreferences,
        chrome: ChromeState,
        navigation: Int?,
    ): ReaderUiState {
        if (comic == null) {
            return ReaderUiState.Error("This comic is no longer in your library.")
        }
        val error = comic.error
        if (error != null) {
            return ReaderUiState.Error(errorMessage(error))
        }
        val pageCount = comic.pageCount.coerceAtLeast(1)
        val pageIndex = (navigation ?: args.pageIndex).coerceIn(0, pageCount - 1)
        return ReaderUiState.Ready(
            comicId = comic.id,
            title = comic.title,
            subtitle = listOfNotNull(comic.series, comic.number).joinToString(" • "),
            bookmarked = comic.bookmarked,
            pageIndex = pageIndex,
            pageCount = pageCount,
            chromeVisible = chrome.visible,
            direction = prefs.direction,
            pageFit = prefs.pageFit,
            cropMargins = prefs.cropMargins,
            settingsOpen = chrome.settingsOpen,
            volumeKeys = prefs.volumeKeys,
            keepScreenOn = prefs.keepScreenOn,
            showTapZones = chrome.showTapZones,
            showPageCounter = prefs.showPageCounter,
        )
    }

    private fun errorMessage(error: ComicError): String = when (error) {
        ComicError.CORRUPT -> "This file could not be read. It may be damaged."
        ComicError.PASSWORD_REQUIRED -> "This archive is password protected."
        ComicError.EMPTY -> "This archive contains no readable pages."
        ComicError.UNSUPPORTED -> "This format is not supported."
    }

    fun onAction(action: ReaderAction) {
        when (action) {
            ReaderAction.ToggleChrome -> chrome.value = chrome.value.copy(
                visible = !chrome.value.visible,
            )
            ReaderAction.HideChrome -> chrome.value = chrome.value.copy(visible = false)
            ReaderAction.NextPage -> moveBy(1)
            ReaderAction.PrevPage -> moveBy(-1)
            is ReaderAction.SeekPage -> moveTo(action.index, hideChrome = false)
            is ReaderAction.PageChanged -> {
                setNavigation(action.index)
                // Swiping to a new page dismisses chrome, like a page turn —
                // but never from under the open settings sheet.
                if (!chrome.value.settingsOpen) {
                    chrome.value = chrome.value.copy(visible = false)
                }
                scheduleProgressSave(action.index)
            }
            ReaderAction.ToggleBookmark -> {
                viewModelScope.launch { repository.toggleBookmark(args.comicId) }
            }
            ReaderAction.OpenSettings -> chrome.value = chrome.value.copy(
                settingsOpen = true,
                visible = true,
            )
            ReaderAction.CloseSettings -> chrome.value = chrome.value.copy(settingsOpen = false)
            is ReaderAction.SetDirection -> updatePrefs { it.copy(direction = action.direction) }
            is ReaderAction.SetPageFit -> updatePrefs { it.copy(pageFit = action.fit) }
            ReaderAction.ToggleCrop -> updatePrefs { it.copy(cropMargins = !it.cropMargins) }
            ReaderAction.ToggleVolumeKeys -> updatePrefs { it.copy(volumeKeys = !it.volumeKeys) }
            ReaderAction.ToggleKeepScreenOn -> updatePrefs { it.copy(keepScreenOn = !it.keepScreenOn) }
            ReaderAction.TogglePageCounter -> updatePrefs { it.copy(showPageCounter = !it.showPageCounter) }
            ReaderAction.ToggleTapZones -> chrome.value = chrome.value.copy(
                showTapZones = !chrome.value.showTapZones,
            )
        }
    }

    private fun moveBy(delta: Int) {
        val ready = uiState.value as? ReaderUiState.Ready ?: return
        // Base on the synchronously-written navigation, not the combined state,
        // so back-to-back turns never read a stale index.
        val base = navigation.value ?: ready.pageIndex
        moveTo(base + delta, hideChrome = true)
    }

    private fun moveTo(index: Int, hideChrome: Boolean) {
        val ready = uiState.value as? ReaderUiState.Ready ?: return
        val clamped = index.coerceIn(0, ready.pageCount - 1)
        setNavigation(clamped)
        // Buttons and zone taps dismiss chrome like a page turn; the slider keeps
        // chrome up so scrubbing stays visible (auto-hide resumes afterwards).
        chrome.value = chrome.value.copy(visible = !hideChrome)
        scheduleProgressSave(clamped)
    }

    private fun setNavigation(index: Int) {
        navigation.value = index
        savedStateHandle[SAVED_PAGE_INDEX] = index
    }

    private fun scheduleProgressSave(index: Int) {
        pendingSave = index
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(PROGRESS_SAVE_DEBOUNCE_MS)
            pendingSave = null
            repository.saveProgress(args.comicId, index)
        }
    }

    private fun updatePrefs(transform: (ReaderPreferences) -> ReaderPreferences) {
        viewModelScope.launch {
            preferences.updateReaderPreferences(transform)
        }
    }

    private data class ChromeState(
        val visible: Boolean = true,
        val settingsOpen: Boolean = false,
        val showTapZones: Boolean = false,
    )

    companion object {
        private const val PROGRESS_SAVE_DEBOUNCE_MS = 500L

        private const val SAVED_PAGE_INDEX = "mori_saved_page_index"

        /** First-launch overview beat: chrome stays up this long, then fades. */
        internal const val READER_OVERVIEW_MS = 2000L
    }
}
