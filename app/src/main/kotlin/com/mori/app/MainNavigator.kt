package com.mori.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.mori.core.model.ResumeTarget

/**
 * The single floating navigator: Library + Settings destinations with
 * caffeine-style selected highlighting — the active tab is a wide pill
 * carrying icon and label, idle tabs are bare icons — plus a Resume action
 * whenever a book is in progress. Visible on both tabs; tab state lives in
 * [MainScreen] and every tap delegates out — no business logic here.
 */
@Composable
internal fun MainNavigator(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    resume: ResumeTarget?,
    onResumeClick: () -> Unit,
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
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
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
            if (resume != null) {
                // An action, not a tab: the label stays so the CTA reads at a
                // glance ("Resume"), while idle tabs keep icon-only.
                NavDestination(
                    selected = false,
                    onClick = onResumeClick,
                    icon = MoriIcons.PlayArrow,
                    label = "Resume",
                    contentDescription = "Resume ${resume.title}",
                    testTag = MainTestTags.ResumeAction,
                    accent = true,
                    showLabel = true,
                )
            }
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
    accent: Boolean = false,
    showLabel: Boolean = selected,
) {
    val container = when {
        selected -> MaterialTheme.colorScheme.secondaryContainer
        accent -> MaterialTheme.colorScheme.tertiaryContainer
        else -> Color.Transparent
    }
    val content = when {
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        accent -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    // The active pill stretches wider than idle icons (reference style); the
    // width glides on a spatial spring while the label fades/expands in.
    val horizontalPadding by animateDpAsState(
        targetValue = if (showLabel) 20.dp else 12.dp,
        animationSpec = MoriMotion.defaultSpatialSpec(),
        label = "pillWidth",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(container, CircleShape)
            .clickable(onClick = onClick, role = Role.Tab)
            .semantics { this.selected = selected }
            .testTag(testTag)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .padding(horizontal = horizontalPadding, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                // The visible label carries the name when shown; the icon
                // only names itself for icon-only destinations.
                contentDescription = if (showLabel) null else contentDescription,
                tint = content,
                modifier = Modifier.size(20.dp),
            )
            AnimatedVisibility(
                visible = showLabel,
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
