package com.mori.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mori.core.model.ColorSchemeChoice
import com.mori.core.model.ThemeMode
import com.mori.core.model.ThemePreferences

/** Whether the named scheme is currently in effect (dynamic off + match). */
fun isSchemeSelected(
    dynamicColor: Boolean,
    current: ColorSchemeChoice,
    choice: ColorSchemeChoice,
): Boolean = !dynamicColor && current == choice

/**
 * Full scheme picker: live dynamic mockup plus one phone per preset, in a
 * horizontal scroll row. Shared by onboarding and settings.
 */
@Composable
fun SchemePickerRow(
    theme: ThemePreferences,
    onDynamic: () -> Unit,
    onScheme: (ColorSchemeChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val darkTheme = when (theme.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        // Keyed slots + remembered clicks: a selection change recomposes only
        // the mockups whose `selected` flag flipped, not all five themes.
        key("dynamic") {
            val onDynamicClick = remember { { onDynamic() } }
            PhoneMockup(
                label = "Dynamic",
                selected = theme.dynamicColor,
                onClick = onDynamicClick,
                darkTheme = darkTheme,
                dynamicColor = true,
                choice = ColorSchemeChoice.MORI,
                amoled = theme.amoled,
            )
        }
        ColorSchemeChoice.entries.forEach { choice ->
            key(choice) {
                val onSchemeClick = remember(choice) { { onScheme(choice) } }
                PhoneMockup(
                    label = choice.name.lowercase().replaceFirstChar { it.uppercase() },
                    selected = isSchemeSelected(theme.dynamicColor, theme.colorScheme, choice),
                    onClick = onSchemeClick,
                    darkTheme = darkTheme,
                    dynamicColor = false,
                    choice = choice,
                    amoled = theme.amoled,
                )
            }
        }
    }
}

@Composable
private fun PhoneMockup(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    darkTheme: Boolean,
    dynamicColor: Boolean,
    choice: ColorSchemeChoice,
    amoled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(104.dp)
            .clickable(onClick = onClick, role = Role.RadioButton)
            .semantics { this.selected = selected },
    ) {
        MoriTheme(
            darkTheme = darkTheme,
            dynamicColor = dynamicColor,
            colorScheme = choice,
            amoled = amoled,
        ) {
            Surface(
                shape = RoundedCornerShape(17.dp),
                color = MaterialTheme.colorScheme.background,
                border = if (selected) {
                    BorderStroke(
                        4.dp,
                        MaterialTheme.colorScheme.primary,
                    )
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(13.dp)),
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // App bar row.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape,
                                ),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .weight(1f)
                                .background(
                                    MaterialTheme.colorScheme.onSurface,
                                    MaterialTheme.shapes.extraSmall,
                                ),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // Headline bar.
                    Box(
                        modifier = Modifier
                            .height(10.dp)
                            .fillMaxWidth(0.7f)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.shapes.extraSmall,
                            ),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Content lines.
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    MaterialTheme.shapes.extraSmall,
                                ),
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // FAB dot.
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    CircleShape,
                                ),
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth(),
        )
    }
}
