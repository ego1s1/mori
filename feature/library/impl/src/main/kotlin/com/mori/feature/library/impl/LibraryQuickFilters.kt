package com.mori.feature.library.impl

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mori.core.designsystem.LocalAppFonts
import com.mori.core.designsystem.MoriHaptic
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.rememberMoriHaptics
import com.mori.core.model.LibraryFilter

private data class QuickFilterOption(
    val filter: LibraryFilter,
    val labelRes: Int,
)

private val FILTER_OPTIONS = listOf(
    QuickFilterOption(LibraryFilter.ALL, R.string.library_filter_all),
    QuickFilterOption(LibraryFilter.IN_PROGRESS, R.string.library_filter_in_progress),
    QuickFilterOption(LibraryFilter.UNREAD, R.string.library_filter_unread),
    QuickFilterOption(LibraryFilter.FAVORITES, R.string.library_filter_favorites),
    QuickFilterOption(LibraryFilter.FINISHED, R.string.library_filter_finished),
)

/**
 * Expressive horizontal category filter pills anchored right below the search bar.
 * Gives immediate, one-touch filtering between reading states without opening the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryQuickFilters(
    selectedFilter: LibraryFilter,
    onFilterSelect: (LibraryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberMoriHaptics()
    val scrollState = rememberScrollState()

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .testTag(LibraryTestTags.QuickFilterCapsule),
    ) {
        FILTER_OPTIONS.forEach { option ->
            val isSelected = option.filter == selectedFilter
            FilterChip(
                selected = isSelected,
                onClick = {
                    haptics(MoriHaptic.Select)
                    onFilterSelect(option.filter)
                },
                label = {
                    Text(
                        text = stringResource(option.labelRes),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = LocalAppFonts.current.displaySoft,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                    )
                },
                leadingIcon = if (isSelected && option.filter == LibraryFilter.FAVORITES) {
                    {
                        Icon(
                            imageVector = MoriIcons.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else null,
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                border = null,
                modifier = Modifier.testTag(LibraryTestTags.quickFilterChip(option.filter)),
            )
        }
    }
}
