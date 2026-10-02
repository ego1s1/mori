package com.mori.feature.library.impl

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.LocalAppFonts
import com.mori.core.designsystem.LocalExpressiveMotionEnabled
import com.mori.core.designsystem.FloatingChromeBottomReserve
import com.mori.core.designsystem.MoriCollapsingTopBar
import com.mori.core.designsystem.MoriContentWell
import com.mori.core.designsystem.WindowWidthClass
import com.mori.core.designsystem.windowWidthClass
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriEmptyState
import com.mori.core.designsystem.MoriEnterKind
import com.mori.core.designsystem.MoriHaptic
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriLoading
import com.mori.core.designsystem.MoriMotion
import com.mori.core.designsystem.MoriProgressBar
import com.mori.core.designsystem.enter
import com.mori.core.designsystem.exit
import com.mori.core.designsystem.rememberMoriHaptics
import com.mori.core.model.Comic
import com.mori.core.model.LibraryDisplayMode
import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibraryQuery
import com.mori.core.model.UserCollection
import com.mori.core.model.ResumeTarget
import kotlinx.coroutines.flow.collectLatest

/**
 * Public tab content for the main viewport pager. Route and tab share one
 * internal content; the ViewModel type never appears in a public signature.
 */
@Composable
fun LibraryTabContent(
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onResumeAvailable: (ResumeTarget?) -> Unit = {},
) {
    LibraryRoute(
        onReadClick = onReadClick,
        onComicLongClick = onComicLongClick,
        onResumeAvailable = onResumeAvailable,
        modifier = modifier,
        viewModel = hiltViewModel(),
    )
}

/**
 * Top-level route for the library screen. Hoists ViewModel state and wires
 * navigation and system pickers.
 */
@Composable
fun LibraryRoute(
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (comicId: String) -> Unit,
    onResumeAvailable: (ResumeTarget?) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.messages.collectLatest { message ->
            val text = when (message) {
                is LibraryMessage.IndexFailed -> context.getString(
                    R.string.library_snack_index_failed,
                    message.failed,
                )

                LibraryMessage.RescanFailed ->
                    context.getString(R.string.library_snack_rescan_failed)
            }
            snackbarHost.showSnackbar(text)
        }
    }
    // Resume rides its own cached flow: chrome-only emissions never rescan
    // the list or bounce the shell.
    val resumeTarget by viewModel.resumeTarget.collectAsStateWithLifecycle()
    LaunchedEffect(resumeTarget) {
        val target = resumeTarget
        onResumeAvailable(
            target?.let { ResumeTarget(it.id, it.resumeIndex, it.title) },
        )
    }
    // Long-press menu target resolves from the grid flow (menu chrome lives
    // outside the query combine); a removed comic closes the sheet by itself.
    val menuState by viewModel.observeMenu().collectAsStateWithLifecycle(
        initialValue = LibraryViewModel.MenuState(comicId = null, deleteConfirm = false),
    )
    val menuComic = (uiState as? LibraryUiState.Success)?.comics?.firstOrNull {
        it.id == menuState.comicId
    }
    LibraryScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onReadClick = onReadClick,
        onComicLongClick = { viewModel.onAction(LibraryAction.OpenMenu(it)) },
        onDetailsClick = onComicLongClick,
        menuComic = menuComic,
        menuDeleteConfirm = menuState.deleteConfirm,
        snackbarHost = snackbarHost,
        modifier = modifier,
    )
}

