package com.mori.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Error card with an explanatory body and up to two actions. Replaces the
 * per-screen error layouts so failure copy and shape stay consistent.
 */
@Composable
fun MoriErrorCard(
    body: String,
    primaryLabel: String?,
    onPrimary: (() -> Unit)?,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    loading: Boolean = false,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (loading) {
                    MoriLoadingIndicator(modifier = Modifier.size(24.dp))
                } else {
                    if (primaryLabel != null && onPrimary != null) {
                        TextButton(onClick = onPrimary) {
                            Text(primaryLabel)
                        }
                    }
                }
                // Secondary stands down while loading: retry-then-remove
                // double-fires otherwise.
                if (secondaryLabel != null && onSecondary != null && !loading) {
                    TextButton(onClick = onSecondary) {
                        Text(secondaryLabel)
                    }
                }
            }
        }
    }
}
