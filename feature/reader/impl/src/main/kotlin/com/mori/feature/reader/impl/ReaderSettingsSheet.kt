package com.mori.feature.reader.impl

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriSettingSwitch
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
    showPageCounter: Boolean,
    swipeToTurn: Boolean,
    dualPageSplit: Boolean,
    dualPageInvert: Boolean,
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
            showPageCounter = showPageCounter,
            swipeToTurn = swipeToTurn,
            dualPageSplit = dualPageSplit,
            dualPageInvert = dualPageInvert,
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
    showPageCounter: Boolean,
    swipeToTurn: Boolean,
    dualPageSplit: Boolean,
    dualPageInvert: Boolean,
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
            text = stringResource(R.string.reader_sheet_title),
            style = MoriEmphasized.titleLarge,
        )

        Text(
            text = stringResource(R.string.reader_sheet_direction),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = direction == ReadingDirection.LEFT_TO_RIGHT,
                onClick = { onAction(ReaderAction.SetDirection(ReadingDirection.LEFT_TO_RIGHT)) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text(stringResource(R.string.reader_direction_ltr)) },
            )
            SegmentedButton(
                selected = direction == ReadingDirection.RIGHT_TO_LEFT,
                onClick = { onAction(ReaderAction.SetDirection(ReadingDirection.RIGHT_TO_LEFT)) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text(stringResource(R.string.reader_direction_rtl)) },
            )
        }

        Text(
            text = stringResource(R.string.reader_sheet_fit),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = pageFit == PageFit.WIDTH,
                onClick = { onAction(ReaderAction.SetPageFit(PageFit.WIDTH)) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                label = { Text(stringResource(R.string.reader_fit_width)) },
            )
            SegmentedButton(
                selected = pageFit == PageFit.HEIGHT,
                onClick = { onAction(ReaderAction.SetPageFit(PageFit.HEIGHT)) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                label = { Text(stringResource(R.string.reader_fit_height)) },
            )
            SegmentedButton(
                selected = pageFit == PageFit.ORIGINAL,
                onClick = { onAction(ReaderAction.SetPageFit(PageFit.ORIGINAL)) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                label = { Text(stringResource(R.string.reader_fit_original)) },
            )
        }

        MoriSettingSwitch(
            title = stringResource(R.string.reader_crop_title),
            subtitle = stringResource(R.string.reader_crop_subtitle),
            checked = cropMargins,
            onCheckedChange = { onAction(ReaderAction.ToggleCrop) },
        )

        Text(
            text = stringResource(R.string.reader_sheet_zones),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TapZoneLegend(direction = direction)
        MoriSettingSwitch(
            title = stringResource(R.string.reader_zones_preview_title),
            subtitle = stringResource(R.string.reader_zones_preview_subtitle),
            checked = showTapZones,
            onCheckedChange = { onAction(ReaderAction.ToggleTapZones) },
        )

        MoriSettingSwitch(
            title = stringResource(R.string.reader_volume_title),
            subtitle = stringResource(R.string.reader_volume_subtitle),
            checked = volumeKeys,
            onCheckedChange = { onAction(ReaderAction.ToggleVolumeKeys) },
        )
        MoriSettingSwitch(
            title = stringResource(R.string.reader_keep_on_title),
            subtitle = stringResource(R.string.reader_keep_on_subtitle),
            checked = keepScreenOn,
            onCheckedChange = { onAction(ReaderAction.ToggleKeepScreenOn) },
        )
        MoriSettingSwitch(
            title = stringResource(R.string.reader_counter_title),
            subtitle = stringResource(R.string.reader_counter_subtitle),
            checked = showPageCounter,
            onCheckedChange = { onAction(ReaderAction.TogglePageCounter) },
        )
        MoriSettingSwitch(
            title = stringResource(R.string.reader_swipe_title),
            subtitle = stringResource(R.string.reader_swipe_subtitle),
            checked = swipeToTurn,
            onCheckedChange = { onAction(ReaderAction.ToggleSwipeToTurn) },
        )

        Text(
            text = stringResource(R.string.reader_sheet_dual),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MoriSettingSwitch(
            title = stringResource(R.string.reader_dual_title),
            subtitle = stringResource(R.string.reader_dual_subtitle),
            checked = dualPageSplit,
            onCheckedChange = { onAction(ReaderAction.ToggleDualSplit) },
        )
        if (dualPageSplit) {
            MoriSettingSwitch(
                title = stringResource(R.string.reader_dual_invert_title),
                subtitle = stringResource(R.string.reader_dual_invert_subtitle),
                checked = dualPageInvert,
                onCheckedChange = { onAction(ReaderAction.ToggleDualInvert) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

