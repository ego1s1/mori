package com.mori.core.designsystem

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * App bottom sheet: single seam for sheet presentation (dismiss, docking)
 * so future expressive sheet APIs land in one body. Callers keep their own
 * test tags on [modifier]; [skipPartiallyExpanded] docks tall content
 * (licenses) instead of stopping at half height. The experimental sheet
 * APIs stay behind this opt-in — callers need none.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoriSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded),
        modifier = modifier,
    ) {
        content()
    }
}
