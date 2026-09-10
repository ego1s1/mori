package com.mori.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mori.core.designsystem.MoriIcons
import com.mori.core.model.ResumeTarget

/**
 * The single floating navigator: Library + Settings destinations with
 * Reddit-style selected highlighting, plus a Resume action whenever a book
 * is in progress. Visible on both tabs; tab state lives in [MainScreen] and
 * every tap delegates out — no business logic here.
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
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        modifier = modifier.testTag(MainTestTags.Navigator),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                NavDestination(
                    selected = false,
                    onClick = onResumeClick,
                    icon = MoriIcons.PlayArrow,
                    label = "Resume",
                    contentDescription = "Resume ${resume.title}",
                    testTag = MainTestTags.ResumeAction,
                    accent = true,
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
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick, role = Role.Tab)
            .semantics { this.selected = selected }
            .testTag(testTag)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(
                    when {
                        selected -> MaterialTheme.colorScheme.secondaryContainer
                        accent -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    CircleShape,
                )
                .padding(horizontal = 14.dp, vertical = 5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = when {
                    selected -> MaterialTheme.colorScheme.onSecondaryContainer
                    accent -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected || accent) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
