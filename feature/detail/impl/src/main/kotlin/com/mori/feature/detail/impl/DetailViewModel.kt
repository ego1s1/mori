package com.mori.feature.detail.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mori.core.data.ComicsRepository
import com.mori.core.model.Comic
import com.mori.feature.detail.api.DetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ComicsRepository,
) : ViewModel() {

    private val args: DetailRoute = savedStateHandle.toRoute<DetailRoute>()

    private val refreshing = MutableStateFlow(false)
    private val confirmRemove = MutableStateFlow(false)
    private val removed = MutableStateFlow(false)

    /** One-shot messages; a channel so rotation never reshows what was seen. */
    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeComic(args.comicId),
        refreshing,
        confirmRemove,
        removed,
        ::toUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState.Loading,
    )

    private fun toUiState(
        comic: Comic?,
        refreshing: Boolean,
        confirmRemove: Boolean,
        removed: Boolean,
    ): DetailUiState {
        if (removed || comic == null) return DetailUiState.Missing
        return DetailUiState.Ready(
            comic = comic,
            refreshing = refreshing,
            confirmRemove = confirmRemove,
            removed = false,
        )
    }

    fun onAction(action: DetailAction) {
        when (action) {
            DetailAction.Refresh -> refresh()
            DetailAction.AskRemove -> confirmRemove.value = true
            DetailAction.CancelRemove -> confirmRemove.value = false
            DetailAction.ConfirmRemove -> remove()
        }
    }

    private fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            try {
                repository.refreshComic(args.comicId)
            } catch (e: Exception) {
                messageChannel.send("Rescan failed. Try again.")
            } finally {
                refreshing.value = false
            }
        }
    }

    private fun remove() {
        viewModelScope.launch {
            try {
                repository.removeComic(args.comicId)
                removed.value = true
            } catch (e: Exception) {
                confirmRemove.value = false
                messageChannel.send("Couldn't remove this comic. Try again.")
            }
        }
    }
}
