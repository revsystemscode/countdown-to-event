package com.revsystems.countdowntoevent.ui.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.core.content.FileProvider
import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.time.Countdown
import com.revsystems.countdowntoevent.ui.format.shareText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.ZoneId

private const val TAG = "ShareEvent"

/** Opens the share sheet with a card image plus the text summary. Falls back to text only. */
suspend fun shareEvent(
    context: Context,
    event: Event,
    countdown: Countdown,
) {
    val locale = context.resources.configuration.locales[0]
    val zone = ZoneId.systemDefault()
    val text = shareText(context.resources, event, countdown, locale, zone)
    val imageUri =
        try {
            withContext(Dispatchers.IO) {
                val bitmap = ShareImageRenderer.render(context, event, countdown, zone)
                // One file per event, overwritten on each share; the cache folder is ours to clean.
                val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                val file = File(dir, "countdown_${event.id}.png")
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't render share image, sharing text only", e)
            null
        }

    val send =
        Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            if (imageUri != null) {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                // ClipData gives the share sheet its preview thumbnail and carries the read grant.
                clipData = ClipData.newRawUri(null, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        }
    context.startActivity(Intent.createChooser(send, null))
}
