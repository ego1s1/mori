package com.mori.feature.onboarding.impl

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.LocalExpressiveMotionEnabled
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriEnterKind
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriMotion
import com.mori.core.designsystem.enter
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
internal fun OnboardingScreen(
    uiState: OnboardingUiState,
    onPickFolder: () -> Unit,
    onPickFiles: () -> Unit,
    onAction: (OnboardingAction) -> Unit,
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        when (uiState) {
            OnboardingUiState.Welcome -> WelcomeContent(
                onPickFolder = onPickFolder,
                onPickFiles = onPickFiles,
            )
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
    onPickFolder: () -> Unit,
    onPickFiles: () -> Unit,
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
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        AnimatedVisibility(
            visible = visibleAt(0),
            enter = MoriMotion.enter(MoriEnterKind.FAB),
            exit = fadeOut(animationSpec = MoriMotion.calmFade()),
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(128.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = MoriIcons.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(64.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        AnimatedVisibility(
            visible = visibleAt(1),
            enter = MoriMotion.enter(MoriEnterKind.RISE),
            exit = fadeOut(animationSpec = MoriMotion.calmFade()),
        ) {
            Text(
                text = "Where are your comics?",
                style = MoriEmphasized.headlineMedium,
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
                text = "Pick a folder and Mori copies your CBZ and CBR files into its " +
                    "private library. Your originals stay exactly where they are.",
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
                    onClick = onPickFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(OnboardingTestTags.PickFolder),
                ) {
                    Text("Choose folder")
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onPickFiles,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(OnboardingTestTags.PickFiles),
                ) {
                    Text("Pick individual files")
                }
            }
        }
    }
}

private const val WELCOME_STEPS = 4

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
            LinearProgressIndicator(
                progress = { done.toFloat() / total },
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
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.size(128.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = MoriIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(64.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Imported ${report.succeeded} of ${report.total}",
            style = MoriEmphasized.headlineSmall,
            textAlign = TextAlign.Center,
        )
        if (report.failed > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${report.failed} file(s) could not be copied.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(OnboardingTestTags.Finish),
        ) {
            Text("Start reading")
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
                        text = "${item.displayName}: ${item.error.orEmpty()}",
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
            onAction = {},
            onOnboardingComplete = {},
        )
    }
}
