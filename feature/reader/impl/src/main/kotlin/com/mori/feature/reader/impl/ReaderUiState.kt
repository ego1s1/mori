package com.mori.feature.reader.impl

sealed interface ReaderUiState {
    data object Loading : ReaderUiState

    data class Ready(val message: String) : ReaderUiState
}
