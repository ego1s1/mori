package com.mori.feature.reader.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.mori.feature.reader.api.ReaderRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args: ReaderRoute = savedStateHandle.toRoute<ReaderRoute>()

    private val _uiState = MutableStateFlow<ReaderUiState>(
        ReaderUiState.Ready("Reader ${args.comicId}@${args.pageIndex} coming in F5"),
    )
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()
}