/**
 * Pure presentation: consumes [LibraryUiState] and emits user actions.
 * No ViewModel dependencies — previews and tests construct state directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (comicId: String) -> Unit,
    onDetailsClick: (comicId: String) -> Unit = onComicLongClick,
    menuComic: Comic? = null,
    menuDeleteConfirm: Boolean = false,
    snackbarHost: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            onAction(LibraryAction.FolderSelected(uri))
        }
    }
    val onChooseFolder: () -> Unit = {
        folderPicker.launch(null)
    }
    Scaffold(
        topBar = {
            val success = uiState as? LibraryUiState.Success
            if (success != null) {
                LibraryTopBar(
                    // Shelf selection or any non-default sort/filter lights
                    // the Tune icon: with the chips row gone, the grid alone
                    // must show that a filter is active.
                    filterActive = success.selectedCollectionId != null ||
                            success.query.hasActiveFilters(),
                    onAction = onAction,
                    scrollBehavior = scrollBehavior,
                )
            } else {
                LibraryTopBar(
                    filterActive = false,
                    onAction = {},
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier.testTag(LibraryTestTags.Snackbar),
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        MoriContentWell(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is LibraryUiState.Loading -> {
                    MoriLoading(modifier = Modifier.testTag(LibraryTestTags.Loading))
                }

                is LibraryUiState.Success -> {
                    LibraryContent(
                        comics = state.comics,
                        query = state.query,
                        refreshing = state.refreshing,
                        indexProgress = state.indexProgress,
                        linked = state.linked,
                        sections = state.sections,
                        onAction = onAction,
                        onReadClick = onReadClick,
                        onComicLongClick = onComicLongClick,
                        onChooseFolder = onChooseFolder,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (state.filterOpen) {
                        LibrarySortFilterSheet(
                            query = state.query,
                            collections = state.collections,
                            selectedCollectionId = state.selectedCollectionId,
                            onAction = onAction,
                        )
                    }
                    if (menuComic != null) {
                        LibraryMenuSheet(
                            comic = menuComic,
                            deleteConfirm = menuDeleteConfirm,
                            onAction = onAction,
                            onReadClick = onReadClick,
                            onDetailsClick = onDetailsClick,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryContent(
    comics: List<Comic>,
    query: LibraryQuery,
    refreshing: Boolean,
    indexProgress: IndexProgress?,
    linked: Boolean,
    sections: List<ShelfSection>,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (comicId: String) -> Unit,
    onChooseFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = indexProgress != null,
            enter = MoriMotion.enter(MoriEnterKind.SEARCH),
            exit = MoriMotion.exit(MoriEnterKind.SEARCH),
        ) {
            if (indexProgress != null) {
                val fraction = if (indexProgress.total > 0) {
                    indexProgress.done.toFloat() / indexProgress.total
                } else {
                    0f
                }
                val label = stringResource(
                    R.string.library_rescan_progress,
                )
                MoriProgressBar(
                    progress = { fraction },
                    contentDescription = label,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        val haptics = rememberMoriHaptics()
        val keyboard = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        val searchFloating by remember {
            derivedStateOf {
                gridState.firstVisibleItemIndex > 0 ||
                        gridState.firstVisibleItemScrollOffset > 0
            }
        }
        val floatColor by animateColorAsState(
            if (searchFloating) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            label = "searchPanelColor",
        )
        val floatElevation by animateDpAsState(
            if (searchFloating) 6.dp else 0.dp,
            label = "searchPanelElevation",
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            LibraryBody(
                comics = comics,
                query = query,
                refreshing = refreshing && indexProgress == null,
                linked = linked,
                sections = sections,
                onAction = onAction,
                onReadClick = onReadClick,
                onComicLongClick = onComicLongClick,
                onChooseFolder = onChooseFolder,
                gridState = gridState,
                modifier = Modifier.fillMaxSize(),
            )

            // Expressive floating capsule: search bar and quick filter chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Surface(
                    color = floatColor,
                    shadowElevation = floatElevation,
                    shape = MaterialTheme.shapes.extraExtraLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    TextField(
                        value = query.text,
                        onValueChange = { onAction(LibraryAction.SearchTextChanged(it)) },
                        placeholder = { Text(stringResource(R.string.library_search_label)) },
                        leadingIcon = {
                            Icon(
                                imageVector = MoriIcons.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingIcon = {
                            if (query.text.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        haptics(MoriHaptic.Select)
                                        onAction(LibraryAction.SearchTextChanged(""))
                                    },
                                    modifier = Modifier.testTag(LibraryTestTags.SearchClear),
                                ) {
                                    Icon(
                                        imageVector = MoriIcons.Close,
                                        contentDescription = stringResource(R.string.library_action_clear_search),
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = MoriEmphasized.bodyLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboard?.hide()
                                focusManager.clearFocus()
                            },
                        ),
                        shape = MaterialTheme.shapes.extraExtraLarge,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .sizeIn(minHeight = 56.dp)
                            .testTag(LibraryTestTags.SearchField),
                    )
                }


                // Quick filter chips capsule
                LibraryQuickFilters(
                    selectedFilter = query.filter,
                    onFilterSelect = { onAction(LibraryAction.FilterSelected(it)) },
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/**
 * Collapsing screen header: the heavy 32sp display title shrinks to a compact
 * bar as the grid scrolls, matching the reference app's header behaviour.
 * Search and filter ride as direct icon actions — no overflow menu; rescans
 * happen on launch and on pull-to-refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    filterActive: Boolean,
    onAction: (LibraryAction) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    MoriCollapsingTopBar(
        title = stringResource(R.string.library_title),
        scrollBehavior = scrollBehavior,
        actions = {
            Box {
                FilledTonalIconButton(
                    onClick = { onAction(LibraryAction.OpenFilter) },
                    modifier = Modifier.testTag(LibraryTestTags.FilterButton),
                ) {
                    Icon(
                        imageVector = MoriIcons.Tune,
                        contentDescription = stringResource(R.string.library_action_sort_filter),
                    )
                }
                if (filterActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp)
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape,
                            ),
                    )
                }
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun LibraryBody(
    comics: List<Comic>,
    query: LibraryQuery,
    refreshing: Boolean,
    linked: Boolean,
    sections: List<ShelfSection>,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (comicId: String) -> Unit,
    onChooseFolder: () -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
) {
    // Only the launching card registers a shared element; null = plain grid.
    var launchingId by remember { mutableStateOf<String?>(null) }
    // Hoisted once per body composition: item lambdas stay referentially
    // stable so unchanged cards skip recomposition during scroll-adjacent
    // updates (launch flags, refresh ticks).
    val onCardRead: (Comic) -> Unit = remember(onReadClick, onComicLongClick) {
        { comic ->
            launchingId = comic.id
            // Errored rows can't open the reader; tap goes to details where
            // retry/remove live, matching the detail screen's own gate.
            if (comic.error != null) {
                onComicLongClick(comic.id)
            } else {
                onReadClick(comic.id, comic.resumeIndex)
            }
        }
    }
    val onCardDetails: (Comic) -> Unit = remember(onComicLongClick) {
        { comic ->
            launchingId = comic.id
            onComicLongClick(comic.id)
        }
    }
    // Fixed reserve for the floating search panel (112dp) plus breathing
    // room: constant by construction, so scroll never relayouts the grid.
    val gridPadding = PaddingValues(
        start = 12.dp,
        top = SearchSlotTop,
        end = 12.dp,
        bottom = FloatingChromeBottomReserve,
    )
    Box(modifier = modifier.fillMaxWidth()) {
        if (comics.isEmpty()) {
            LibraryEmptyState(
                searching = query.text.isNotBlank(),
                linked = linked,
                onRefresh = { onAction(LibraryAction.Refresh) },
                onChooseFolder = onChooseFolder,
                modifier = Modifier.padding(top = SearchSlotTop),
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // Expanded windows get roomier book cells; Compact/Medium
                // keep the phone-tuned minimum. Adaptive still decides the
                // final column count — only the floor changes.
                val gridCells = when {
                    query.displayMode == LibraryDisplayMode.LIST -> GridCells.Fixed(1)
                    query.gridColumns > 0 -> GridCells.Fixed(query.gridColumns)
                    else -> {
                        val minCell = if (windowWidthClass() == WindowWidthClass.Expanded) {
                            160.dp
                        } else {
                            GRID_CELL_MIN
                        }
                        GridCells.Adaptive(minCell)
                    }
                }
                LazyVerticalGrid(
                    state = gridState,
                    columns = gridCells,
                    contentPadding = gridPadding,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(LibraryTestTags.Grid),
                ) {
                    // "Now Reading" Hero Spotlight Card
                    val nowReading = if (query.text.isBlank() && query.filter == LibraryFilter.ALL) {
                        comics.firstOrNull { it.isInProgress && it.error == null }
                    } else null

                    if (nowReading != null && sections.isEmpty()) {
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "hero_now_reading_${nowReading.id}",
                            contentType = "nowReadingHero",
                        ) {
                            NowReadingHeroCard(
                                comic = nowReading,
                                onResume = onCardRead,
                                onDetails = onCardDetails,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    if (sections.isNotEmpty()) {
                        // Sectioned grid: one collapsible shelf after another.
                        sections.forEach { section ->
                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                key = "shelf-header-${section.id}",
                                contentType = "shelfHeader",
                            ) {
                                ShelfSectionHeader(
                                    section = section,
                                    onToggle = {
                                        onAction(
                                            LibraryAction.ToggleShelfCollapsed(section.id),
                                        )
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                            comicItems(
                                comics = section.comics,
                                keyPrefix = "shelf-${section.id}",
                                displayMode = query.displayMode,
                                onCardRead = onCardRead,
                                onCardDetails = onCardDetails,
                                launchingId = launchingId,
                                contentVisible = !section.collapsed,
                            )
                        }
                    } else {
                        comicItems(
                            comics = comics,
                            keyPrefix = "card",
                            displayMode = query.displayMode,
                            onCardRead = onCardRead,
                            onCardDetails = onCardDetails,
                            launchingId = launchingId,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Grid book cells shared by flat and sectioned grids: stable keys plus the
 * variant bitmask bucket, so error/in-progress/finished variants never
 * cross-recycle.
 */
