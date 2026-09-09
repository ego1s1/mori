package com.mori.feature.library.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mori.core.model.ThemeMode
import com.mori.core.model.ThemePreferences

/**
 * App settings sheet: theme mode, dynamic color, and AMOLED black.
 *
 * Body is split out so unit tests render it without the modal wrapper.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibrarySettingsSheet(
    theme: ThemePreferences,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = { onAction(LibraryAction.CloseSettings) },
        modifier = modifier.testTag(LibraryTestTags.SettingsSheet),
    ) {
        LibrarySettingsSheetContent(
            theme = theme,
            onAction = onAction,
        )
    }
}

/** Sheet body content, exposed for testing. */
@Composable
internal fun LibrarySettingsSheetContent(
    theme: ThemePreferences,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(text = "Appearance", style = MaterialTheme.typography.titleLarge)

        Text(
            text = "Theme",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = theme.mode == ThemeMode.SYSTEM,
                onClick = { onAction(LibraryAction.SetThemeMode(ThemeMode.SYSTEM)) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                label = { Text("System") },
            )
            SegmentedButton(
                selected = theme.mode == ThemeMode.LIGHT,
                onClick = { onAction(LibraryAction.SetThemeMode(ThemeMode.LIGHT)) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                label = { Text("Light") },
            )
            SegmentedButton(
                selected = theme.mode == ThemeMode.DARK,
                onClick = { onAction(LibraryAction.SetThemeMode(ThemeMode.DARK)) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                label = { Text("Dark") },
            )
        }

        SettingSwitch(
            title = "Dynamic color",
            subtitle = "Match your wallpaper on Android 12+",
            checked = theme.dynamicColor,
            onCheckedChange = { onAction(LibraryAction.SetDynamicColor(it)) },
        )
        SettingSwitch(
            title = "AMOLED black",
            subtitle = "True-black backgrounds in dark mode",
            checked = theme.amoled,
            onCheckedChange = { onAction(LibraryAction.SetAmoled(it)) },
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
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
            onCheckedChange = onCheckedChange,
        )
    }
}
