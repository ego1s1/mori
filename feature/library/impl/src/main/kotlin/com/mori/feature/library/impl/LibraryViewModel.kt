package com.mori.feature.library.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.core.data.ComicsRepository
import com.mori.core.model.Comic
import com.mori.core.model.LibraryQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: ComicsRepository,
) : ViewModel() {

    private val query = MutableStateFlow(LibraryQuery())
    private val refreshing = MutableStateFlow(false)
    private val filterOpen = MutableStateFlow(false)
    private val snackbar = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LibraryUiState> = combine(
        query.flatMapLatest { repository.observeLibrary(it) },
        query,
        combine(refreshing, filterOpen, snackbar, ::Chrome),
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
        snackbar = chrome.snackbar,
    )

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.SearchTextChanged -> query.update { it.copy(text = action.text) }
            is LibraryAction.SortSelected -> query.update { it.copy(sortOrder = action.sort) }
            is LibraryAction.FilterSelected -> query.update { it.copy(filter = action.filter) }
            is LibraryAction.ToggleHideErrors -> query.update { it.copy(hideErrors = action.hide) }
            LibraryAction.OpenFilter -> filterOpen.value = true
            LibraryAction.CloseFilter -> filterOpen.value = false
            LibraryAction.Refresh -> refresh()
            LibraryAction.DismissSnackbar -> snackbar.value = null
        }
    }

    /** Ephemeral chrome state kept out of the query/data flows. */
    private data class Chrome(
        val refreshing: Boolean,
        val filterOpen: Boolean,
        val snackbar: String?,
    )

    private fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            try {
                val report = repository.refreshLibrary()
                if (report.failed > 0) {
                    snackbar.value = "${report.failed} file(s) could not be indexed"
                }
            } catch (e: Exception) {
                snackbar.value = e.message ?: "Refresh failed"
            } finally {
                refreshing.value = false
            }
        }
    }
}
