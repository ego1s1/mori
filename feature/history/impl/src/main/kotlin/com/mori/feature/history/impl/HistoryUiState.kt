package com.mori.feature.history.impl

import com.mori.core.model.HistoryDay

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    data class Success(
        val days: List<HistoryDay>,
        val queryText: String,
        /** Collapsible search field open (mirrors the library toggle). */
        val searchOpen: Boolean = false,
    ) : HistoryUiState {
        val isEmpty: Boolean get() = days.isEmpty()

        /** Searching with zero matches is "no results", not "no history". */
        val isNoResults: Boolean get() = isEmpty && queryText.isNotBlank()
    }
}

sealed interface HistoryAction {
    data class SearchTextChanged(val text: String) : HistoryAction

    data object ClearSearch : HistoryAction

    data object ToggleSearch : HistoryAction
}
