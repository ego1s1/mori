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
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.LocalExpressiveMotionEnabled
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriMotion
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.ThemePreviews
import com.mori.core.model.Comic
import com.mori.core.model.LibraryQuery

@Composable
internal fun LibraryRoute(
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onReadClick = onReadClick,
        onComicLongClick = onComicLongClick,
        onSettingsClick = onSettingsClick,
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
) {
    val snackbarHost = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
    )

    Scaffold(
        topBar = {
            if (uiState is LibraryUiState.Success) {
                LibraryTopBar(
                    comicCount = uiState.comics.size,
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
    ) { padding ->
        Surface(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when (uiState) {
                LibraryUiState.Loading -> Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(LibraryTestTags.Loading),
                ) {
                    CircularProgressIndicator()
                }

                is LibraryUiState.Success -> {
                    LaunchedEffect(uiState.snackbar) {
                        val message = uiState.snackbar ?: return@LaunchedEffect
                        snackbarHost.showSnackbar(message)
                        onAction(LibraryAction.DismissSnackbar)
                    }
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
    var searchOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = searchOpen,
                enter = searchEnter(),
                exit = fadeOut(animationSpec = MoriMotion.calmFade()),
            ) {
                OutlinedTextField(
                    value = state.query.text,
                    onValueChange = { onAction(LibraryAction.SearchTextChanged(it)) },
                    label = { Text("Search title, series, number") },
                    leadingIcon = {
                        Icon(imageVector = MoriIcons.Search, contentDescription = null)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { searchOpen = false }),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
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
            enter = toolbarEnter(),
            exit = fadeOut(animationSpec = MoriMotion.calmFade()),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        ) {
            FloatingToolbar(
                onSearchClick = { searchOpen = !searchOpen },
                onSettingsClick = onSettingsClick,
                onAction = onAction,
            )
        }

        val resume = state.resumeTarget
        AnimatedVisibility(
            visible = resume != null,
            enter = resumeEnter(),
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
                        contentDescription = "Resume ${resume.title}",
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

/**
 * Motion-aware entrances: expressive springs, calm fades. Read from
 * [LocalExpressiveMotionEnabled] so the settings toggle applies everywhere.
 */
@Composable
private fun searchEnter() =
    if (LocalExpressiveMotionEnabled.current) {
        fadeIn(animationSpec = MoriMotion.defaultEffectsSpec()) +
            expandVertically(animationSpec = MoriMotion.chromeSpring())
    } else {
        fadeIn(animationSpec = MoriMotion.calmFade())
    }

@Composable
private fun toolbarEnter() =
    if (LocalExpressiveMotionEnabled.current) {
        fadeIn(animationSpec = MoriMotion.defaultEffectsSpec()) +
            slideInVertically(animationSpec = MoriMotion.chromeSpring()) { it / 2 }
    } else {
        fadeIn(animationSpec = MoriMotion.calmFade())
    }

@Composable
private fun resumeEnter() =
    if (LocalExpressiveMotionEnabled.current) {
        fadeIn(animationSpec = MoriMotion.defaultEffectsSpec()) +
            scaleIn(animationSpec = MoriMotion.heroSpring(), initialScale = 0.6f)
    } else {
        fadeIn(animationSpec = MoriMotion.calmFade())
    }

/**
 * M3 large app bar: emphasized collapsing headline with a live collection subtitle.
 * Tonal elevation on scroll comes from TopAppBarDefaults (surface → surfaceContainer),
 * exactly per spec — no decorative gradients, no alpha-hacked text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    comicCount: Int,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    LargeTopAppBar(
        title = {
            Column {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (comicCount == 0) {
                        "Import comics to start your shelf"
                    } else {
                        "$comicCount comic${if (comicCount == 1) "" else "s"} on the shelf"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        scrollBehavior = scrollBehavior,
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
                    contentDescription = "Search library",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                onClick = { onAction(LibraryAction.OpenFilter) },
                modifier = Modifier.testTag(LibraryTestTags.FilterButton),
            ) {
                Icon(
                    imageVector = MoriIcons.Tune,
                    contentDescription = "Sort and filter",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                onClick = { onAction(LibraryAction.Refresh) },
                modifier = Modifier.testTag(LibraryTestTags.RefreshButton),
            ) {
                Icon(
                    imageVector = MoriIcons.Refresh,
                    contentDescription = "Rescan library",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.testTag(LibraryTestTags.SettingsButton),
            ) {
                Icon(
                    imageVector = MoriIcons.Settings,
                    contentDescription = "Library settings",
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
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(bottom = 96.dp)
            .testTag(LibraryTestTags.EmptyState),
    ) {
        Icon(
            imageVector = MoriIcons.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = if (searching) "No comics match your search" else "Your library is empty",
            style = MoriEmphasized.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = if (searching) {
                "Try a different title, series, or number."
            } else {
                "Import comics to see them here."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = onRefresh,
            modifier = Modifier
                .padding(top = 24.dp)
                .testTag(LibraryTestTags.EmptyRescan),
        ) {
            Text("Rescan library")
        }
    }
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
                snackbar = null,
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
