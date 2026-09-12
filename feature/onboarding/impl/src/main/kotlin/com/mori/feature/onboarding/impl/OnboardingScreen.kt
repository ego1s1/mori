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
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.mori.core.designsystem.exit
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
    OnboardingScreen(
        uiState = uiState,
        onPickFolder = { folderLauncher.launch(null) },
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
    onAction: (OnboardingAction) -> Unit,
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        // Gated transitions hoisted out: transitionSpec is not a composable
        // context, so the expressive-aware specs resolve here.
        val stepEnter = MoriMotion.enter(MoriEnterKind.FADE_THROUGH)
        val stepExit = MoriMotion.exit(MoriEnterKind.FADE_THROUGH)
        // Step changes fade through so the wizard never hard-cuts.
        val stepKey = when (uiState) {
            OnboardingUiState.Welcome -> 0
            is OnboardingUiState.Folder -> 1
            is OnboardingUiState.Appearance -> 2
        }
        AnimatedContent(
            targetState = stepKey,
            transitionSpec = {
                stepEnter togetherWith stepExit
            },
            label = "onboardingStep",
        ) { _ ->
        // Renders the live state (not the step key) so theme ticks
        // recompose in place without restarting the transition.
        when (uiState) {
            OnboardingUiState.Welcome -> WelcomeContent(
                onGetStarted = { onAction(OnboardingAction.GetStarted) },
                onSkip = {
                    onAction(OnboardingAction.Skip)
                    onOnboardingComplete()
                },
            )
            is OnboardingUiState.Folder -> WizardStep(
                modifier = Modifier.testTag(OnboardingTestTags.FolderStep),
                stepIndex = 0,
                totalSteps = 2,
                title = stringResource(R.string.onboarding_folder_title),
                body = stringResource(R.string.onboarding_folder_body),
                onBack = { onAction(OnboardingAction.BackStep) },
                onSkip = {
                    onAction(OnboardingAction.Skip)
                    onOnboardingComplete()
                },
                continueLabel = "",
                continueCaption = "",
            ) {
                FolderOptions(onPickFolder = onPickFolder)
            }
            is OnboardingUiState.Appearance -> WizardStep(
                modifier = Modifier.testTag(OnboardingTestTags.AppearanceStep),
                stepIndex = 1,
                totalSteps = 2,
                title = stringResource(R.string.onboarding_appearance_title),
                body = stringResource(R.string.onboarding_appearance_body),
                onBack = { onAction(OnboardingAction.BackStep) },
                onSkip = {
                    onAction(OnboardingAction.Skip)
                    onOnboardingComplete()
                },
                onContinue = {
                    onAction(OnboardingAction.Finish)
                    onOnboardingComplete()
                },
                continueLabel = stringResource(R.string.onboarding_continue),
                continueCaption = stringResource(R.string.onboarding_appearance_caption),
            ) {
                AppearanceOptions(theme = uiState.theme, onAction = onAction)
            }
        }
        }
    }
}

/** Step changes ride the shared fade-through; theme ticks stay in place. */

@Composable
private fun WelcomeContent(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Staggered entrance: hero, then actions cascade in.
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
                .verticalScroll(rememberScrollState())
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            }
            Spacer(modifier = Modifier.height(32.dp))
            AnimatedVisibility(
                visible = visibleAt(2),
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
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

private const val WELCOME_STEPS = 3

/** Folder step: the single question. The picker button launches SAF; the
 * surrounding card explains nothing is copied. */
@Composable
private fun FolderOptions(
    onPickFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Button(
            onClick = onPickFolder,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag(OnboardingTestTags.PickFolder),
        ) {
            Text(stringResource(R.string.onboarding_pick_folder))
        }
        Text(
            text = stringResource(R.string.onboarding_folder_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

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
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            repeat(totalSteps) { index ->
                key(index) {
                    // M3 expressive segments keep their stop-indicator dots;
                    // equal weights, one height, one gap, centered row keeps
                    // every dot and cap on the same baseline. Done and current
                    // steps read full — a partial fill parks mid-segment and
                    // looks stalled; the sweep between steps is the motion.
                    val target = if (index <= stepIndex) 1f else 0f
                    val fill by animateFloatAsState(
                        targetValue = target,
                        animationSpec = MoriMotion.defaultEffectsSpec(),
                        label = "stepSegment",
                    )
                    LinearProgressIndicator(
                        progress = { fill },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
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
    val expressiveMotion = LocalExpressiveMotionEnabled.current
    val corner by animateDpAsState(
        targetValue = if (morphed) 64.dp else 28.dp,
        animationSpec = if (expressiveMotion) {
            MoriMotion.heroSpring()
        } else {
            tween(durationMillis = 300, easing = MoriMotion.Emphasized)
        },
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

@ThemePreviews
@Composable
private fun OnboardingWelcomePreview() {
    MoriTheme {
            OnboardingScreen(
                uiState = OnboardingUiState.Welcome,
                onPickFolder = {},
                onAction = {},
                onOnboardingComplete = {},
            )
    }
}
