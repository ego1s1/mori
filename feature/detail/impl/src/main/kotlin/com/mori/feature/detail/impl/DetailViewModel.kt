package com.mori.feature.detail.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.mori.feature.detail.api.DetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args: DetailRoute = savedStateHandle.toRoute<DetailRoute>()

    private val _uiState =
        MutableStateFlow<DetailUiState>(DetailUiState.Ready("Detail ${args.comicId} coming in F4"))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
}
