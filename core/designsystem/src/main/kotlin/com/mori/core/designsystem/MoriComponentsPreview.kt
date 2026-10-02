package com.mori.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Light/dark catalog of the shared component language: every Mori*
 * building block screens render with, in one place so visual drift shows
 * up in the IDE instead of on devices.
 */
@ThemePreviews
@Composable
private fun MoriComponentsPreview() {
    MoriTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MoriPrimaryButton(onClick = {}) {
                    Text("Primary")
                }
                MoriTonalButton(onClick = {}) {
                    Text("Tonal")
                }
                MoriOutlinedButton(onClick = {}) {
                    Text("Outlined")
                }
            }
            MoriSettingRow(
                title = "Reader",
                subtitle = "Direction, display, incognito",
                icon = MoriIcons.MenuBook,
                onClick = {},
            )
            MoriSettingSwitch(
                title = "Dynamic color",
                subtitle = "Follow the wallpaper",
                checked = true,
                onCheckedChange = {},
                icon = MoriIcons.Palette,
            )
            MoriSliderRow(
                label = "Brightness",
                value = 0.4f,
                valueRange = -1f..1f,
                onValueChange = {},
                valueText = "+40%",
            )
            MoriChoiceGroup(
                options = listOf(
                    MoriChoiceOption(label = "System"),
                    MoriChoiceOption(label = "Light"),
                    MoriChoiceOption(label = "Dark"),
                ),
                selectedIndex = 0,
                onSelect = {},
            )
            MoriFilterPills(
                options = listOf(
                    MoriChoiceOption(label = "All"),
                    MoriChoiceOption(label = "Action (12)"),
                    MoriChoiceOption(label = "Finished"),
                ),
                selectedIndex = 1,
                onSelect = {},
            )
            MoriSectionCard(title = "Storage") {
                Text("480 MB · 12 books")
            }
            MoriErrorCard(
                body = "The license list failed to load.",
                primaryLabel = "Retry",
                onPrimary = {},
            )
            MoriEmptyState(
                icon = MoriIcons.MenuBook,
                title = "No reading yet",
                body = "Open a comic and stats build up here.",
                actionLabel = null,
                onAction = null,
            )
            MoriProgressBar(progress = { 0.6f })
            MoriScrimPill(text = "12 / 173")
        }
    }
}
