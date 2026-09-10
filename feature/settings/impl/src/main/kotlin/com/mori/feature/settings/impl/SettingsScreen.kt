package com.mori.feature.settings.impl

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriLoading
import com.mori.core.designsystem.MoriSettingSwitch
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.SchemePickerRow
import com.mori.core.designsystem.ThemePreviews
import com.mori.core.designsystem.previewColor
import com.mori.core.model.ColorSchemeChoice
import com.mori.core.model.PageFit
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.ReadingDirection
import com.mori.core.common.formatBytes
import com.mori.core.model.MotionStyle
import com.mori.core.model.StorageUsage
import com.mori.core.model.ThemeMode
import com.mori.core.model.ThemePreferences

@Composable
fun SettingsTabContent(
    modifier: Modifier = Modifier,
) {
    SettingsRouteContent(
        modifier = modifier,
        viewModel = hiltViewModel(),
    )
}

@Composable
private fun SettingsRouteContent(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
    ) { padding ->
        Surface(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when (uiState) {
                SettingsUiState.Loading -> MoriLoading()

                is SettingsUiState.Ready -> SettingsContent(
                    theme = uiState.theme,
                    reader = uiState.reader,
                    motion = uiState.motion,
                    storage = uiState.storage,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
internal fun SettingsContent(
    theme: ThemePreferences,
    reader: ReaderPreferences,
    motion: MotionStyle,
    storage: StorageUsage?,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(SettingsTestTags.Content),
    ) {
        Text(
            text = "Settings",
            style = MoriEmphasized.displaySmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )

        SettingsCard(title = "Appearance") {
            OptionLabel("Theme")
            PillToggleRow(
                options = listOf("System", "Light", "Dark"),
                selectedIndex = when (theme.mode) {
                    ThemeMode.SYSTEM -> 0
                    ThemeMode.LIGHT -> 1
                    ThemeMode.DARK -> 2
                },
                onSelect = {
                    onAction(
                        SettingsAction.SetThemeMode(
                            when (it) {
                                1 -> ThemeMode.LIGHT
                                2 -> ThemeMode.DARK
                                else -> ThemeMode.SYSTEM
                            },
                        ),
                    )
                },
            )
            MoriSettingSwitch(
                title = "Dynamic color",
                subtitle = "Match your wallpaper on Android 12+",
                checked = theme.dynamicColor,
                onCheckedChange = { onAction(SettingsAction.SetDynamicColor(it)) },
            )
            MoriSettingSwitch(
                title = "AMOLED black",
                subtitle = "True-black backgrounds in dark mode",
                checked = theme.amoled,
                onCheckedChange = { onAction(SettingsAction.SetAmoled(it)) },
            )
            OptionLabel("Motion")
            PillToggleRow(
                options = listOf("Expressive", "Calm"),
                selectedIndex = if (motion == MotionStyle.EXPRESSIVE) 0 else 1,
                onSelect = {
                    onAction(
                        SettingsAction.SetMotionStyle(
                            if (it == 0) MotionStyle.EXPRESSIVE else MotionStyle.CALM,
                        ),
                    )
                },
            )
            Text(
                text = "Expressive uses spring physics; calm fades quietly and honors reduced motion.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OptionLabel("Colors")
            SchemePickerRow(
                theme = theme,
                onDynamic = { onAction(SettingsAction.SetDynamicColor(true)) },
                onScheme = { onAction(SettingsAction.SetColorScheme(it)) },
            )
        }

        SettingsCard(title = "Reader defaults") {
            OptionLabel("Reading direction")
            PillToggleRow(
                options = listOf("Left to right", "Right to left"),
                selectedIndex = if (reader.direction == ReadingDirection.LEFT_TO_RIGHT) 0 else 1,
                onSelect = {
                    onAction(
                        SettingsAction.SetDirection(
                            if (it == 0) {
                                ReadingDirection.LEFT_TO_RIGHT
                            } else {
                                ReadingDirection.RIGHT_TO_LEFT
                            },
                        ),
                    )
                },
            )
            OptionLabel("Page fit")
            PillToggleRow(
                options = listOf("Width", "Height", "Original"),
                selectedIndex = when (reader.pageFit) {
                    PageFit.WIDTH -> 0
                    PageFit.HEIGHT -> 1
                    PageFit.ORIGINAL -> 2
                },
                onSelect = {
                    onAction(
                        SettingsAction.SetPageFit(
                            when (it) {
                                1 -> PageFit.HEIGHT
                                2 -> PageFit.ORIGINAL
                                else -> PageFit.WIDTH
                            },
                        ),
                    )
                },
            )
            MoriSettingSwitch(
                title = "Volume keys turn pages",
                subtitle = "Volume down goes forward, volume up goes back",
                checked = reader.volumeKeys,
                onCheckedChange = { onAction(SettingsAction.ToggleVolumeKeys) },
            )
            MoriSettingSwitch(
                title = "Keep screen on",
                subtitle = "Prevent the display from sleeping while reading",
                checked = reader.keepScreenOn,
                onCheckedChange = { onAction(SettingsAction.ToggleKeepScreenOn) },
            )
        }

        SettingsCard(title = "Storage") {
            if (storage != null) {
                Text(
                    text = "${storage.comicCount} comics • " +
                        "${formatBytes(storage.libraryBytes)} library • " +
                        "${formatBytes(storage.coversBytes)} covers",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = { onAction(SettingsAction.ClearThumbnailCache) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clear thumbnail cache")
            }
            Text(
                text = "Covers regenerate the next time each comic is indexed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PlaceholderRow(
                title = "Library location",
                subtitle = "App-private storage",
            )
        }

        SettingsCard(title = "Coming soon") {
            PlaceholderRow(
                title = "CB7 and CBT support",
                subtitle = "More archive formats",
            )
            PlaceholderRow(
                title = "Cloud sync",
                subtitle = "Progress across devices",
            )
        }

        SettingsCard(title = "About") {
            PlaceholderRow(
                title = "Mori",
                subtitle = "Work in progress",
            )
            PlaceholderRow(
                title = "Open-source licenses",
                subtitle = "Coming soon",
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Section card (reference style): large rounded tonal container with an
 * emphasized title and evenly spaced rows.
 */
@Composable
private fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = title,
                style = MoriEmphasized.headlineSmall,
            )
            content()
        }
    }
}

@Composable
private fun OptionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * Pill toggle pair/row (reference style): the selected pill fills with the
 * primary container, the rest stay tonal. Replaces segmented buttons for
 * option choices.
 */
@Composable
private fun PillToggleRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Surface(
                shape = CircleShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag(SettingsTestTags.pillFor(label))
                    .semantics { this.selected = selected }
                    .clickable(
                        onClick = { onSelect(index) },
                        role = Role.RadioButton,
                    ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = "Soon",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SettingsScreenPreview() {
    MoriTheme {
        SettingsScreen(
            uiState = SettingsUiState.Ready(
                theme = ThemePreferences(),
                reader = ReaderPreferences(),
                motion = MotionStyle.EXPRESSIVE,
                storage = StorageUsage(comicCount = 12, libraryBytes = 480_000_000L, coversBytes = 6_000_000L),
            ),
            onAction = {},
        )
    }
}
