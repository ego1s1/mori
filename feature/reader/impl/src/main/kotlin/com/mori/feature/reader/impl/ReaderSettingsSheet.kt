package com.mori.feature.reader.impl

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mori.core.common.formatPercent
import com.mori.core.designsystem.MoriChoiceGroup
import com.mori.core.designsystem.MoriChoiceOption
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriHaptic
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriSettingSwitch
import com.mori.core.designsystem.MoriSheet
import com.mori.core.designsystem.MoriSliderRow
import com.mori.core.designsystem.MoriTonalButton
import com.mori.core.designsystem.rememberMoriHaptics
import com.mori.core.model.DisplayFilter
import com.mori.core.model.PageFit
import com.mori.core.model.ReaderNavMode
import com.mori.core.model.ReadingDirection
import com.mori.core.model.TapInvertMode

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
    navMode: ReaderNavMode = ReaderNavMode.DEFAULT,
    invertTaps: TapInvertMode = TapInvertMode.NONE,
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
            navMode = navMode,
            invertTaps = invertTaps,
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
    keepScreenOn: Boolean,
    showTapZones: Boolean,
    showPageCounter: Boolean,
    swipeToTurn: Boolean,
    dualPageSplit: Boolean,
    dualPageInvert: Boolean,
    displayFilter: DisplayFilter,
    hasFilterOverride: Boolean,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
    navMode: ReaderNavMode = ReaderNavMode.DEFAULT,
    invertTaps: TapInvertMode = TapInvertMode.NONE,
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
            .padding(horizontal = 24.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Text(
            text = stringResource(R.string.reader_sheet_title),
            style = MoriEmphasized.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )

        Text(
            text = stringResource(R.string.reader_sheet_direction),
            style = MoriEmphasized.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        MoriChoiceGroup(
            options = listOf(
                MoriChoiceOption(label = stringResource(R.string.reader_direction_ltr)),
                MoriChoiceOption(label = stringResource(R.string.reader_direction_rtl)),
            ),
            selectedIndex = when (direction) {
                ReadingDirection.LEFT_TO_RIGHT -> 0
                ReadingDirection.RIGHT_TO_LEFT -> 1
            },
            onSelect = {
                val next = when (it) {
                    0 -> ReadingDirection.LEFT_TO_RIGHT
                    else -> ReadingDirection.RIGHT_TO_LEFT
                }
                onAction(ReaderAction.SetDirection(next))
            },
            fillWidth = false,
        )

        Text(
            text = stringResource(R.string.reader_sheet_fit),
            style = MoriEmphasized.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
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
                val next = when (it) {
                    0 -> PageFit.WIDTH
                    1 -> PageFit.HEIGHT
                    else -> PageFit.ORIGINAL
                }
                onAction(ReaderAction.SetPageFit(next))
            },
            fillWidth = false,
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
            style = MoriEmphasized.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        NavModeChoiceCards(
            selectedMode = navMode,
            direction = direction,
            invertMode = invertTaps,
            onSelectMode = { onAction(ReaderAction.SetNavMode(it)) },
        )

        Text(
            text = stringResource(R.string.reader_sheet_tap_invert),
            style = MoriEmphasized.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        MoriChoiceGroup(
            options = listOf(
                MoriChoiceOption(label = stringResource(R.string.reader_tap_invert_none)),
                MoriChoiceOption(label = stringResource(R.string.reader_tap_invert_horizontal)),
                MoriChoiceOption(label = stringResource(R.string.reader_tap_invert_vertical)),
                MoriChoiceOption(label = stringResource(R.string.reader_tap_invert_both)),
            ),
            selectedIndex = invertTaps.ordinal,
            onSelect = { onAction(ReaderAction.SetInvertTaps(TapInvertMode.entries[it])) },
            fillWidth = false,
        )
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
            style = MoriEmphasized.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
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
            style = MoriEmphasized.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
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
            MoriTonalButton(onClick = { onAction(ReaderAction.ResetDisplayFilter) }) {
                Text(stringResource(R.string.reader_filter_reset))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun NavModeChoiceCards(
    selectedMode: ReaderNavMode,
    direction: ReadingDirection,
    invertMode: TapInvertMode,
    onSelectMode: (ReaderNavMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberMoriHaptics()
    val scrollState = rememberScrollState()
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
    ) {
        ReaderNavMode.entries.forEach { mode ->
            val selected = mode == selectedMode
            val label = when (mode) {
                ReaderNavMode.DEFAULT -> stringResource(R.string.reader_nav_mode_default)
                ReaderNavMode.L_SHAPE -> stringResource(R.string.reader_nav_mode_l_shape)
                ReaderNavMode.KINDLISH -> stringResource(R.string.reader_nav_mode_kindlish)
                ReaderNavMode.EDGE -> stringResource(R.string.reader_nav_mode_edge)
                ReaderNavMode.RIGHT_AND_LEFT -> stringResource(R.string.reader_nav_mode_right_and_left)
                ReaderNavMode.DISABLED -> stringResource(R.string.reader_nav_mode_disabled)
            }
            NavModeCard(
                label = label,
                mode = mode,
                selected = selected,
                direction = direction,
                invertMode = invertMode,
                onClick = {
                    haptics(MoriHaptic.Select)
                    onSelectMode(mode)
                },
            )
        }
    }
}

@Composable
private fun NavModeCard(
    label: String,
    mode: ReaderNavMode,
    selected: Boolean,
    direction: ReadingDirection,
    invertMode: TapInvertMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(width = if (selected) 2.dp else 1.dp, color = borderColor),
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier
            .width(84.dp)
            .semantics {
                this.selected = selected
                this.role = Role.RadioButton
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
        ) {
            NavZoneBoxDiagram(
                mode = mode,
                direction = direction,
                invertMode = invertMode,
                selected = selected,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Minimal box-partition architectural layout representing screen tap zones.
 * Renders cohesive, cleanly partitioned geometric blocks rather than disconnected dots.
 */
@Composable
private fun NavZoneBoxDiagram(
    mode: ReaderNavMode,
    direction: ReadingDirection,
    invertMode: TapInvertMode,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = modifier.size(width = 54.dp, height = 48.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
        ) {
            when (mode) {
                ReaderNavMode.DEFAULT -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        ZoneBox(
                            zone = zoneForTap(0.1f, 0.5f, direction, mode, invertMode),
                            label = "‹",
                            selected = selected,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        ZoneBox(
                            zone = zoneForTap(0.5f, 0.5f, direction, mode, invertMode),
                            label = "•",
                            selected = selected,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        ZoneBox(
                            zone = zoneForTap(0.9f, 0.5f, direction, mode, invertMode),
                            label = "›",
                            selected = selected,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }

                ReaderNavMode.RIGHT_AND_LEFT -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        ZoneBox(
                            zone = zoneForTap(0.1f, 0.5f, direction, mode, invertMode),
                            label = "‹",
                            selected = selected,
                            modifier = Modifier.weight(1.2f).fillMaxHeight(),
                        )
                        ZoneBox(
                            zone = zoneForTap(0.5f, 0.5f, direction, mode, invertMode),
                            label = "•",
                            selected = selected,
                            modifier = Modifier.weight(0.6f).fillMaxHeight(),
                        )
                        ZoneBox(
                            zone = zoneForTap(0.9f, 0.5f, direction, mode, invertMode),
                            label = "›",
                            selected = selected,
                            modifier = Modifier.weight(1.2f).fillMaxHeight(),
                        )
                    }
                }

                ReaderNavMode.KINDLISH -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(1.5.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        ZoneBox(
                            zone = zoneForTap(0.5f, 0.1f, direction, mode, invertMode),
                            label = "•",
                            selected = selected,
                            modifier = Modifier.fillMaxWidth().height(12.dp),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        ) {
                            ZoneBox(
                                zone = zoneForTap(0.1f, 0.6f, direction, mode, invertMode),
                                label = "‹",
                                selected = selected,
                                modifier = Modifier.weight(0.35f).fillMaxHeight(),
                            )
                            ZoneBox(
                                zone = zoneForTap(0.6f, 0.6f, direction, mode, invertMode),
                                label = "›",
                                selected = selected,
                                modifier = Modifier.weight(0.65f).fillMaxHeight(),
                            )
                        }
                    }
                }

                ReaderNavMode.L_SHAPE -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(1.5.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        ZoneBox(
                            zone = zoneForTap(0.5f, 0.1f, direction, mode, invertMode),
                            label = "‹",
                            selected = selected,
                            modifier = Modifier.fillMaxWidth().height(11.dp),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        ) {
                            ZoneBox(
                                zone = zoneForTap(0.1f, 0.5f, direction, mode, invertMode),
                                label = "‹",
                                selected = selected,
                                modifier = Modifier.weight(0.32f).fillMaxHeight(),
                            )
                            ZoneBox(
                                zone = zoneForTap(0.5f, 0.5f, direction, mode, invertMode),
                                label = "•",
                                selected = selected,
                                modifier = Modifier.weight(0.36f).fillMaxHeight(),
                            )
                            ZoneBox(
                                zone = zoneForTap(0.9f, 0.5f, direction, mode, invertMode),
                                label = "›",
                                selected = selected,
                                modifier = Modifier.weight(0.32f).fillMaxHeight(),
                            )
                        }
                        ZoneBox(
                            zone = zoneForTap(0.5f, 0.9f, direction, mode, invertMode),
                            label = "›",
                            selected = selected,
                            modifier = Modifier.fillMaxWidth().height(11.dp),
                        )
                    }
                }

                ReaderNavMode.EDGE -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val edgeZone = zoneForTap(0.05f, 0.05f, direction, mode, invertMode)
                        ZoneBox(
                            zone = edgeZone,
                            label = "",
                            selected = selected,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.5.dp),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(0.52f)
                                .fillMaxHeight(0.72f),
                        ) {
                            ZoneBox(
                                zone = zoneForTap(0.5f, 0.5f, direction, mode, invertMode),
                                label = "•",
                                selected = selected,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            )
                            ZoneBox(
                                zone = zoneForTap(0.5f, 0.8f, direction, mode, invertMode),
                                label = "‹",
                                selected = selected,
                                modifier = Modifier.fillMaxWidth().height(10.dp),
                            )
                        }
                    }
                }

                ReaderNavMode.DISABLED -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                RoundedCornerShape(3.dp),
                            ),
                    ) {
                        Text(
                            text = "OFF",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneBox(
    zone: ReaderZone,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg = when (zone) {
        ReaderZone.PREV -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (selected) 0.85f else 0.55f)
        ReaderZone.NEXT -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (selected) 0.85f else 0.55f)
        ReaderZone.MENU -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (selected) 0.95f else 0.7f)
    }
    val contentColor = when (zone) {
        ReaderZone.PREV -> MaterialTheme.colorScheme.onTertiaryContainer
        ReaderZone.NEXT -> MaterialTheme.colorScheme.onPrimaryContainer
        ReaderZone.MENU -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val displayLabel = if (label.isNotEmpty()) {
        label
    } else {
        when (zone) {
            ReaderZone.PREV -> "‹"
            ReaderZone.NEXT -> "›"
            ReaderZone.MENU -> "•"
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(bg, shape = RoundedCornerShape(3.dp)),
    ) {
        if (displayLabel.isNotEmpty()) {
            Text(
                text = displayLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 9.sp,
                ),
                color = contentColor,
            )
        }
    }
}
