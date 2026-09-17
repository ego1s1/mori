package com.mori.feature.library.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.core.data.ComicsRepository
import com.mori.core.datastore.MoriPreferencesDataSource
import com.mori.core.model.Comic
import com.mori.core.model.LibraryDisplay
import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibraryQuery
import com.mori.core.model.LibrarySortOrder
import com.mori.core.model.continueShelf
import com.mori.core.model.resumeTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ComicsRepository,
    private val preferences: MoriPreferencesDataSource,
) : ViewModel() {

    /**
     * Effective query: persisted display options (sort/filter/errors, survive
     * restarts) overlaid with ephemeral search text (restored across process
     * death via [SavedStateHandle], cleared on full restart).
     */
    private val searchText = MutableStateFlow(
        savedStateHandle.get<String>(KEY_QUERY_TEXT).orEmpty(),
    )
    private val query: StateFlow<LibraryQuery> = combine(
        preferences.libraryDisplay,
        searchText,
        LibraryDisplay::toQuery,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryQuery(),
    )
    private val refreshing = MutableStateFlow(false)
    /**
     * Serializes reindex runs: a folder pick is never dropped behind a
     * running rescan, and rapid refresh taps queue instead of overlapping
     * index writes. The counter keeps the spinner up across queued runs —
     * a finisher never clears it while another run is still parked.
     */
    private val reindexMutex = Mutex()
    private val reindexPending = AtomicInteger(0)
    /** Last reported index callback; cleared when no run is active. */
    private val indexProgress = MutableStateFlow<IndexProgress?>(null)
    private val filterOpen = MutableStateFlow(false)
    private val searchOpen = MutableStateFlow(false)

    /**
     * Database subscription query: the text field echoes instantly through
     * [query], but the grid re-queries at most once per typing pause instead
     * of once per keystroke. Empty text (initial load, cleared search) passes
     * through with no delay.
     */
    private val dbQuery: Flow<LibraryQuery> = combine(
        preferences.libraryDisplay,
        searchText.debounce { text -> if (text.isEmpty()) 0L else SEARCH_DEBOUNCE_MS },
        LibraryDisplay::toQuery,
    )

    /**
     * One-shot messages (errors, confirmations). A channel, not state: rotation
     * must not reshow a message the user already saw.
     */
    private val messageChannel = Channel<LibraryMessage>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    /**
     * Shared list subscription: one DB observer feeding both the screen and
     * the resume candidate, so chrome-only changes never touch this pipeline.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val comics: StateFlow<List<Comic>> = dbQuery
        .flatMapLatest { repository.observeLibrary(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val uiState: StateFlow<LibraryUiState> = combine(
        comics,
        query,
        combine(refreshing, filterOpen, searchOpen, ::Chrome),
        preferences.sourceTreeUri,
        indexProgress,
    ) { comics, query, chrome, treeUri, progress ->
        toUiState(comics, query, chrome, linked = treeUri != null, progress = progress)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState.Loading,
    )

    /**
     * Most recently touched comic; the resume button opens it at its saved
     * page. Cached here — not a per-read scan — and re-emitted only when the
     * comic identity or saved page changes, so chrome-only emissions never
     * rescan the list or bounce the shell.
     */
    val resumeTarget: StateFlow<Comic?> = comics
        .map { list -> list.resumeTarget() }
        .distinctUntilChanged { a, b ->
            a?.id == b?.id && a?.lastPageIndex == b?.lastPageIndex && a?.error == b?.error
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private fun toUiState(
        comics: List<Comic>,
        query: LibraryQuery,
        chrome: Chrome,
        linked: Boolean,
        progress: IndexProgress?,
    ): LibraryUiState = LibraryUiState.Success(
        comics = comics,
        query = query,
        refreshing = chrome.refreshing,
        filterOpen = chrome.filterOpen,
        searchOpen = chrome.searchOpen,
        linked = linked,
        continueReading = comics.continueShelf(),
        indexProgress = progress,
    )

    init {
        // Rescan on every launch: the library reads user folders in place,
        // so a launch pass picks up files added, moved or removed outside
        // the app. With no tree linked there is nothing to rescan; manual
        // rescans stay on pull-to-refresh (plus the empty-state button).
        viewModelScope.launch {
            if (preferences.sourceTreeUri.first() == null) return@launch
            reindex()
        }
    }

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.SearchTextChanged -> {
                searchText.value = action.text
                savedStateHandle[KEY_QUERY_TEXT] = action.text
            }
            is LibraryAction.SortSelected -> updateDisplay { it.copy(sortOrder = action.sort) }
            is LibraryAction.FilterSelected -> updateDisplay { it.copy(filter = action.filter) }
            is LibraryAction.ToggleHideErrors -> updateDisplay { it.copy(hideErrors = action.hide) }
            LibraryAction.OpenFilter -> filterOpen.value = true
            LibraryAction.CloseFilter -> filterOpen.value = false
            LibraryAction.ToggleSearch -> searchOpen.update { !it }
            LibraryAction.Refresh -> reindex()
            is LibraryAction.FolderSelected -> reindex(linkUri = action.uri.toString())
        }
    }

    /** Ephemeral chrome state kept out of the query/data flows. */
    private data class Chrome(
        val refreshing: Boolean,
        val filterOpen: Boolean,
        val searchOpen: Boolean,
    )

    private fun updateDisplay(transform: (LibraryDisplay) -> LibraryDisplay) {
        viewModelScope.launch {
            preferences.updateLibraryDisplay(transform)
        }
    }

    /**
     * Link-only rescan: re-indexes a tree in place — nothing is ever copied.
     * With no tree linked there is nothing to rescan. [linkUri] persists a
     * freshly picked folder first, so a pick during a running rescan is
     * never dropped: it queues behind the lock instead.
     */
    private fun reindex(linkUri: String? = null) {
        viewModelScope.launch {
            if (linkUri != null) preferences.setSourceTreeUri(linkUri)
            // Raised before parking: queued runs show the spinner instead
            // of a dead gap. withLock (not manual lock/unlock) releases
            // the mutex on cancellation instead of deadlocking the next run.
            reindexPending.incrementAndGet()
            refreshing.value = true
            try {
                reindexMutex.withLock {
                    try {
                        val treeUri = preferences.sourceTreeUri.first() ?: return@withLock
                        val failed = repository.indexLinkedTree(android.net.Uri.parse(treeUri)) { done, total ->
                            indexProgress.value = IndexProgress(done, total)
                        }.failed
                        if (failed > 0) {
                            messageChannel.send(LibraryMessage.IndexFailed(failed))
                        }
                    } catch (e: Exception) {
                        messageChannel.send(LibraryMessage.RescanFailed)
                    }
                }
            } finally {
                if (reindexPending.decrementAndGet() == 0) {
                    refreshing.value = false
                    indexProgress.value = null
                }
            }
        }
    }

    private companion object {
        const val KEY_QUERY_TEXT = "mori_query_text"
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
