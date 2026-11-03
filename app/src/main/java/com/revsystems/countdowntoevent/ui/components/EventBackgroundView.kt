package com.revsystems.countdowntoevent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.revsystems.countdowntoevent.domain.model.EventBackground
import com.revsystems.countdowntoevent.ui.theme.BackgroundPresets

@Composable
fun EventBackgroundView(
    background: EventBackground,
    modifier: Modifier = Modifier,
) {
    when (background) {
        is EventBackground.Solid -> Box(modifier.background(Color(background.argb)))
        is EventBackground.Preset -> Box(modifier.background(BackgroundPresets.gradient(background.key).brush))
        is EventBackground.LocalImage ->
            AsyncImage(
                model = background.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            )
    }
}

/** Darkening layer so white text stays readable over photos and light gradients. */
fun EventBackground.scrim(strong: Boolean = false): Brush? =
    when (this) {
        is EventBackground.Solid -> null
        else ->
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = if (strong) 0.55f else 0.45f),
                1f to Color.Black.copy(alpha = if (strong) 0.45f else 0.2f),
            )
    }
