package com.mori.feature.library.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mori.core.designsystem.MoriChoiceGroup
import com.mori.core.designsystem.MoriChoiceOption
import com.mori.core.designsystem.MoriFilterPills
import com.mori.core.designsystem.MoriSettingSwitch
import com.mori.core.designsystem.MoriSheet
import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibraryQuery
import com.mori.core.model.LibrarySortOrder
import com.mori.core.model.UserCollection

/**
 * Tab-less Filter/Sort/Display sheet as a single scrolling M3 sheet:
 * shelf, filter chips, sort chips, and display switches.
 */
@Composable
internal fun LibrarySortFilterSheet(
    query: LibraryQuery,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
    collections: List<UserCollection> = emptyList(),
    selectedCollectionId: Long? = null,
) {
    MoriSheet(
        onDismissRequest = { onAction(LibraryAction.CloseFilter) },
        modifier = modifier.testTag(LibraryTestTags.SortFilterSheet),
    ) {
        LibrarySortFilterContent(
            query = query,
            onAction = onAction,
            collections = collections,
            selectedCollectionId = selectedCollectionId,
        )
    }
}

/** Sheet body, exposed for testing without the modal wrapper. */
@Composable
internal fun LibrarySortFilterContent(
    query: LibraryQuery,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
    collections: List<UserCollection> = emptyList(),
    selectedCollectionId: Long? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        // Shelf filter lives here now that the grid chips row is gone:
        // All plus every user shelf with its count. Management
        // (create/rename/delete) lives in Settings Groups.
        Text(
            text = stringResource(R.string.library_sheet_shelf),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        val allLabel = stringResource(R.string.library_collection_all)
        MoriFilterPills(
            options = listOf(MoriChoiceOption(label = allLabel)) +
                collections.map { collection ->
                    MoriChoiceOption(
                        label = shelfLabel(collection.name, collection.bookCount),
                    )
                },
            selectedIndex = (listOf<Long?>(null) + collections.map { it.id })
                .indexOf(selectedCollectionId).coerceAtLeast(0),
            onSelect = { index ->
                onAction(
                    LibraryAction.SelectCollection(
                        (listOf<Long?>(null) + collections.map { it.id })[index],
                    ),
                )
            },
            modifier = Modifier.testTag(LibraryTestTags.CollectionRow),
            testTagFor = { index ->
                val id = (listOf<Long?>(null) + collections.map { it.id })[index]
                if (id == null) "collection:all" else LibraryTestTags.collectionChip(id)
            },
        )

        Text(
            text = stringResource(R.string.library_sheet_filter),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        MoriChoiceGroup(
            options = LibraryFilter.entries.map { MoriChoiceOption(label = filterLabel(it)) },
            selectedIndex = LibraryFilter.entries.indexOf(query.filter),
            onSelect = { onAction(LibraryAction.FilterSelected(LibraryFilter.entries[it])) },
            fillWidth = false,
        )

        Text(
            text = stringResource(R.string.library_sheet_sort_by),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        MoriChoiceGroup(
            options = LibrarySortOrder.entries.map { MoriChoiceOption(label = sortLabel(it)) },
            selectedIndex = LibrarySortOrder.entries.indexOf(query.sortOrder),
            onSelect = { onAction(LibraryAction.SortSelected(LibrarySortOrder.entries[it])) },
            fillWidth = false,
        )

        MoriSettingSwitch(
            title = stringResource(R.string.library_sheet_hide_errors),
            checked = query.hideErrors,
            onCheckedChange = { onAction(LibraryAction.ToggleHideErrors(it)) },
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun shelfLabel(name: String, bookCount: Int): String = stringResource(
    R.string.library_collection_labeled,
    name,
    bookCount,
)

@Composable
private fun filterLabel(filter: LibraryFilter): String = stringResource(
    when (filter) {
        LibraryFilter.ALL -> R.string.library_filter_all
        LibraryFilter.IN_PROGRESS -> R.string.library_filter_in_progress
        LibraryFilter.UNREAD -> R.string.library_filter_unread
        LibraryFilter.FINISHED -> R.string.library_filter_finished
        LibraryFilter.FAVORITES -> R.string.library_filter_favorites
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
