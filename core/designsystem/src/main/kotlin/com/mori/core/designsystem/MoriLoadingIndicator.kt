package com.mori.core.designsystem

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Expressive loading-indicator seam.
 *
 * material3 1.3.1 has no `LoadingIndicator`/`ContainedLoadingIndicator`
 * API, so this wraps the legacy indeterminate spinner under the expressive
 * contract (primary color, round caps, compact sizes). Every indeterminate
 * spinner in the app goes through here, so when the BOM reaches 1.4+ only
 * this body changes — `LoadingIndicator()` — and all call sites upgrade at
 * once. Determinate progress (page bars, step segments) correctly stays
 * `LinearProgressIndicator` and never routes here.
 */
@Composable
fun MoriLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
) {
    CircularProgressIndicator(
        color = color,
        strokeWidth = strokeWidth,
        strokeCap = StrokeCap.Round,
        modifier = modifier,
    )
}
