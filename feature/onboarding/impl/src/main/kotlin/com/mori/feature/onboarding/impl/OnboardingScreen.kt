package com.mori.feature.onboarding.impl

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.LocalExpressiveMotionEnabled
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriEnterKind
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriMotion
import com.mori.core.designsystem.SchemePickerRow
import com.mori.core.designsystem.enter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.mori.core.designsystem.previewColor
import com.mori.core.model.ColorSchemeChoice
import com.mori.core.model.StorageLocation
import com.mori.core.model.ThemeMode
import com.mori.core.model.ThemePreferences
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.ThemePreviews
import com.mori.core.model.ImportReport
import kotlinx.coroutines.delay

@Composable
internal fun OnboardingRoute(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.onAction(OnboardingAction.FolderSelected(uri))
        }
    }
    val filesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onAction(OnboardingAction.FilesSelected(uris))
        }
    }
    OnboardingScreen(
        uiState = uiState,
        onPickFolder = { folderLauncher.launch(null) },
        onPickFiles = { filesLauncher.launch(arrayOf("*/*")) },
        onAction = viewModel::onAction,
        onOnboardingComplete = onOnboardingComplete,
        modifier = modifier,
    )
}

@Composable
// The step transition keys on the step alone while rendering live state,
// so progress ticks recompose in place without restarting the animation.
@Suppress("UnusedContentLambdaTargetStateParameter")
internal fun OnboardingScreen(
    uiState: OnboardingUiState,
    onPickFolder: () -> Unit,
    onPickFiles: () -> Unit,
    onAction: (OnboardingAction) -> Unit,
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        // Step changes fade through so the wizard never hard-cuts. Keyed on
        // the step alone: progress ticks inside Importing recompose in place
        // without restarting the transition.
        val stepKey = when (uiState) {
            OnboardingUiState.Welcome -> 0
            is OnboardingUiState.Storage -> 1
            is OnboardingUiState.Appearance -> 2
            is OnboardingUiState.Import -> 3
            is OnboardingUiState.Importing -> 4
            is OnboardingUiState.Done -> 5
        }
        AnimatedContent(
            targetState = stepKey,
            transitionSpec = {
                (fadeIn(
                    animationSpec = tween(
                        STEP_FADE_MS,
                        easing = MoriMotion.EmphasizedDecelerate,
                    ),
                ) + scaleIn(
                    animationSpec = tween(
                        STEP_FADE_MS,
                        easing = MoriMotion.EmphasizedDecelerate,
                    ),
                    initialScale = STEP_SCALE_FROM,
                )) togetherWith fadeOut(
                    animationSpec = tween(
                        STEP_FADE_MS,
                        easing = MoriMotion.EmphasizedAccelerate,
                    ),
                )
            },
            label = "onboardingStep",
        ) { _ ->
        // Renders the live state (not the step key) so progress ticks
        // recompose in place without restarting the transition.
        when (uiState) {
            OnboardingUiState.Welcome -> WelcomeContent(
                onGetStarted = { onAction(OnboardingAction.GetStarted) },
                onSkip = {
                    onAction(OnboardingAction.Skip)
                    onOnboardingComplete()
                },
            )
            is OnboardingUiState.Storage -> WizardStep(
                modifier = Modifier.testTag(OnboardingTestTags.StorageStep),
                stepIndex = 0,
                totalSteps = 3,
                title = stringResource(R.string.onboarding_storage_title),
                body = stringResource(R.string.onboarding_storage_body),
                onBack = { onAction(OnboardingAction.BackStep) },
                onSkip = {
                    onAction(OnboardingAction.Skip)
                    onOnboardingComplete()
                },
                onContinue = { onAction(OnboardingAction.ContinueStep) },
                continueLabel = stringResource(R.string.onboarding_continue),
                continueCaption = stringResource(R.string.onboarding_storage_caption),
            ) {
                StorageOptions(
                    location = uiState.location,
                    onSelectApp = { onAction(OnboardingAction.SelectStorage(StorageLocation.APP)) },
                    onSelectCustom = { onAction(OnboardingAction.SelectStorage(StorageLocation.CUSTOM)) },
                )
            }
            is OnboardingUiState.Appearance -> WizardStep(
                modifier = Modifier.testTag(OnboardingTestTags.AppearanceStep),
                stepIndex = 1,
                totalSteps = 3,
                title = stringResource(R.string.onboarding_appearance_title),
                body = stringResource(R.string.onboarding_appearance_body),
                onBack = { onAction(OnboardingAction.BackStep) },
                onSkip = {
                    onAction(OnboardingAction.Skip)
                    onOnboardingComplete()
                },
                onContinue = { onAction(OnboardingAction.ContinueStep) },
                continueLabel = stringResource(R.string.onboarding_continue),
                continueCaption = stringResource(R.string.onboarding_appearance_caption),
            ) {
                AppearanceOptions(theme = uiState.theme, onAction = onAction)
            }
            is OnboardingUiState.Import -> WizardStep(
                modifier = Modifier.testTag(OnboardingTestTags.ImportStep),
                stepIndex = 2,
                totalSteps = 3,
                title = stringResource(R.string.onboarding_import_title),
                body = if (uiState.link) {
                    stringResource(R.string.onboarding_import_body_link)
                } else {
                    stringResource(R.string.onboarding_import_body_default)
                },
                onBack = { onAction(OnboardingAction.BackStep) },
                onSkip = {
                    onAction(OnboardingAction.Skip)
                    onOnboardingComplete()
                },
                continueLabel = "",
                continueCaption = "",
            ) {
                ImportOptions(
                    customOnly = uiState.location == StorageLocation.CUSTOM,
                    link = uiState.link,
                    onSetLink = { onAction(OnboardingAction.SetLinkMode(it)) },
                    onPickFolder = onPickFolder,
                    onPickFiles = onPickFiles,
                )
            }
            is OnboardingUiState.Importing -> ImportingContent(
                done = uiState.done,
                total = uiState.total,
                link = uiState.link,
                onCancel = { onAction(OnboardingAction.CancelImport) },
            )
            is OnboardingUiState.Done -> DoneContent(
                report = uiState.report,
                onImportMore = { onAction(OnboardingAction.ImportMore) },
                onFinish = {
                    onAction(OnboardingAction.Finish)
                    onOnboardingComplete()
                },
            )
        }
        }
    }
}

