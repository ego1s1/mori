package com.mori.feature.library.impl

import com.mori.core.model.Comic
import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibraryQuery
import com.mori.core.model.LibrarySortOrder
import com.mori.core.model.UserCollection

sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data class Success(
        val comics: List<Comic>,
        val query: LibraryQuery,
        val refreshing: Boolean,
        val filterOpen: Boolean,
        val searchOpen: Boolean,
        val linked: Boolean,
        /** In-progress books by recency, backing the continue shelf. */
        val continueReading: List<Comic>,
        /** Determinate rescan progress (done/total); null when idle. */
        val indexProgress: IndexProgress? = null,
        /** All user shelves for the chips row. */
        val collections: List<UserCollection> = emptyList(),
        /** Selected shelf filter; null shows everything. */
        val selectedCollectionId: Long? = null,
        /**
         * Sectioned grid (only when no shelf is selected and shelves exist):
         * one entry per non-empty shelf plus a trailing unsorted section.
         * Empty when the flat grid applies.
         */
        val sections: List<ShelfSection> = emptyList(),
    ) : LibraryUiState {
        val isEmpty: Boolean get() = comics.isEmpty()
    }
}

/**
 * One collapsible library section: a shelf and its visible books in grid
 * order. [collection] null marks the trailing unsorted section.
 */
data class ShelfSection(
    val collection: UserCollection?,
    val comics: List<Comic>,
    val collapsed: Boolean,
) {
    /** Stable identity for headers (-1 for unsorted). */
    val id: Long get() = collection?.id ?: UNSORTED_SHELF_ID

    companion object {
        const val UNSORTED_SHELF_ID = -1L
    }
}

/** Determinate rescan progress forwarded from the index callback. */
data class IndexProgress(val done: Int, val total: Int)

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

    /** Filter the grid to one shelf; null clears back to everything. */
    data class SelectCollection(val collectionId: Long?) : LibraryAction

    /** Collapse/expand one shelf section; persisted in display prefs. */
    data class ToggleShelfCollapsed(val collectionId: Long) : LibraryAction
}

/** One-shot library messages; the UI maps each to localized copy. */
sealed interface LibraryMessage {
    data class IndexFailed(val failed: Int) : LibraryMessage

    data object RescanFailed : LibraryMessage
}
