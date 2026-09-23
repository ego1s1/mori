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
        /** Open collection dialog, if any. */
        val collectionDialog: CollectionDialog? = null,
    ) : LibraryUiState {
        val isEmpty: Boolean get() = comics.isEmpty()
    }
}

/** Collection dialogs: create vs delete-confirm. */
sealed interface CollectionDialog {
    data object Create : CollectionDialog

    data class Delete(val collectionId: Long, val name: String) : CollectionDialog
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

    data object OpenCreateCollection : LibraryAction

    data object CloseCollectionDialog : LibraryAction

    data class CreateCollection(val name: String) : LibraryAction

    data class OpenDeleteCollection(val collectionId: Long, val name: String) : LibraryAction

    data class ConfirmDeleteCollection(val collectionId: Long) : LibraryAction
}

/** One-shot library messages; the UI maps each to localized copy. */
sealed interface LibraryMessage {
    data class IndexFailed(val failed: Int) : LibraryMessage

    data object RescanFailed : LibraryMessage
}
