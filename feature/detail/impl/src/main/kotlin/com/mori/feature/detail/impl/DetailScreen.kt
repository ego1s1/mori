package com.mori.feature.detail.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.mori.core.designsystem.MoriCoverArt
import com.mori.core.designsystem.MoriComicErrorCard
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriEmptyState
import com.mori.core.designsystem.MoriEnterKind
import com.mori.core.designsystem.MoriHaptic
import com.mori.core.designsystem.LocalAppFonts
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriLoading
import com.mori.core.designsystem.MoriMotion
import com.mori.core.designsystem.MoriSettingSwitch
import com.mori.core.designsystem.enter
import com.mori.core.designsystem.exit
import com.mori.core.designsystem.MoriProgressBar
import com.mori.core.designsystem.rememberMoriHaptics
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.sharedCoverModifier
import com.mori.core.designsystem.ThemePreviews
import com.mori.core.model.Comic
import com.mori.core.model.ComicError
import com.mori.core.model.ComicFormat

@Composable
internal fun DetailRoute(
    onBackClick: () -> Unit,
    onReadClick: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            when (message) {
                DetailMessage.RescanFailed ->
                    snackbarHost.showSnackbar(context.getString(R.string.detail_snack_rescan_failed))
                DetailMessage.RemoveFailed ->
                    snackbarHost.showSnackbar(context.getString(R.string.detail_snack_remove_failed))
                is DetailMessage.ShareFile -> {
                    val launched = launchShare(context, message, context.getString(R.string.detail_share_title))
                    if (!launched) {
                        snackbarHost.showSnackbar(context.getString(R.string.detail_snack_share_unavailable))
                    }
                }
            }
        }
    }
    DetailScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        onReadClick = onReadClick,
        snackbarHost = snackbarHost,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DetailScreen(
    uiState: DetailUiState,
    onAction: (DetailAction) -> Unit,
    onBackClick: () -> Unit,
    onReadClick: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val titleFont = LocalAppFonts.current.topBarTitle
    Scaffold(
        topBar = {
            val readyComic = (uiState as? DetailUiState.Ready)?.comic
            if (readyComic != null) {
                LargeFlexibleTopAppBar(
                    scrollBehavior = scrollBehavior,
                    title = {
                        Text(
                            text = readyComic.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = titleFont),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        FilledTonalIconButton(onClick = onBackClick) {
                            Icon(imageVector = MoriIcons.Back, contentDescription = stringResource(R.string.detail_back))
                        }
                    },
                    actions = {
                        DetailTopActions(
                            bookmarked = readyComic.bookmarked,
                            onAction = onAction,
                            modifier = Modifier,
                        )
                    },
                )
            } else {
                LargeFlexibleTopAppBar(
                    scrollBehavior = scrollBehavior,
                    title = {
                        Text(
                            text = stringResource(R.string.detail_title),
                            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = titleFont),
                        )
                    },
                    navigationIcon = {
                        FilledTonalIconButton(onClick = onBackClick) {
                            Icon(imageVector = MoriIcons.Back, contentDescription = stringResource(R.string.detail_back))
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        Surface(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when (uiState) {
                DetailUiState.Loading -> MoriLoading(
                    modifier = Modifier.testTag(DetailTestTags.Loading),
                )

                DetailUiState.Missing -> MoriEmptyState(
                    icon = MoriIcons.MenuBook,
                    title = stringResource(R.string.detail_removed_title),
                    body = stringResource(R.string.detail_removed_body),
                    actionLabel = stringResource(R.string.detail_back_to_library),
                    onAction = onBackClick,
                    modifier = Modifier.testTag(DetailTestTags.Missing),
                )

                is DetailUiState.Ready -> {
                    DetailContent(
                        comic = uiState.comic,
                        refreshing = uiState.refreshing,
                        onAction = onAction,
                        onReadClick = onReadClick,
                    )
                    // Plain conditionals, not AnimatedVisibility: AlertDialog
                    // owns a window, so a hidden-but-composed dialog still
                    // takes focus and swallows dismiss. Dialogs animate via
                    // the window, not the content transition.
                    if (uiState.confirmRemove) {
                        RemoveDialog(
                            title = uiState.comic.title,
                            onConfirm = { onAction(DetailAction.ConfirmRemove) },
                            onDismiss = { onAction(DetailAction.CancelRemove) },
                        )
                    }
                    if (uiState.shelves != null) {
                        uiState.shelves?.let { shelves ->
                            ShelvesDialog(
                                shelves = shelves,
                                onAction = onAction,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTopActions(
    bookmarked: Boolean,
    onAction: (DetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        val haptics = rememberMoriHaptics()
        IconButton(
            onClick = {
                haptics(if (bookmarked) MoriHaptic.ToggleOff else MoriHaptic.ToggleOn)
                onAction(DetailAction.ToggleBookmark)
            },
            modifier = Modifier.testTag(DetailTestTags.BookmarkButton),
        ) {
            Icon(
                imageVector = if (bookmarked) MoriIcons.Bookmark else MoriIcons.BookmarkBorder,
                contentDescription = stringResource(
                    if (bookmarked) {
                        R.string.detail_action_unbookmark
                    } else {
                        R.string.detail_action_bookmark
                    },
                ),
            )
        }
        // Secondary actions ride an overflow: M3 Expressive keeps 1-2
        // essential actions in the app bar.
        var menuOpen by remember { mutableStateOf(false) }
        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.testTag(DetailTestTags.OverflowButton),
            ) {
                Icon(
                    imageVector = MoriIcons.MoreVert,
                    contentDescription = stringResource(R.string.detail_action_more),
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.detail_action_share)) },
                    leadingIcon = { Icon(MoriIcons.Share, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        haptics(MoriHaptic.Select)
                        onAction(DetailAction.Share)
                    },
                    modifier = Modifier.testTag(DetailTestTags.ShareButton),
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.detail_action_rescan)) },
                    leadingIcon = { Icon(MoriIcons.Refresh, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        haptics(MoriHaptic.Tick)
                        onAction(DetailAction.Refresh)
                    },
                    modifier = Modifier.testTag(DetailTestTags.RefreshButton),
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.detail_action_remove_comic),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            MoriIcons.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        menuOpen = false
                        haptics(MoriHaptic.Select)
                        onAction(DetailAction.AskRemove)
                    },
                    modifier = Modifier.testTag(DetailTestTags.RemoveButton),
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun DetailContent(
    comic: Comic,
    refreshing: Boolean,
    onAction: (DetailAction) -> Unit,
    onReadClick: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Centered readable column on expanded windows; phones stay full-bleed.
    androidx.compose.foundation.layout.BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        val contentWidth = minOf(maxWidth, EXPANDED_CONTENT_MAX_WIDTH)
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .width(contentWidth)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(DetailTestTags.Hero),
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(2f / 3f)
                    .then(sharedCoverModifier(comic.id)),
            ) {
                MoriCoverArt(
                    coverPath = comic.coverPath,
                    contentDescription = comic.title,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = comic.title,
                    style = MoriEmphasized.headlineSmall.copy(
                        fontFamily = LocalAppFonts.current.displayFlex,
                        fontWeight = FontWeight.Black,
                    ),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                if (comic.series != null) {
                    Text(
                        text = comic.series.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.detail_meta_pages, comic.pageCount, comic.format.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (comic.isInProgress || comic.isFinished) {
                    MoriProgressBar(
                        progress = { comic.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                    Text(
                        text = stringResource(
                            R.string.detail_meta_position,
                            comic.lastPageIndex + 1,
                            comic.pageCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val error = comic.error
        if (error != null) {
            ErrorCard(
                error = error,
                refreshing = refreshing,
                onRetry = { onAction(DetailAction.Refresh) },
                onRemove = { onAction(DetailAction.AskRemove) },
            )
        } else {
            val startPage = comic.resumeIndex
            Button(
                onClick = { onReadClick(comic.id, startPage) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DetailTestTags.ReadButton),
            ) {
                Text(
                    if (comic.lastPageIndex > 0) {
                        stringResource(R.string.detail_read_resume, startPage + 1)
                    } else {
                        stringResource(R.string.detail_read_start)
                    },
                )
            }
            FilledTonalButton(
                onClick = { onAction(DetailAction.OpenShelves) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DetailTestTags.ShelvesButton),
            ) {
                Text(stringResource(R.string.detail_shelves))
            }
        }

        MetadataRows(comic = comic)

    }
}
    }

private val EXPANDED_CONTENT_MAX_WIDTH = 840.dp

@Composable
private fun MetadataRows(comic: Comic, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        comic.number?.let { MetadataRow(label = stringResource(R.string.detail_meta_number), value = it) }
        MetadataRow(label = stringResource(R.string.detail_meta_format), value = comic.format.name)
        MetadataRow(label = stringResource(R.string.detail_meta_file), value = comic.sourceDisplayName)
    }
}

@Composable
private fun MetadataRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // Minimum slot with wrap: long locale labels grow instead of
            // clipping against a fixed 88dp.
            modifier = Modifier
                .widthIn(min = 88.dp)
                .padding(end = 8.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ErrorCard(
    error: ComicError,
    refreshing: Boolean,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MoriComicErrorCard(
        error = error,
        primaryLabel = stringResource(R.string.detail_error_retry),
        onPrimary = onRetry,
        secondaryLabel = stringResource(R.string.detail_error_remove),
        onSecondary = onRemove,
        loading = refreshing,
        modifier = modifier.testTag(DetailTestTags.ErrorCard),
    )
}

@Composable
private fun RemoveDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.detail_remove_title),
                style = MoriEmphasized.headlineSmall.copy(
                    fontFamily = LocalAppFonts.current.displayFlex,
                    fontWeight = FontWeight.Black,
                ),
            )
        },
        text = {
            Text(
                text = stringResource(R.string.detail_remove_body, title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            val haptics = rememberMoriHaptics()
            Button(
                onClick = {
                    haptics(MoriHaptic.Confirm)
                    onConfirm()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.testTag(DetailTestTags.ConfirmRemove),
            ) {
                Text(stringResource(R.string.detail_remove_confirm))
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = onDismiss) {
                Text(stringResource(R.string.detail_remove_cancel))
            }
        },
        modifier = modifier.testTag(DetailTestTags.RemoveDialog),
    )
}

/** Shelves dialog: toggle this book's membership, or create a shelf for it. */
@Composable
private fun ShelvesDialog(
    shelves: ShelvesSheet,
    onAction: (DetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onAction(DetailAction.CloseShelves) },
        title = {
            Text(
                text = stringResource(R.string.detail_shelves_title),
                style = MoriEmphasized.headlineSmall.copy(
                    fontFamily = LocalAppFonts.current.displayFlex,
                    fontWeight = FontWeight.Black,
                ),
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (shelves.collections.isEmpty()) {
                    Text(
                        text = stringResource(R.string.detail_shelves_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                shelves.collections.forEach { collection ->
                    MoriSettingSwitch(
                        title = collection.name,
                        checked = collection.id in shelves.memberIds,
                        onCheckedChange = {
                            onAction(DetailAction.ToggleShelfMember(collection.id))
                        },
                        modifier = Modifier.testTag(DetailTestTags.shelfRow(collection.id)),
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.detail_shelves_new_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag(DetailTestTags.ShelfCreateField),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAction(DetailAction.CreateShelf(name))
                    name = ""
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag(DetailTestTags.ShelfCreateConfirm),
            ) {
                Text(stringResource(R.string.detail_shelves_create))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(DetailAction.CloseShelves) }) {
                Text(stringResource(R.string.detail_shelves_close))
            }
        },
        modifier = modifier.testTag(DetailTestTags.ShelvesDialog),
    )
}

@ThemePreviews
@Composable
private fun DetailScreenPreview() {
    MoriTheme {
        DetailScreen(
            uiState = DetailUiState.Ready(
                comic = Comic(
                    id = "1",
                    title = "Batman: Court of Owls",
                    series = "Batman",
                    number = "1",
                    format = ComicFormat.CBZ,
                    pageCount = 173,
                    sourcePath = "/lib/1.cbz",
                    coverPath = null,
                    lastPageIndex = 12,
                    sourceDisplayName = "batman.cbz",
                    createdAt = 1L,
                    updatedAt = 2L,
                ),
                refreshing = false,
                confirmRemove = false,
                removed = false,
            ),
            onAction = {},
            onBackClick = {},
            onReadClick = { _, _ -> },
        )
    }
}
