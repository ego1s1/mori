package com.mori.feature.settings.impl


import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import androidx.documentfile.provider.DocumentFile
import com.mori.core.designsystem.MoriSectionCard
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.FloatingChromeBottomReserve
import com.mori.core.designsystem.LocalAppFonts
import com.mori.core.designsystem.LocalExpressiveMotionEnabled
import com.mori.core.designsystem.MoriChoiceGroup
import com.mori.core.designsystem.MoriChoiceOption
import com.mori.core.designsystem.MoriCollapsingTopBar
import com.mori.core.designsystem.MoriEnterKind
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriHaptic
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriLoading
import com.mori.core.designsystem.MoriMotion
import com.mori.core.designsystem.screenEnter
import com.mori.core.designsystem.screenExit
import com.mori.core.designsystem.screenPopEnter
import com.mori.core.designsystem.screenPopExit
import com.mori.core.designsystem.MoriSettingRow
import com.mori.core.designsystem.enter
import com.mori.core.designsystem.exit
import com.mori.core.designsystem.MoriSettingSwitch
import com.mori.core.designsystem.MoriSliderRow
import com.mori.core.designsystem.MoriAlertDialog
import com.mori.core.designsystem.MoriConfirmDialog
import com.mori.core.designsystem.MoriPrimaryButton
import com.mori.core.designsystem.MoriTonalButton
import com.mori.core.designsystem.MoriOutlinedButton
import com.mori.core.designsystem.MoriTextButton
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.SchemePickerRow
import com.mori.core.designsystem.ThemePreviews
import com.mori.core.designsystem.rememberMoriHaptics
import com.mori.core.common.formatBytes
import com.mori.core.common.formatPercent
import com.mori.core.model.PageFit
import com.mori.core.model.ReaderNavMode
import com.mori.core.model.TapInvertMode
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.ReadingDirection
import com.mori.core.model.UserCollection
import com.mori.core.model.MotionStyle
import com.mori.core.model.StorageUsage
import com.mori.core.model.ThemeMode
import com.mori.core.model.ThemePreferences
import kotlinx.coroutines.flow.SharedFlow

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
        events = viewModel.events,
        onLicensesClick = onLicensesClick,
        appVersion = appVersion,
        modifier = modifier,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onLicensesClick: () -> Unit = {},
    appVersion: String = "",
    modifier: Modifier = Modifier,
    events: SharedFlow<SettingsEvent>? = null,
    snackbarHost: SnackbarHostState = remember { SnackbarHostState() },
) {
    val clearedMessage = stringResource(R.string.settings_cache_cleared)
    val failedMessage = stringResource(R.string.settings_cache_failed)
    val haptics = rememberMoriHaptics()
    LaunchedEffect(events) {
        events?.collect { event ->
            when (event) {
                SettingsEvent.CacheCleared -> haptics(MoriHaptic.Confirm)
                SettingsEvent.CacheClearFailed -> haptics(MoriHaptic.Reject)
            }
            snackbarHost.showSnackbar(
                when (event) {
                    SettingsEvent.CacheCleared -> clearedMessage
                    SettingsEvent.CacheClearFailed -> failedMessage
                },
            )
        }
    }
    Surface(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            SettingsUiState.Loading -> MoriLoading()

            is SettingsUiState.Ready -> SettingsContent(
                theme = uiState.theme,
                reader = uiState.reader,
                motion = uiState.motion,
                storage = uiState.storage,
                appLock = uiState.appLock,
                groups = uiState.groups,
                groupDialog = uiState.groupDialog,
                sourceFolders = uiState.sourceFolders,
                removeFolderUri = uiState.removeFolderUri,
                onAction = onAction,
                onLicensesClick = onLicensesClick,
                appVersion = appVersion,
                snackbarHost = snackbarHost,
            )
        }
    }
}

/** Hub categories, each opening a detail screen. Order is the hub order. */
@Serializable
enum class SettingsCategory {    APPEARANCE,
    READER,
    SHELVES,
    PRIVACY,
    STORAGE,
    ABOUT,
}

/** Type-safe nested destinations: hub list + one detail per category. */
@Serializable
internal data object SettingsHub

@Serializable
internal data class SettingsDetail(val category: SettingsCategory)

