package com.mori.feature.reader.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mori.core.model.ReaderNavMode
import com.mori.core.model.ReadingDirection
import com.mori.core.model.TapInvertMode

/**
 * Translucent overlay previewing the tap zones over the current page.
 *
 * An expressive 3x3 zone preview showing which area corresponds to
 * previous, next, or menu based on [navMode], [invertMode], and [direction].
 */
@Composable
internal fun TapZoneOverlay(
    direction: ReadingDirection,
    modifier: Modifier = Modifier,
    navMode: ReaderNavMode = ReaderNavMode.DEFAULT,
    invertMode: TapInvertMode = TapInvertMode.NONE,
) {
    Column(modifier = modifier.fillMaxSize()) {
        for (row in 0..2) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                for (col in 0..2) {
                    val zone = zoneForTap(
                        fractionX = (col + 0.5f) / 3f,
                        fractionY = (row + 0.5f) / 3f,
                        direction = direction,
                        navMode = navMode,
                        invertMode = invertMode,
                    )
                    val label = when (zone) {
                        ReaderZone.PREV -> stringResource(R.string.reader_zone_previous)
                        ReaderZone.NEXT -> stringResource(R.string.reader_zone_next)
                        ReaderZone.MENU -> stringResource(R.string.reader_zone_menu)
                    }
                    val bg = when (zone) {
                        ReaderZone.PREV -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f)
                        ReaderZone.NEXT -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                        ReaderZone.MENU -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                    }
                    ZoneCell(
                        label = label,
                        containerColor = bg,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoneCell(
    label: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .background(containerColor)
            .padding(4.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

/** Mini 3x3 layout diagram for the settings sheet. */
@Composable
internal fun TapZoneLegend(
    direction: ReadingDirection,
    modifier: Modifier = Modifier,
    navMode: ReaderNavMode = ReaderNavMode.DEFAULT,
    invertMode: TapInvertMode = TapInvertMode.NONE,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.size(width = 84.dp, height = 56.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            for (row in 0..2) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    for (col in 0..2) {
                        val zone = zoneForTap(
                            fractionX = (col + 0.5f) / 3f,
                            fractionY = (row + 0.5f) / 3f,
                            direction = direction,
                            navMode = navMode,
                            invertMode = invertMode,
                        )
                        val text = when (zone) {
                            ReaderZone.PREV -> "P"
                            ReaderZone.NEXT -> "N"
                            ReaderZone.MENU -> "M"
                        }
                        val bg = when (zone) {
                            ReaderZone.PREV -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                            ReaderZone.NEXT -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ReaderZone.MENU -> MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(1.dp)
                                .background(bg, shape = MaterialTheme.shapes.extraSmall),
                        ) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
