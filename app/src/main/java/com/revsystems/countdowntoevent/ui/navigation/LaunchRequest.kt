package com.revsystems.countdowntoevent.ui.navigation

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.revsystems.countdowntoevent.MainActivity

/** A screen to open when the app is launched from outside (e.g. a widget tap). */
sealed interface LaunchRequest {
    data class OpenEvent(
        val eventId: Long,
    ) : LaunchRequest

    data object NewEvent : LaunchRequest

    companion object {
        private const val ACTION_OPEN_EVENT = "com.revsystems.countdowntoevent.OPEN_EVENT"
        private const val ACTION_NEW_EVENT = "com.revsystems.countdowntoevent.NEW_EVENT"
        private const val EXTRA_EVENT_ID = "event_id"

        fun from(intent: Intent?): LaunchRequest? =
            when (intent?.action) {
                ACTION_OPEN_EVENT -> intent.getLongExtra(EXTRA_EVENT_ID, -1).takeIf { it > 0 }?.let(::OpenEvent)
                ACTION_NEW_EVENT -> NewEvent
                else -> null
            }

        // Fresh task so Back from the opened screen lands on Home.
        private fun base(context: Context) =
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        fun openEventIntent(
            context: Context,
            eventId: Long,
        ): Intent =
            base(context)
                .setAction(ACTION_OPEN_EVENT)
                // Unique data keeps one PendingIntent per event.
                .setData("countdowntoevent://event/$eventId".toUri())
                .putExtra(EXTRA_EVENT_ID, eventId)

        fun newEventIntent(context: Context): Intent = base(context).setAction(ACTION_NEW_EVENT)

        fun openAppIntent(context: Context): Intent = base(context).setAction(Intent.ACTION_MAIN)
    }
}
