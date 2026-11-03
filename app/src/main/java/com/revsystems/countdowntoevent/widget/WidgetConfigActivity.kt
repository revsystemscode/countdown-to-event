package com.revsystems.countdowntoevent.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.revsystems.countdowntoevent.R
import com.revsystems.countdowntoevent.domain.EventClassifier
import com.revsystems.countdowntoevent.domain.EventCountdown
import com.revsystems.countdowntoevent.domain.repository.EventRepository
import com.revsystems.countdowntoevent.ui.format.formatEventDates
import com.revsystems.countdowntoevent.ui.format.titleWithEmoji
import com.revsystems.countdowntoevent.ui.theme.CountdownTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/** Opened by the launcher when adding (optional) or reconfiguring a countdown widget. */
@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {
    @Inject lateinit var repository: EventRepository

    @Inject lateinit var clock: Clock

    @Inject lateinit var widgetUpdater: WidgetUpdater

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out must not add or change the widget.
        setResult(RESULT_CANCELED, resultIntent())
        // The activity is exported for the launcher: only accept our own widgets.
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
        if (info?.provider != ComponentName(this, EventWidgetReceiver::class.java)) {
            finish()
            return
        }
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)

        enableEdgeToEdge()
        setContent {
            CountdownTheme {
                val data by produceState<ConfigData?>(null) {
                    val prefs = getAppWidgetState(this@WidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId)
                    val classified = EventClassifier.classify(repository.getEvents(), clock.instant(), clock.zone)
                    value = ConfigData(WidgetPrefs.config(prefs), classified.upcoming + classified.reached)
                }
                data?.let { loaded ->
                    WidgetConfigScreen(loaded, onClose = ::finish, onDone = { save(glanceId, it) })
                } ?: Column(Modifier.padding(48.dp)) { CircularProgressIndicator() }
            }
        }
    }

    private fun save(
        glanceId: GlanceId,
        config: EventWidgetConfig,
    ) {
        lifecycleScope.launch {
            updateAppWidgetState(this@WidgetConfigActivity, glanceId) { WidgetPrefs.write(it, config) }
            widgetUpdater.refreshEventWidget(glanceId)
            setResult(RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

private data class ConfigData(
    val initial: EventWidgetConfig,
    val events: List<EventCountdown>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    data: ConfigData,
    onClose: () -> Unit,
    onDone: (EventWidgetConfig) -> Unit,
) {
    // A pinned event that no longer exists shows as "next event".
    val initialId = data.initial.eventId?.takeIf { id -> data.events.any { it.event.id == id } }
    var selectedId by remember { mutableStateOf(initialId) }
    var live by remember { mutableStateOf(data.initial.live) }
    val locale = LocalConfiguration.current.locales[0]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_config_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close))
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = { onDone(EventWidgetConfig(eventId = selectedId, live = live)) },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            ) { Text(stringResource(R.string.widget_config_done)) }
        },
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), modifier = Modifier.padding(padding)) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.widget_config_live)) },
                    supportingContent = { Text(stringResource(R.string.widget_config_live_desc)) },
                    trailingContent = { Switch(checked = live, onCheckedChange = null) },
                    modifier = Modifier.toggleable(value = live, role = Role.Switch, onValueChange = { live = it }),
                )
                HorizontalDivider()
                SectionLabel(stringResource(R.string.widget_config_show))
                ChoiceRow(
                    title = stringResource(R.string.widget_config_next),
                    subtitle = stringResource(R.string.widget_config_next_desc),
                    selected = selectedId == null,
                    onClick = { selectedId = null },
                )
            }
            items(data.events, key = { it.event.id }) { item ->
                ChoiceRow(
                    title = titleWithEmoji(item.event),
                    subtitle = formatEventDates(item.event, item.countdown, locale, java.time.ZoneId.systemDefault()),
                    selected = selectedId == item.event.id,
                    onClick = { selectedId = item.event.id },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun ChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}
