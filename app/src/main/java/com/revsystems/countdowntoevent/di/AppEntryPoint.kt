package com.revsystems.countdowntoevent.di

import android.content.Context
import com.revsystems.countdowntoevent.domain.repository.EventRepository
import com.revsystems.countdowntoevent.notifications.ReminderScheduler
import com.revsystems.countdowntoevent.widget.WidgetRefreshScheduler
import com.revsystems.countdowntoevent.widget.WidgetUpdater
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import java.time.Clock

/** Widgets and broadcast receivers are created by the system, so they fetch dependencies here. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun eventRepository(): EventRepository

    fun clock(): Clock

    fun widgetUpdater(): WidgetUpdater

    fun widgetRefreshScheduler(): WidgetRefreshScheduler

    fun reminderScheduler(): ReminderScheduler

    @ApplicationScope
    fun applicationScope(): CoroutineScope
}

fun Context.appEntryPoint(): AppEntryPoint = EntryPointAccessors.fromApplication(applicationContext, AppEntryPoint::class.java)
