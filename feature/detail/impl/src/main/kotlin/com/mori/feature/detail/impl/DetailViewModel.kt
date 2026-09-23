package com.mori.feature.detail.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mori.core.data.ComicsRepository
import com.mori.core.model.Comic
import com.mori.feature.detail.api.DetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
internal class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ComicsRepository,
) : ViewModel() {

    private val args: DetailRoute = savedStateHandle.toRoute<DetailRoute>()

    private val refreshing = MutableStateFlow(false)
    /**
     * Serializes refresh runs: rapid rescan taps queue instead of the
     * second tap being dropped behind the running refresh.
     */
    private val refreshMutex = Mutex()

    /**
     * Serializes removals: rapid double-confirms delete once instead of
     * enqueueing duplicate remove calls behind the first.
     */
    private val removeMutex = Mutex()
    private val confirmRemove = MutableStateFlow(false)
    private val removed = MutableStateFlow(false)

    /** One-shot messages; a channel so rotation never reshows what was seen. */
    private val messageChannel = Channel<DetailMessage>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    private val shelvesOpen = MutableStateFlow(false)

    /**
     * Shelves dialog state: emitted only while open so membership streams
     * don't run for every detail visit.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val shelvesSheet: StateFlow<ShelvesSheet?> = shelvesOpen
        .flatMapLatest { open ->
            if (!open) {
                kotlinx.coroutines.flow.flowOf(null)
            } else {
                combine(
                    repository.observeCollections(),
                    repository.observeComicCollections(args.comicId),
                    ::ShelvesSheet,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeComic(args.comicId),
        refreshing,
        confirmRemove,
        removed,
        shelvesSheet,
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
        shelves: ShelvesSheet?,
    ): DetailUiState {
        if (removed || comic == null) return DetailUiState.Missing
        return DetailUiState.Ready(
            comic = comic,
            refreshing = refreshing,
            confirmRemove = confirmRemove,
            removed = false,
            shelves = shelves,
        )
    }

    fun onAction(action: DetailAction) {
        when (action) {
            DetailAction.Refresh -> refresh()
            DetailAction.AskRemove -> confirmRemove.value = true
            DetailAction.CancelRemove -> confirmRemove.value = false
            DetailAction.ConfirmRemove -> remove()
            DetailAction.ToggleBookmark -> {
                // Bookmarks belong to a row: ignore taps once the comic is
                // gone instead of toggling a phantom id.
                if (uiState.value !is DetailUiState.Ready) return
                viewModelScope.launch { repository.toggleBookmark(args.comicId) }
            }
            DetailAction.Share -> {
                val comic = (uiState.value as? DetailUiState.Ready)?.comic ?: return
                viewModelScope.launch {
                    messageChannel.send(
                        DetailMessage.ShareFile(
                            uri = comic.sourcePath,
                            displayName = comic.sourceDisplayName,
                            mimeType = shareMimeType(comic.sourceDisplayName),
                        ),
                    )
                }
            }
            DetailAction.OpenShelves -> shelvesOpen.value = true
            DetailAction.CloseShelves -> shelvesOpen.value = false
            is DetailAction.ToggleShelfMember -> toggleShelfMember(action.collectionId)
            is DetailAction.CreateShelf -> createShelf(action.name)
        }
    }

    private fun toggleShelfMember(collectionId: Long) {
        viewModelScope.launch {
            val members = repository.observeComicCollections(args.comicId).first()
            if (collectionId in members) {
                repository.removeFromCollection(collectionId, args.comicId)
            } else {
                repository.addToCollection(collectionId, args.comicId)
            }
        }
    }

    private fun createShelf(name: String) {
        viewModelScope.launch {
            val id = runCatching { repository.createCollection(name) }.getOrNull()
                ?: return@launch
            runCatching { repository.addToCollection(id, args.comicId) }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            refreshMutex.withLock {
                refreshing.value = true
                try {
                    // A null return means the document vanished: surface it like
                    // any other rescan failure instead of clearing the spinner
                    // silently.
                    if (repository.refreshComic(args.comicId) == null) {
                        messageChannel.send(DetailMessage.RescanFailed)
                    }
                } catch (e: Exception) {
                    messageChannel.send(DetailMessage.RescanFailed)
                } finally {
                    refreshing.value = false
                }
            }
        }
    }

    private fun remove() {
        viewModelScope.launch {
            removeMutex.withLock {
                // Second confirm while the first is in flight is a no-op:
                // the row is already going away.
                if (removed.value) return@withLock
                try {
                    repository.removeComic(args.comicId)
                    removed.value = true
                } catch (e: Exception) {
                    confirmRemove.value = false
                    messageChannel.send(DetailMessage.RemoveFailed)
                }
            }
        }
    }
}
