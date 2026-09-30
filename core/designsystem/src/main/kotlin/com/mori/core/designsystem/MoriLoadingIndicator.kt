package com.mori.core.designsystem

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * App loading indicator: the M3E morphing indicator instead of a raw
 * circular spinner, so decoding states feel native to the expressive
 * system rather than bolted on.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MoriLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    LoadingIndicator(
        modifier = modifier,
        color = color,
    )
}
