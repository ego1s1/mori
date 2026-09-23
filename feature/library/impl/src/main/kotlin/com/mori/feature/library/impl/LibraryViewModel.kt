package com.mori.feature.library.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.core.data.ComicsRepository
import com.mori.core.datastore.MoriPreferencesDataSource
import com.mori.core.model.Comic
import com.mori.core.model.LibraryDisplay
import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibraryQuery
import com.mori.core.model.LibrarySortOrder
import com.mori.core.model.UserCollection
import com.mori.core.model.continueShelf
import com.mori.core.model.resumeTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ComicsRepository,
    private val preferences: MoriPreferencesDataSource,
) : ViewModel() {

    /**
     * Effective query: persisted display options (sort/filter/errors, survive
     * restarts) overlaid with ephemeral search text (restored across process
     * death via [SavedStateHandle], cleared on full restart).
     */
    private val searchText = MutableStateFlow(
        savedStateHandle.get<String>(KEY_QUERY_TEXT).orEmpty(),
    )
    private val query: StateFlow<LibraryQuery> = combine(
        preferences.libraryDisplay,
        searchText,
        LibraryDisplay::toQuery,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryQuery(),
    )
    private val refreshing = MutableStateFlow(false)
    /**
     * Serializes reindex runs: a folder pick is never dropped behind a
     * running rescan, and rapid refresh taps queue instead of overlapping
     * index writes. The counter keeps the spinner up across queued runs —
     * a finisher never clears it while another run is still parked.
     */
    private val reindexMutex = Mutex()
    private val reindexPending = AtomicInteger(0)

    /** A refresh tap that arrived mid-run, folded into one follow-up pass. */
    private val reindexQueued = AtomicBoolean(false)
    /** Last reported index callback; cleared when no run is active. */
    private val indexProgress = MutableStateFlow<IndexProgress?>(null)
    private val filterOpen = MutableStateFlow(false)
    private val searchOpen = MutableStateFlow(false)

    /**
     * Database subscription query: the text field echoes instantly through
     * [query], but the grid re-queries at most once per typing pause instead
     * of once per keystroke. Empty text (initial load, cleared search) passes
     * through with no delay.
     */
    private val dbQuery: Flow<LibraryQuery> = combine(
        preferences.libraryDisplay,
        searchText.debounce { text -> if (text.isEmpty()) 0L else SEARCH_DEBOUNCE_MS },
        LibraryDisplay::toQuery,
    )

    /**
     * One-shot messages (errors, confirmations). A channel, not state: rotation
     * must not reshow a message the user already saw.
     */
    private val messageChannel = Channel<LibraryMessage>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    /**
     * Shared list subscription: one DB observer feeding both the screen and
     * the resume candidate, so chrome-only changes never touch this pipeline.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val comics: StateFlow<List<Comic>> = dbQuery
        .flatMapLatest { repository.observeLibrary(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Shelf-filter state: shelves, selection, members, and open dialog. */
    private data class CollectionsState(
        val collections: List<UserCollection>,
        val selectedId: Long?,
        val memberIds: Set<String>?,
        val dialog: CollectionDialog?,
    )

    /**
     * Selected shelf filter (ephemeral, restored across process death).
     * Members stream only while a shelf is selected.
     */
    private val selectedCollection =
        MutableStateFlow(savedStateHandle.get<Long>(KEY_COLLECTION))

    private val collectionDialog = MutableStateFlow<CollectionDialog?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val collectionMembers: StateFlow<Set<String>?> = selectedCollection
        .flatMapLatest { id ->
            if (id == null) {
                kotlinx.coroutines.flow.flowOf(null)
            } else {
                repository.observeCollectionMembers(id)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val collectionsState: StateFlow<CollectionsState> = combine(
        repository.observeCollections(),
        selectedCollection,
        collectionMembers,
        collectionDialog,
        ::CollectionsState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CollectionsState(emptyList(), null, null, null),
    )

    val uiState: StateFlow<LibraryUiState> = combine(
        combine(
            comics,
            query,
            combine(refreshing, filterOpen, searchOpen, ::Chrome),
            preferences.sourceTreeUri,
            indexProgress,
        ) { comics, query, chrome, treeUri, progress ->
            LibraryBase(comics, query, chrome, treeUri != null, progress)
        },
        collectionsState,
    ) { base, collections ->
        toUiState(base, collections)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState.Loading,
    )

    /**
     * Most recently touched comic; the resume button opens it at its saved
     * page. Cached here — not a per-read scan — and re-emitted only when the
     * comic identity or saved page changes, so chrome-only emissions never
     * rescan the list or bounce the shell.
     */
    val resumeTarget: StateFlow<Comic?> = comics
        .map { list -> list.resumeTarget() }
        .distinctUntilChanged { a, b ->
            a?.id == b?.id && a?.lastPageIndex == b?.lastPageIndex && a?.error == b?.error
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private fun toUiState(
        base: LibraryBase,
        collections: CollectionsState,
    ): LibraryUiState {
        // Shelf filter applies after the query: a selected collection narrows
        // the already sorted/filtered grid (and its shelf) to members.
        val visible = if (collections.selectedId != null && collections.memberIds != null) {
            base.comics.filter { it.id in collections.memberIds }
        } else {
            base.comics
        }
        return LibraryUiState.Success(
            comics = visible,
            query = base.query,
            refreshing = base.chrome.refreshing,
            filterOpen = base.chrome.filterOpen,
            searchOpen = base.chrome.searchOpen,
            linked = base.linked,
            continueReading = visible.continueShelf(),
            indexProgress = base.progress,
            collections = collections.collections,
            selectedCollectionId = collections.selectedId,
            collectionDialog = collections.dialog,
        )
    }

    init {
        // Rescan on every launch: the library reads user folders in place,
        // so a launch pass picks up files added, moved or removed outside
        // the app. With no tree linked there is nothing to rescan; manual
        // rescans stay on pull-to-refresh (plus the empty-state button).
        viewModelScope.launch {
            if (preferences.sourceTreeUri.first() == null) return@launch
            reindex()
        }
    }

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.SearchTextChanged -> {
                searchText.value = action.text
                savedStateHandle[KEY_QUERY_TEXT] = action.text
            }
            is LibraryAction.SortSelected -> updateDisplay { it.copy(sortOrder = action.sort) }
            is LibraryAction.FilterSelected -> updateDisplay { it.copy(filter = action.filter) }
            is LibraryAction.ToggleHideErrors -> updateDisplay { it.copy(hideErrors = action.hide) }
            LibraryAction.OpenFilter -> filterOpen.value = true
            LibraryAction.CloseFilter -> filterOpen.value = false
            LibraryAction.ToggleSearch -> searchOpen.update { !it }
            LibraryAction.Refresh -> reindex()
            is LibraryAction.FolderSelected -> reindex(linkUri = action.uri.toString())
            is LibraryAction.SelectCollection -> {
                selectedCollection.value = action.collectionId
                if (action.collectionId == null) {
                    savedStateHandle.remove<Long>(KEY_COLLECTION)
                } else {
                    savedStateHandle[KEY_COLLECTION] = action.collectionId
                }
            }
            LibraryAction.OpenCreateCollection ->
                collectionDialog.value = CollectionDialog.Create
            LibraryAction.CloseCollectionDialog -> collectionDialog.value = null
            is LibraryAction.CreateCollection -> createCollection(action.name)
            is LibraryAction.OpenDeleteCollection ->
                collectionDialog.value =
                    CollectionDialog.Delete(action.collectionId, action.name)
            is LibraryAction.ConfirmDeleteCollection -> deleteCollection(action.collectionId)
        }
    }

    private fun createCollection(name: String) {
        viewModelScope.launch {
            val id = runCatching { repository.createCollection(name) }.getOrNull()
                ?: return@launch
            collectionDialog.value = null
            selectedCollection.value = id
            savedStateHandle[KEY_COLLECTION] = id
        }
    }

    private fun deleteCollection(id: Long) {
        if (selectedCollection.value == id) {
            selectedCollection.value = null
            savedStateHandle.remove<Long>(KEY_COLLECTION)
        }
        collectionDialog.value = null
        viewModelScope.launch {
            runCatching { repository.deleteCollection(id) }
        }
    }

    /** Ephemeral chrome state kept out of the query/data flows. */
    private data class Chrome(
        val refreshing: Boolean,
        val filterOpen: Boolean,
        val searchOpen: Boolean,
    )

    /** Five-flow combine carrier (fixed-arity combine caps at five). */
    private data class LibraryBase(
        val comics: List<Comic>,
        val query: LibraryQuery,
        val chrome: Chrome,
        val linked: Boolean,
        val progress: IndexProgress?,
    )

    private fun updateDisplay(transform: (LibraryDisplay) -> LibraryDisplay) {
        viewModelScope.launch {
            preferences.updateLibraryDisplay(transform)
        }
    }

    /**
     * Link-only rescan: re-indexes a tree in place — nothing is ever copied.
     * With no tree linked there is nothing to rescan. [linkUri] persists a
     * freshly picked folder first, so a pick during a running rescan is
     * never dropped: it folds into at most one follow-up run instead of
     * queueing a full pass per tap.
     */
    private fun reindex(linkUri: String? = null) {
        viewModelScope.launch {
            if (linkUri != null) preferences.setSourceTreeUri(linkUri)
            if (reindexMutex.isLocked) {
                // A run is active (or a follow-up already folded): merge this
                // tap into it. Folder picks persist above, so the follow-up
                // indexes the newest tree; pure refresh taps collapse to one.
                reindexQueued.set(true)
                return@launch
            }
            // Raised before parking: queued runs show the spinner instead
            // of a dead gap. withLock (not manual lock/unlock) releases
            // the mutex on cancellation instead of deadlocking the next run.
            reindexPending.incrementAndGet()
            refreshing.value = true
            try {
                do {
                    reindexQueued.set(false)
                    reindexMutex.withLock {
                        try {
                            val treeUri = preferences.sourceTreeUri.first() ?: return@withLock
                            val failed = repository.indexLinkedTree(android.net.Uri.parse(treeUri)) { done, total ->
                                indexProgress.value = IndexProgress(done, total)
                            }.failed
                            if (failed > 0) {
                                messageChannel.send(LibraryMessage.IndexFailed(failed))
                            }
                        } catch (e: Exception) {
                            messageChannel.send(LibraryMessage.RescanFailed)
                        }
                    }
                } while (reindexQueued.getAndSet(false))
            } finally {
                if (reindexPending.decrementAndGet() == 0) {
                    refreshing.value = false
                    indexProgress.value = null
                }
            }
        }
    }

    private companion object {
        const val KEY_QUERY_TEXT = "mori_query_text"
        const val KEY_COLLECTION = "mori_collection"
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
