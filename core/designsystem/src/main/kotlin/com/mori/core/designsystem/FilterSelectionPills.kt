package com.mori.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mori.core.model.FilterBlendMode
import com.mori.core.model.FilterColorTone

@Composable
fun MoriToneChoiceGroup(
    selectedTone: FilterColorTone,
    onSelectTone: (FilterColorTone) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberMoriHaptics()
    val scrollState = rememberScrollState()

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
    ) {
        FilterColorTone.entries.forEach { tone ->
            val isSelected = tone == selectedTone
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                animationSpec = MoriMotion.defaultEffectsSpec(),
                label = "toneContainerColor",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = MoriMotion.defaultEffectsSpec(),
                label = "toneContentColor",
            )
            val label = when (tone) {
                FilterColorTone.WARM_AMBER -> stringResource(R.string.filter_tone_amber)
                FilterColorTone.SEPIA_PAPER -> stringResource(R.string.filter_tone_sepia)
                FilterColorTone.COOL_SLATE -> stringResource(R.string.filter_tone_slate)
                FilterColorTone.FOREST_MINT -> stringResource(R.string.filter_tone_mint)
                FilterColorTone.E_INK_HIGH_CONTRAST -> stringResource(R.string.filter_tone_eink)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(containerColor)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                            )
                        } else Modifier
                    )
                    .clickable(
                        role = Role.RadioButton,
                        onClick = {
                            if (!isSelected) {
                                haptics(MoriHaptic.Select)
                                onSelectTone(tone)
                            }
                        },
                    )
                    .semantics {
                        selected = isSelected
                        role = Role.RadioButton
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(tone.argb))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = contentColor,
                )
            }
        }
    }
}

@Composable
fun MoriBlendChoiceGroup(
    selectedMode: FilterBlendMode,
    onSelectMode: (FilterBlendMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberMoriHaptics()
    val scrollState = rememberScrollState()

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
    ) {
        FilterBlendMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                animationSpec = MoriMotion.defaultEffectsSpec(),
                label = "blendContainerColor",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = MoriMotion.defaultEffectsSpec(),
                label = "blendContentColor",
            )
            val label = when (mode) {
                FilterBlendMode.DEFAULT -> stringResource(R.string.filter_blend_default)
                FilterBlendMode.MULTIPLY -> stringResource(R.string.filter_blend_multiply)
                FilterBlendMode.SCREEN -> stringResource(R.string.filter_blend_screen)
                FilterBlendMode.OVERLAY -> stringResource(R.string.filter_blend_overlay)
                FilterBlendMode.LIGHTEN -> stringResource(R.string.filter_blend_lighten)
                FilterBlendMode.DARKEN -> stringResource(R.string.filter_blend_darken)
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(containerColor)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.secondary,
                                shape = CircleShape,
                            )
                        } else Modifier
                    )
                    .clickable(
                        role = Role.RadioButton,
                        onClick = {
                            if (!isSelected) {
                                haptics(MoriHaptic.Select)
                                onSelectMode(mode)
                            }
                        },
                    )
                    .semantics {
                        selected = isSelected
                        role = Role.RadioButton
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = contentColor,
                )
            }
        }
    }
}
