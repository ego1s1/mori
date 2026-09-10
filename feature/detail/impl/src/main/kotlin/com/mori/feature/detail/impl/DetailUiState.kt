package com.mori.feature.detail.impl

import com.mori.core.model.Comic

sealed interface DetailUiState {
    data object Loading : DetailUiState

    data class Ready(
        val comic: Comic,
        val refreshing: Boolean,
        val confirmRemove: Boolean,
        val removed: Boolean,
    ) : DetailUiState

    /** The comic disappeared from the index (removed elsewhere). */
    data object Missing : DetailUiState
}

sealed interface DetailAction {
    data object Refresh : DetailAction

    data object AskRemove : DetailAction

    data object CancelRemove : DetailAction

    data object ConfirmRemove : DetailAction
}

/** One-shot detail messages; the UI maps each to localized copy. */
sealed interface DetailMessage {
    data object RescanFailed : DetailMessage

    data object RemoveFailed : DetailMessage
}
