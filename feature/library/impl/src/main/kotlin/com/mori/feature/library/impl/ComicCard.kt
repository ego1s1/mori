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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriScrimPill
import com.mori.core.designsystem.sharedCoverModifier
import com.mori.core.model.Comic

/**
 * Compact grid cell in the Mihon tradition: full-bleed 2:3 cover with the
 * title set below it in normal flow — no scrim gradient, no text blended
 * over the image — plus a progress bar for started comics and an error pill
 * for failed rows.
 *
 * Tapping the card continues at the saved page, so there is no separate
 * continue button; the pages-left badge marks in-progress books. The
 * continue shelf reuses the same card in [compact] form: fixed width, shelf
 * identity for tests, no long-press (the grid owns details).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun ComicCard(
    comic: Comic,
    onRead: (Comic) -> Unit,
    onDetails: ((Comic) -> Unit)?,
    modifier: Modifier = Modifier,
    sharedCover: Boolean = false,
    compact: Boolean = false,
    cardTag: String = LibraryTestTags.cardFor(comic.id),
) {
    // Wrappers keyed by click-relevant fields (identity, saved page, error)
    // — not full-comic equality: cover/metadata re-emissions (updatedAt
    // bumps on any write) no longer invalidate the handler, while a real
    // progress save still refreshes the captured resume index.
    val click = remember(comic.id, comic.lastPageIndex, comic.error, onRead) { { onRead(comic) } }
    val longClick = remember(comic.id, onDetails) { onDetails?.let { action -> { action(comic) } } }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .then(if (compact) Modifier.width(120.dp) else Modifier)
            .testTag(cardTag)
            .combinedClickable(
                onClick = click,
                onClickLabel = stringResource(R.string.library_card_read, comic.title),
                onLongClick = longClick,
                onLongClickLabel = longClick?.let { stringResource(R.string.library_card_details) },
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
                // Otherwise a bookmark badge marks favorites in the same slot.
                if (comic.isInProgress) {
                    MoriScrimPill(
                        text = stringResource(R.string.library_card_pages_left, comic.pagesLeft),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                    )
                } else if (comic.bookmarked) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                    ) {
                        Icon(
                            imageVector = MoriIcons.Bookmark,
                            contentDescription = stringResource(R.string.library_card_favorite),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(16.dp),
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
