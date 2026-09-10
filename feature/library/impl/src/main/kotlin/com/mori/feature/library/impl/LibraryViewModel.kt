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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
     * One-shot messages (errors, confirmations). A channel, not state: rotation
     * must not reshow a message the user already saw.
     */
    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LibraryUiState> = combine(
        query.flatMapLatest { repository.observeLibrary(it) },
        query,
        combine(refreshing, filterOpen, searchOpen, ::Chrome),
    ) { comics, query, chrome ->
        toUiState(comics, query, chrome)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState.Loading,
    )

    private fun toUiState(
        comics: List<Comic>,
        query: LibraryQuery,
        chrome: Chrome,
    ): LibraryUiState = LibraryUiState.Success(
        comics = comics,
        query = query,
        refreshing = chrome.refreshing,
        filterOpen = chrome.filterOpen,
        searchOpen = chrome.searchOpen,
    )

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
                val report = repository.refreshLibrary()
                if (report.failed > 0) {
                    messageChannel.send("Couldn't index ${report.failed} file(s). Check the files and rescan.")
                }
            } catch (e: Exception) {
                messageChannel.send("Rescan failed. Try again.")
            } finally {
                refreshing.value = false
            }
        }
    }

    private companion object {
        const val KEY_QUERY_TEXT = "mori_query_text"
    }
}
