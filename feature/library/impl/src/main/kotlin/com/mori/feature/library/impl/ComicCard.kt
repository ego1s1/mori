package com.mori.feature.library.impl

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mori.core.designsystem.MoriCoverArt
import com.mori.core.designsystem.sharedCoverModifier
import com.mori.core.model.Comic

/**
 * Compact grid cell in the Mihon tradition: full-bleed 2:3 cover with the
 * title set below it in normal flow — no scrim gradient, no text blended
 * over the image — plus a progress bar for started comics and an error pill
 * for failed rows.
 *
 * Tapping the card continues at the saved page, so there is no separate
 * continue button; the pages-left badge marks in-progress books.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun ComicCard(
    comic: Comic,
    onRead: (Comic) -> Unit,
    onDetails: (Comic) -> Unit,
    modifier: Modifier = Modifier,
    sharedCover: Boolean = false,
) {
    // Wrappers remembered on the full comic: grid items skip recomposition
    // when handlers and content are unchanged, and progress updates refresh
    // the captured comic (keyed by equality, not id).
    val click = remember(comic, onRead) { { onRead(comic) } }
    val longClick = remember(comic, onDetails) { { onDetails(comic) } }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .testTag(LibraryTestTags.cardFor(comic.id))
            .combinedClickable(
                onClick = click,
                onClickLabel = stringResource(R.string.library_card_read, comic.title),
                onLongClick = longClick,
                onLongClickLabel = stringResource(R.string.library_card_details),
            ),
    ) {
        Column {
            // Cover morphs into the detail hero on launch (shared element), but
            // only the launching card registers — tracking every card costs a
            // shared-transition overlay per scroll frame.
            Box(
                modifier = Modifier
                    .aspectRatio(COVER_ASPECT)
                    .then(if (sharedCover) sharedCoverModifier(comic.id) else Modifier),
            ) {
                MoriCoverArt(
                    coverPath = comic.coverPath,
                    contentDescription = comic.title,
                )

                if (comic.error != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.library_card_unreadable),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                // Pages-left corner badge while in progress (reference-library
                // style): glanceable remaining count next to the progress bar.
                if (comic.isInProgress) {
                    val left = comic.pageCount - comic.lastPageIndex - 1
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.library_card_pages_left, left),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
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
                if (comic.isInProgress || comic.isFinished) {
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
}

private const val COVER_ASPECT = 2f / 3f
