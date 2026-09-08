package com.mori.feature.onboarding.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.ThemePreviews

@Composable
internal fun OnboardingRoute(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(
        uiState = uiState,
        onOnboardingComplete = onOnboardingComplete,
        modifier = modifier,
    )
}

@Composable
internal fun OnboardingScreen(
    uiState: OnboardingUiState,
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
            when (uiState) {
                OnboardingUiState.Loading -> CircularProgressIndicator()
                is OnboardingUiState.Ready -> Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun OnboardingScreenPreview() {
    MoriTheme {
        OnboardingScreen(
            uiState = OnboardingUiState.Ready("Onboarding coming in F1"),
            onOnboardingComplete = {},
        )
    }
}
