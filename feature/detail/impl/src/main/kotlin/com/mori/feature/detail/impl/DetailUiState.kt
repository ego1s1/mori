package com.mori.feature.detail.impl

import com.mori.core.model.Comic

sealed interface DetailUiState {
    data object Loading : DetailUiState

    data class Ready(
        val comic: Comic,
        val refreshing: Boolean,
        val confirmRemove: Boolean,
        val removed: Boolean,
        val snackbar: String?,
    ) : DetailUiState

    /** The comic disappeared from the index (removed elsewhere). */
    data object Missing : DetailUiState
}

sealed interface DetailAction {
    data object Refresh : DetailAction

    data object AskRemove : DetailAction

    data object CancelRemove : DetailAction

    data object ConfirmRemove : DetailAction

    data object DismissSnackbar : DetailAction
}
