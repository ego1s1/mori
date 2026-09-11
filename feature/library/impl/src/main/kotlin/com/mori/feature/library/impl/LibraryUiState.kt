package com.mori.feature.library.impl

import com.mori.core.model.Comic
import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibraryQuery
import com.mori.core.model.LibrarySortOrder

sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data class Success(
        val comics: List<Comic>,
        val query: LibraryQuery,
        val refreshing: Boolean,
        val filterOpen: Boolean,
        val searchOpen: Boolean,
        /** A source tree is linked: rescan and rescue paths apply. */
        val linked: Boolean,
    ) : LibraryUiState {
        val isEmpty: Boolean get() = comics.isEmpty()
    }
}

sealed interface LibraryAction {
    data class SearchTextChanged(val text: String) : LibraryAction

    data class SortSelected(val sort: LibrarySortOrder) : LibraryAction

    data class FilterSelected(val filter: LibraryFilter) : LibraryAction

    data class ToggleHideErrors(val hide: Boolean) : LibraryAction

    data object OpenFilter : LibraryAction

    data object CloseFilter : LibraryAction

    data object ToggleSearch : LibraryAction

    data object Refresh : LibraryAction

    /** Link a folder straight from the empty shelf (post-onboarding rescue). */
    data class FolderSelected(val uri: android.net.Uri) : LibraryAction
}

/** One-shot library messages; the UI maps each to localized copy. */
sealed interface LibraryMessage {
    data class IndexFailed(val failed: Int) : LibraryMessage

    data object RescanFailed : LibraryMessage
}