private fun LazyGridScope.comicItems(
    comics: List<Comic>,
    keyPrefix: String,
    displayMode: LibraryDisplayMode,
    onCardRead: (Comic) -> Unit,
    onCardDetails: (Comic) -> Unit,
    launchingId: String?,
    contentVisible: Boolean = true,
) {
    if (!contentVisible) return
    items(
        comics,
        key = { comic -> "$keyPrefix-${comic.id}" },
        contentType = { comic ->
            displayMode.ordinal * 16 +
                (if (comic.error != null) 4 else 0) +
                (if (comic.isInProgress) 2 else 0) +
                (if (comic.isFinished) 1 else 0) +
                (if (comic.bookmarked) 8 else 0)
        },
    ) { comic ->
        when (displayMode) {
            LibraryDisplayMode.COMPACT_GRID -> ComicCard(
                comic = comic,
                onRead = onCardRead,
                onDetails = onCardDetails,
                sharedCover = launchingId == comic.id,
                modifier = Modifier.animateItem(),
            )
            LibraryDisplayMode.COMFORTABLE_GRID -> ComfortableComicCard(
                comic = comic,
                onRead = onCardRead,
                onDetails = onCardDetails,
                sharedCover = launchingId == comic.id,
                modifier = Modifier.animateItem(),
            )
            LibraryDisplayMode.COVER_ONLY_GRID -> CoverOnlyComicCard(
                comic = comic,
                onRead = onCardRead,
                onDetails = onCardDetails,
                sharedCover = launchingId == comic.id,
                modifier = Modifier.animateItem(),
            )
            LibraryDisplayMode.LIST -> ComicListRow(
                comic = comic,
                onRead = onCardRead,
                onDetails = onCardDetails,
                modifier = Modifier.animateItem(),
            )
        }
    }
}

