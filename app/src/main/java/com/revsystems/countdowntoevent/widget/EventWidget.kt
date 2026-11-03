package com.revsystems.countdowntoevent.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.revsystems.countdowntoevent.R
import com.revsystems.countdowntoevent.di.AppEntryPoint
import com.revsystems.countdowntoevent.di.appEntryPoint
import com.revsystems.countdowntoevent.domain.model.EventBackground
import com.revsystems.countdowntoevent.domain.model.RepeatRule
import com.revsystems.countdowntoevent.ui.format.formatEventDates
import com.revsystems.countdowntoevent.ui.format.labelRes
import com.revsystems.countdowntoevent.ui.format.titleWithEmoji
import com.revsystems.countdowntoevent.ui.navigation.LaunchRequest
import com.revsystems.countdowntoevent.ui.theme.Teal500
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Countdown to one event (or always the next one). Resizable: small square or wide card. */
class EventWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val entryPoint = context.appEntryPoint()
        provideContent {
            val prefs = currentState<Preferences>()
            val config = WidgetPrefs.config(prefs)
            val tick = prefs[WidgetPrefs.TICK] ?: 0L
            // Loaded inside the composition so config changes and refreshes reload it
            // even while a Glance session is already running.
            val data by produceState<EventWidgetData?>(null, config, tick) {
                value = withContext(Dispatchers.IO) { loadData(entryPoint, config) }
            }
            EventWidgetContent(data)
        }
    }

    private suspend fun loadData(
        entryPoint: AppEntryPoint,
        config: EventWidgetConfig,
    ): EventWidgetData {
        val clock = entryPoint.clock()
        val zone = clock.zone
        val model = EventWidgetModels.build(entryPoint.eventRepository().getEvents(), config, clock.instant(), zone)
        val shown = model as? EventWidgetModel.Showing
        val chronometerBase =
            shown?.takeIf { it.live }?.let {
                SystemClock.elapsedRealtime() + EventWidgetModels.liveSplit(it.item.countdown.remaining).remainder.toMillis()
            }
        return EventWidgetData(
            model = model,
            background = shown?.let { WidgetBitmaps.forBackground(it.item.event.background) },
            zone = zone,
            chronometerBase = chronometerBase,
        )
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(220.dp, 110.dp)
    }
}

class EventWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EventWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        rescheduleRefresh(context)
    }

    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        super.onDeleted(context, appWidgetIds)
        rescheduleRefresh(context)
    }
}

internal fun rescheduleRefresh(context: Context) {
    val entryPoint = context.appEntryPoint()
    entryPoint.applicationScope().launch { entryPoint.widgetRefreshScheduler().reschedule() }
}

data class EventWidgetData(
    val model: EventWidgetModel,
    val background: Bitmap?,
    val zone: ZoneId,
    /** SystemClock.elapsedRealtime() base for the live Chronometer. */
    val chronometerBase: Long?,
)

private val LIVE_WIDE_WIDTH = 150.dp
private val White = ColorProvider(Color.White)
private val WhiteSoft = ColorProvider(Color.White.copy(alpha = 0.85f))
private val Scrim = ColorProvider(Color.Black.copy(alpha = 0.35f))

@Composable
private fun EventWidgetContent(data: EventWidgetData?) {
    val context = LocalContext.current
    val shown = data?.model as? EventWidgetModel.Showing
    val event = shown?.item?.event
    val color = (event?.background as? EventBackground.Solid)?.let { Color(it.argb) } ?: Teal500
    val target =
        when {
            event != null -> LaunchRequest.openEventIntent(context, event.id)
            data?.model == EventWidgetModel.Empty -> LaunchRequest.newEventIntent(context)
            else -> LaunchRequest.openAppIntent(context)
        }

    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .cornerRadius(24.dp)
                .background(color)
                .clickable(actionStartActivity(target)),
    ) {
        data?.background?.let { bitmap ->
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize(),
            )
            Box(GlanceModifier.fillMaxSize().background(Scrim)) {}
        }
        when {
            data == null -> Unit
            shown == null -> EmptyContent()
            LocalSize.current.width >= EventWidget.WIDE.width -> WideContent(shown, data)
            else -> SmallContent(shown, data)
        }
    }
}