@Composable
internal fun SettingsContent(
    theme: ThemePreferences,
    reader: ReaderPreferences,
    motion: MotionStyle,
    storage: StorageUsage?,
    appLock: Boolean = false,
    groups: List<UserCollection> = emptyList(),
    groupDialog: GroupDialog? = null,
    sourceFolders: List<SourceFolder> = emptyList(),
    removeFolderUri: String? = null,
    onAction: (SettingsAction) -> Unit,
    onLicensesClick: () -> Unit = {},
    appVersion: String = "",
    modifier: Modifier = Modifier,
    snackbarHost: SnackbarHostState = remember { SnackbarHostState() },
) {
    val haptics = rememberMoriHaptics()
    val expressiveMotion = LocalExpressiveMotionEnabled.current
    val rtl = LocalLayoutDirection.current != LayoutDirection.Ltr
    // Nested backstack like the reference app: the system predictive-back
    // gesture scrubs the pop transition natively, so no custom back preview,
    // snap, or BackHandler lives here — the NavHost owns hub<->detail.
    // Push mirrors Tomato: full-width slide-in over a 1/4 parallax fade-out;
    // pop reverses it. Calm motion keeps the plain fade.
    val settingsNav = rememberNavController()
    NavHost(
        navController = settingsNav,
        startDestination = SettingsHub,
        enterTransition = { screenEnter(expressiveMotion) },
        exitTransition = { screenExit(expressiveMotion) },
        popEnterTransition = { screenPopEnter(expressiveMotion) },
        popExitTransition = { screenPopExit(expressiveMotion) },
        modifier = modifier,
    ) {
        composable<SettingsHub> {
            SettingsScaffold(
                title = stringResource(R.string.settings_title),
                snackbarHost = snackbarHost,
                contentTag = SettingsTestTags.Content,
            ) {
                SettingsCategory.entries.forEach { entry ->
                    HubRow(
                        category = entry,
                        onClick = {
                            haptics(MoriHaptic.Select)
                            settingsNav.navigate(SettingsDetail(entry))
                        },
                    )
                }
            }
        }
        composable<SettingsDetail> { detailEntry ->
            // Type-safe: an unknown category can no longer arrive here and
            // blank the screen; the route carries the enum itself.
            val selected = detailEntry.toRoute<SettingsDetail>().category
            SettingsScaffold(
                title = categoryTitle(selected),
                navigationBack = { settingsNav.popBackStack() },
                snackbarHost = snackbarHost,
                contentTag = SettingsTestTags.categoryFor(selected),
            ) {
                when (selected) {
                    SettingsCategory.APPEARANCE -> AppearanceSection(
                        theme = theme,
                        motion = motion,
                        onAction = onAction,
                    )
                    SettingsCategory.READER -> ReaderSection(
                        reader = reader,
                        onAction = onAction,
                    )
                    SettingsCategory.SHELVES -> ShelvesSection(
                        groups = groups,
                        groupDialog = groupDialog,
                        onAction = onAction,
                    )
                    SettingsCategory.PRIVACY -> PrivacySection(
                        appLock = appLock,
                        incognito = reader.incognito,
                        onAction = onAction,
                    )
                    SettingsCategory.STORAGE -> StorageSection(
                        storage = storage,
                        folders = sourceFolders,
                        removeFolderUri = removeFolderUri,
                        onAction = onAction,
                    )
                    SettingsCategory.ABOUT -> AboutSection(
                        appVersion = appVersion,
                        onLicensesClick = onLicensesClick,
                    )
                }
            }
        }
    }
}

