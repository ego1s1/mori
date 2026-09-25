package com.mori.feature.library.impl

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.LocalExpressiveMotionEnabled
import com.mori.core.designsystem.FloatingChromeBottomReserve
import com.mori.core.designsystem.MoriContentWell
import com.mori.core.designsystem.MoriCoverArt
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriEmptyState
import com.mori.core.designsystem.MoriEnterKind
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriLoading
import com.mori.core.designsystem.MoriMotion
import com.mori.core.designsystem.MoriProgressBar
import com.mori.core.designsystem.enter
import com.mori.core.designsystem.exit
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.ThemePreviews
import com.mori.core.model.Comic
import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibraryQuery
import com.mori.core.model.ResumeTarget

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
    LibraryRouteContent(
        onReadClick = onReadClick,
        onComicLongClick = onComicLongClick,
        modifier = modifier,
        viewModel = hiltViewModel(),
        onResumeAvailable = onResumeAvailable,
    )
}

@Composable
internal fun LibraryRoute(
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    LibraryRouteContent(
        onReadClick = onReadClick,
        onComicLongClick = onComicLongClick,
        modifier = modifier,
        viewModel = viewModel,
    )
}

@Composable
private fun LibraryRouteContent(
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel,
    onResumeAvailable: (ResumeTarget?) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    // Post-onboarding rescue: linking straight from the empty shelf, with the
    // same persistable permission the onboarding picker takes.
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.onAction(LibraryAction.FolderSelected(uri))
        }
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
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
    LibraryScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onReadClick = onReadClick,
        onComicLongClick = onComicLongClick,
        onChooseFolder = { folderLauncher.launch(null) },
        snackbarHost = snackbarHost,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryScreen(
    uiState: LibraryUiState,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onChooseFolder: () -> Unit = {},
    snackbarHost: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = {
            if (uiState is LibraryUiState.Success) {
                LibraryTopBar(
                    searchOpen = uiState.searchOpen,
                    // Shelf selection or any non-default sort/filter lights
                    // the Tune icon: with the chips row gone, the grid alone
                    // must show that a filter is active.
                    filterActive = uiState.selectedCollectionId != null ||
                        uiState.query.copy(text = "") != LibraryQuery(),
                    onSearchClick = { onAction(LibraryAction.ToggleSearch) },
                    onAction = onAction,
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier.testTag(LibraryTestTags.Snackbar),
            )
        },
        modifier = modifier,
    ) { padding ->
        Surface(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when (uiState) {
                LibraryUiState.Loading -> MoriLoading(
                    modifier = Modifier.testTag(LibraryTestTags.Loading),
                )

                is LibraryUiState.Success -> {
                    LibraryContent(
                        comics = uiState.comics,
                        query = uiState.query,
                        refreshing = uiState.refreshing,
                        indexProgress = uiState.indexProgress,
                        searchOpen = uiState.searchOpen,
                        linked = uiState.linked,
                        shelf = uiState.continueReading,
                        sections = uiState.sections,
                        onAction = onAction,
                        onReadClick = onReadClick,
                        onComicLongClick = onComicLongClick,
                        onChooseFolder = onChooseFolder,
                    )
                    if (uiState.filterOpen) {
                        LibrarySortFilterSheet(
                            query = uiState.query,
                            onAction = onAction,
                            collections = uiState.collections,
                            selectedCollectionId = uiState.selectedCollectionId,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryContent(
    comics: List<Comic>,
    query: LibraryQuery,
    refreshing: Boolean,
    indexProgress: IndexProgress?,
    searchOpen: Boolean,
    linked: Boolean,
    shelf: List<Comic>,
    sections: List<ShelfSection>,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    onChooseFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Centered well on expanded windows; phones stay full-bleed.
    MoriContentWell(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Determinate rescan bar: done/total from the index callback,
            // so large rescans never read as a stuck spinner.
            val progress = indexProgress
            if (refreshing && progress != null && progress.total > 0) {
                MoriProgressBar(
                    progress = { (progress.done.coerceAtMost(progress.total)).toFloat() / progress.total },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AnimatedVisibility(
                visible = searchOpen,
                enter = MoriMotion.enter(MoriEnterKind.SEARCH),
                exit = MoriMotion.exit(MoriEnterKind.SEARCH),
            ) {
                // Focus + keyboard follow the toggle both ways: opening focuses
                // and lifts the keyboard, closing releases both.
                val searchFocus = remember { FocusRequester() }
                val keyboard = LocalSoftwareKeyboardController.current
                LaunchedEffect(searchOpen) {
                    if (searchOpen) {
                        searchFocus.requestFocus()
                        keyboard?.show()
                    } else {
                        keyboard?.hide()
                    }
                }
                OutlinedTextField(
                    value = query.text,
                    onValueChange = { onAction(LibraryAction.SearchTextChanged(it)) },
                    label = { Text(stringResource(R.string.library_search_label)) },
                    leadingIcon = {
                        Icon(imageVector = MoriIcons.Search, contentDescription = null)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onAction(LibraryAction.ToggleSearch) }),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
                        .focusRequester(searchFocus)
                        .focusable()
                        .testTag(LibraryTestTags.SearchField),
                )
            }
            LibraryBody(
                comics = comics,
                queryText = query.text,
                // Spinner only when no determinate bar: the two indicators
                // overlap otherwise during indexed rescans.
                refreshing = refreshing && indexProgress == null,
                linked = linked,
                shelf = shelf,
                sections = sections,
                onAction = onAction,
                onReadClick = onReadClick,
                onComicLongClick = onComicLongClick,
                onChooseFolder = onChooseFolder,
                modifier = Modifier.weight(1f),
    )
        }
    }
}

/**
 * Static compact app bar: the bold brand title never moves and the background
 * never shifts while scrolling (reference-reader style). One bar, one color,
 * always. Search and filter ride as direct icon actions — no overflow menu;
 * rescans happen on launch and on pull-to-refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    searchOpen: Boolean,
    filterActive: Boolean,
    onSearchClick: () -> Unit,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.library_title),
                // Same emphasized screen-title role as Settings and
                // History so sibling tabs match.
                style = MoriEmphasized.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.testTag(LibraryTestTags.SearchToggle),
            ) {
                Icon(
                    imageVector = if (searchOpen) {
                        MoriIcons.Close
                    } else {
                        MoriIcons.Search
                    },
                    contentDescription = stringResource(R.string.library_action_search),
                )
            }
            IconButton(
                onClick = { onAction(LibraryAction.OpenFilter) },
                modifier = Modifier.testTag(LibraryTestTags.FilterButton),
            ) {
                Icon(
                    imageVector = MoriIcons.Tune,
                    contentDescription = stringResource(R.string.library_action_sort_filter),
                    tint = if (filterActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        LocalContentColor.current
                    },
                )
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun LibraryBody(
    comics: List<Comic>,
    queryText: String,
    refreshing: Boolean,
    linked: Boolean,
    shelf: List<Comic>,
    sections: List<ShelfSection>,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    onChooseFolder: () -> Unit,
    modifier: Modifier = Modifier,
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
    val gridPadding = remember {
        PaddingValues(
            start = 12.dp,
            top = 12.dp,
            end = 12.dp,
            bottom = FloatingChromeBottomReserve,
        )
    }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { onAction(LibraryAction.Refresh) },
        modifier = modifier.fillMaxSize(),
    ) {
        if (comics.isEmpty()) {
            LibraryEmptyState(
                searching = queryText.isNotBlank(),
                linked = linked,
                onRefresh = { onAction(LibraryAction.Refresh) },
                onChooseFolder = onChooseFolder,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(GRID_CELL_MIN),
                contentPadding = gridPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(LibraryTestTags.Grid),
            ) {
                if (sections.isNotEmpty()) {
                    // Sectioned grid: one collapsible shelf after another.
                    // The continue shelf still leads; flat views use the
                    // legacy path in else below.
                    if (shelf.isNotEmpty() && queryText.isBlank()) {
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = "continueShelf",
                        ) {
                            ContinueShelf(
                                comics = shelf,
                                onReadClick = onReadClick,
                                onComicLongClick = onComicLongClick,
                            )
                        }
                    }
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
                            )
                        }
                        comicItems(
                            comics = section.comics,
                            keyPrefix = "shelf-${section.id}",
                            onCardRead = onCardRead,
                            onCardDetails = onCardDetails,
                            launchingId = launchingId,
                            contentVisible = !section.collapsed,
                        )
                    }
                } else {
                    // Continue shelf rides above the grid when anything is in
                    // progress; hidden entirely otherwise (no empty header).
                    if (shelf.isNotEmpty() && queryText.isBlank()) {
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = "continueShelf",
                            key = "continueShelf",
                        ) {
                            ContinueShelf(
                                comics = shelf,
                                onReadClick = onReadClick,
                                onComicLongClick = onComicLongClick,
                            )
                        }
                    }
                    comicItems(
                        comics = comics,
                        keyPrefix = "card",
                        onCardRead = onCardRead,
                        onCardDetails = onCardDetails,
                        launchingId = launchingId,
                    )
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
    onCardRead: (Comic) -> Unit,
    onCardDetails: (Comic) -> Unit,
    launchingId: String?,
    contentVisible: Boolean = true,
) {
    items(
        comics,
        key = { "$keyPrefix-${it.id}" },
        // Bitmask bucket: error/in-progress/finished variants never
        // cross-recycle, with no per-item string allocation.
        contentType = { comic ->
            (if (comic.error != null) 4 else 0) +
                (if (comic.isInProgress) 2 else 0) +
                (if (comic.isFinished) 1 else 0)
        },
    ) { comic ->
        // Collapse runs through AnimatedVisibility (not item removal) so
        // cards glide out on the motion setting instead of snapping away.
        AnimatedVisibility(
            visible = contentVisible,
            enter = MoriMotion.enter(MoriEnterKind.SEARCH),
            exit = MoriMotion.exit(MoriEnterKind.SEARCH),
        ) {
            ComicCard(
                comic = comic,
                onRead = onCardRead,
                onDetails = onCardDetails,
                sharedCover = launchingId == comic.id,
                modifier = Modifier.animateItem(),
            )
        }
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
                style = MoriEmphasized.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelLarge,
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

/**
 * Horizontal continue-reading shelf: compact cards for in-progress books by
 * recency. Same information as the grid cards, denser; tapping continues at
 * the saved page.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueShelf(
    comics: List<Comic>,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.testTag(LibraryTestTags.Shelf)) {
        Text(
            text = stringResource(R.string.library_continue_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                comics,
                key = { it.id },
                // Same bitmask bucket as the grid: variants never
                // cross-recycle when the shelf list re-emits.
                contentType = { comic ->
                    (if (comic.error != null) 4 else 0) +
                        (if (comic.isInProgress) 2 else 0) +
                        (if (comic.isFinished) 1 else 0)
                },
            ) { comic ->
                ComicCard(
                    comic = comic,
                    onRead = {
                        if (it.error != null) {
                            onComicLongClick(it.id)
                        } else {
                            onReadClick(it.id, it.resumeIndex)
                        }
                    },
                    onDetails = null,
                    compact = true,
                    cardTag = LibraryTestTags.shelfCardFor(comic.id),
                    modifier = Modifier.animateItem(),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun LibraryEmptyState(
    searching: Boolean,
    linked: Boolean,
    onRefresh: () -> Unit,
    onChooseFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Unlinked and not searching, rescan is a dead end: offer the folder
    // rescue instead. Searching or linked shelves keep rescan.
    val rescue = !searching && !linked
    MoriEmptyState(
        icon = MoriIcons.MenuBook,
        title = if (searching) {
            stringResource(R.string.library_empty_search_title)
        } else {
            stringResource(R.string.library_empty_title)
        },
        body = if (searching) {
            stringResource(R.string.library_empty_search_body)
        } else {
            stringResource(R.string.library_empty_body)
        },
        actionLabel = if (rescue) {
            stringResource(R.string.library_empty_choose_folder)
        } else {
            stringResource(R.string.library_empty_rescan)
        },
        onAction = if (rescue) onChooseFolder else onRefresh,
        modifier = modifier.testTag(LibraryTestTags.EmptyState),
        bottomPadding = FloatingChromeBottomReserve,
        actionTestTag = if (rescue) {
            LibraryTestTags.EmptyChooseFolder
        } else {
            LibraryTestTags.EmptyRescan
        },
    )
}

@ThemePreviews
@Composable
private fun LibraryScreenPreview() {
    MoriTheme {
        LibraryScreen(
            uiState = LibraryUiState.Success(
                comics = listOf(
                    previewComic("1", "Batman: Court of Owls", 2, 10),
                    previewComic("2", "Saga", 0, 0),
                ),
                query = com.mori.core.model.LibraryQuery(),
                refreshing = false,
                filterOpen = false,
                searchOpen = false,
                linked = true,
                continueReading = emptyList(),
            ),
            onAction = {},
            onReadClick = { _, _ -> },
            onComicLongClick = {},
        )
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
