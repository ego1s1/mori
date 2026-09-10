package com.mori.feature.reader.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mori.core.model.ReadingDirection

/**
 * Translucent overlay previewing the tap zones over the current page.
 *
 * An original take on the zone-diagram idea: M3 tonal pills label each third, mirrored
 * for right-to-left. Non-interactive so it never swallows gestures; dismissed from the
 * settings sheet.
 */
@Composable
internal fun TapZoneOverlay(
    direction: ReadingDirection,
    modifier: Modifier = Modifier,
) {
    val left = if (direction == ReadingDirection.LEFT_TO_RIGHT) {
        stringResource(R.string.reader_zone_previous)
    } else {
        stringResource(R.string.reader_zone_next)
    }
    val right = if (direction == ReadingDirection.LEFT_TO_RIGHT) {
        stringResource(R.string.reader_zone_next)
    } else {
        stringResource(R.string.reader_zone_previous)
    }
    Row(modifier = modifier.fillMaxSize()) {
        ZoneCell(label = left, modifier = Modifier.weight(1f))
        ZoneCell(label = stringResource(R.string.reader_zone_menu), modifier = Modifier.weight(1f))
        ZoneCell(label = right, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ZoneCell(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f))
            .padding(4.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

/** Horizontal divider-free spacing helper kept beside the overlay for sheet diagrams. */
@Composable
internal fun TapZoneLegend(
    direction: ReadingDirection,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        val left = if (direction == ReadingDirection.LEFT_TO_RIGHT) "Previous" else "Next"
        val right = if (direction == ReadingDirection.LEFT_TO_RIGHT) "Next" else "Previous"
        listOf(left, "Menu", right).forEach { label ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}