/**
 * Shared scaffold for the hub and every detail screen: collapsing title,
 * optional back navigation, snackbar, and bottom reserve for the floating
 * navigator.
 *
 * Uses enter-always: the header hides on swipe up but returns on any swipe
 * down. exitUntilCollapsed sticks fully off-screen with this M3 Expressive
 * bar and never comes back.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(
    title: String,
    snackbarHost: SnackbarHostState,
    contentTag: String,
    modifier: Modifier = Modifier,
    navigationBack: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    // Enter-always: the header hides on swipe up but returns on any swipe
    // down. exitUntilCollapsed scrolled fully off this bar and stuck there.
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val contentScroll = rememberScrollState()
    Scaffold(
        topBar = {
            MoriCollapsingTopBar(
                title = title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    if (navigationBack != null) {
                        IconButton(onClick = navigationBack) {
                            Icon(
                                imageVector = MoriIcons.Back,
                                contentDescription = stringResource(R.string.settings_navigate_back),
                            )
                        }
                    }
                },
            )
        },
        // Always attached: gating on canScroll created a one-way trap — once
        // the connection detached, nothing could ever re-expand the bar.
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier.testTag(SettingsTestTags.Snackbar),
            )
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(contentScroll)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    // Reserve the floating navigator's height so the last row
                    // never scrolls underneath it.
                    .padding(bottom = FloatingChromeBottomReserve)
                    .testTag(contentTag),
            ) {
                content()
            }
        }
    }
}

/** One hub row: icon, title, subtitle, chevron. 72dp expressive target. */
@Composable
private fun HubRow(
    category: SettingsCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MoriSettingRow(
        title = categoryTitle(category),
        subtitle = categorySubtitle(category),
        icon = categoryIcon(category),
        onClick = onClick,
        titleStyle = MoriEmphasized.titleMedium.copy(
            fontFamily = LocalAppFonts.current.displaySoft,
            fontWeight = FontWeight.Black,
        ),
        modifier = modifier.testTag(SettingsTestTags.categoryFor(category)),
        trailing = {
            Icon(
                imageVector = MoriIcons.Forward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun categoryTitle(category: SettingsCategory): String = stringResource(
    when (category) {
        SettingsCategory.APPEARANCE -> R.string.settings_card_appearance
        SettingsCategory.READER -> R.string.settings_card_reader
        SettingsCategory.SHELVES -> R.string.settings_card_groups
        SettingsCategory.PRIVACY -> R.string.settings_card_privacy
        SettingsCategory.STORAGE -> R.string.settings_card_storage
        SettingsCategory.ABOUT -> R.string.settings_card_about
    },
)

@Composable
private fun categorySubtitle(category: SettingsCategory): String = stringResource(
    when (category) {
        SettingsCategory.APPEARANCE -> R.string.settings_hub_appearance_sub
        SettingsCategory.READER -> R.string.settings_hub_reader_sub
        SettingsCategory.SHELVES -> R.string.settings_hub_shelves_sub
        SettingsCategory.PRIVACY -> R.string.settings_hub_privacy_sub
        SettingsCategory.STORAGE -> R.string.settings_hub_storage_sub
        SettingsCategory.ABOUT -> R.string.settings_hub_about_sub
    },
)

private fun categoryIcon(category: SettingsCategory): ImageVector = when (category) {
    SettingsCategory.APPEARANCE -> MoriIcons.Palette
    SettingsCategory.READER -> MoriIcons.MenuBook
    SettingsCategory.SHELVES -> MoriIcons.Shelves
    SettingsCategory.PRIVACY -> MoriIcons.PrivacyLock
    SettingsCategory.STORAGE -> MoriIcons.Storage
    SettingsCategory.ABOUT -> MoriIcons.Info
}

@Composable
private fun AppearanceSection(
    theme: ThemePreferences,
    motion: MotionStyle,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
            OptionLabel(stringResource(R.string.settings_theme))
            SegmentedChoiceRow(
                options = listOf(
                    stringResource(R.string.settings_theme_system),
                    stringResource(R.string.settings_theme_light),
                    stringResource(R.string.settings_theme_dark),
                ),
                icons = listOf(MoriIcons.Contrast, MoriIcons.LightMode, MoriIcons.DarkMode),
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
                icon = MoriIcons.Palette,
            )
            MoriSettingSwitch(
                title = stringResource(R.string.settings_haptics_title),
                subtitle = stringResource(R.string.settings_haptics_subtitle),
                checked = theme.hapticsEnabled,
                onCheckedChange = { onAction(SettingsAction.SetHapticsEnabled(it)) },
                icon = MoriIcons.Vibration,
            )
            // True-black only applies in dark mode; hide it under explicit
            // light so the toggle never reads as broken.
            AnimatedVisibility(
                visible = theme.mode != ThemeMode.LIGHT,
                enter = MoriMotion.enter(MoriEnterKind.FADE),
                exit = MoriMotion.exit(MoriEnterKind.FADE),
            ) {
                MoriSettingSwitch(
                    title = stringResource(R.string.settings_amoled_title),
                    subtitle = stringResource(R.string.settings_amoled_subtitle),
                    checked = theme.amoled,
                    onCheckedChange = { onAction(SettingsAction.SetAmoled(it)) },
                    icon = MoriIcons.DarkMode,
                )
            }
            OptionLabel(stringResource(R.string.settings_motion))
            SegmentedChoiceRow(
                options = listOf(
                    stringResource(R.string.settings_motion_expressive),
                    stringResource(R.string.settings_motion_calm),
                ),
                icons = listOf(MoriIcons.Animation, MoriIcons.Spa),
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
}

@Composable
private fun ReaderSection(
    reader: ReaderPreferences,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
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
            OptionLabel(stringResource(R.string.settings_nav_mode_title))
            SegmentedChoiceRow(
                options = listOf(
                    stringResource(R.string.settings_nav_mode_default),
                    stringResource(R.string.settings_nav_mode_l_shape),
                    stringResource(R.string.settings_nav_mode_kindlish),
                    stringResource(R.string.settings_nav_mode_edge),
                    stringResource(R.string.settings_nav_mode_right_and_left),
                    stringResource(R.string.settings_nav_mode_disabled),
                ),
                selectedIndex = reader.navMode.ordinal,
                onSelect = { onAction(SettingsAction.SetReaderNavMode(ReaderNavMode.entries[it])) },
                fillWidth = false,
            )
            OptionLabel(stringResource(R.string.settings_tap_invert_title))
            SegmentedChoiceRow(
                options = listOf(
                    stringResource(R.string.settings_tap_invert_none),
                    stringResource(R.string.settings_tap_invert_horizontal),
                    stringResource(R.string.settings_tap_invert_vertical),
                    stringResource(R.string.settings_tap_invert_both),
                ),
                selectedIndex = reader.invertTaps.ordinal,
                onSelect = { onAction(SettingsAction.SetTapInvertMode(TapInvertMode.entries[it])) },
                fillWidth = false,
            )
            MoriSettingSwitch(
                title = stringResource(R.string.settings_volume_title),
                subtitle = stringResource(R.string.settings_volume_subtitle),
                checked = reader.volumeKeys,
                onCheckedChange = { onAction(SettingsAction.ToggleVolumeKeys) },
            )
            AnimatedVisibility(
                visible = reader.volumeKeys,
                enter = MoriMotion.enter(MoriEnterKind.FADE),
                exit = MoriMotion.exit(MoriEnterKind.FADE),
            ) {
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
                icon = MoriIcons.Crop,
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
            OptionLabel(stringResource(R.string.settings_display_title))
            Text(
                text = stringResource(R.string.settings_display_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MoriSliderRow(
                label = stringResource(R.string.settings_filter_brightness),
                value = reader.displayFilter.brightness,
                valueRange = -1f..1f,
                valueText = formatPercent(reader.displayFilter.brightness),
                onValueChange = { onAction(SettingsAction.SetDisplayBrightness(it)) },
            )
            MoriSliderRow(
                label = stringResource(R.string.settings_filter_night),
                value = reader.displayFilter.nightTint,
                valueRange = 0f..1f,
                valueText = formatPercent(reader.displayFilter.nightTint),
                onValueChange = { onAction(SettingsAction.SetDisplayNightTint(it)) },
            )
            MoriSettingSwitch(
                title = stringResource(R.string.settings_filter_grayscale_title),
                subtitle = stringResource(R.string.settings_filter_grayscale_subtitle),
                checked = reader.displayFilter.grayscale,
                onCheckedChange = { onAction(SettingsAction.ToggleDisplayGrayscale) },
            )
            MoriSettingSwitch(
                title = stringResource(R.string.settings_filter_invert_title),
                subtitle = stringResource(R.string.settings_filter_invert_subtitle),
                checked = reader.displayFilter.invert,
                onCheckedChange = { onAction(SettingsAction.ToggleDisplayInvert) },
            )
            if (!reader.displayFilter.isNeutral) {
                MoriTextButton(onClick = { onAction(SettingsAction.ResetDisplayFilter) }) {
                    Text(stringResource(R.string.settings_filter_reset))
                }
            }
    }
}

@Composable
private fun StorageSection(
    storage: StorageUsage?,
    folders: List<SourceFolder>,
    removeFolderUri: String?,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = rememberMoriHaptics()

    val addFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            onAction(SettingsAction.AddSourceTree(uri))
        }
    }

    var relinkingTargetUri by remember { mutableStateOf<String?>(null) }
    val relinkFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        val oldUri = relinkingTargetUri
        relinkingTargetUri = null
        if (uri != null && oldUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            onAction(SettingsAction.RelinkSource(oldUri = oldUri, newUri = uri))
        }
    }

    val persistedUris = remember(context) {
        runCatching {
            context.contentResolver.persistedUriPermissions
                .filter { it.isReadPermission }
                .map { it.uri.toString() }
                .toSet()
        }.getOrDefault(emptySet())
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (storage != null) {
            // Finance-card hero: everything the app holds on disk. Covers
            // plus the transient read cache (materialized working copies),
            // reported on separate lines so one comic's read cache can
            // never again read as "covers".
            Text(
                text = formatBytes(storage.coversBytes + storage.cacheBytes),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = LocalAppFonts.current.displaySoft,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.settings_storage_summary,
                    storage.comicCount,
                    formatBytes(storage.cacheBytes),
                    formatBytes(storage.coversBytes),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        MoriTonalButton(
            onClick = {
                onAction(SettingsAction.ClearThumbnailCache)
            },
            modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
        ) {
            Text(stringResource(R.string.settings_clear_cache))
        }
        Text(
            text = stringResource(R.string.settings_clear_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        MoriSectionCard(
            title = stringResource(R.string.settings_storage_folders_title),
        ) {
            if (folders.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_storage_folders_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                folders.forEach { folder ->
                    val isReachable = folder.uri in persistedUris
                    val displayName = remember(folder.uri) {
                        resolveFolderDisplayName(context, folder.uri)
                    }

                    if (isReachable) {
                        MoriSettingRow(
                            title = displayName,
                            subtitle = if (folder.bookCount == 1) {
                                stringResource(R.string.settings_folder_books_count_one)
                            } else {
                                stringResource(R.string.settings_folder_books_count, folder.bookCount)
                            },
                            icon = MoriIcons.Folder,
                            modifier = Modifier.testTag(SettingsTestTags.folderRow(folder.uri)),
                            trailing = {
                                IconButton(
                                    onClick = {
                                        haptics(MoriHaptic.Select)
                                        onAction(SettingsAction.AskRemoveSource(folder.uri))
                                    },
                                ) {
                                    Icon(
                                        imageVector = MoriIcons.Delete,
                                        contentDescription = stringResource(R.string.settings_folder_remove_button_desc),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            },
                        )
                    } else {
                        UnreachableFolderRow(
                            displayName = displayName,
                            onRelink = {
                                haptics(MoriHaptic.Select)
                                relinkingTargetUri = folder.uri
                                relinkFolderLauncher.launch(null)
                            },
                            onRemove = {
                                haptics(MoriHaptic.Select)
                                onAction(SettingsAction.AskRemoveSource(folder.uri))
                            },
                            modifier = Modifier.testTag(SettingsTestTags.folderRow(folder.uri)),
                        )
                    }
                }
            }

            MoriPrimaryButton(
                onClick = {
                    addFolderLauncher.launch(null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag(SettingsTestTags.FolderAddButton),
            ) {
                Icon(
                    imageVector = MoriIcons.CreateNewFolder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_folder_add),
                    style = MoriEmphasized.labelLarge,
                )
            }
        }
    }

    if (removeFolderUri != null) {
        MoriAlertDialog(
            onDismissRequest = { onAction(SettingsAction.DismissRemoveSource) },
            icon = MoriIcons.Warning,
            title = {
                Text(stringResource(R.string.settings_folder_remove_title))
            },
            text = {
                Text(stringResource(R.string.settings_folder_remove_message))
            },
            confirmButton = {
                MoriPrimaryButton(
                    onClick = {
                        onAction(SettingsAction.ConfirmRemoveSource)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    haptic = MoriHaptic.Reject,
                    modifier = Modifier.testTag(SettingsTestTags.FolderRemoveConfirm),
                ) {
                    Text(stringResource(R.string.settings_folder_remove_confirm))
                }
            },
            dismissButton = {
                MoriTonalButton(
                    onClick = { onAction(SettingsAction.DismissRemoveSource) },
                ) {
                    Text(stringResource(R.string.settings_folder_remove_cancel))
                }
            },
            modifier = Modifier.testTag(SettingsTestTags.FolderRemoveDialog),
        )
    }
}

@Composable
private fun UnreachableFolderRow(
    displayName: String,
    onRelink: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 72.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
            ) {
                Icon(
                    imageVector = MoriIcons.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = displayName,
                    style = MoriEmphasized.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.settings_folder_unreachable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            MoriTonalButton(
                onClick = onRelink,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.heightIn(min = 36.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_folder_relink),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = MoriIcons.Delete,
                    contentDescription = stringResource(R.string.settings_folder_remove_button_desc),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun resolveFolderDisplayName(context: Context, uriString: String): String {
    val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return uriString
    val docName = runCatching { DocumentFile.fromTreeUri(context, uri)?.name }.getOrNull()
    if (!docName.isNullOrBlank()) return docName
    val decoded = Uri.decode(uriString)
    val candidate = decoded.substringAfterLast(':').substringAfterLast('/')
    return if (candidate.isNotBlank()) candidate else uriString
}

@Composable
private fun ShelvesSection(
    groups: List<UserCollection>,
    groupDialog: GroupDialog?,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberMoriHaptics()
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
            if (groups.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_groups_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            groups.forEach { group ->
                GroupRow(
                    group = group,
                    onRename = { onAction(SettingsAction.OpenRenameGroup(group.id, group.name)) },
                    onDelete = { onAction(SettingsAction.OpenDeleteGroup(group.id, group.name)) },
                )
            }
            MoriOutlinedButton(
                onClick = {
                    onAction(SettingsAction.OpenCreateGroup)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SettingsTestTags.GroupCreateButton),
            ) {
                Text(stringResource(R.string.settings_groups_create))
            }

        GroupDialogHost(
            dialog = groupDialog,
            onAction = onAction,
        )
    }
}

@Composable
private fun PrivacySection(
    appLock: Boolean,
    incognito: Boolean,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
            MoriSettingSwitch(
                title = stringResource(R.string.settings_applock_title),
                subtitle = stringResource(R.string.settings_applock_subtitle),
                checked = appLock,
                onCheckedChange = { onAction(SettingsAction.ToggleAppLock) },
                icon = MoriIcons.PrivacyLock,
            )
            MoriSettingSwitch(
                title = stringResource(R.string.settings_incognito_title),
                subtitle = stringResource(R.string.settings_incognito_subtitle),
                checked = incognito,
                onCheckedChange = { onAction(SettingsAction.ToggleIncognito) },
                icon = MoriIcons.Incognito,
            )
    }
}

@Composable
private fun AboutSection(
    appVersion: String,
    onLicensesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberMoriHaptics()
    val context = LocalContext.current
    var privacyOpen by remember { mutableStateOf(false) }
    // Staggered entrance: hero lands first, link rows follow one beat
    // apart. Calm motion reveals everything at once. Idle-waiting tests
    // see the settled tree either way.
    val expressive = LocalExpressiveMotionEnabled.current
    var revealed by remember { mutableIntStateOf(if (expressive) 0 else AboutRevealSteps) }
    LaunchedEffect(expressive) {
        if (!expressive) {
            revealed = AboutRevealSteps
            return@LaunchedEffect
        }
        repeat(AboutRevealSteps) {
            kotlinx.coroutines.delay(45)
            revealed++
        }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        // Unified Brand & Creator Hero element
        AnimatedVisibility(
            visible = revealed > 0,
            enter = MoriMotion.enter(MoriEnterKind.RISE),
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                        ) {
                            Icon(
                                imageVector = MoriIcons.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_about_app),
                                    style = MoriEmphasized.headlineMedium.copy(
                                        fontFamily = LocalAppFonts.current.displaySoft,
                                        fontWeight = FontWeight.Black,
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
                                        Icon(
                                            imageVector = MoriIcons.Sparkle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp),
                                        )
                                        Text(
                                            text = stringResource(R.string.settings_about_version, appVersion),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = LocalAppFonts.current.topBarTitle,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "A tranquil, high-craft comic reader",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Creator Credit: Spacious, minimal attribution with "by", avatar, developer name and handle
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClickLabel = stringResource(R.string.settings_about_handle),
                                onClick = {
                                    haptics(MoriHaptic.Select)
                                    context.openUrl(AboutLinks.DEVELOPER)
                                },
                                role = Role.Button,
                            ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = "by",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Image(
                                painter = painterResource(R.drawable.dev_avatar),
                                contentDescription = stringResource(R.string.settings_about_developer),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_about_developer),
                                    style = MoriEmphasized.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                                Text(
                                    text = stringResource(R.string.settings_about_handle),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = LocalAppFonts.current.topBarTitle,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            Icon(
                                painter = painterResource(R.drawable.ic_github_mark),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        RevealRow(visible = revealed > 1) {
            MoriSettingRow(
                title = stringResource(R.string.settings_about_github),
                subtitle = stringResource(R.string.settings_about_github_subtitle),
                icon = MoriIcons.Code,
                minHeight = 56.dp,
                onClick = {
                    haptics(MoriHaptic.Select)
                    context.openUrl(AboutLinks.REPOSITORY)
                },
                trailing = {
                    Icon(
                        imageVector = MoriIcons.Forward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }

        RevealRow(visible = revealed > 2) {
            MoriSettingRow(
                title = stringResource(R.string.settings_about_issue),
                subtitle = stringResource(R.string.settings_about_issue_subtitle),
                icon = MoriIcons.BugReport,
                minHeight = 56.dp,
                onClick = {
                    haptics(MoriHaptic.Select)
                    context.openUrl(AboutLinks.ISSUES)
                },
                trailing = {
                    Icon(
                        imageVector = MoriIcons.Forward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }

        RevealRow(visible = revealed > 3) {
            MoriSettingRow(
                title = stringResource(R.string.settings_about_changelog),
                subtitle = stringResource(R.string.settings_about_changelog_subtitle),
                icon = MoriIcons.NewReleases,
                minHeight = 56.dp,
                onClick = {
                    haptics(MoriHaptic.Select)
                    context.openUrl(AboutLinks.RELEASES)
                },
                trailing = {
                    Icon(
                        imageVector = MoriIcons.Forward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }

        RevealRow(visible = revealed > 4) {
            MoriSettingRow(
                title = stringResource(R.string.settings_about_privacy),
                subtitle = stringResource(R.string.settings_about_privacy_subtitle),
                icon = MoriIcons.PrivacyLock,
                minHeight = 56.dp,
                onClick = {
                    haptics(MoriHaptic.Select)
                    privacyOpen = true
                },
                trailing = {
                    Icon(
                        imageVector = MoriIcons.Forward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }

        RevealRow(visible = revealed > 5) {
            MoriSettingRow(
                title = stringResource(R.string.settings_about_licenses),
                subtitle = stringResource(R.string.settings_about_licenses_subtitle),
                icon = MoriIcons.Info,
                minHeight = 56.dp,
                onClick = {
                    haptics(MoriHaptic.Select)
                    onLicensesClick()
                },
                modifier = Modifier.testTag(SettingsTestTags.LicensesRow),
                trailing = {
                    Icon(
                        imageVector = MoriIcons.Forward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
    if (privacyOpen) {
        MoriAlertDialog(
            onDismissRequest = { privacyOpen = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_about_privacy),
                    style = MoriEmphasized.headlineSmall.copy(
                        fontFamily = LocalAppFonts.current.displaySoft,
                        fontWeight = FontWeight.Black,
                    ),
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_about_privacy_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                MoriTonalButton(onClick = { privacyOpen = false }) {
                    Text(stringResource(R.string.settings_dialog_understood))
                }
            },
        )
    }
}

/** External destinations for the About rows. */
private object AboutLinks {
    const val REPOSITORY = "https://github.com/ego1s1/mori"
    const val ISSUES = "https://github.com/ego1s1/mori/issues"
    const val RELEASES = "https://github.com/ego1s1/mori/releases"
    const val DEVELOPER = "https://github.com/ego1s1"
}

private fun android.content.Context.openUrl(url: String) {
    runCatching {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse(url),
        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}

/** Staggered reveal wrapper for About rows: rise on open, nothing on close. */
@Composable
private fun RevealRow(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = MoriMotion.enter(MoriEnterKind.RISE),
        modifier = modifier,
    ) {
        content()
    }
}

/** Hero, dev card, and five link rows revealed one beat apart. */
private const val AboutRevealSteps = 6

@Composable
private fun OptionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MoriEmphasized.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * Single-choice pill group: connected morphing toggle buttons with optional
 * icons, Flex-emphasized selection. Selection semantics keep the tests on
 * tags, not pixels.
 */
@Composable
private fun SegmentedChoiceRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector?>? = null,
    fillWidth: Boolean = true,
) {
    MoriChoiceGroup(
        options = options.mapIndexed { index, label ->
            MoriChoiceOption(label = label, icon = icons?.getOrNull(index))
        },
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        modifier = modifier,
        testTagFor = SettingsTestTags::segmentFor,
        fillWidth = fillWidth,
    )
}

/**
 * One shelf row: name plus book count, with rename and delete actions.
 * Books are managed from the library and detail screens; Settings owns
 * the shelf list itself.
 */
@Composable
private fun GroupRow(
    group: UserCollection,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MoriSettingRow(
        title = group.name,
        subtitle = stringResource(R.string.settings_groups_count, group.bookCount),
        icon = MoriIcons.Shelves,
        modifier = modifier.testTag(SettingsTestTags.groupRow(group.id)),
        trailing = {
            val haptics = rememberMoriHaptics()
            IconButton(onClick = {
                haptics(MoriHaptic.Select)
                onRename()
            }) {
                Icon(
                    imageVector = MoriIcons.Edit,
                    contentDescription = stringResource(R.string.settings_group_rename_title),
                )
            }
            IconButton(onClick = {
                haptics(MoriHaptic.Select)
                onDelete()
            }) {
                Icon(
                    imageVector = MoriIcons.Delete,
                    contentDescription = stringResource(R.string.settings_group_delete_title),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

/** Create/rename/delete dialogs for shelves. Null renders nothing. */
@Composable
private fun GroupDialogHost(
    dialog: GroupDialog?,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Plain when, not AnimatedVisibility: AlertDialog owns a window, so a
    // hidden-but-composed dialog still takes focus and swallows dismiss.
    when (dialog) {
            null -> Unit
            GroupDialog.Create -> GroupNameDialog(
            title = stringResource(R.string.settings_group_create_title),
            label = stringResource(R.string.settings_group_create_label),
            confirmText = stringResource(R.string.settings_group_create_confirm),
            initial = "",
            onConfirm = { onAction(SettingsAction.CreateGroup(it)) },
            onDismiss = { onAction(SettingsAction.CloseGroupDialog) },
            modifier = modifier,
        )
        is GroupDialog.Rename -> GroupNameDialog(
            title = stringResource(R.string.settings_group_rename_title),
            label = stringResource(R.string.settings_group_rename_label),
            confirmText = stringResource(R.string.settings_group_rename_confirm),
            initial = dialog.name,
            onConfirm = { onAction(SettingsAction.RenameGroup(dialog.groupId, it)) },
            onDismiss = { onAction(SettingsAction.CloseGroupDialog) },
            modifier = modifier,
        )
        is GroupDialog.Delete -> MoriConfirmDialog(
            title = stringResource(R.string.settings_group_delete_title),
            message = stringResource(R.string.settings_group_delete_body, dialog.name),
            confirmLabel = stringResource(R.string.settings_group_delete_confirm),
            onConfirm = { onAction(SettingsAction.ConfirmDeleteGroup(dialog.groupId)) },
            dismissLabel = stringResource(R.string.settings_dialog_cancel),
            onDismiss = { onAction(SettingsAction.CloseGroupDialog) },
            destructive = true,
            confirmTestTag = SettingsTestTags.GroupDeleteConfirm,
            modifier = modifier.testTag(SettingsTestTags.GroupDialog),
        )
    }
}

/** Shared text-field dialog for shelf create and rename. */
@Composable
private fun GroupNameDialog(
    title: String,
    label: String,
    confirmText: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember(initial) { mutableStateOf(initial) }
    MoriAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MoriEmphasized.headlineSmall.copy(
                    fontFamily = LocalAppFonts.current.displaySoft,
                    fontWeight = FontWeight.Black,
                ),
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SettingsTestTags.GroupNameField),
            )
        },
        confirmButton = {
            MoriPrimaryButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
                haptic = MoriHaptic.Confirm,
                modifier = Modifier.testTag(SettingsTestTags.GroupConfirm),
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            MoriTonalButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_dialog_cancel))
            }
        },
        modifier = modifier.testTag(SettingsTestTags.GroupDialog),
    )
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
