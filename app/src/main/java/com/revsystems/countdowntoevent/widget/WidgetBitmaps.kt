package com.revsystems.countdowntoevent.widget

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.revsystems.countdowntoevent.domain.model.EventBackground
import com.revsystems.countdowntoevent.ui.theme.BackgroundPresets
import kotlin.math.max

/**
 * Widgets render in the launcher's process, which can't read our private files and has no
 * gradient support, so backgrounds are handed over as small bitmaps.
 */
object WidgetBitmaps {
    // Keeps several widgets well under the RemoteViews bitmap memory limit.
    private const val MAX_SIDE = 720
    private const val GRADIENT_WIDTH = 400
    private const val GRADIENT_HEIGHT = 200

    /** null for solid colors, which are drawn as a plain background. */
    fun forBackground(background: EventBackground): Bitmap? =
        when (background) {
            is EventBackground.Solid -> null
            is EventBackground.Preset -> gradient(background.key)
            is EventBackground.LocalImage -> photo(background.uri)
        }

    private fun gradient(key: String): Bitmap {
        val colors =
            BackgroundPresets
                .gradient(key)
                .colors
                .map { it.toArgb() }
                .toIntArray()
        val bitmap = createBitmap(GRADIENT_WIDTH, GRADIENT_HEIGHT)
        val paint =
            Paint().apply {
                shader =
                    LinearGradient(0f, 0f, GRADIENT_WIDTH.toFloat(), GRADIENT_HEIGHT.toFloat(), colors, null, Shader.TileMode.CLAMP)
            }
        Canvas(bitmap).drawRect(0f, 0f, GRADIENT_WIDTH.toFloat(), GRADIENT_HEIGHT.toFloat(), paint)
        return bitmap
    }

    private fun photo(uri: String): Bitmap? {
        val path = uri.toUri().path ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
