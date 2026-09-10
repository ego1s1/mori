package com.mori.feature.settings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.ThemePreviews
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
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
internal fun SettingsRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    SettingsTabContent(
        onBackClick = onBackClick,
        modifier = modifier,
        viewModel = viewModel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = MoriIcons.Back, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        Surface(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when (uiState) {
                SettingsUiState.Loading -> androidx.compose.foundation.layout.Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CircularProgressIndicator()
                }

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
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag(SettingsTestTags.Content),
    ) {
        SectionTitle("Appearance")
        Text(
            text = "Theme",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = theme.mode == ThemeMode.SYSTEM,
                onClick = { onAction(SettingsAction.SetThemeMode(ThemeMode.SYSTEM)) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                label = { Text("System") },
            )
            SegmentedButton(
                selected = theme.mode == ThemeMode.LIGHT,
                onClick = { onAction(SettingsAction.SetThemeMode(ThemeMode.LIGHT)) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                label = { Text("Light") },
            )
            SegmentedButton(
                selected = theme.mode == ThemeMode.DARK,
                onClick = { onAction(SettingsAction.SetThemeMode(ThemeMode.DARK)) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                label = { Text("Dark") },
            )
        }
        SettingSwitch(
            title = "Dynamic color",
            subtitle = "Match your wallpaper on Android 12+",
            checked = theme.dynamicColor,
            onCheckedChange = { onAction(SettingsAction.SetDynamicColor(it)) },
        )
        SettingSwitch(
            title = "AMOLED black",
            subtitle = "True-black backgrounds in dark mode",
            checked = theme.amoled,
            onCheckedChange = { onAction(SettingsAction.SetAmoled(it)) },
        )
        Text(
            text = "Motion",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = motion == MotionStyle.EXPRESSIVE,
                onClick = { onAction(SettingsAction.SetMotionStyle(MotionStyle.EXPRESSIVE)) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text("Expressive") },
            )
            SegmentedButton(
                selected = motion == MotionStyle.CALM,
                onClick = { onAction(SettingsAction.SetMotionStyle(MotionStyle.CALM)) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text("Calm") },
            )
        }
        Text(
            text = "Expressive uses spring physics; calm fades quietly and honors reduced motion.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle("Reader defaults", topPadding = 20.dp)
        Text(
            text = "Reading direction",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = reader.direction == ReadingDirection.LEFT_TO_RIGHT,
                onClick = { onAction(SettingsAction.SetDirection(ReadingDirection.LEFT_TO_RIGHT)) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text("Left to right") },
            )
            SegmentedButton(
                selected = reader.direction == ReadingDirection.RIGHT_TO_LEFT,
                onClick = { onAction(SettingsAction.SetDirection(ReadingDirection.RIGHT_TO_LEFT)) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text("Right to left") },
            )
        }
        Text(
            text = "Page fit",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = reader.pageFit == PageFit.WIDTH,
                onClick = { onAction(SettingsAction.SetPageFit(PageFit.WIDTH)) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                label = { Text("Width") },
            )
            SegmentedButton(
                selected = reader.pageFit == PageFit.HEIGHT,
                onClick = { onAction(SettingsAction.SetPageFit(PageFit.HEIGHT)) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                label = { Text("Height") },
            )
            SegmentedButton(
                selected = reader.pageFit == PageFit.ORIGINAL,
                onClick = { onAction(SettingsAction.SetPageFit(PageFit.ORIGINAL)) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                label = { Text("Original") },
            )
        }
        SettingSwitch(
            title = "Volume keys turn pages",
            subtitle = "Volume down goes forward, volume up goes back",
            checked = reader.volumeKeys,
            onCheckedChange = { onAction(SettingsAction.ToggleVolumeKeys) },
        )
        SettingSwitch(
            title = "Keep screen on",
            subtitle = "Prevent the display from sleeping while reading",
            checked = reader.keepScreenOn,
            onCheckedChange = { onAction(SettingsAction.ToggleKeepScreenOn) },
        )

        SectionTitle("Storage", topPadding = 20.dp)
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

        SectionTitle("Coming soon", topPadding = 20.dp)
        PlaceholderRow(
            title = "CB7 and CBT support",
            subtitle = "More archive formats",
        )
        PlaceholderRow(
            title = "Cloud sync",
            subtitle = "Progress across devices",
        )

        SectionTitle("About", topPadding = 20.dp)
        PlaceholderRow(
            title = "Mori",
            subtitle = "Work in progress",
        )
        PlaceholderRow(
            title = "Open-source licenses",
            subtitle = "Coming soon",
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    topPadding: Dp = 4.dp,
) {    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = topPadding, bottom = 4.dp),
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The whole row toggles with switch semantics, so text taps and TalkBack
    // gestures flip the setting — not just the thumb itself.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            ),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
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
            onBackClick = {},
        )
    }
}
