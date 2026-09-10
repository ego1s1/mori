package com.mori.feature.library.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.LocalExpressiveMotionEnabled
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

/**
 * Public tab content for the main viewport pager. Route and tab share one
 * internal content; the ViewModel type never appears in a public signature.
 */
@Composable
fun LibraryTabContent(
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LibraryRouteContent(
        onReadClick = onReadClick,
        onComicLongClick = onComicLongClick,
        onSettingsClick = onSettingsClick,
        modifier = modifier,
        viewModel = hiltViewModel(),
    )
}

@Composable
internal fun LibraryRoute(
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    LibraryRouteContent(
        onReadClick = onReadClick,
        onComicLongClick = onComicLongClick,
        onSettingsClick = onSettingsClick,
        modifier = modifier,
        viewModel = viewModel,
    )
}

@Composable
private fun LibraryRouteContent(
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
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
    LibraryScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onReadClick = onReadClick,
        onComicLongClick = onComicLongClick,
        onSettingsClick = onSettingsClick,
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
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = {
            if (uiState is LibraryUiState.Success) {
                LibraryTopBar(comicCount = uiState.comics.size)
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
                        state = uiState,
                        onAction = onAction,
                        onReadClick = onReadClick,
                        onComicLongClick = onComicLongClick,
                        onSettingsClick = onSettingsClick,
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
    state: LibraryUiState.Success,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = state.searchOpen,
                enter = MoriMotion.enter(MoriEnterKind.SEARCH),
                exit = MoriMotion.exit(MoriEnterKind.SEARCH),
            ) {
                // Focus + keyboard follow the toggle both ways: opening focuses
                // and lifts the keyboard, closing releases both.
                val searchFocus = remember { FocusRequester() }
                val keyboard = LocalSoftwareKeyboardController.current
                LaunchedEffect(state.searchOpen) {
                    if (state.searchOpen) {
                        searchFocus.requestFocus()
                        keyboard?.show()
                    } else {
                        keyboard?.hide()
                    }
                }
                OutlinedTextField(
                    value = state.query.text,
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
                state = state,
                onAction = onAction,
                onReadClick = onReadClick,
                onComicLongClick = onComicLongClick,
                modifier = Modifier.weight(1f),
            )
        }

        AnimatedVisibility(
            visible = true,
            enter = MoriMotion.enter(MoriEnterKind.TOOLBAR),
            exit = fadeOut(animationSpec = MoriMotion.calmFade()),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        ) {
            FloatingToolbar(
                onSearchClick = { onAction(LibraryAction.ToggleSearch) },
                onSettingsClick = onSettingsClick,
                onAction = onAction,
            )
        }

        val resume = state.resumeTarget
        AnimatedVisibility(
            visible = resume != null,
            enter = MoriMotion.enter(MoriEnterKind.FAB),
            exit = scaleOut(animationSpec = MoriMotion.calmFade()) +
                fadeOut(animationSpec = MoriMotion.calmFade()),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 96.dp),
        ) {
            if (resume != null) {
                FilledIconButton(
                    onClick = { onReadClick(resume.id, resume.lastPageIndex) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                    modifier = Modifier
                        .size(64.dp)
                        .testTag(LibraryTestTags.ResumeFab),
                ) {
                    Icon(
                        imageVector = MoriIcons.PlayArrow,
                        contentDescription = stringResource(R.string.library_resume, resume.title),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

/**
 * Static compact app bar: the title and collection subtitle never move and the
 * background never shifts while scrolling (reference-reader style). One bar,
 * one color, always.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    comicCount: Int,
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
        modifier = modifier,
    )
}

/**
 * Floating M3 Expressive toolbar: a tonal pill with the primary library actions,
 * detached from the screen edges.
 */
@Composable
private fun FloatingToolbar(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        modifier = modifier.testTag(LibraryTestTags.Toolbar),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.testTag(LibraryTestTags.SearchToggle),
            ) {
                Icon(
                    imageVector = MoriIcons.Search,
                    contentDescription = stringResource(R.string.library_action_search),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                onClick = { onAction(LibraryAction.OpenFilter) },
                modifier = Modifier.testTag(LibraryTestTags.FilterButton),
            ) {
                Icon(
                    imageVector = MoriIcons.Tune,
                    contentDescription = stringResource(R.string.library_action_sort_filter),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                onClick = { onAction(LibraryAction.Refresh) },
                modifier = Modifier.testTag(LibraryTestTags.RefreshButton),
            ) {
                Icon(
                    imageVector = MoriIcons.Refresh,
                    contentDescription = stringResource(R.string.library_action_rescan),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.testTag(LibraryTestTags.SettingsButton),
            ) {
                Icon(
                    imageVector = MoriIcons.Settings,
                    contentDescription = stringResource(R.string.library_action_settings),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun LibraryBody(
    state: LibraryUiState.Success,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = { onAction(LibraryAction.Refresh) },
        modifier = modifier.fillMaxSize(),
    ) {
        if (state.isEmpty) {
            LibraryEmptyState(
                searching = state.query.text.isNotBlank(),
                onRefresh = { onAction(LibraryAction.Refresh) },
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(GRID_CELL_MIN),
                contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(LibraryTestTags.Grid),
            ) {
                items(state.comics, key = { it.id }) { comic ->
                    ComicCard(
                        comic = comic,
                        onClick = { onReadClick(comic.id, comic.lastPageIndex) },
                        onLongClick = { onComicLongClick(comic.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(
    searching: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        actionLabel = stringResource(R.string.library_empty_rescan),
        onAction = onRefresh,
        modifier = modifier.testTag(LibraryTestTags.EmptyState),
        bottomPadding = 96.dp,
        actionTestTag = LibraryTestTags.EmptyRescan,
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
            ),
            onAction = {},
            onReadClick = { _, _ -> },
            onComicLongClick = {},
            onSettingsClick = {},
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
