package com.mori.feature.detail.impl

import com.mori.core.model.Comic
import com.mori.core.model.UserCollection

/** Shelves dialog content: all shelves plus this book's membership. */
data class ShelvesSheet(
    val collections: List<UserCollection>,
    val memberIds: Set<Long>,
)

sealed interface DetailUiState {
    data object Loading : DetailUiState

    data class Ready(
        val comic: Comic,
        val refreshing: Boolean,
        val confirmRemove: Boolean,
        val removed: Boolean,
        /** Open shelves dialog state; null when closed. */
        val shelves: ShelvesSheet? = null,
    ) : DetailUiState

    /** The comic disappeared from the index (removed elsewhere). */
    data object Missing : DetailUiState
}

sealed interface DetailAction {
    data object Refresh : DetailAction

    data object AskRemove : DetailAction

    data object CancelRemove : DetailAction

    data object ConfirmRemove : DetailAction

    data object ToggleBookmark : DetailAction

    data object Share : DetailAction

    data object OpenShelves : DetailAction

    data object CloseShelves : DetailAction

    data class ToggleShelfMember(val collectionId: Long) : DetailAction

    data class CreateShelf(val name: String) : DetailAction
}

/** One-shot detail messages; the UI maps each to localized copy. */
sealed interface DetailMessage {
    data object RescanFailed : DetailMessage

    data object RemoveFailed : DetailMessage

    /** Open the Sharesheet for the comic file (URI + display name + MIME). */
    data class ShareFile(
        val uri: String,
        val displayName: String,
        val mimeType: String,
    ) : DetailMessage
}
