package com.mori.core.designsystem

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The one determinate progress language: primary fill on a
 * surfaceContainerHighest track. Callers own size and padding through
 * [modifier]; color roles stay here so cards, rows, hero bars, and the
 * rescan strip never drift apart.
 */
@Composable
fun MoriProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = progress,
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier,
    )
}
