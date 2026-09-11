package com.mori.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriMotion

/**
 * The floating navigator: Library + Settings destinations with caffeine-style
 * selected highlighting — the active tab is a wide pill carrying icon and
 * label, idle tabs are bare icons. Tab state lives in [MainScreen] and every
 * tap delegates out — no business logic here.
 *
 * Geometry is fully deterministic (M3 expressive 4dp grid): 48dp cells on a
 * 4dp gap inside 8x4dp chrome, so the bar always measures 56dp tall — the
 * [ResumeButton] circle matches that height exactly.
 */
@Composable
internal fun MainNavigator(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 3.dp,
        modifier = modifier.testTag(MainTestTags.Navigator),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(4.dp),
        ) {
            NavDestination(
                selected = selectedTab == 0,
                onClick = { onSelectTab(0) },
                icon = MoriIcons.MenuBook,
                label = "Library",
                contentDescription = "Library tab",
                testTag = MainTestTags.LibraryTab,
            )
            NavDestination(
                selected = selectedTab == 1,
                onClick = { onSelectTab(1) },
                icon = MoriIcons.Settings,
                label = "Settings",
                contentDescription = "Settings tab",
                testTag = MainTestTags.SettingsTab,
            )
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
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        tonalElevation = 0.dp,
        shadowElevation = 3.dp,
        modifier = modifier
            .size(56.dp)
            .testTag(MainTestTags.ResumeAction),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = MoriIcons.PlayArrow,
                contentDescription = "Resume $title",
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun NavDestination(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    contentDescription: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val container = when {
        selected -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    val content = when {
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick, role = Role.Tab)
            .semantics { this.selected = selected }
            .testTag(testTag)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
    ) {
        // Indicator pill carries even insets on every side, so the highlight
        // never sits closer to one bar edge than the others; the label only
        // widens it, expanding in place with a fade.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .background(container, CircleShape)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Icon(
                imageVector = icon,
                // The visible label carries the name when shown; the icon
                // only names itself for icon-only destinations.
                contentDescription = if (selected) null else contentDescription,
                tint = content,
                modifier = Modifier.size(20.dp),
            )
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(animationSpec = MoriMotion.defaultEffectsSpec()) +
                    expandHorizontally(animationSpec = MoriMotion.defaultSpatialSpec()),
                exit = fadeOut(animationSpec = MoriMotion.calmFade()) +
                    shrinkHorizontally(animationSpec = MoriMotion.calmFade()),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    maxLines = 1,
                )
            }
        }
    }
}
