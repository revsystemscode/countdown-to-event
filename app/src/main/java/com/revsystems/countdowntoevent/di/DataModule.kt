package com.revsystems.countdowntoevent.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.revsystems.countdowntoevent.data.DeviceClock
import com.revsystems.countdowntoevent.data.RoomEventRepository
import com.revsystems.countdowntoevent.data.images.BackgroundImageStore
import com.revsystems.countdowntoevent.data.images.FileBackgroundImageStore
import com.revsystems.countdowntoevent.data.local.AppDatabase
import com.revsystems.countdowntoevent.data.local.EventDao
import com.revsystems.countdowntoevent.data.settings.DataStoreSettingsRepository
import com.revsystems.countdowntoevent.domain.repository.EventRepository
import com.revsystems.countdowntoevent.domain.settings.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun provideEventDao(db: AppDatabase): EventDao = db.eventDao()

    @Provides
    @Singleton
    fun provideClock(): Clock = DeviceClock()

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindEventRepository(impl: RoomEventRepository): EventRepository

    @Binds
    abstract fun bindBackgroundImageStore(impl: FileBackgroundImageStore): BackgroundImageStore

    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
}
