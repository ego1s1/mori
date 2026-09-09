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
        Text(text = "Filter", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            LibraryFilter.entries.forEach { filter ->
                FilterChip(
                    selected = query.filter == filter,
                    onClick = { onAction(LibraryAction.FilterSelected(filter)) },
                    label = { Text(filter.label) },
                )
            }
        }

        Text(text = "Sort by", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            LibrarySortOrder.entries.forEach { sort ->
                FilterChip(
                    selected = query.sortOrder == sort,
                    onClick = { onAction(LibraryAction.SortSelected(sort)) },
                    label = { Text(sort.label) },
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Hide unreadable comics",
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

private val LibraryFilter.label: String
    get() = when (this) {
        LibraryFilter.ALL -> "All"
        LibraryFilter.IN_PROGRESS -> "In progress"
        LibraryFilter.UNREAD -> "Unread"
        LibraryFilter.FINISHED -> "Finished"
    }

private val LibrarySortOrder.label: String
    get() = when (this) {
        LibrarySortOrder.RECENTLY_ADDED -> "Recently added"
        LibrarySortOrder.RECENTLY_READ -> "Recently read"
        LibrarySortOrder.TITLE -> "Title"
        LibrarySortOrder.UNFINISHED_FIRST -> "Unfinished first"
    }