/** Empty state branch with rescue actions. */
@Composable
private fun LibraryEmptyState(
    searching: Boolean,
    linked: Boolean,
    onRefresh: () -> Unit,
    onChooseFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberMoriHaptics()
    if (searching) {
        MoriEmptyState(
            icon = MoriIcons.Search,
            title = stringResource(R.string.library_empty_search_title),
            body = stringResource(R.string.library_empty_search_body),
            actionLabel = null,
            onAction = null,
            modifier = modifier.testTag(LibraryTestTags.EmptyState),
        )
    } else {
        val actionLabel = stringResource(
            if (linked) {
                R.string.library_empty_rescan
            } else {
                R.string.library_empty_choose_folder
            },
        )
        val onAction = if (linked) onRefresh else onChooseFolder
        val actionTag = if (linked) {
            LibraryTestTags.EmptyRescan
        } else {
            LibraryTestTags.EmptyChooseFolder
        }
        MoriEmptyState(
            icon = MoriIcons.MenuBook,
            title = stringResource(R.string.library_empty_title),
            body = stringResource(R.string.library_empty_body),
            actionLabel = actionLabel,
            onAction = {
                haptics(MoriHaptic.PrimaryAction)
                onAction()
            },
            actionTestTag = actionTag,
            modifier = modifier.testTag(LibraryTestTags.EmptyState),
        )
    }
}

