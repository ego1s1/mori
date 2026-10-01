package com.mori.core.designsystem

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * The one determinate progress language: expressive wavy fill on a
 * surfaceContainerHighest track. Callers own size and padding through
 * [modifier]; color roles stay here so cards, rows, hero bars, and the
 * rescan strip never drift apart.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MoriProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    LinearWavyProgressIndicator(
        progress = progress,
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.then(
            if (contentDescription != null) {
                Modifier.semantics { this.contentDescription = contentDescription }
            } else {
                Modifier
            },
        ),
    )
}
