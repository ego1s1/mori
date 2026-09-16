package com.mori.feature.reader.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mori.core.data.ComicsRepository
import com.mori.core.datastore.MoriPreferencesDataSource
import com.mori.core.model.Comic
import com.mori.core.model.ComicError
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.ReadingDirection
import com.mori.feature.reader.api.ReaderRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reader state holder backed by the real repository and persisted preferences.
 *
 * Navigation position lives in [navigation] (route argument first, then user movement)
 * so repository re-emissions — including our own progress saves — never yank the pager
 * back. Progress saves debounce 500ms after the page settles.
 *
 * On the very first reader open the chrome (top bar, controls) stays up for a brief
 * overview beat before fading, then the moment is persisted so later opens fall back
 * to the stillness auto-hide in [ReaderContent].
 */
@HiltViewModel
internal class ReaderViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ComicsRepository,
    private val preferences: MoriPreferencesDataSource,
) : ViewModel() {

    private val args: ReaderRoute = savedStateHandle.toRoute<ReaderRoute>()

    /** Ephemeral chrome state; resets are harmless, so it is not persisted. */
    private val chrome = MutableStateFlow(ChromeState())

    /**
     * User-driven position, restored from [SavedStateHandle] before the
     * repository emits so process death never rewinds the page. Written
     * synchronously on every move, so rapid turns accumulate instead of
     * racing the combined [uiState].
     */
    private val navigation = MutableStateFlow<Int?>(
        savedStateHandle.get<Int>(SAVED_PAGE_INDEX),
    )

    /** Last programmatic move style; drives pager glide-vs-jump in the UI. */
    private val turnAnimated = MutableStateFlow(true)

    /**
     * Wide-page scan results per comic, for the dual-page split. Absent means
     * "not scanned yet"; present-but-empty means "scanned, no wide pages" and
     * renders whole pages. Failures are never cached, so a later toggle
     * retries instead of disabling the split for the session.
     */
    private val wideCache = MutableStateFlow<Map<String, Set<Int>>>(emptyMap())

    /**
     * Comics with a scan currently in flight. The cache guard alone is
     * check-then-suspend: rapid split toggles while a bounds decode runs
     * would otherwise launch duplicate full-book decodes.
     */
    private val wideInFlight = MutableStateFlow<Set<String>>(emptySet())

    /**
     * Declared before [init] on purpose: the init collectors can run eagerly
     * (Unconfined dispatchers execute them mid-construction), and they read
     * [uiState] — a later declaration would still hold null there.
     */
    val uiState: StateFlow<ReaderUiState> = combine(
        combine(
            repository.observeComic(args.comicId),
            preferences.readerPreferences,
            chrome,
            navigation,
            turnAnimated,
        ) { comic, prefs, chromeState, nav, animated ->
            ReaderInputs(comic, prefs, chromeState, nav, animated)
        },
        wideCache,
    ) { inputs, wide ->
        toUiState(
            inputs.comic,
            inputs.prefs,
            inputs.chrome,
            inputs.navigation,
            inputs.turnAnimated,
            wide,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReaderUiState.Loading,
    )

    private var saveJob: Job? = null
    private var pendingSave: Int? = null

    /**
     * Flushes the last progress write even as the scope dies. The job is
     * cancelled once the flush lands (or immediately when nothing is
     * pending) so closing the reader never leaks a scope per session.
     */
    private val flushScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + NonCancellable)

    override fun onCleared() {
        saveJob?.cancel()
        val index = pendingSave
        if (index != null) {
            flushScope.launch {
                try {
                    repository.saveProgress(args.comicId, index)
                } finally {
                    flushScope.cancel()
                }
            }
        } else {
            flushScope.cancel()
        }
    }

    init {
        viewModelScope.launch {
            if (!preferences.readerOverviewSeen.first()) {
                delay(READER_OVERVIEW_MS)
                // Never yank chrome out from under open sheets.
                if (!chrome.value.settingsOpen && !chrome.value.overviewOpen) {
                    chrome.value = chrome.value.copy(visible = false)
                }
                preferences.setReaderOverviewSeen()
            }
        }
        // Dual-page split scan: bounds-decodes the book once per session when
        // the split is enabled. Failures yield empty (whole pages), never an
        // error state — reading must not break over a display preference.
        // The id flow is de-duplicated so progress saves (which re-emit the
        // comic) never retrigger a decode; the pair distinct guards toggles.
        viewModelScope.launch {
            combine(
                repository.observeComic(args.comicId).map { it?.id }.distinctUntilChanged(),
                preferences.readerPreferences.map { it.dualPageSplit }.distinctUntilChanged(),
            ) { id, split -> id to split }
                .distinctUntilChanged()
                .collect { (id, split) ->
                    if (id != null && split &&
                        !wideCache.value.containsKey(id) &&
                        !wideInFlight.value.contains(id)
                    ) {
                        wideInFlight.update { it + id }
                        try {
                            val wide = repository.widePageIndices(id)
                            // Wide first, anchor second: the intermediate state
                            // keeps a valid index under the longer count (the
                            // pager never moves, so no phantom save fires), and
                            // the anchor write then settles the right archive
                            // with a correct save. Reversed order would strand an
                            // expanded index under the short count instead.
                            wideCache.value = wideCache.value + (id to wide)
                            // The pager just gained positions; re-anchor on the
                            // archive page being read instead of stranding it.
                            // The scan is async: bail if the split was turned
                            // back off mid-decode, and build from fresh prefs
                            // so a mid-scan flip can't strand a stale layout.
                            val prefs = preferences.readerPreferences.first()
                            if (!prefs.dualPageSplit) return@collect
                            val ready = uiState.value as? ReaderUiState.Ready
                            if (ready != null && ready.comicId == id) {
                                val pages = buildViewerPages(
                                    ready.archivePageCount, wide, prefs.direction, prefs.dualPageInvert,
                                )
                                val archive = ready.currentArchiveIndex
                                    .coerceIn(0, ready.archivePageCount - 1)
                                setNavigation(
                                    archiveToExpandedPositions(pages, ready.archivePageCount)[archive],
                                )
                            }
                        } catch (e: Exception) {
                            // Never cache failures: the next toggle retries.
                            // The collector itself must survive any impl.
                        } finally {
                            wideInFlight.update { it - id }
                        }
                    }
                }
        }
    }

    private fun toUiState(
        comic: Comic?,
        prefs: ReaderPreferences,
        chrome: ChromeState,
        navigation: Int?,
        turnAnimated: Boolean,
        wideByComic: Map<String, Set<Int>>,
    ): ReaderUiState {
        if (comic == null) {
            return ReaderUiState.Error(ReaderErrorCause.Removed)
        }
        val error = comic.error
        if (error != null) {
            return ReaderUiState.Error(ReaderErrorCause.Failed(error))
        }
        val archivePageCount = comic.pageCount.coerceAtLeast(1)
        // Expanded pager positions (Mihon's InsertPage model): wide pages
        // become two halves when the split is on, identity otherwise. The
        // scan arrives asynchronously; until then whole pages render.
        val wide = if (prefs.dualPageSplit) wideByComic[comic.id].orEmpty() else emptySet()
        val viewerPages = buildViewerPages(archivePageCount, wide, prefs.direction, prefs.dualPageInvert)
        val expandedForArchive = archiveToExpandedPositions(viewerPages, archivePageCount)
        val pageCount = viewerPages.size.coerceAtLeast(1)
        // Fresh opens seed from the route's archive index (the library speaks
        // archive pages); restored navigation is already expanded. Either way
        // the position survives list rebuilds via clamping + retargeting.
        val pageIndex = if (navigation != null) {
            navigation.coerceIn(0, pageCount - 1)
        } else {
            expandedForArchive[args.pageIndex.coerceIn(0, archivePageCount - 1)]
        }
        return ReaderUiState.Ready(
            comicId = comic.id,
            title = comic.title,
            subtitle = listOfNotNull(comic.series, comic.number).joinToString(" • "),
            bookmarked = comic.bookmarked,
            pageIndex = pageIndex,
            pageCount = pageCount,
            chromeVisible = chrome.visible,
            direction = prefs.direction,
            pageFit = prefs.pageFit,
            cropMargins = prefs.cropMargins,
            settingsOpen = chrome.settingsOpen,
            overviewOpen = chrome.overviewOpen,
            volumeKeys = prefs.volumeKeys,
            keepScreenOn = prefs.keepScreenOn,
            showTapZones = prefs.showTapZones,
            showPageCounter = prefs.showPageCounter,
            swipeToTurn = prefs.swipeToTurn,
            turnAnimated = turnAnimated,
            viewerPages = viewerPages,
            archivePageCount = archivePageCount,
            expandedForArchive = expandedForArchive,
            dualPageSplit = prefs.dualPageSplit,
            dualPageInvert = prefs.dualPageInvert,
        )
    }

    fun onAction(action: ReaderAction) {
        when (action) {
            ReaderAction.ToggleChrome -> chrome.value = chrome.value.copy(
                visible = !chrome.value.visible,
            )
            ReaderAction.HideChrome -> chrome.value = chrome.value.copy(visible = false)
            ReaderAction.NextPage -> moveBy(1)
            ReaderAction.PrevPage -> moveBy(-1)
            is ReaderAction.SeekPage -> moveTo(action.index, hideChrome = false, animated = false)
            is ReaderAction.PageChanged -> {
                setNavigation(action.index)
                // Swiping to a new page dismisses chrome, like a page turn —
                // but never from under an open sheet.
                if (!chrome.value.settingsOpen && !chrome.value.overviewOpen) {
                    chrome.value = chrome.value.copy(visible = false)
                }
                scheduleProgressSave(archiveIndexFor(action.index))
            }
            ReaderAction.ToggleBookmark -> {
                viewModelScope.launch { repository.toggleBookmark(args.comicId) }
            }
            ReaderAction.OpenSettings -> chrome.value = chrome.value.copy(
                settingsOpen = true,
                visible = true,
            )
            ReaderAction.CloseSettings -> chrome.value = chrome.value.copy(settingsOpen = false)
            ReaderAction.OpenOverview -> chrome.value = chrome.value.copy(
                overviewOpen = true,
                visible = true,
            )
            ReaderAction.CloseOverview -> chrome.value = chrome.value.copy(overviewOpen = false)
            is ReaderAction.SetDirection -> {
                // Halves read in direction order, so a flip reorders split
                // positions — stay on the same archive page. Anchor AFTER the
                // prefs land (same pattern as ToggleDualSplit): navigation
                // must index into the list the new prefs build.
                val ready = uiState.value as? ReaderUiState.Ready
                val archive = ready?.currentArchiveIndex
                updatePrefs { it.copy(direction = action.direction) }
                if (ready != null && archive != null && ready.dualPageSplit) {
                    viewModelScope.launch {
                        val prefs = preferences.readerPreferences.first { it.direction == action.direction }
                        retarget(
                            ready, archive, prefs.direction,
                            split = true,
                            wide = wideCache.value[ready.comicId].orEmpty(),
                            invert = prefs.dualPageInvert,
                        )
                    }
                }
            }
            is ReaderAction.SetPageFit -> updatePrefs { it.copy(pageFit = action.fit) }
            ReaderAction.ToggleDualSplit -> {
                val ready = uiState.value as? ReaderUiState.Ready
                val archive = ready?.currentArchiveIndex
                val split = !(ready?.dualPageSplit ?: false)
                updatePrefs { it.copy(dualPageSplit = split) }
                if (ready != null && archive != null) {
                    // Anchor AFTER the prefs land: navigation must index into
                    // the list the new prefs build. Writing it first would
                    // strand a stale expanded index under the new count and
                    // save the wrong archive. The transient in between stays
                    // coercible, and the scan (split-on) re-anchors on arrival.
                    viewModelScope.launch {
                        val prefs = preferences.readerPreferences.first { it.dualPageSplit == split }
                        val wide = if (split) wideCache.value[ready.comicId].orEmpty() else emptySet()
                        retarget(ready, archive, prefs.direction, split, wide, prefs.dualPageInvert)
                    }
                }
            }
            ReaderAction.ToggleDualInvert -> {
                val ready = uiState.value as? ReaderUiState.Ready
                val archive = ready?.currentArchiveIndex
                val invert = !(ready?.dualPageInvert ?: false)
                updatePrefs { it.copy(dualPageInvert = invert) }
                if (ready != null && archive != null && ready.dualPageSplit) {
                    // Anchor AFTER the prefs land: same race as the split
                    // toggle — a stale synchronous retarget would index into
                    // the pre-flip list.
                    viewModelScope.launch {
                        val prefs = preferences.readerPreferences.first { it.dualPageInvert == invert }
                        retarget(
                            ready, archive, prefs.direction,
                            split = true,
                            wide = wideCache.value[ready.comicId].orEmpty(),
                            invert = prefs.dualPageInvert,
                        )
                    }
                }
            }
            ReaderAction.ToggleCrop -> updatePrefs { it.copy(cropMargins = !it.cropMargins) }
            ReaderAction.ToggleVolumeKeys -> updatePrefs { it.copy(volumeKeys = !it.volumeKeys) }
            ReaderAction.ToggleKeepScreenOn -> updatePrefs { it.copy(keepScreenOn = !it.keepScreenOn) }
            ReaderAction.TogglePageCounter -> updatePrefs { it.copy(showPageCounter = !it.showPageCounter) }
            ReaderAction.ToggleSwipeToTurn -> updatePrefs { it.copy(swipeToTurn = !it.swipeToTurn) }
            ReaderAction.ToggleTapZones -> updatePrefs { it.copy(showTapZones = !it.showTapZones) }
        }
    }

    private fun moveBy(delta: Int) {
        val ready = uiState.value as? ReaderUiState.Ready ?: return
        // Atomically advance from the synchronously-written navigation, so
        // back-to-back turns never read a stale index.
        var bumped = false
        navigation.update { current ->
            val base = current ?: ready.pageIndex
            val clamped = (base + delta).coerceIn(0, ready.pageCount - 1)
            bumped = clamped == base
            clamped
        }
        if (bumped) {
            // Bump into the end of the book: surface chrome as orientation
            // feedback instead of silently swallowing the turn.
            chrome.value = chrome.value.copy(visible = true)
            return
        }
        // navigation.value was just written above; moveTo clamps identically.
        moveTo(navigation.value ?: ready.pageIndex, hideChrome = true)
    }

    private fun moveTo(index: Int, hideChrome: Boolean, animated: Boolean = true) {
        val ready = uiState.value as? ReaderUiState.Ready ?: return
        val clamped = index.coerceIn(0, ready.pageCount - 1)
        turnAnimated.value = animated
        setNavigation(clamped)
        // Buttons and zone taps dismiss chrome like a page turn; the slider keeps
        // chrome up so scrubbing stays visible (auto-hide resumes afterwards).
        chrome.value = chrome.value.copy(visible = !hideChrome)
        scheduleProgressSave(archiveIndexFor(clamped))
    }

    private fun setNavigation(index: Int) {
        navigation.value = index
        savedStateHandle[SAVED_PAGE_INDEX] = index
    }

    /**
     * Translates an expanded pager position into its archive page.
     *
     * Progress is persisted in the archive domain — the library resumes by
     * archive page and knows nothing of splits — so every save path maps
     * through here. Without the split this is the identity.
     */
    private fun archiveIndexFor(expanded: Int): Int {
        val ready = uiState.value as? ReaderUiState.Ready ?: return expanded
        return ready.viewerPages.getOrNull(expanded)?.archiveIndex
            ?: expanded.coerceIn(0, ready.archivePageCount - 1)
    }

    /**
     * Re-anchors [navigation] on [archiveIndex]'s first position after the
     * pager list is rebuilt (split toggled, halves inverted, direction
     * flipped). Every input is passed explicitly — callers await the prefs
     * write that triggered the rebuild and forward the fresh values, so no
     * stale read can race the preference update.
     */
    private fun retarget(
        ready: ReaderUiState.Ready,
        archiveIndex: Int,
        direction: ReadingDirection,
        split: Boolean = ready.dualPageSplit,
        wide: Set<Int> = wideCache.value[ready.comicId].orEmpty(),
        invert: Boolean = ready.dualPageInvert,
    ) {
        val pages = buildViewerPages(ready.archivePageCount, if (split) wide else emptySet(), direction, invert)
        val clamped = archiveIndex.coerceIn(0, ready.archivePageCount - 1)
        setNavigation(archiveToExpandedPositions(pages, ready.archivePageCount)[clamped])
    }

    private fun scheduleProgressSave(index: Int) {
        pendingSave = index
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(PROGRESS_SAVE_DEBOUNCE_MS)
            pendingSave = null
            repository.saveProgress(args.comicId, index)
        }
    }

    private fun updatePrefs(transform: (ReaderPreferences) -> ReaderPreferences) {
        viewModelScope.launch {
            preferences.updateReaderPreferences(transform)
        }
    }

    private data class ChromeState(
        val visible: Boolean = true,
        val settingsOpen: Boolean = false,
        val overviewOpen: Boolean = false,
    )

    /** Five-flow combine carrier (coroutines caps fixed-arity combine at five). */
    private data class ReaderInputs(
        val comic: Comic?,
        val prefs: ReaderPreferences,
        val chrome: ChromeState,
        val navigation: Int?,
        val turnAnimated: Boolean,
    )

    companion object {
        private const val PROGRESS_SAVE_DEBOUNCE_MS = 500L

        private const val SAVED_PAGE_INDEX = "mori_saved_page_index"

        /** First-launch overview beat: chrome stays up this long, then fades. */
        internal const val READER_OVERVIEW_MS = 2000L
    }
}
