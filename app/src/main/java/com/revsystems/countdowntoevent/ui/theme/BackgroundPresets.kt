package com.revsystems.countdowntoevent.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.revsystems.countdowntoevent.R

data class SolidPreset(
    val argb: Int,
    @param:StringRes val nameRes: Int,
)

data class GradientPreset(
    val key: String,
    val colors: List<Color>,
    @param:StringRes val nameRes: Int,
) {
    val brush: Brush get() = Brush.linearGradient(colors)
}

/** Event background choices. Keys are stored in the database: never rename them. */
object BackgroundPresets {
    val solids =
        listOf(
            SolidPreset(0xFF1A9AA6.toInt(), R.string.color_teal),
            SolidPreset(0xFF3E63DD.toInt(), R.string.color_blue),
            SolidPreset(0xFF8E4EC6.toInt(), R.string.color_purple),
            SolidPreset(0xFFD6409F.toInt(), R.string.color_pink),
            SolidPreset(0xFFDC3E42.toInt(), R.string.color_red),
            SolidPreset(0xFFCC4E00.toInt(), R.string.color_orange),
            SolidPreset(0xFF2B8A3E.toInt(), R.string.color_green),
            SolidPreset(0xFF1C2024.toInt(), R.string.color_charcoal),
        )

    val gradients =
        listOf(
            GradientPreset("sunset", listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B)), R.string.gradient_sunset),
            GradientPreset("ocean", listOf(Color(0xFF2193B0), Color(0xFF6DD5ED)), R.string.gradient_ocean),
            GradientPreset("forest", listOf(Color(0xFF134E5E), Color(0xFF71B280)), R.string.gradient_forest),
            GradientPreset("berry", listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)), R.string.gradient_berry),
            GradientPreset("night", listOf(Color(0xFF0F2027), Color(0xFF2C5364)), R.string.gradient_night),
            GradientPreset("aurora", listOf(Color(0xFF00C9A7), Color(0xFF845EC2)), R.string.gradient_aurora),
        )

    fun gradient(key: String): GradientPreset = gradients.firstOrNull { it.key == key } ?: gradients.first()
}
