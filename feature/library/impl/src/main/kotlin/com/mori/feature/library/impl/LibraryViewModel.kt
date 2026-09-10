package com.mori.feature.library.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.core.data.ComicsRepository
import com.mori.core.model.Comic
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
) : ViewModel() {

    private val query = MutableStateFlow(restoreQuery())
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
            is LibraryAction.SearchTextChanged -> updateQuery { it.copy(text = action.text) }
            is LibraryAction.SortSelected -> updateQuery { it.copy(sortOrder = action.sort) }
            is LibraryAction.FilterSelected -> updateQuery { it.copy(filter = action.filter) }
            is LibraryAction.ToggleHideErrors -> updateQuery { it.copy(hideErrors = action.hide) }
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

    private fun updateQuery(transform: (LibraryQuery) -> LibraryQuery) {
        val updated = transform(query.value)
        query.value = updated
        savedStateHandle[KEY_QUERY_TEXT] = updated.text
        savedStateHandle[KEY_QUERY_SORT] = updated.sortOrder.name
        savedStateHandle[KEY_QUERY_FILTER] = updated.filter.name
        savedStateHandle[KEY_QUERY_HIDE_ERRORS] = updated.hideErrors
    }

    private fun restoreQuery(): LibraryQuery {
        val sort = savedStateHandle.get<String>(KEY_QUERY_SORT)?.let {
            runCatching { LibrarySortOrder.valueOf(it) }.getOrNull()
        } ?: LibraryQuery().sortOrder
        val filter = savedStateHandle.get<String>(KEY_QUERY_FILTER)?.let {
            runCatching { LibraryFilter.valueOf(it) }.getOrNull()
        } ?: LibraryQuery().filter
        return LibraryQuery(
            text = savedStateHandle.get<String>(KEY_QUERY_TEXT).orEmpty(),
            sortOrder = sort,
            filter = filter,
            hideErrors = savedStateHandle.get<Boolean>(KEY_QUERY_HIDE_ERRORS) ?: false,
        )
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
        const val KEY_QUERY_SORT = "mori_query_sort"
        const val KEY_QUERY_FILTER = "mori_query_filter"
        const val KEY_QUERY_HIDE_ERRORS = "mori_query_hide_errors"
    }
}
