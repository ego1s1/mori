package com.mori.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Max content width on expanded windows (M3 readability guidance). */
val ExpandedContentMaxWidth = 840.dp

/**
 * Centered readable well: phones stay full-bleed, tablets and foldables cap
 * at [ExpandedContentMaxWidth] so grids, lists, and search fields never
 * stretch edge to edge.
 */
@Composable
fun MoriContentWell(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .width(minOf(maxWidth, ExpandedContentMaxWidth))
                .fillMaxHeight(),
        ) {
            content()
        }
    }
}

/**
 * Bottom reserve for the floating navigator + resume CTA so last rows never
 * slide underneath. System bars are already handled by scaffold padding;
 * this covers only the floating chrome.
 */
val FloatingChromeBottomReserve = 112.dp

/**
 * Window width bucket from BoxWithConstraints maxWidth only (no
 * material3-window-size-class dependency). Breakpoints follow M3 guidance:
 * Compact < 600.dp (phones), Medium < 840.dp (foldables/small tablets),
 * Expanded >= 840.dp (tablets/desktop).
 */
enum class WindowWidthClass {
    Compact,
    Medium,
    Expanded,
}

/**
 * Classify the current [BoxWithConstraintsScope.maxWidth]. Must be called
 * from inside a BoxWithConstraints content lambda (e.g. around an Adaptive
 * grid) so maxWidth reflects the real window/well width.
 */
@Composable
fun BoxWithConstraintsScope.windowWidthClass(): WindowWidthClass = when {
    maxWidth < 600.dp -> WindowWidthClass.Compact
    maxWidth < 840.dp -> WindowWidthClass.Medium
    else -> WindowWidthClass.Expanded
}
