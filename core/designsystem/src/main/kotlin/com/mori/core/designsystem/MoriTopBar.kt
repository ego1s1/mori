package com.mori.core.designsystem

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Header container colors: fully transparent, never a filled band. The title
 * floats over the page background and stays legible as it collapses, so no
 * surface role is applied at rest or on scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun moriTopBarColors(): TopAppBarColors =
    TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
    )

/**
 * Collapsing, center-aligned screen header matching the reference app: a
 * heavy display title in the top-bar face that collapses on scroll.
 *
 * Single-slot centered bar (no subtitle row): the subtitle overload reserves
 * a second row even when empty, wasting ~40dp of vertical space on every
 * screen. The container is transparent so no band appears.
 *
 * Callers wire `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)`
 * on the Scaffold and share the same [scrollBehavior] instance so the bar
 * actually collapses.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MoriCollapsingTopBar(
    title: String,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
    colors: TopAppBarColors = moriTopBarColors(),
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val titleFont = LocalAppFonts.current.topBarTitle
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = titleFont,
                    fontSize = ScreenTitleSize,
                    lineHeight = ScreenTitleLineHeight,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        scrollBehavior = scrollBehavior,
        modifier = modifier,
    )
}
