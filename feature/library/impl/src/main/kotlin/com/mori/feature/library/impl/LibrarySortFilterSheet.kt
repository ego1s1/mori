package com.mori.feature.library.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibraryQuery
import com.mori.core.model.LibrarySortOrder

/**
 * Tab-less Filter/Sort/Display sheet in the Mihon settings-dialog spirit, restyled as a
 * single scrolling M3 sheet: filter chips, sort chips, and display switches.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibrarySortFilterSheet(
    query: LibraryQuery,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = { onAction(LibraryAction.CloseFilter) },
        modifier = modifier.testTag(LibraryTestTags.SortFilterSheet),
    ) {
        LibrarySortFilterContent(
            query = query,
            onAction = onAction,
        )
    }
}

/** Sheet body, exposed for testing without the modal wrapper. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LibrarySortFilterContent(
    query: LibraryQuery,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(text = stringResource(R.string.library_sheet_filter), style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            LibraryFilter.entries.forEach { filter ->
                FilterChip(
                    selected = query.filter == filter,
                    onClick = { onAction(LibraryAction.FilterSelected(filter)) },
                    label = { Text(filterLabel(filter)) },
                )
            }
        }

        Text(text = stringResource(R.string.library_sheet_sort_by), style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            LibrarySortOrder.entries.forEach { sort ->
                FilterChip(
                    selected = query.sortOrder == sort,
                    onClick = { onAction(LibraryAction.SortSelected(sort)) },
                    label = { Text(sortLabel(sort)) },
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.library_sheet_hide_errors),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = query.hideErrors,
                onCheckedChange = { onAction(LibraryAction.ToggleHideErrors(it)) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun filterLabel(filter: LibraryFilter): String = stringResource(
    when (filter) {
        LibraryFilter.ALL -> R.string.library_filter_all
        LibraryFilter.IN_PROGRESS -> R.string.library_filter_in_progress
        LibraryFilter.UNREAD -> R.string.library_filter_unread
        LibraryFilter.FINISHED -> R.string.library_filter_finished
    },
)

@Composable
private fun sortLabel(sort: LibrarySortOrder): String = stringResource(
    when (sort) {
        LibrarySortOrder.RECENTLY_ADDED -> R.string.library_sort_recently_added
        LibrarySortOrder.RECENTLY_READ -> R.string.library_sort_recently_read
        LibrarySortOrder.TITLE -> R.string.library_sort_title
        LibrarySortOrder.UNFINISHED_FIRST -> R.string.library_sort_unfinished_first
    },
)
