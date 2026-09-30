package com.mori.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape

/** One option in a [MoriChoiceGroup]: label plus an optional leading icon. */
data class MoriChoiceOption(
    val label: String,
    val icon: ImageVector? = null,
)

private val OuterCorner = 20.dp
private val InnerCorner = 8.dp

/**
 * Expressive single-choice group: connected morphing [ToggleButton] pills in
 * the "Going / Maybe" language of the reference work — 56dp targets, icon +
 * label content, Flex-emphasized selected label. Selection semantics stay on
 * tags, not pixels.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MoriChoiceGroup(
    options: List<MoriChoiceOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTagFor: (String) -> String = { it },
) {
    val haptics = rememberMoriHaptics()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            // Connected-pill morphing: bold outer corners, tight inner joins.
            val pill = when {
                options.size == 1 -> CircleShape
                index == 0 -> RoundedCornerShape(
                    topStart = OuterCorner,
                    bottomStart = OuterCorner,
                    topEnd = InnerCorner,
                    bottomEnd = InnerCorner,
                )
                index == options.size - 1 -> RoundedCornerShape(
                    topStart = InnerCorner,
                    bottomStart = InnerCorner,
                    topEnd = OuterCorner,
                    bottomEnd = OuterCorner,
                )
                else -> RoundedCornerShape(InnerCorner)
            }
            ToggleButton(
                checked = selected,
                onCheckedChange = {
                    haptics(MoriHaptic.Select)
                    onSelect(index)
                },
                shapes = ToggleButtonDefaults.shapes(pill, pill, pill),
                modifier = Modifier
                    .weight(1f)
                    .testTag(testTagFor(option.label)),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    if (option.icon != null) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                        )
                    }
                    Text(
                        text = option.label,
                        style = if (selected) {
                            MoriEmphasized.labelLarge
                        } else {
                            MaterialTheme.typography.labelLarge
                        },
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
