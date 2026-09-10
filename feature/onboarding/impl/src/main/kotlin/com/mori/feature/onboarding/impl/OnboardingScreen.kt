package com.mori.feature.onboarding.impl

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.ButtonDefaults
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
    val customFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val name = runCatching {
                DocumentFile.fromTreeUri(context, uri)?.name
            }.getOrNull()
            viewModel.onAction(OnboardingAction.CustomFolderChosen(uri, name))
        }
    }

    OnboardingScreen(
        uiState = uiState,
        onPickFolder = { folderLauncher.launch(null) },
        onPickFiles = { filesLauncher.launch(arrayOf("*/*")) },
        onPickCustomFolder = { customFolderLauncher.launch(null) },
        onAction = viewModel::onAction,
        onOnboardingComplete = onOnboardingComplete,
        modifier = modifier,
    )
}

@Composable
internal fun OnboardingScreen(
    uiState: OnboardingUiState,
    onPickFolder: () -> Unit,
    onPickFiles: () -> Unit,
    onPickCustomFolder: () -> Unit,
    onAction: (OnboardingAction) -> Unit,
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
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
                title = "Where do your comics live?",
                body = "Keep copies inside Mori, or link a folder you already organize.",
                onBack = { onAction(OnboardingAction.BackStep) },
                onSkip = {
                    onAction(OnboardingAction.Skip)
                    onOnboardingComplete()
                },
                onContinue = { onAction(OnboardingAction.ContinueStep) },
                continueLabel = "Continue",
                continueCaption = "You can rescan or relocate later.",
            ) {
                StorageOptions(
                    location = uiState.location,
                    folderName = uiState.folderName,
                    onSelectApp = { onAction(OnboardingAction.SelectStorage(StorageLocation.APP)) },
                    onPickCustom = onPickCustomFolder,
                )
            }
            is OnboardingUiState.Appearance -> WizardStep(
                modifier = Modifier.testTag(OnboardingTestTags.AppearanceStep),
                stepIndex = 1,
                totalSteps = 3,
                title = "Make it yours",
                body = "Theme and color follow you everywhere in Mori.",
                onBack = { onAction(OnboardingAction.BackStep) },
                onSkip = {
                    onAction(OnboardingAction.Skip)
                    onOnboardingComplete()
                },
                onContinue = { onAction(OnboardingAction.ContinueStep) },
                continueLabel = "Continue",
                continueCaption = "Everything stays changeable in Settings.",
            ) {
                AppearanceOptions(theme = uiState.theme, onAction = onAction)
            }
            is OnboardingUiState.Import -> WizardStep(
                modifier = Modifier.testTag(OnboardingTestTags.ImportStep),
                stepIndex = 2,
                totalSteps = 3,
                title = "Import your comics",
                body = if (uiState.location == StorageLocation.CUSTOM && uiState.folderName != null) {
                    stringResource(R.string.onboarding_import_body_custom, uiState.folderName)
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
                    onPickFolder = onPickFolder,
                    onPickFiles = onPickFiles,
                )
            }
            is OnboardingUiState.Importing -> ImportingContent(
                done = uiState.done,
                total = uiState.total,
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
            delay(90)
            step++
        }
    }
    fun visibleAt(index: Int) = step > index
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            Text(
                text = "Mori",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 16.dp),
            )
            TextButton(onClick = onSkip) {
                Text("Skip")
            }
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
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
                    text = "WELCOME",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildAnnotatedString {
                        append("Your comics,\n")
                        withStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            append("beautifully")
                        }
                        append(" shelved.")
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
                    text = "Three quick steps and your library is ready. Your files stay yours.",
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
                modifier = Modifier.padding(horizontal = 32.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onGetStarted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(OnboardingTestTags.GetStarted),
                    ) {
                        Text("Get started")
                    }
                    Text(
                        text = "Takes about a minute · No account needed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = MoriIcons.Back, contentDescription = "Back")
            }
            Text(
                text = "STEP ${stepIndex + 1} OF $totalSteps",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSkip) {
                Text("Skip")
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    content()
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (onContinue != null) {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp),
                ) {
                    Button(
                        onClick = { onContinue?.invoke() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(OnboardingTestTags.StepContinue),
                    ) {
                        Text(continueLabel)
                    }
                    if (continueCaption.isNotBlank()) {
                        Text(
                            text = continueCaption,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Storage step: app-private copies vs a linked custom folder. */
@Composable
private fun StorageOptions(
    location: StorageLocation,
    folderName: String?,
    onSelectApp: () -> Unit,
    onPickCustom: () -> Unit,
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
            subtitle = "Mori keeps copies inside the app. Originals stay untouched.",
            onClick = onSelectApp,
        )
        OptionRow(
            selected = location == StorageLocation.CUSTOM,
            icon = MoriIcons.MenuBook,
            title = folderName?.let { stringResource(R.string.onboarding_storage_custom_named, it) } ?: stringResource(R.string.onboarding_storage_custom),
            subtitle = "Read from a folder you organize. Stays linked for rescans.",
            onClick = onPickCustom,
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
                label = { Text("System") },
            )
            SegmentedButton(
                selected = theme.mode == ThemeMode.LIGHT,
                onClick = { onAction(OnboardingAction.SetThemeMode(ThemeMode.LIGHT)) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                label = { Text("Light") },
            )
            SegmentedButton(
                selected = theme.mode == ThemeMode.DARK,
                onClick = { onAction(OnboardingAction.SetThemeMode(ThemeMode.DARK)) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                label = { Text("Dark") },
            )
        }
        Text(
            text = "Colors",
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

/** Import step: folder + files pickers, location-aware copy. */
@Composable
private fun ImportOptions(
    onPickFolder: () -> Unit,
    onPickFiles: () -> Unit,
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
                .testTag(OnboardingTestTags.PickFolder),
        ) {
            Text("Choose folder")
        }
        OutlinedButton(
            onClick = onPickFiles,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(OnboardingTestTags.PickFiles),
        ) {
            Text("Pick individual files")
        }
        Text(
            text = "CBZ and CBR supported. Copies land in your library.",
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

/** Summary beat before Done auto-advances home. */
private const val DONE_AUTO_ADVANCE_MS = 1500L

@Composable
private fun ImportingContent(
    done: Int,
    total: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        if (total <= 0) {
            CircularProgressIndicator(modifier = Modifier.testTag(OnboardingTestTags.Progress))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Scanning for comics…", style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(
                text = "Copying $done of $total",
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
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}

@Composable
private fun DoneContent(
    report: ImportReport,
    onImportMore: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Auto-advance home once the summary lands; the button skips the wait.
    // Guarded so fast taps can't finish twice.
    var finished by remember { mutableStateOf(false) }
    fun finishOnce() {
        if (!finished) {
            finished = true
            onFinish()
        }
    }
    LaunchedEffect(report) {
        delay(DONE_AUTO_ADVANCE_MS)
        finishOnce()
    }
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        MorphingHero(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            icon = MoriIcons.Check,
        )
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
                    text = "comics imported",
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
        Button(
            onClick = ::finishOnce,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(OnboardingTestTags.Finish),
        ) {
            Text(stringResource(R.string.onboarding_start_reading))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onImportMore,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Import more")
        }
        if (report.failed > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
            ) {
                items(report.items.filter { it.error != null }, key = { it.displayName }) { item ->
                    Text(
                        text = stringResource(R.string.onboarding_error_item, item.displayName, item.error.orEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
            onPickCustomFolder = {},
            onAction = {},
            onOnboardingComplete = {},
        )
    }
}
