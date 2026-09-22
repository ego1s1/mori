package com.mori.feature.history.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.core.data.ComicsRepository
import com.mori.core.model.HistoryDay
import com.mori.core.model.LibraryQuery
import com.mori.core.model.historyGroups
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject

/**
 * Reading history, Mihon-style but chapter-free: one entry per touched
 * comic, ordered by recency and bucketed by local day. Progress saves are
 * the tracking event — opening past the cover lands a book here — so no
 * separate history writes exist. Taps route through the same error gate as
 * the library cards: errored rows open details, never the reader.
 */
@HiltViewModel
internal class HistoryViewModel @Inject constructor(
    repository: ComicsRepository,
) : ViewModel() {

    private val searchText = MutableStateFlow("")

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeLibrary(LibraryQuery()),
        searchText,
    ) { comics, text ->
        toUiState(comics.historyGroups(), text)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState.Loading,
    )

    private fun toUiState(days: List<HistoryDay>, text: String): HistoryUiState {
        if (text.isBlank()) return HistoryUiState.Success(days, text)
        // ROOT folding on both sides: default-locale casing (Turkish dotted
        // I) must never split a match asymmetrically.
        val needle = text.trim().lowercase(Locale.ROOT)
        val filtered = days.mapNotNull { day ->
            val matches = day.comics.filter { comic ->
                comic.title.lowercase(Locale.ROOT).contains(needle) ||
                    comic.series?.lowercase(Locale.ROOT)?.contains(needle) == true
            }
            if (matches.isEmpty()) null else day.copy(comics = matches)
        }
        return HistoryUiState.Success(filtered, text)
    }

    fun onAction(action: HistoryAction) {
        when (action) {
            is HistoryAction.SearchTextChanged -> searchText.value = action.text
            HistoryAction.ClearSearch -> searchText.value = ""
        }
    }
}
