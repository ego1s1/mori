package com.mori.feature.settings.impl


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.mori.core.designsystem.MoriSettingRow
import com.mori.core.designsystem.enter
import com.mori.core.designsystem.exit
import com.mori.core.designsystem.MoriSettingSwitch
import com.mori.core.designsystem.MoriSliderRow
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.SchemePickerRow
import com.mori.core.designsystem.ThemePreviews
import com.mori.core.designsystem.rememberMoriHaptics
import com.mori.core.common.formatBytes
import com.mori.core.model.PageFit
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
                onAction = onAction,
                onLicensesClick = onLicensesClick,
                appVersion = appVersion,
                snackbarHost = snackbarHost,
            )
        }
    }
}

/** Hub categories, each opening a detail screen. Order is the hub order. */
enum class SettingsCategory {
    APPEARANCE,
    READER,
    SHELVES,
    PRIVACY,
    STORAGE,
    ABOUT,
}

private const val SettingsHubRoute = "hub"
private const val SettingsDetailRoute = "detail/{name}"
private const val SettingsDetailArg = "name"

@Composable
internal fun SettingsContent(
    theme: ThemePreferences,
    reader: ReaderPreferences,
    motion: MotionStyle,
    storage: StorageUsage?,
    appLock: Boolean = false,
    groups: List<UserCollection> = emptyList(),
    groupDialog: GroupDialog? = null,
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
        startDestination = SettingsHubRoute,
        enterTransition = {
            if (!expressiveMotion) {
                fadeIn(animationSpec = MoriMotion.calmFade())
            } else {
                slideInHorizontally(animationSpec = MoriMotion.tabEnterSpec()) {
                    if (rtl) -it else it
                }
            }
        },
        exitTransition = {
            if (!expressiveMotion) {
                fadeOut(animationSpec = MoriMotion.calmFade())
            } else {
                fadeOut(animationSpec = MoriMotion.tabExitSpec()) +
                    slideOutHorizontally(animationSpec = MoriMotion.tabExitSpec()) {
                        if (rtl) it / 4 else -it / 4
                    }
            }
        },
        popEnterTransition = {
            if (!expressiveMotion) {
                fadeIn(animationSpec = MoriMotion.calmFade())
            } else {
                fadeIn(animationSpec = MoriMotion.tabEnterSpec()) +
                    slideInHorizontally(animationSpec = MoriMotion.tabEnterSpec()) {
                        if (rtl) it / 4 else -it / 4
                    }
            }
        },
        popExitTransition = {
            if (!expressiveMotion) {
                fadeOut(animationSpec = MoriMotion.calmFade())
            } else {
                slideOutHorizontally(animationSpec = MoriMotion.tabExitSpec()) {
                    if (rtl) -it else it
                }
            }
        },
        modifier = modifier,
    ) {
        composable(SettingsHubRoute) {
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
                            settingsNav.navigate("detail/${entry.name}")
                        },
                    )
                }
            }
        }
        composable(
            route = SettingsDetailRoute,
            arguments = listOf(navArgument(SettingsDetailArg) { type = NavType.StringType }),
        ) { detailEntry ->
            val selected = detailEntry.arguments?.getString(SettingsDetailArg)?.let { name ->
                runCatching { SettingsCategory.valueOf(name) }.getOrNull()
            } ?: return@composable
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
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 72.dp)
            .testTag(SettingsTestTags.categoryFor(category)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Icon(
                    imageVector = categoryIcon(category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = categoryTitle(category),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = categorySubtitle(category),
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
                valueText = percentText(reader.displayFilter.brightness),
                onValueChange = { onAction(SettingsAction.SetDisplayBrightness(it)) },
            )
            MoriSliderRow(
                label = stringResource(R.string.settings_filter_night),
                value = reader.displayFilter.nightTint,
                valueRange = 0f..1f,
                valueText = percentText(reader.displayFilter.nightTint),
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
                TextButton(onClick = { onAction(SettingsAction.ResetDisplayFilter) }) {
                    Text(stringResource(R.string.settings_filter_reset))
                }
            }
    }
}

