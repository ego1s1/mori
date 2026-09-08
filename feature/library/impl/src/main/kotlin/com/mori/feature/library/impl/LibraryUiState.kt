package com.mori.feature.library.impl

sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data class Ready(val message: String) : LibraryUiState
}
