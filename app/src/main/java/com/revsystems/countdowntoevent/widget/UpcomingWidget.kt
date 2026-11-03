package com.revsystems.countdowntoevent.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.revsystems.countdowntoevent.R
import com.revsystems.countdowntoevent.di.appEntryPoint
import com.revsystems.countdowntoevent.domain.EventCountdown
import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.model.EventBackground
import com.revsystems.countdowntoevent.ui.format.titleWithEmoji
import com.revsystems.countdowntoevent.ui.navigation.LaunchRequest
import com.revsystems.countdowntoevent.ui.theme.BackgroundPresets
import com.revsystems.countdowntoevent.ui.theme.DarkColors
import com.revsystems.countdowntoevent.ui.theme.LightColors
import com.revsystems.countdowntoevent.ui.theme.Teal500
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** List of the next upcoming events. */
class UpcomingWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val entryPoint = context.appEntryPoint()
        provideContent {
            val tick = currentState<Preferences>()[WidgetPrefs.TICK] ?: 0L
            val data by produceState<UpcomingData?>(null, tick) {
                value =
                    withContext(Dispatchers.IO) {
                        val clock = entryPoint.clock()
                        UpcomingData(
                            items =
                                EventWidgetModels.upcoming(
                                    entryPoint.eventRepository().getEvents(),
                                    clock.instant(),
                                    clock.zone,
                                    MAX_ITEMS,
                                ),
                            zone = clock.zone,
                        )
                    }
            }
            GlanceTheme(colors = WidgetColors) { UpcomingContent(data) }
        }
    }

    private companion object {
        const val MAX_ITEMS = 10
    }
}

class UpcomingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpcomingWidget()

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

data class UpcomingData(
    val items: List<EventCountdown>,
    val zone: ZoneId,
)

private val WidgetColors = ColorProviders(light = LightColors, dark = DarkColors)

@Composable
private fun UpcomingContent(data: UpcomingData?) {
    val context = LocalContext.current
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .cornerRadius(24.dp)
                .background(GlanceTheme.colors.background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = context.getString(R.string.tab_upcoming),
                style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(LaunchRequest.openAppIntent(context))),
            )
            Image(
                provider = ImageProvider(R.drawable.ic_widget_add),
                contentDescription = context.getString(R.string.action_add_event),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(28.dp).clickable(actionStartActivity(LaunchRequest.newEventIntent(context))),
            )
        }
        Spacer(GlanceModifier.height(6.dp))

        when {
            data == null -> Unit
            data.items.isEmpty() ->
                Text(
                    text = context.getString(R.string.widget_list_empty),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp),
                )
            else ->
                LazyColumn {
                    items(data.items, itemId = { it.event.id }) { item -> UpcomingRow(item, data.zone) }
                }
        }
    }
}

@Composable
private fun UpcomingRow(
    item: EventCountdown,
    zone: ZoneId,
) {
    val context = LocalContext.current
    Row(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clickable(actionStartActivity(LaunchRequest.openEventIntent(context, item.event.id))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(GlanceModifier.size(10.dp).cornerRadius(5.dp).background(ColorProvider(dotColor(item.event)))) {}
        Spacer(GlanceModifier.width(10.dp))
        Text(
            text = titleWithEmoji(item.event),
            style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = shortBadge(context, EventWidgetModels.badge(item.event, item.countdown, zone)),
            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold),
        )
    }
}

private fun dotColor(event: Event): Color =
    when (val bg = event.background) {
        is EventBackground.Solid -> Color(bg.argb)
        is EventBackground.Preset -> BackgroundPresets.gradient(bg.key).colors.first()
        is EventBackground.LocalImage -> Teal500
    }

private fun shortBadge(
    context: Context,
    badge: WidgetBadge,
): String {
    val res = context.resources
    return when (badge) {
        is WidgetBadge.Days -> res.getQuantityString(R.plurals.a11y_days, badge.count.toInt(), badge.count)
        is WidgetBadge.TodayAt ->
            context.getString(
                R.string.widget_today_at,
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(res.configuration.locales[0]).format(badge.time),
            )
        is WidgetBadge.DaysAgo -> res.getQuantityString(R.plurals.detail_days_ago, badge.count.toInt(), badge.count)
        WidgetBadge.Today -> context.getString(R.string.badge_today)
        WidgetBadge.Ongoing -> context.getString(R.string.badge_ongoing)
    }
}
