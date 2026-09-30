package com.mori.feature.library.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriHaptic
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriSettingRow
import com.mori.core.designsystem.MoriSheet
import com.mori.core.designsystem.rememberMoriHaptics
import com.mori.core.model.Comic

/**
 * Long-press quick actions for one comic: read/resume, details, bookmark,
 * and remove (behind a confirm dialog). Title in the Flex emphasized face;
 * rows share the settings row language.
 */
@Composable
fun LibraryMenuSheet(
    comic: Comic,
    deleteConfirm: Boolean,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onDetailsClick: (comicId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    MoriSheet(
        onDismissRequest = { onAction(LibraryAction.CloseMenu) },
        // Short menu: open fully so every row is tappable instead of
        // peeking half-expanded with rows below the fold.
        skipPartiallyExpanded = true,
        modifier = modifier.testTag(LibraryTestTags.MenuSheet),
    ) {
        LibraryMenuContent(
            comic = comic,
            deleteConfirm = deleteConfirm,
            onAction = onAction,
            onReadClick = onReadClick,
            onDetailsClick = onDetailsClick,
        )
    }
}

/** Sheet body, exposed for testing without the modal wrapper. */
@Composable
internal fun LibraryMenuContent(
    comic: Comic,
    deleteConfirm: Boolean,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onDetailsClick: (comicId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberMoriHaptics()
    Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Text(
                text = comic.title,
                style = MoriEmphasized.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(LibraryTestTags.MenuTitle),
            )
            MoriSettingRow(
                title = stringResource(
                    if (comic.isInProgress) {
                        R.string.library_menu_resume
                    } else {
                        R.string.library_menu_read
                    },
                ),
                subtitle = stringResource(R.string.library_card_pages_left, comic.pagesLeft),
                icon = MoriIcons.PlayArrow,
                onClick = {
                    haptics(MoriHaptic.Select)
                    onReadClick(comic.id, comic.lastPageIndex)
                },
                modifier = Modifier.testTag(LibraryTestTags.MenuRead),
            )
            MoriSettingRow(
                title = stringResource(R.string.library_menu_details),
                subtitle = stringResource(R.string.library_card_details),
                icon = MoriIcons.MenuBook,
                onClick = {
                    haptics(MoriHaptic.Select)
                    onDetailsClick(comic.id)
                },
                modifier = Modifier.testTag(LibraryTestTags.MenuDetails),
            )
            MoriSettingRow(
                title = stringResource(
                    if (comic.bookmarked) {
                        R.string.library_menu_bookmark_remove
                    } else {
                        R.string.library_menu_bookmark_add
                    },
                ),
                subtitle = stringResource(R.string.library_card_bookmarked),
                icon = if (comic.bookmarked) {
                    MoriIcons.Bookmark
                } else {
                    MoriIcons.BookmarkBorder
                },
                onClick = {
                    haptics(
                        if (comic.bookmarked) {
                            MoriHaptic.ToggleOff
                        } else {
                            MoriHaptic.ToggleOn
                        },
                    )
                    onAction(LibraryAction.ToggleMenuBookmark)
                },
                modifier = Modifier.testTag(LibraryTestTags.MenuBookmark),
            )
            MoriSettingRow(
                title = stringResource(R.string.library_menu_delete),
                subtitle = stringResource(R.string.library_menu_delete_title),
                icon = MoriIcons.Delete,
                titleColor = MaterialTheme.colorScheme.error,
                iconTint = MaterialTheme.colorScheme.error,
                onClick = { onAction(LibraryAction.OpenMenuDelete) },
                modifier = Modifier.testTag(LibraryTestTags.MenuDelete),
            )
        }
    if (deleteConfirm) {
        AlertDialog(
            onDismissRequest = { onAction(LibraryAction.CloseMenu) },
            title = {
                Text(
                    text = stringResource(R.string.library_menu_delete_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.library_menu_delete_body, comic.title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics(MoriHaptic.Confirm)
                        onAction(LibraryAction.ConfirmMenuDelete)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.testTag(LibraryTestTags.MenuDeleteConfirm),
                ) {
                    Text(text = stringResource(R.string.library_menu_delete_confirm))
                }
            },
            dismissButton = {
                FilledTonalButton(onClick = { onAction(LibraryAction.CloseMenu) }) {
                    Text(stringResource(R.string.library_dialog_cancel))
                }
            },
            modifier = Modifier.testTag(LibraryTestTags.MenuDeleteDialog),
        )
    }
}