/** Step-change fade-through; slightly unhurried so the wizard feels calm. */
private const val STEP_FADE_MS = 250
private const val STEP_SCALE_FROM = 0.98f

@Composable
private fun WelcomeContent(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Staggered entrance: hero, headline, body, actions cascade in.
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        repeat(WELCOME_STEPS) {
            delay(110)
            step++
        }
    }
    fun visibleAt(index: Int) = step > index
    Column(modifier = modifier.fillMaxSize()) {
        // Status-bar clearance: the brand/Skip row draws edge-to-edge and
        // must clear the clock and status icons.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_brand),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 16.dp),
            )
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            AnimatedVisibility(
                visible = visibleAt(0),
                enter = MoriMotion.enter(MoriEnterKind.FAB),
                exit = fadeOut(animationSpec = MoriMotion.calmFade()),
            ) {
                // Playful tonal collage (banner-style): accent tiles peek from
                // behind the morphing hero.
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .size(88.dp)
                            .offset((-58).dp, 44.dp)
                            .graphicsLayer { rotationZ = -14f },
                    ) {}
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier
                            .size(56.dp)
                            .offset(66.dp, (-52).dp),
                    ) {}
                    MorphingHero(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        icon = MoriIcons.MenuBook,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            AnimatedVisibility(
                visible = visibleAt(1),
                enter = MoriMotion.enter(MoriEnterKind.RISE),
                exit = fadeOut(animationSpec = MoriMotion.calmFade()),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_eyebrow),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center,
                )
                // Breathing room before the display headline: tight tracking
                // above huge type otherwise reads as a collision.
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = buildAnnotatedString {
                        append(stringResource(R.string.onboarding_hero_prefix))
                        withStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            append(stringResource(R.string.onboarding_hero_accent))
                        }
                        append(stringResource(R.string.onboarding_hero_suffix))
                    },
                    style = MoriEmphasized.displaySmall,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedVisibility(
                visible = visibleAt(2),
                enter = MoriMotion.enter(MoriEnterKind.RISE),
                exit = fadeOut(animationSpec = MoriMotion.calmFade()),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_intro_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            AnimatedVisibility(
                visible = visibleAt(3),
                enter = MoriMotion.enter(MoriEnterKind.RISE),
                exit = fadeOut(animationSpec = MoriMotion.calmFade()),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onGetStarted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(OnboardingTestTags.GetStarted),
                    ) {
                        Text(stringResource(R.string.onboarding_get_started))
                    }
                    Text(
                        text = stringResource(R.string.onboarding_get_started_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                    )
                }
            }
        }
    }
}

