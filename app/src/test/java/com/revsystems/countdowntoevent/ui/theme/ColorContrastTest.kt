package com.revsystems.countdowntoevent.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards WCAG contrast of the brand palette so later color tweaks don't hurt readability. */
class ColorContrastTest {
    private fun contrast(
        a: Color,
        b: Color,
    ): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return maxOf(la, lb) / minOf(la, lb)
    }

    @Test
    fun bodyTextMeetsAa() {
        listOf(LightColors, DarkColors).forEach { scheme ->
            assertTrue(contrast(scheme.onBackground, scheme.background) >= 4.5)
            assertTrue(contrast(scheme.onSurfaceVariant, scheme.background) >= 4.5)
        }
    }

    @Test
    fun primaryButtonIconsMeetNonTextContrast() {
        listOf(LightColors, DarkColors).forEach { scheme ->
            assertTrue(contrast(scheme.onPrimary, scheme.primary) >= 3.0)
        }
    }

    @Test
    fun textOnPrimaryContainerMeetsAa() {
        listOf(LightColors, DarkColors).forEach { scheme ->
            assertTrue(contrast(scheme.onPrimaryContainer, scheme.primaryContainer) >= 4.5)
        }
    }
}
