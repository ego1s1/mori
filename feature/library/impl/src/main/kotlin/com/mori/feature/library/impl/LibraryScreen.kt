package com.mori.feature.library.impl

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.LocalExpressiveMotionEnabled
import com.mori.core.designsystem.MoriCoverArt
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriEmptyState
import com.mori.core.designsystem.MoriEnterKind
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriLoading
import com.mori.core.designsystem.MoriMotion
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
            target?.let { ResumeTarget(it.id, it.lastPageIndex, it.title) },
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
                var menuOpen by rememberSaveable { mutableStateOf(false) }
                LibraryTopBar(
                    comicCount = uiState.comics.size,
                    searchOpen = uiState.searchOpen,
                    menuOpen = menuOpen,
                    onMenuOpenChange = { menuOpen = it },
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
                        searchOpen = uiState.searchOpen,
                        linked = uiState.linked,
                        shelf = uiState.continueReading,
                        onAction = onAction,
                        onReadClick = onReadClick,
                        onComicLongClick = onComicLongClick,
                        onChooseFolder = onChooseFolder,
                    )
                    if (uiState.filterOpen) {
                        LibrarySortFilterSheet(
                            query = uiState.query,
                            onAction = onAction,
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
    searchOpen: Boolean,
    linked: Boolean,
    shelf: List<Comic>,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    onChooseFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                refreshing = refreshing,
                linked = linked,
                shelf = shelf,
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
 * Static compact app bar: the title and collection subtitle never move and the
 * background never shifts while scrolling (reference-reader style). One bar,
 * one color, always. Search stays visible; everything else lives in the
 * top-end overflow menu (M3 convention).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    comicCount: Int,
    searchOpen: Boolean,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onSearchClick: () -> Unit,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = stringResource(R.string.library_title),
                    style = MoriEmphasized.displaySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (comicCount == 0) {
                        stringResource(R.string.library_empty_hint)
                    } else {
                        pluralStringResource(R.plurals.library_shelf_subtitle, comicCount, comicCount)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
            Box {
                IconButton(
                    onClick = { onMenuOpenChange(true) },
                    modifier = Modifier.testTag(LibraryTestTags.MenuButton),
                ) {
                    Icon(
                        imageVector = MoriIcons.More,
                        contentDescription = stringResource(R.string.library_menu),
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { onMenuOpenChange(false) },
                    offset = DpOffset(0.dp, 4.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 0.dp,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .widthIn(min = 112.dp, max = 280.dp)
                        .testTag(LibraryTestTags.MenuPopup),
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_action_sort_filter)) },
                        onClick = {
                            onMenuOpenChange(false)
                            onAction(LibraryAction.OpenFilter)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = MoriIcons.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        modifier = Modifier.testTag(LibraryTestTags.FilterButton),
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_action_rescan)) },
                        onClick = {
                            onMenuOpenChange(false)
                            onAction(LibraryAction.Refresh)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = MoriIcons.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        modifier = Modifier.testTag(LibraryTestTags.RefreshButton),
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
    queryText: String,
    refreshing: Boolean,
    linked: Boolean,
    shelf: List<Comic>,
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
    val onCardRead: (Comic) -> Unit = remember(onReadClick) {
        { comic ->
            launchingId = comic.id
            onReadClick(comic.id, comic.lastPageIndex)
        }
    }
    val onCardDetails: (Comic) -> Unit = remember(onComicLongClick) {
        { comic ->
            launchingId = comic.id
            onComicLongClick(comic.id)
        }
    }
    val gridPadding = remember {
        PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 112.dp)
    }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { onAction(LibraryAction.Refresh) },
        modifier = modifier.fillMaxSize(),
    ) {
        if (comics.isEmpty()) {
            // One-shot arrival fade; static visibility never replays it.
            AnimatedVisibility(
                visible = true,
                enter = MoriMotion.enter(MoriEnterKind.FADE),
                exit = MoriMotion.exit(MoriEnterKind.FADE),
            ) {
                LibraryEmptyState(
                    searching = queryText.isNotBlank(),
                    linked = linked,
                    onRefresh = { onAction(LibraryAction.Refresh) },
                    onChooseFolder = onChooseFolder,
                )
            }
        } else {
            // One-shot arrival fade; static visibility never replays it.
            AnimatedVisibility(
                visible = true,
                enter = MoriMotion.enter(MoriEnterKind.FADE),
                exit = MoriMotion.exit(MoriEnterKind.FADE),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(GRID_CELL_MIN),
                    contentPadding = gridPadding,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(LibraryTestTags.Grid),
            ) {
                // Continue shelf rides above the grid when anything is in
                // progress; hidden entirely otherwise (no empty header).
                if (shelf.isNotEmpty() && queryText.isBlank()) {
                    item(
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = "continueShelf",
                    ) {
                        ContinueShelf(
                            comics = shelf,
                            onReadClick = onReadClick,
                        )
                    }
                }
                items(
                        comics,
                        key = { it.id },
                        // Bitmask bucket: error/in-progress/finished variants never
                        // cross-recycle, with no per-item string allocation.
                        contentType = { comic ->
                            (if (comic.error != null) 4 else 0) +
                                (if (comic.isInProgress) 2 else 0) +
                                (if (comic.isFinished) 1 else 0)
                        },
                    ) { comic ->
                        ComicCard(
                            comic = comic,
                            onRead = onCardRead,
                            onDetails = onCardDetails,
                            sharedCover = launchingId == comic.id,
                            modifier = Modifier,
                        )
                    }
                }
            }
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
            items(comics, key = { it.id }) { comic ->
                ContinueCard(
                    comic = comic,
                    onReadClick = onReadClick,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueCard(
    comic: Comic,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val click = remember(comic) { { onReadClick(comic.id, comic.lastPageIndex) } }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .width(120.dp)
            .testTag(LibraryTestTags.shelfCardFor(comic.id))
            .combinedClickable(
                onClick = click,
                onClickLabel = stringResource(R.string.library_card_read, comic.title),
            ),
    ) {
        Column {
            Box(
                modifier = Modifier.aspectRatio(COVER_ASPECT),
            ) {
                MoriCoverArt(
                    coverPath = comic.coverPath,
                    contentDescription = comic.title,
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    minLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                LinearProgressIndicator(
                    progress = { comic.progress },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .height(3.dp),
                )
            }
        }
    }
}

private const val COVER_ASPECT = 2f / 3f

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
        bottomPadding = 112.dp,
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
