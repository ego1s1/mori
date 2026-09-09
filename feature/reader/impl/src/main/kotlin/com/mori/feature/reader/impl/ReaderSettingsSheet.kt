package com.mori.feature.reader.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mori.core.model.PageFit
import com.mori.core.model.ReadingDirection

/**
 * Modal sheet hosting [ReaderSettingsSheetContent].
 *
 * Sheet-body content is split out so unit tests can render it directly (the modal
 * presentation does not settle under Robolectric legacy graphics).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderSettingsSheet(
    direction: ReadingDirection,
    pageFit: PageFit,
    cropMargins: Boolean,
    volumeKeys: Boolean,
    keepScreenOn: Boolean,
    showTapZones: Boolean,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = { onAction(ReaderAction.CloseSettings) },
        modifier = modifier.testTag(ReaderTestTags.SettingsSheet),
    ) {
        ReaderSettingsSheetContent(
            direction = direction,
            pageFit = pageFit,
            cropMargins = cropMargins,
            volumeKeys = volumeKeys,
            keepScreenOn = keepScreenOn,
            showTapZones = showTapZones,
            onAction = onAction,
        )
    }
}

/** Sheet body content, exposed for testing. */
@Composable
internal fun ReaderSettingsSheetContent(
    direction: ReadingDirection,
    pageFit: PageFit,
    cropMargins: Boolean,
    volumeKeys: Boolean,
    keepScreenOn: Boolean,
    showTapZones: Boolean,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = "Reading settings",
            style = MaterialTheme.typography.titleLarge,
        )

        Text(
            text = "Reading direction",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = direction == ReadingDirection.LEFT_TO_RIGHT,
                onClick = { onAction(ReaderAction.SetDirection(ReadingDirection.LEFT_TO_RIGHT)) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text("Left to right") },
            )
            SegmentedButton(
                selected = direction == ReadingDirection.RIGHT_TO_LEFT,
                onClick = { onAction(ReaderAction.SetDirection(ReadingDirection.RIGHT_TO_LEFT)) },
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
                selected = pageFit == PageFit.WIDTH,
                onClick = { onAction(ReaderAction.SetPageFit(PageFit.WIDTH)) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                label = { Text("Width") },
            )
            SegmentedButton(
                selected = pageFit == PageFit.HEIGHT,
                onClick = { onAction(ReaderAction.SetPageFit(PageFit.HEIGHT)) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                label = { Text("Height") },
            )
            SegmentedButton(
                selected = pageFit == PageFit.ORIGINAL,
                onClick = { onAction(ReaderAction.SetPageFit(PageFit.ORIGINAL)) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                label = { Text("Original") },
            )
        }

        SettingSwitch(
            title = "Crop margins",
            subtitle = "Trim page borders when decoding",
            checked = cropMargins,
            onCheckedChange = { onAction(ReaderAction.ToggleCrop) },
        )

        Text(
            text = "Tap zones",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TapZoneLegend(direction = direction)
        SettingSwitch(
            title = "Preview tap zones",
            subtitle = "Overlay the navigation zones on the page",
            checked = showTapZones,
            onCheckedChange = { onAction(ReaderAction.ToggleTapZones) },
        )

        SettingSwitch(
            title = "Volume keys turn pages",
            subtitle = "Volume down goes forward, volume up goes back",
            checked = volumeKeys,
            onCheckedChange = { onAction(ReaderAction.ToggleVolumeKeys) },
        )
        SettingSwitch(
            title = "Keep screen on",
            subtitle = "Prevent the display from sleeping while reading",
            checked = keepScreenOn,
            onCheckedChange = { onAction(ReaderAction.ToggleKeepScreenOn) },
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
