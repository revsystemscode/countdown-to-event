package com.revsystems.countdowntoevent

import android.app.Application
import com.revsystems.countdowntoevent.data.settings.LockScreenStore
import com.revsystems.countdowntoevent.di.ApplicationScope
import com.revsystems.countdowntoevent.domain.repository.EventRepository
import com.revsystems.countdowntoevent.domain.settings.SettingsRepository
import com.revsystems.countdowntoevent.notifications.LockScreenController
import com.revsystems.countdowntoevent.notifications.LockScreenNotifier
import com.revsystems.countdowntoevent.notifications.ReminderNotifier
import com.revsystems.countdowntoevent.notifications.ReminderScheduler
import com.revsystems.countdowntoevent.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CountdownApp : Application() {
    @Inject lateinit var repository: EventRepository

    @Inject lateinit var settings: SettingsRepository

    @Inject lateinit var widgetUpdater: WidgetUpdater

    @Inject lateinit var reminderScheduler: ReminderScheduler

    @Inject lateinit var notifier: ReminderNotifier

    @Inject lateinit var lockScreenStore: LockScreenStore

    @Inject lateinit var lockScreenNotifier: LockScreenNotifier

    @Inject lateinit var lockScreen: LockScreenController

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        notifier.createChannels()
        lockScreenNotifier.createChannel()
        // Any change to events (create, edit, delete, undo) redraws widgets and replans reminders.
        // The first emission also covers alarms missed while the process wasn't running.
        applicationScope.launch {
            repository.observeEvents().collectLatest {
                reminderScheduler.reschedule()
                widgetUpdater.refreshAll()
            }
        }
        // Pinning, unpinning or editing the pinned event redraws the lock-screen notification.
        applicationScope.launch {
            combine(repository.observeEvents(), lockScreenStore.pinnedEventId) { _, pinned -> pinned }
                .collectLatest { lockScreen.refresh() }
        }
        applicationScope.launch {
            settings.settings
                .map { it.reminderOffset to it.notifyOnEventDay }
                .distinctUntilChanged()
                .drop(1)
                .collectLatest { reminderScheduler.restartFromNow() }
        }
    }
}
