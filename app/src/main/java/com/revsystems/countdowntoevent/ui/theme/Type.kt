package com.revsystems.countdowntoevent.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.revsystems.countdowntoevent.R

private fun outfit(weight: FontWeight) =
    Font(
        resId = R.font.outfit_variable,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

val Outfit =
    FontFamily(
        outfit(FontWeight.Normal),
        outfit(FontWeight.Medium),
        outfit(FontWeight.SemiBold),
        outfit(FontWeight.Bold),
        outfit(FontWeight.ExtraBold),
    )

private val base = Typography()

val AppTypography =
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Outfit, fontWeight = FontWeight.ExtraBold),
        displayMedium = base.displayMedium.copy(fontFamily = Outfit, fontWeight = FontWeight.ExtraBold),
        displaySmall = base.displaySmall.copy(fontFamily = Outfit, fontWeight = FontWeight.Bold),
        headlineLarge = base.headlineLarge.copy(fontFamily = Outfit, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontFamily = Outfit, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontFamily = Outfit, fontWeight = FontWeight.Medium),
        bodyLarge = base.bodyLarge.copy(fontFamily = Outfit),
        bodyMedium = base.bodyMedium.copy(fontFamily = Outfit),
        bodySmall = base.bodySmall.copy(fontFamily = Outfit),
        labelLarge = base.labelLarge.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontFamily = Outfit, fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(fontFamily = Outfit, fontWeight = FontWeight.Medium),
    )

/** Big number on event cards ("10" days). */
val CountdownNumberStyle =
    TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 56.sp,
        lineHeight = 56.sp,
    )
