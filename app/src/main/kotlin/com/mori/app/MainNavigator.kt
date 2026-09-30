package com.mori.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mori.core.designsystem.LocalExpressiveMotionEnabled
import com.mori.core.designsystem.MoriHaptic
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriMotion
import com.mori.core.designsystem.rememberMoriHaptics

/**
 * Floating navigator: a vibrant [HorizontalFloatingToolbar] carrying the three
 * destinations as pill [ToggleButton]s — the selected tab swaps to its filled
 * icon and expands to reveal its label, idle tabs stay bare outlined icons
 * (reference-app parity). The bar hides on scroll and rides above the system
 * nav bar.
 *
 * Tab state lives in [MainScreen] and every tap delegates out — no business
 * logic here.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MainNavigator(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    scrollBehavior: FloatingToolbarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val expressive = LocalExpressiveMotionEnabled.current
    val motionScheme = MaterialTheme.motionScheme

    // The pill is one element of a caller-centred row (pill + resume); together
    // they read as a single floating unit centred on the screen.
    HorizontalFloatingToolbar(
        expanded = true,
        scrollBehavior = scrollBehavior,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
            toolbarContainerColor = primaryContainer,
            toolbarContentColor = onPrimaryContainer,
        ),
        modifier = modifier.testTag(MainTestTags.Navigator),
    ) {
            NavDestination(
                selected = selectedTab == 0,
                onClick = { onSelectTab(0) },
                outlinedIcon = MoriIcons.MenuBookOutlined,
                filledIcon = MoriIcons.MenuBook,
                label = "Library",
                testTag = MainTestTags.LibraryTab,
                colors = toggleColors(primary, onPrimary, primaryContainer, onPrimaryContainer),
                expandSpec = { if (expressive) motionScheme.defaultSpatialSpec() else MoriMotion.calmFade() },
            )
            NavDestination(
                selected = selectedTab == 1,
                onClick = { onSelectTab(1) },
                outlinedIcon = MoriIcons.BarChartOutlined,
                filledIcon = MoriIcons.BarChart,
                label = "Stats",
                testTag = MainTestTags.StatsTab,
                colors = toggleColors(primary, onPrimary, primaryContainer, onPrimaryContainer),
                expandSpec = { if (expressive) motionScheme.defaultSpatialSpec() else MoriMotion.calmFade() },
            )
            NavDestination(
                selected = selectedTab == 2,
                onClick = { onSelectTab(2) },
                outlinedIcon = MoriIcons.SettingsOutlined,
                filledIcon = MoriIcons.Settings,
                label = "Settings",
                testTag = MainTestTags.SettingsTab,
                colors = toggleColors(primary, onPrimary, primaryContainer, onPrimaryContainer),
                expandSpec = { if (expressive) motionScheme.defaultSpatialSpec() else MoriMotion.calmFade() },
            )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun toggleColors(
    primary: androidx.compose.ui.graphics.Color,
    onPrimary: androidx.compose.ui.graphics.Color,
    primaryContainer: androidx.compose.ui.graphics.Color,
    onPrimaryContainer: androidx.compose.ui.graphics.Color,
) = ToggleButtonDefaults.toggleButtonColors(
    containerColor = primaryContainer,
    contentColor = onPrimaryContainer,
    checkedContainerColor = primary,
    checkedContentColor = onPrimary,
)

/**
 * One destination: a circular [ToggleButton] that crossfades outlined↔filled on
 * selection and expands to reveal its label, wrapped in a tooltip so icon-only
 * tabs stay discoverable.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NavDestination(
    selected: Boolean,
    onClick: () -> Unit,
    outlinedIcon: ImageVector,
    filledIcon: ImageVector,
    label: String,
    testTag: String,
    colors: androidx.compose.material3.ToggleButtonColors,
    expandSpec: () -> androidx.compose.animation.core.FiniteAnimationSpec<androidx.compose.ui.unit.IntSize>,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberMoriHaptics()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        ToggleButton(
            checked = selected,
            onCheckedChange = {
                haptics(MoriHaptic.Select)
                onClick()
            },
            colors = colors,
            shapes = ToggleButtonDefaults.shapes(CircleShape, CircleShape, CircleShape),
            modifier = modifier
                .height(56.dp)
                .testTag(testTag)
                .semantics { this.selected = selected },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Crossfade(targetState = selected, label = "navIcon") { isSelected ->
                    if (isSelected) {
                        Icon(filledIcon, contentDescription = label)
                    } else {
                        Icon(outlinedIcon, contentDescription = label)
                    }
                }
                AnimatedVisibility(
                    visible = selected,
                    enter = expandHorizontally(animationSpec = expandSpec()),
                    exit = shrinkHorizontally(animationSpec = expandSpec()),
                ) {
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                    )
                }
            }
        }
    }
}

/**
 * Standalone resume action: a 56dp circle riding beside the navigator at the
 * same height, icon-only. Split out of the pill so the tab bar keeps one job
 * (tabs) and the CTA keeps its own (resume).
 */
@Composable
internal fun ResumeButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberMoriHaptics()
    Surface(
        onClick = {
            haptics(MoriHaptic.PrimaryAction)
            onClick()
        },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        tonalElevation = 0.dp,
        shadowElevation = 3.dp,
        modifier = modifier
            .size(56.dp)
            .testTag(MainTestTags.ResumeAction),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = MoriIcons.PlayArrow,
                contentDescription = "Resume $title",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