private const val WELCOME_STEPS = 4

/**
 * Wizard step shell (reference style): back / STEP x OF n / Skip header,
 * segmented progress, display title + body, grouped option card, bottom CTA
 * zone on a tonal container with pill button + caption.
 */
@Composable
private fun WizardStep(
    stepIndex: Int,
    totalSteps: Int,
    title: String,
    body: String,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: (() -> Unit)? = null,
    continueLabel: String,
    continueCaption: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Same status-bar clearance as the welcome header.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = MoriIcons.Back, contentDescription = stringResource(R.string.onboarding_back))
            }
            Text(
                text = stringResource(R.string.onboarding_step, stepIndex + 1, totalSteps),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            repeat(totalSteps) { index ->
                val fraction = when {
                    index < stepIndex -> 1f
                    index == stepIndex -> 0.6f
                    else -> 0f
                }
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MoriEmphasized.headlineMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    content()
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (onContinue != null) {
            // Muted bottom panel: the CTA button carries the emphasis, the
            // panel itself stays a quiet tonal container.
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                ) {
                    Button(
                        onClick = { onContinue?.invoke() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag(OnboardingTestTags.StepContinue),
                    ) {
                        Text(continueLabel)
                    }
                    if (continueCaption.isNotBlank()) {
                        Text(
                            text = continueCaption,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Storage step: a pure choice. The folder itself is picked later, once. */
@Composable
private fun StorageOptions(
    location: StorageLocation,
    onSelectApp: () -> Unit,
    onSelectCustom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        OptionRow(
            selected = location == StorageLocation.APP,
            icon = MoriIcons.FolderOpen,
            title = stringResource(R.string.onboarding_storage_app),
            subtitle = stringResource(R.string.onboarding_storage_app_subtitle),
            onClick = onSelectApp,
        )
        OptionRow(
            selected = location == StorageLocation.CUSTOM,
            icon = MoriIcons.MenuBook,
            title = stringResource(R.string.onboarding_storage_custom),
            subtitle = stringResource(R.string.onboarding_storage_custom_subtitle),
            onClick = onSelectCustom,
        )
    }
}

@Composable
private fun OptionRow(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = border,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                role = Role.RadioButton,
            )
            .semantics { this.selected = selected },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}

/** Appearance step: theme mode, color scheme swatches, AMOLED. */
@Composable
private fun AppearanceOptions(
    theme: ThemePreferences,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.onboarding_theme),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = theme.mode == ThemeMode.SYSTEM,
                onClick = { onAction(OnboardingAction.SetThemeMode(ThemeMode.SYSTEM)) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                label = { Text(stringResource(R.string.onboarding_theme_system)) },
            )
            SegmentedButton(
                selected = theme.mode == ThemeMode.LIGHT,
                onClick = { onAction(OnboardingAction.SetThemeMode(ThemeMode.LIGHT)) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                label = { Text(stringResource(R.string.onboarding_theme_light)) },
            )
            SegmentedButton(
                selected = theme.mode == ThemeMode.DARK,
                onClick = { onAction(OnboardingAction.SetThemeMode(ThemeMode.DARK)) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                label = { Text(stringResource(R.string.onboarding_theme_dark)) },
            )
        }
        Text(
            text = stringResource(R.string.onboarding_colors),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SchemePickerRow(
            theme = theme,
            onDynamic = { onAction(OnboardingAction.SetDynamicColor(true)) },
            onScheme = { onAction(OnboardingAction.SetColorScheme(it)) },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = theme.amoled,
                    onValueChange = { onAction(OnboardingAction.SetAmoled(it)) },
                    role = Role.Switch,
                ),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.onboarding_amoled_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.onboarding_amoled_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = theme.amoled, onCheckedChange = null)
        }
    }
}

/** Import step: folder + files pickers, plus the copy/link choice for folders. */
@Composable
private fun ImportOptions(
    customOnly: Boolean,
    link: Boolean,
    onSetLink: (Boolean) -> Unit,
    onPickFolder: () -> Unit,
    onPickFiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.onboarding_import_mode_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OptionRow(
            selected = !link,
            icon = MoriIcons.FolderOpen,
            title = stringResource(R.string.onboarding_import_copy),
            subtitle = stringResource(R.string.onboarding_import_copy_subtitle),
            onClick = { onSetLink(false) },
            modifier = Modifier.testTag(OnboardingTestTags.CopyMode),
        )
        OptionRow(
            selected = link,
            icon = MoriIcons.Link,
            title = stringResource(R.string.onboarding_import_link),
            subtitle = stringResource(R.string.onboarding_import_link_subtitle),
            onClick = { onSetLink(true) },
            modifier = Modifier.testTag(OnboardingTestTags.LinkMode),
        )
        Button(
            onClick = onPickFolder,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(OnboardingTestTags.PickFolder),
        ) {
            Text(
                if (customOnly) {
                    stringResource(R.string.onboarding_pick_custom_folder)
                } else {
                    stringResource(R.string.onboarding_pick_folder)
                },
            )
        }
        if (!customOnly) {
            OutlinedButton(
                onClick = onPickFiles,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(OnboardingTestTags.PickFiles),
            ) {
                Text(stringResource(R.string.onboarding_pick_files))
            }
        }
        Text(
            text = stringResource(R.string.onboarding_import_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Hero tile whose corners morph from rounded square toward circle on a bouncy
 * spring shortly after appearing (M3 Expressive shape language).
 */
@Composable
private fun MorphingHero(
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    var morphed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(HERO_MORPH_DELAY_MS)
        morphed = true
    }
    val corner by animateDpAsState(
        targetValue = if (morphed) 64.dp else 28.dp,
        animationSpec = MoriMotion.heroSpring(),
        label = "heroMorph",
    )
    Surface(
        shape = RoundedCornerShape(corner),
        color = containerColor,
        modifier = modifier.size(128.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(64.dp),
            )
        }
    }
}

private const val HERO_MORPH_DELAY_MS = 350L

@Composable
private fun ImportingContent(
    done: Int,
    total: Int,
    link: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        AnimatedContent(
            targetState = total <= 0,
            transitionSpec = {
                fadeIn(animationSpec = MoriMotion.defaultEffectsSpec()) togetherWith
                    fadeOut(animationSpec = MoriMotion.calmFade())
            },
            label = "importPhase",
        ) { scanning ->
            if (scanning) {
                CircularProgressIndicator(modifier = Modifier.testTag(OnboardingTestTags.Progress))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.onboarding_scanning),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
            Text(
                text = if (link) {
                    stringResource(R.string.onboarding_adding, done, total)
                } else {
                    stringResource(R.string.onboarding_copying, done, total)
                },
                style = MoriEmphasized.headlineSmall,
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Spring-smoothed bar: file copies land in bursts, the indicator
            // glides instead of jumping.
            val rawProgress = done.toFloat() / total
            val smoothProgress by animateFloatAsState(
                targetValue = rawProgress,
                animationSpec = MoriMotion.heroSpring(),
                label = "importProgress",
            )
            LinearProgressIndicator(
                progress = { smoothProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(OnboardingTestTags.Progress),
            )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onCancel) {
            Text(stringResource(R.string.onboarding_cancel))
        }
    }
}

/** Summary beat before Done auto-advances home. */
private const val DONE_BEAT_MS = 1200L

/**
 * Transient finish beat: the import summary scales in, then the wizard hands
 * off to the library — no blocking Start Reading page. The import-more link
 * stands the handoff down and returns to the Import step.
 */
@Composable
private fun DoneContent(
    report: ImportReport,
    onImportMore: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Guarded so the timer and the link can't finish twice.
    var gone by remember { mutableStateOf(false) }
    fun finishOnce() {
        if (!gone) {
            gone = true
            onFinish()
        }
    }
    LaunchedEffect(report) {
        delay(DONE_BEAT_MS)
        finishOnce()
    }
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        AnimatedVisibility(
            visible = true,
            enter = MoriMotion.enter(MoriEnterKind.FAB),
            exit = fadeOut(animationSpec = MoriMotion.calmFade()),
        ) {
            MorphingHero(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                icon = MoriIcons.Check,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_imported_count, report.succeeded, report.total),
                    style = MoriEmphasized.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.onboarding_imported_caption),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
                if (report.failed > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_import_failed, report.failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_done_opening),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(
            onClick = {
                gone = true
                onImportMore()
            },
        ) {
            Text(stringResource(R.string.onboarding_import_more))
        }
    }
}

@ThemePreviews
@Composable
private fun OnboardingWelcomePreview() {
    MoriTheme {
        OnboardingScreen(
            uiState = OnboardingUiState.Welcome,
            onPickFolder = {},
            onPickFiles = {},
            onAction = {},
            onOnboardingComplete = {},
        )
    }
}
