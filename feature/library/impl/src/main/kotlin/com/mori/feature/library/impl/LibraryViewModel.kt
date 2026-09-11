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
    ) { comics, query, chrome, treeUri ->
        toUiState(comics, query, chrome, linked = treeUri != null)
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
        .map { list -> list.maxByOrNull { it.updatedAt } }
        .distinctUntilChanged { a, b ->
            a?.id == b?.id && a?.lastPageIndex == b?.lastPageIndex
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
    ): LibraryUiState = LibraryUiState.Success(
        comics = comics,
        query = query,
        refreshing = chrome.refreshing,
        filterOpen = chrome.filterOpen,
        searchOpen = chrome.searchOpen,
        linked = linked,
    )

    init {
        // Lazy first index: onboarding navigates straight through with no
        // progress screens, so an empty shelf with a linked tree indexes
        // itself once here and fills live. Manual rescans cover the rest. A
        // fresh subscription (not the shared flow's replay) decides
        // emptiness, so a populated shelf never re-indexes on cold start.
        viewModelScope.launch {
            val treeUri = preferences.sourceTreeUri.first() ?: return@launch
            if (repository.observeLibrary(LibraryQuery()).first().isNotEmpty()) return@launch
            refreshing.value = true
            try {
                runCatching {
                    repository.indexLinkedTree(android.net.Uri.parse(treeUri)) { _, _ -> }
                }
            } finally {
                refreshing.value = false
            }
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
            LibraryAction.Refresh -> refresh()
            is LibraryAction.FolderSelected -> linkFolder(action.uri.toString())
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

    private fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            try {
                // Link-only rescan: re-index the persisted tree in place.
                // Nothing is ever copied; with no tree linked there is
                // nothing to rescan.
                val treeUri = preferences.sourceTreeUri.first() ?: return@launch
                val failed = repository.indexLinkedTree(android.net.Uri.parse(treeUri)) { _, _ -> }.failed
                if (failed > 0) {
                    messageChannel.send(LibraryMessage.IndexFailed(failed))
                }
            } catch (e: Exception) {
                messageChannel.send(LibraryMessage.RescanFailed)
            } finally {
                refreshing.value = false
            }
        }
    }

    /** Post-onboarding rescue: link a folder straight from the empty shelf. */
    private fun linkFolder(treeUri: String) {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            try {
                preferences.setSourceTreeUri(treeUri)
                val failed = repository.indexLinkedTree(android.net.Uri.parse(treeUri)) { _, _ -> }.failed
                if (failed > 0) {
                    messageChannel.send(LibraryMessage.IndexFailed(failed))
                }
            } catch (e: Exception) {
                messageChannel.send(LibraryMessage.RescanFailed)
            } finally {
                refreshing.value = false
            }
        }
    }

    private companion object {
        const val KEY_QUERY_TEXT = "mori_query_text"
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
