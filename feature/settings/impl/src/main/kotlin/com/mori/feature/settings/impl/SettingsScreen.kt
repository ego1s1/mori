package com.mori.feature.settings.impl


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriLoading
import com.mori.core.designsystem.MoriSectionCard
import com.mori.core.designsystem.MoriSettingSwitch
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.SchemePickerRow
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
    onLicensesClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    appVersion: String,
) {
    SettingsRouteContent(
        onLicensesClick = onLicensesClick,
        modifier = modifier,
        appVersion = appVersion,
        viewModel = hiltViewModel(),
    )
}

@Composable
private fun SettingsRouteContent(
    onLicensesClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    appVersion: String,
    viewModel: SettingsViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onLicensesClick = onLicensesClick,
        appVersion = appVersion,
        modifier = modifier,
    )
}

@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onLicensesClick: () -> Unit = {},
    appVersion: String = "",
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
                    onLicensesClick = onLicensesClick,
                    appVersion = appVersion,
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
    onLicensesClick: () -> Unit = {},
    appVersion: String = "",
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
            text = stringResource(R.string.settings_title),
            style = MoriEmphasized.displaySmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )

        MoriSectionCard(title = stringResource(R.string.settings_card_appearance)) {
            OptionLabel(stringResource(R.string.settings_theme))
            SegmentedChoiceRow(
                options = listOf(
                    stringResource(R.string.settings_theme_system),
                    stringResource(R.string.settings_theme_light),
                    stringResource(R.string.settings_theme_dark),
                ),
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
                title = stringResource(R.string.settings_dynamic_title),
                subtitle = stringResource(R.string.settings_dynamic_subtitle),
                checked = theme.dynamicColor,
                onCheckedChange = { onAction(SettingsAction.SetDynamicColor(it)) },
            )
            // True-black only applies in dark mode; hide it under explicit
            // light so the toggle never reads as broken.
            if (theme.mode != ThemeMode.LIGHT) {
                MoriSettingSwitch(
                    title = stringResource(R.string.settings_amoled_title),
                    subtitle = stringResource(R.string.settings_amoled_subtitle),
                    checked = theme.amoled,
                    onCheckedChange = { onAction(SettingsAction.SetAmoled(it)) },
                )
            }
            OptionLabel(stringResource(R.string.settings_motion))
            SegmentedChoiceRow(
                options = listOf(
                    stringResource(R.string.settings_motion_expressive),
                    stringResource(R.string.settings_motion_calm),
                ),
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
                text = stringResource(R.string.settings_motion_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OptionLabel(stringResource(R.string.settings_colors))
            SchemePickerRow(
                theme = theme,
                onDynamic = { onAction(SettingsAction.SetDynamicColor(true)) },
                onScheme = { onAction(SettingsAction.SetColorScheme(it)) },
            )
        }

        MoriSectionCard(title = stringResource(R.string.settings_card_reader)) {
            OptionLabel(stringResource(R.string.settings_direction))
            SegmentedChoiceRow(
                options = listOf(
                    stringResource(R.string.settings_direction_ltr),
                    stringResource(R.string.settings_direction_rtl),
                ),
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
            OptionLabel(stringResource(R.string.settings_fit))
            SegmentedChoiceRow(
                options = listOf(
                    stringResource(R.string.settings_fit_width),
                    stringResource(R.string.settings_fit_height),
                    stringResource(R.string.settings_fit_original),
                ),
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
                title = stringResource(R.string.settings_volume_title),
                subtitle = stringResource(R.string.settings_volume_subtitle),
                checked = reader.volumeKeys,
                onCheckedChange = { onAction(SettingsAction.ToggleVolumeKeys) },
            )
            if (reader.volumeKeys) {
                MoriSettingSwitch(
                    title = stringResource(R.string.settings_volume_invert_title),
                    subtitle = stringResource(R.string.settings_volume_invert_subtitle),
                    checked = reader.volumeKeysInverted,
                    onCheckedChange = { onAction(SettingsAction.ToggleVolumeKeysInverted) },
                )
            }
            MoriSettingSwitch(
                title = stringResource(R.string.settings_keep_on_title),
                subtitle = stringResource(R.string.settings_keep_on_subtitle),
                checked = reader.keepScreenOn,
                onCheckedChange = { onAction(SettingsAction.ToggleKeepScreenOn) },
            )
            MoriSettingSwitch(
                title = stringResource(R.string.settings_crop_title),
                subtitle = stringResource(R.string.settings_crop_subtitle),
                checked = reader.cropMargins,
                onCheckedChange = { onAction(SettingsAction.ToggleCropMargins) },
            )
            MoriSettingSwitch(
                title = stringResource(R.string.settings_counter_title),
                subtitle = stringResource(R.string.settings_counter_subtitle),
                checked = reader.showPageCounter,
                onCheckedChange = { onAction(SettingsAction.TogglePageCounter) },
            )
            MoriSettingSwitch(
                title = stringResource(R.string.settings_swipe_title),
                subtitle = stringResource(R.string.settings_swipe_subtitle),
                checked = reader.swipeToTurn,
                onCheckedChange = { onAction(SettingsAction.ToggleSwipeToTurn) },
            )
        }

        MoriSectionCard(title = stringResource(R.string.settings_card_storage)) {
            if (storage != null) {
                Text(
                    text = stringResource(
                        R.string.settings_storage_summary,
                        storage.comicCount,
                        formatBytes(storage.libraryBytes),
                        formatBytes(storage.coversBytes),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = { onAction(SettingsAction.ClearThumbnailCache) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_clear_cache))
            }
            Text(
                text = stringResource(R.string.settings_clear_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        MoriSectionCard(title = stringResource(R.string.settings_card_soon)) {
            PlaceholderRow(
                title = stringResource(R.string.settings_soon_sync),
                subtitle = stringResource(R.string.settings_soon_sync_subtitle),
            )
        }

        MoriSectionCard(title = stringResource(R.string.settings_card_about)) {
            PlaceholderRow(
                title = stringResource(R.string.settings_about_app),
                subtitle = stringResource(R.string.settings_about_version, appVersion),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .clickable(onClick = onLicensesClick, role = Role.Button)
                    .padding(vertical = 8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_about_licenses),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_about_licenses_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = MoriIcons.Forward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun OptionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier,
    )
}

/**
 * Single-choice segmented row: the M3 control for option choices, shared
 * with the onboarding and reader sheets. Selection semantics keep the
 * tests on tags, not pixels.
 */
@Composable
private fun SegmentedChoiceRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(label) },
                modifier = Modifier.testTag(SettingsTestTags.segmentFor(label)),
            )
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = stringResource(R.string.settings_soon_badge),
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
            appVersion = "1.1.0",
        )
    }
}
