package com.revsystems.countdowntoevent.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.revsystems.countdowntoevent.R
import com.revsystems.countdowntoevent.domain.model.EventBackground
import com.revsystems.countdowntoevent.ui.theme.BackgroundPresets

@Composable
fun BackgroundPicker(
    selected: EventBackground,
    isImporting: Boolean,
    imageError: Boolean,
    onSelect: (EventBackground) -> Unit,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubLabel(stringResource(R.string.bg_colors))
        SwatchRow {
            BackgroundPresets.solids.forEach { preset ->
                val bg = EventBackground.Solid(preset.argb)
                Swatch(
                    selected = selected == bg,
                    description = stringResource(preset.nameRes),
                    onClick = { onSelect(bg) },
                ) { Box(Modifier.fillMaxSize().background(Color(preset.argb))) }
            }
        }

        SubLabel(stringResource(R.string.bg_gradients))
        SwatchRow {
            BackgroundPresets.gradients.forEach { preset ->
                val bg = EventBackground.Preset(preset.key)
                Swatch(
                    selected = selected == bg,
                    description = stringResource(preset.nameRes),
                    onClick = { onSelect(bg) },
                ) { Box(Modifier.fillMaxSize().background(preset.brush)) }
            }
        }

        SubLabel(stringResource(R.string.bg_photo))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (selected is EventBackground.LocalImage) {
                Swatch(selected = true, description = stringResource(R.string.bg_photo), onClick = {}) {
                    AsyncImage(
                        model = selected.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            OutlinedButton(onClick = onPickImage, enabled = !isImporting) {
                if (isImporting) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(
                    text =
                        stringResource(
                            if (selected is EventBackground.LocalImage) R.string.bg_change_photo else R.string.bg_pick_photo,
                        ),
                    modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                )
            }
        }
        if (imageError) {
            Text(
                text = stringResource(R.string.error_image),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SubLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SwatchRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

@Composable
private fun Swatch(
    selected: Boolean,
    description: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier =
            Modifier
                .size(56.dp)
                .clip(shape)
                .border(
                    width = if (selected) 3.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = shape,
                ).selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        content()
        if (selected) {
            Box(
                Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent))),
            )
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
        }
    }
}
