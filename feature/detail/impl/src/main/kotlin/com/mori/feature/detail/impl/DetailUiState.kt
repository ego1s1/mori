package com.mori.feature.detail.impl

sealed interface DetailUiState {
    data object Loading : DetailUiState

    data class Ready(val message: String) : DetailUiState
}