@Composable
private fun SmallContent(
    shown: EventWidgetModel.Showing,
    data: EventWidgetData,
) {
    Column(GlanceModifier.fillMaxSize().padding(14.dp)) {
        Text(
            text = titleWithEmoji(shown.item.event),
            style = TextStyle(color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            maxLines = 2,
        )
        Spacer(GlanceModifier.defaultWeight())
        Badge(shown, data, numberSize = 40.sp, alignEnd = false)
    }
}

@Composable
private fun WideContent(
    shown: EventWidgetModel.Showing,
    data: EventWidgetData,
) {
    val context = LocalContext.current
    val event = shown.item.event
    val locale = context.resources.configuration.locales[0]
    Row(GlanceModifier.fillMaxSize().padding(16.dp)) {
        Column(GlanceModifier.defaultWeight().fillMaxHeight()) {
            Text(
                text = titleWithEmoji(event),
                style = TextStyle(color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                maxLines = 2,
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = formatEventDates(event, shown.item.countdown, locale, data.zone),
                style = TextStyle(color = WhiteSoft, fontSize = 12.sp),
                maxLines = 1,
            )
            Spacer(GlanceModifier.defaultWeight())
            if (event.repeat != RepeatRule.NONE) {
                Text(
                    text = "↻ " + context.getString(event.repeat.labelRes()),
                    style = TextStyle(color = WhiteSoft, fontSize = 12.sp),
                )
            }
        }
        Spacer(GlanceModifier.width(8.dp))
        Badge(shown, data, numberSize = 48.sp, alignEnd = true)
    }
}

@Composable
private fun Badge(
    shown: EventWidgetModel.Showing,
    data: EventWidgetData,
    numberSize: TextUnit,
    alignEnd: Boolean,
) {
    val context = LocalContext.current
    val res = context.resources
    val align = if (alignEnd) Alignment.End else Alignment.Start
    val textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    // Only set in live mode.
    val base = data.chronometerBase?.takeIf { shown.live }
    val width =
        when {
            base == null -> GlanceModifier
            alignEnd -> GlanceModifier.width(LIVE_WIDE_WIDTH)
            else -> GlanceModifier.fillMaxWidth()
        }
    Column(modifier = width, horizontalAlignment = align) {
        if (base != null) {
            val days = EventWidgetModels.liveSplit(shown.item.countdown.remaining).days
            if (days > 0) {
                Text(
                    text = res.getQuantityString(R.plurals.a11y_days, days.toInt(), days),
                    style = TextStyle(color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = textAlign),
                )
            }
            LiveChronometer(base, textSizeSp = if (alignEnd) 28f else 24f, alignEnd = alignEnd)
            return@Column
        }

        val bigStyle = TextStyle(color = White, fontSize = numberSize, fontWeight = FontWeight.Bold, textAlign = textAlign)
        val labelStyle = TextStyle(color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = textAlign)
        val messageStyle = TextStyle(color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = textAlign)

        when (val badge = EventWidgetModels.badge(shown.item.event, shown.item.countdown, data.zone)) {
            is WidgetBadge.Days -> {
                Text(badge.count.toString(), style = bigStyle)
                Text(res.getQuantityString(R.plurals.unit_days, badge.count.toInt()), style = labelStyle)
            }
            is WidgetBadge.DaysAgo -> {
                Text(badge.count.toString(), style = bigStyle)
                Text(res.getQuantityString(R.plurals.unit_days_ago, badge.count.toInt()), style = labelStyle)
            }
            is WidgetBadge.TodayAt -> {
                Text(context.getString(R.string.badge_today), style = messageStyle)
                Text(
                    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(res.configuration.locales[0]).format(badge.time),
                    style = labelStyle,
                )
            }
            WidgetBadge.Today -> Text(context.getString(R.string.badge_today), style = messageStyle)
            WidgetBadge.Ongoing -> Text(context.getString(R.string.badge_ongoing), style = messageStyle)
        }
    }
}

/** A system Chronometer ticks every second without the app redrawing the widget. */
@Composable
private fun LiveChronometer(
    base: Long,
    textSizeSp: Float,
    alignEnd: Boolean,
) {
    val context = LocalContext.current
    val views =
        RemoteViews(context.packageName, R.layout.widget_chronometer).apply {
            setChronometer(R.id.widget_chronometer, base, null, true)
            setChronometerCountDown(R.id.widget_chronometer, true)
            setTextViewTextSize(R.id.widget_chronometer, TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            setInt(R.id.widget_chronometer, "setGravity", if (alignEnd) Gravity.END else Gravity.START)
        }
    // Full width: a wrap-content Chronometer gets clipped as its text grows.
    AndroidRemoteViews(views, GlanceModifier.fillMaxWidth())
}

@Composable
private fun EmptyContent() {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = context.getString(R.string.widget_empty_title),
            style = TextStyle(color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        )
        Text(
            text = context.getString(R.string.widget_empty_body),
            style = TextStyle(color = WhiteSoft, fontSize = 12.sp, textAlign = TextAlign.Center),
        )
    }
}
