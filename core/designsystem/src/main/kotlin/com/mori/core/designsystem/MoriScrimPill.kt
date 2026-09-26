package com.mori.core.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * One-line label on a translucent scrim, for text drawn over artwork
 * (page counters, remaining-count badges). The scrim uses the scheme scrim
 * role so it tracks the theme instead of pinning raw black.
 */
@Composable
fun MoriScrimPill(
    text: String,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            maxLines = 1,
            modifier = Modifier.padding(contentPadding),
        )
    }
}
