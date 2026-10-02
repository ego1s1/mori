package com.mori.core.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * One-line label on a translucent scrim, for text drawn over artwork
 * (page counters, remaining-count badges). Content is fixed white: theme
 * roles like inverseOnSurface turn dark-on-black in dark mode and vanish.
 * An optional leading icon covers icon-only badges.
 */
@Composable
fun MoriScrimPill(
    text: String,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    icon: ImageVector? = null,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(contentPadding),
        ) {
            if (icon != null) {
                Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
                )
            }
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    style = textStyle,
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
    }
}