@Composable
private fun StorageSection(
    storage: StorageUsage?,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                        fontFamily = LocalAppFonts.current.topBarTitle,
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
            FilledTonalButton(
                onClick = { onAction(SettingsAction.ClearThumbnailCache) },
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.settings_clear_cache))
            }
            Text(
                text = stringResource(R.string.settings_clear_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
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
            OutlinedButton(
                onClick = {
                    haptics(MoriHaptic.Select)
                    onAction(SettingsAction.OpenCreateGroup)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 48.dp)
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
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
            // Cardfolio hero: app-mark plus Flex headline name, version, and
            // credit. Cookies stay on heroes only, never on rows.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Icon(
                        imageVector = MoriIcons.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_about_app),
                        style = MoriEmphasized.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_about_version, appVersion),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = LocalAppFonts.current.topBarTitle,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.settings_about_credit),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable(
                            onClickLabel = stringResource(R.string.settings_about_credit),
                            onClick = {
                                haptics(MoriHaptic.Select)
                                context.openUrl(AboutLinks.DEVELOPER)
                            },
                            role = Role.Button,
                        ),
                    )
                }
            }
            MoriSettingRow(
                title = stringResource(R.string.settings_about_github),
                subtitle = stringResource(R.string.settings_about_github_subtitle),
                icon = MoriIcons.Code,
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
            MoriSettingRow(
                title = stringResource(R.string.settings_about_issue),
                subtitle = stringResource(R.string.settings_about_issue_subtitle),
                icon = MoriIcons.BugReport,
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
            MoriSettingRow(
                title = stringResource(R.string.settings_about_changelog),
                subtitle = stringResource(R.string.settings_about_changelog_subtitle),
                icon = MoriIcons.NewReleases,
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
            MoriSettingRow(
                title = stringResource(R.string.settings_about_privacy),
                subtitle = stringResource(R.string.settings_about_privacy_subtitle),
                icon = MoriIcons.PrivacyLock,
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
            MoriSettingRow(
                title = stringResource(R.string.settings_about_licenses),
                subtitle = stringResource(R.string.settings_about_licenses_subtitle),
                icon = MoriIcons.Info,
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
            PlaceholderRow(
                title = stringResource(R.string.settings_soon_sync),
                subtitle = stringResource(R.string.settings_soon_sync_subtitle),
            )
    }
    if (privacyOpen) {
        AlertDialog(
            onDismissRequest = { privacyOpen = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_about_privacy),
                    style = MaterialTheme.typography.headlineSmall,
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
                FilledTonalButton(onClick = { privacyOpen = false }) {
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

/** Percent readout for -1..1 / 0..1 filter sliders, e.g. "-40%", "75%". */
private fun percentText(value: Float): String {
    val percent = (value * 100).toInt()
    return "$percent%"
}

@Composable
private fun OptionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MoriEmphasized.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
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
) {
    MoriChoiceGroup(
        options = options.mapIndexed { index, label ->
            MoriChoiceOption(label = label, icon = icons?.getOrNull(index))
        },
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        modifier = modifier,
        testTagFor = SettingsTestTags::segmentFor,
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
    AnimatedVisibility(
        visible = dialog != null,
        enter = MoriMotion.enter(MoriEnterKind.FADE),
        exit = MoriMotion.exit(MoriEnterKind.FADE),
    ) {
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
        is GroupDialog.Delete -> AlertDialog(
            onDismissRequest = { onAction(SettingsAction.CloseGroupDialog) },
            title = {
                Text(
                    text = stringResource(R.string.settings_group_delete_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_group_delete_body, dialog.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                val haptics = rememberMoriHaptics()
                Button(
                    onClick = {
                        haptics(MoriHaptic.Confirm)
                        onAction(SettingsAction.ConfirmDeleteGroup(dialog.groupId))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.testTag(SettingsTestTags.GroupDeleteConfirm),
                ) {
                    Text(text = stringResource(R.string.settings_group_delete_confirm))
                }
            },
            dismissButton = {
                FilledTonalButton(onClick = { onAction(SettingsAction.CloseGroupDialog) }) {
                    Text(stringResource(R.string.settings_dialog_cancel))
                }
            },
            modifier = modifier.testTag(SettingsTestTags.GroupDialog),
        )
        }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
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
            val haptics = rememberMoriHaptics()
            Button(
                onClick = {
                    haptics(MoriHaptic.Confirm)
                    onConfirm(name)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag(SettingsTestTags.GroupConfirm),
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_dialog_cancel))
            }
        },
        modifier = modifier.testTag(SettingsTestTags.GroupDialog),
    )
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
                color = MaterialTheme.colorScheme.onSurface,
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