/**
 * Collapsible shelf header: full-row 48dp touch target (the whole row
 * toggles, not just the chevron), shelf name in emphasized type, tonal
 * count pill, and a chevron that rotates with the state.
 *
 * M3-expressive motion, setting-aware: spring expand + fade when expressive,
 * quiet calm fade otherwise (same pair as the SEARCH enter/exit kind).
 * State is exposed to accessibility as expanded/collapsed, never color-only.
 */
@Composable
private fun ShelfSectionHeader(
    section: ShelfSection,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expressiveMotion = LocalExpressiveMotionEnabled.current
    val title = section.collection?.name
        ?: stringResource(R.string.library_shelf_unsorted)
    val count = section.comics.size
    val toggleLabel = stringResource(
        if (section.collapsed) {
            R.string.library_shelf_expand
        } else {
            R.string.library_shelf_collapse
        },
    )
    val stateLabel = stringResource(
        if (section.collapsed) {
            R.string.library_shelf_collapsed
        } else {
            R.string.library_shelf_expanded
        },
    )
    val chevronAngle by animateFloatAsState(
        targetValue = if (section.collapsed) 0f else 180f,
        animationSpec = if (expressiveMotion) {
            MoriMotion.defaultSpatialSpec()
        } else {
            MoriMotion.calmFade()
        },
        label = "shelfChevron",
    )
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .testTag(LibraryTestTags.shelfHeader(section.id)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                // 48dp touch target without the 1.7-missing
                // minimumInteractiveComponentSize API.
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clickable(
                    onClick = onToggle,
                    role = Role.Button,
                    onClickLabel = toggleLabel,
                )
                .semantics {
                    stateDescription = stateLabel
                }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = title,
                style = MoriEmphasized.titleLarge.copy(
                    fontFamily = LocalAppFonts.current.displaySoft,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = LocalAppFonts.current.displaySoft,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Icon(
                imageVector = MoriIcons.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer {
                    rotationZ = chevronAngle
                },
            )
        }
    }
}

private fun previewComic(id: String, title: String, lastPage: Int, pages: Int) = Comic(
    id = id,
    title = title,
    series = null,
    number = null,
    format = com.mori.core.model.ComicFormat.CBZ,
    pageCount = pages,
    sourcePath = "/lib/$id.cbz",
    coverPath = null,
    lastPageIndex = lastPage,
    sourceDisplayName = "$id.cbz",
    createdAt = 1L,
    updatedAt = 1L,
)

private val GRID_CELL_MIN = 128.dp

/** Fixed search & quick-filter geometry: 8dp top + 56dp field + 6dp gap + 36dp filters + 6dp bottom. */
private val SearchPanelHeight = 112.dp

/** Breathing room between the floating panel and the grid it covers. */
private val SearchGridGap = 12.dp

/** Grid top reserve: panel slot plus gap. Constant — never measured. */
private val SearchSlotTop = SearchPanelHeight + SearchGridGap
