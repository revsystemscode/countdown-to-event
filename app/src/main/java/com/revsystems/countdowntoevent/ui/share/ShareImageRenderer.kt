package com.revsystems.countdowntoevent.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import androidx.core.net.toUri
import com.revsystems.countdowntoevent.R
import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.model.EventBackground
import com.revsystems.countdowntoevent.domain.time.Countdown
import com.revsystems.countdowntoevent.ui.format.CountdownBadge
import com.revsystems.countdowntoevent.ui.format.badgeFor
import com.revsystems.countdowntoevent.ui.format.formatEventDates
import com.revsystems.countdowntoevent.ui.format.titleWithEmoji
import com.revsystems.countdowntoevent.ui.theme.BackgroundPresets
import java.time.ZoneId
import kotlin.math.max

/**
 * Draws a share-ready 4:5 card with plain Canvas calls. Deterministic, works off the main
 * thread and needs no on-screen view (unlike capturing a Composable).
 */
object ShareImageRenderer {
    const val WIDTH = 1080
    const val HEIGHT = 1350
    private const val PADDING = 96f
    private const val PHOTO_MAX_SIDE = 1600

    fun render(
        context: Context,
        event: Event,
        countdown: Countdown,
        zone: ZoneId,
    ): Bitmap {
        val bitmap = createBitmap(WIDTH, HEIGHT)
        val canvas = Canvas(bitmap)
        drawBackground(canvas, event.background)
        // Same readability scrim on every background, a bit stronger at the bottom where the footer sits.
        canvas.drawRect(
            0f,
            0f,
            WIDTH.toFloat(),
            HEIGHT.toFloat(),
            Paint().apply { shader = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), 0x40000000, 0x8C000000.toInt(), Shader.TileMode.CLAMP) },
        )

        val res = context.resources
        val locale = res.configuration.locales[0]
        val font = ResourcesCompat.getFont(context, R.font.outfit_variable) ?: Typeface.SANS_SERIF
        val textWidth = (WIDTH - 2 * PADDING).toInt()

        val (number, label) =
            when (val badge = badgeFor(event, countdown)) {
                is CountdownBadge.Days -> badge.count.toString() to res.getQuantityString(R.plurals.unit_days, badge.count.toInt())
                is CountdownBadge.Hours -> badge.count.toString() to res.getQuantityString(R.plurals.unit_hours, badge.count.toInt())
                is CountdownBadge.Minutes -> badge.count.toString() to res.getQuantityString(R.plurals.unit_minutes, badge.count.toInt())
                is CountdownBadge.DaysAgo -> badge.count.toString() to res.getQuantityString(R.plurals.unit_days_ago, badge.count.toInt())
                CountdownBadge.Today -> null to context.getString(R.string.badge_today)
                CountdownBadge.Ongoing -> null to context.getString(R.string.badge_ongoing)
            }

        val title = layout(titleWithEmoji(event), font, weight = 700, 76f, textWidth, maxLines = 3)
        val numberLayout = number?.let { layout(it, font, weight = 800, if (it.length > 3) 260f else 340f, textWidth, maxLines = 1) }
        val labelLayout = layout(label, font, weight = 600, if (number == null) 140f else 72f, textWidth, maxLines = 2)
        val dates =
            layout(formatEventDates(event, countdown, locale, zone), font, weight = 500, 44f, textWidth, maxLines = 2, alpha = 0xE0)
        val footer = layout(context.getString(R.string.app_name), font, weight = 600, 36f, textWidth, maxLines = 1, alpha = 0xB0)

        val gap = 40f
        val blocks = listOfNotNull(title, numberLayout, labelLayout, dates)
        val total = blocks.sumOf { it.height } + gap * (blocks.size - 1)
        var y = max(PADDING, (HEIGHT - total) / 2f - 40f)
        blocks.forEach { block ->
            draw(canvas, block, y)
            y += block.height + if (block === numberLayout) -24f else gap // keep "66" and "days" together
        }
        draw(canvas, footer, HEIGHT - PADDING - footer.height)
        return bitmap
    }

    private fun layout(
        text: String,
        typeface: Typeface,
        weight: Int,
        size: Float,
        width: Int,
        maxLines: Int,
        alpha: Int = 0xFF,
    ): StaticLayout {
        val paint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                // Outfit is a variable font: pick the weight on its axis (Typeface.create can't).
                fontVariationSettings = "'wght' $weight"
                textSize = size
                color = (alpha shl 24) or 0xFFFFFF
            }
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setIncludePad(false)
            .build()
    }

    private fun draw(
        canvas: Canvas,
        layout: StaticLayout,
        top: Float,
    ) {
        canvas.withTranslation(PADDING, top) { layout.draw(this) }
    }

    private fun drawBackground(
        canvas: Canvas,
        background: EventBackground,
    ) {
        val full = RectF(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat())
        when (background) {
            is EventBackground.Solid -> canvas.drawColor(background.argb)
            is EventBackground.Preset -> {
                val colors =
                    BackgroundPresets
                        .gradient(background.key)
                        .colors
                        .map { it.toArgb() }
                        .toIntArray()
                val paint = Paint().apply { shader = LinearGradient(0f, 0f, full.right, full.bottom, colors, null, Shader.TileMode.CLAMP) }
                canvas.drawRect(full, paint)
            }
            is EventBackground.LocalImage -> {
                val photo = loadPhoto(background.uri)
                if (photo == null) {
                    canvas.drawColor((EventBackground.Default as EventBackground.Solid).argb)
                } else {
                    canvas.drawBitmap(photo, centerCrop(photo.width, photo.height), full, Paint(Paint.FILTER_BITMAP_FLAG))
                    photo.recycle()
                }
            }
        }
    }

    /** Largest centered source rectangle with the output's aspect ratio. */
    internal fun centerCrop(
        width: Int,
        height: Int,
    ): Rect {
        val target = WIDTH.toFloat() / HEIGHT
        return if (width.toFloat() / height > target) {
            val w = (height * target).toInt()
            Rect((width - w) / 2, 0, (width + w) / 2, height)
        } else {
            val h = (width / target).toInt()
            Rect(0, (height - h) / 2, width, (height + h) / 2)
        }
    }

    private fun loadPhoto(uri: String): Bitmap? {
        val path = uri.toUri().path ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= PHOTO_MAX_SIDE) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
