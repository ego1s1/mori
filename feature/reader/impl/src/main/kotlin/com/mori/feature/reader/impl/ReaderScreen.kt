package com.mori.feature.reader.impl

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
internal fun ReaderRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReaderScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
internal fun ReaderScreen(
    uiState: ReaderUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
            when (uiState) {
                ReaderUiState.Loading -> CircularProgressIndicator()
                is ReaderUiState.Ready -> Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ReaderScreenPreview() {
    MoriTheme {
        ReaderScreen(
            uiState = ReaderUiState.Ready("Reader preview"),
            onBackClick = {},
        )
    }
}
