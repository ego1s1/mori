package com.mori.feature.reader.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mori.core.common.formatPercent
import com.mori.core.designsystem.MoriChoiceGroup
import com.mori.core.designsystem.MoriChoiceOption
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriSettingSwitch
import com.mori.core.designsystem.MoriSheet
import com.mori.core.designsystem.MoriSliderRow
import com.mori.core.model.DisplayFilter
import com.mori.core.model.PageFit
import com.mori.core.model.ReadingDirection

/**
 * Modal sheet hosting [ReaderSettingsSheetContent].
 *
 * Sheet-body content is split out so unit tests can render it directly (the modal
 * presentation does not settle under Robolectric legacy graphics).
 */
@Composable
internal fun ReaderSettingsSheet(
    direction: ReadingDirection,
    pageFit: PageFit,
    cropMargins: Boolean,
    volumeKeys: Boolean,
    volumeKeysInverted: Boolean,
    incognito: Boolean,
    displayFilter: DisplayFilter,
    hasFilterOverride: Boolean,
    keepScreenOn: Boolean,
    showTapZones: Boolean,
    showPageCounter: Boolean,
    swipeToTurn: Boolean,
    dualPageSplit: Boolean,
    dualPageInvert: Boolean,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    MoriSheet(
        onDismissRequest = { onAction(ReaderAction.CloseSettings) },
        modifier = modifier.testTag(ReaderTestTags.SettingsSheet),
    ) {
        ReaderSettingsSheetContent(
            direction = direction,
            pageFit = pageFit,
            cropMargins = cropMargins,
            volumeKeys = volumeKeys,
            volumeKeysInverted = volumeKeysInverted,
            incognito = incognito,
            displayFilter = displayFilter,
            hasFilterOverride = hasFilterOverride,
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
    volumeKeysInverted: Boolean,
    incognito: Boolean,
    displayFilter: DisplayFilter,
    hasFilterOverride: Boolean,
    keepScreenOn: Boolean,
    showTapZones: Boolean,
    showPageCounter: Boolean,
    swipeToTurn: Boolean,
    dualPageSplit: Boolean,
    dualPageInvert: Boolean,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var previewBrightness by remember(displayFilter.brightness) {
        mutableStateOf(displayFilter.brightness)
    }
    var previewNightTint by remember(displayFilter.nightTint) {
        mutableStateOf(displayFilter.nightTint)
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
    Text(
        text = stringResource(R.string.reader_sheet_title),
        style = MaterialTheme.typography.titleMedium,
        )

        Text(
            text = stringResource(R.string.reader_sheet_direction),
            style = MoriEmphasized.titleSmall,
        )
        MoriChoiceGroup(
            options = listOf(
                MoriChoiceOption(label = stringResource(R.string.reader_direction_ltr)),
                MoriChoiceOption(label = stringResource(R.string.reader_direction_rtl)),
            ),
            selectedIndex = if (direction == ReadingDirection.LEFT_TO_RIGHT) 0 else 1,
            onSelect = {
                onAction(
                    ReaderAction.SetDirection(
                        if (it == 0) ReadingDirection.LEFT_TO_RIGHT else ReadingDirection.RIGHT_TO_LEFT,
                    ),
                )
            },
        )

        Text(
            text = stringResource(R.string.reader_sheet_fit),
            style = MoriEmphasized.titleSmall,
        )
        MoriChoiceGroup(
            options = listOf(
                MoriChoiceOption(label = stringResource(R.string.reader_fit_width)),
                MoriChoiceOption(label = stringResource(R.string.reader_fit_height)),
                MoriChoiceOption(label = stringResource(R.string.reader_fit_original)),
            ),
            selectedIndex = when (pageFit) {
                PageFit.WIDTH -> 0
                PageFit.HEIGHT -> 1
                PageFit.ORIGINAL -> 2
            },
            onSelect = {
                onAction(
                    ReaderAction.SetPageFit(
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
            title = stringResource(R.string.reader_crop_title),
            subtitle = stringResource(R.string.reader_crop_subtitle),
            checked = cropMargins,
            onCheckedChange = { onAction(ReaderAction.ToggleCrop) },
            icon = MoriIcons.Crop,
        )

        Text(
            text = stringResource(R.string.reader_sheet_zones),
            style = MoriEmphasized.titleSmall,
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
        if (volumeKeys) {
            MoriSettingSwitch(
                title = stringResource(R.string.reader_volume_invert_title),
                subtitle = stringResource(R.string.reader_volume_invert_subtitle),
                checked = volumeKeysInverted,
                onCheckedChange = { onAction(ReaderAction.ToggleVolumeKeysInverted) },
            )
        }
        MoriSettingSwitch(
            title = stringResource(R.string.reader_keep_on_title),
            subtitle = stringResource(R.string.reader_keep_on_subtitle),
            checked = keepScreenOn,
            onCheckedChange = { onAction(ReaderAction.ToggleKeepScreenOn) },
        )
        MoriSettingSwitch(
            title = stringResource(R.string.reader_incognito_title),
            subtitle = stringResource(R.string.reader_incognito_subtitle),
            checked = incognito,
            onCheckedChange = { onAction(ReaderAction.ToggleIncognito) },
            icon = MoriIcons.Incognito,
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
            style = MoriEmphasized.titleSmall,
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

        Text(
            text = stringResource(R.string.reader_sheet_display),
            style = MoriEmphasized.titleSmall,
        )
        Text(
            text = stringResource(R.string.reader_display_scope),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MoriSliderRow(
            label = stringResource(R.string.reader_filter_brightness),
            value = previewBrightness,
            valueRange = -1f..1f,
            valueText = formatPercent(previewBrightness),
            onValueChange = { previewBrightness = it },
            onValueChangeFinished = { onAction(ReaderAction.SetFilterBrightness(previewBrightness)) },
        )
        MoriSliderRow(
            label = stringResource(R.string.reader_filter_night),
            value = previewNightTint,
            valueRange = 0f..1f,
            valueText = formatPercent(previewNightTint),
            onValueChange = { previewNightTint = it },
            onValueChangeFinished = { onAction(ReaderAction.SetFilterNightTint(previewNightTint)) },
        )
        MoriSettingSwitch(
            title = stringResource(R.string.reader_filter_grayscale_title),
            subtitle = stringResource(R.string.reader_filter_grayscale_subtitle),
            checked = displayFilter.grayscale,
            onCheckedChange = { onAction(ReaderAction.ToggleFilterGrayscale) },
        )
        MoriSettingSwitch(
            title = stringResource(R.string.reader_filter_invert_title),
            subtitle = stringResource(R.string.reader_filter_invert_subtitle),
            checked = displayFilter.invert,
            onCheckedChange = { onAction(ReaderAction.ToggleFilterInvert) },
        )
        if (hasFilterOverride) {
            FilledTonalButton(onClick = { onAction(ReaderAction.ResetDisplayFilter) }) {
                Text(stringResource(R.string.reader_filter_reset))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

