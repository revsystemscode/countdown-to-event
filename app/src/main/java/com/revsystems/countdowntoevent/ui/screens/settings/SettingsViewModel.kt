package com.revsystems.countdowntoevent.ui.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revsystems.countdowntoevent.data.backup.BackupManager
import com.revsystems.countdowntoevent.data.backup.ImportResult
import com.revsystems.countdowntoevent.data.backup.InvalidBackupException
import com.revsystems.countdowntoevent.domain.settings.AppSettings
import com.revsystems.countdowntoevent.domain.settings.ReminderOffset
import com.revsystems.countdowntoevent.domain.settings.SettingsRepository
import com.revsystems.countdowntoevent.domain.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Outcome of a backup action, shown once in a snackbar. */
sealed interface BackupMessage {
    data class Exported(
        val count: Int,
    ) : BackupMessage

    data class Imported(
        val result: ImportResult,
    ) : BackupMessage

    data object NotABackup : BackupMessage

    data object Failed : BackupMessage
}

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val repository: SettingsRepository,
        private val backupManager: BackupManager,
    ) : ViewModel() {
        private val _backupMessage = MutableStateFlow<BackupMessage?>(null)
        val backupMessage: StateFlow<BackupMessage?> = _backupMessage.asStateFlow()

        private val _backupBusy = MutableStateFlow(false)
        val backupBusy: StateFlow<Boolean> = _backupBusy.asStateFlow()

        /** null until loaded, so the screen never flashes default values. */
        val settings: StateFlow<AppSettings?> =
            repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

        fun setReminderOffset(offset: ReminderOffset) {
            viewModelScope.launch { repository.setReminderOffset(offset) }
        }

        fun setNotifyOnEventDay(enabled: Boolean) {
            viewModelScope.launch { repository.setNotifyOnEventDay(enabled) }
        }

        fun setThemeMode(mode: ThemeMode) {
            viewModelScope.launch { repository.setThemeMode(mode) }
        }

        fun export(destination: String) = runBackup { BackupMessage.Exported(backupManager.export(destination)) }

        fun import(source: String) = runBackup { BackupMessage.Imported(backupManager.import(source)) }

        fun backupMessageShown() {
            _backupMessage.value = null
        }

        private fun runBackup(action: suspend () -> BackupMessage) {
            if (_backupBusy.value) return
            _backupBusy.value = true
            viewModelScope.launch {
                _backupMessage.value =
                    try {
                        action()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: InvalidBackupException) {
                        Log.w(TAG, "Rejected backup", e)
                        BackupMessage.NotABackup
                    } catch (e: Exception) {
                        // Storage errors (disk full, provider gone): report, never crash.
                        Log.w(TAG, "Backup failed", e)
                        BackupMessage.Failed
                    } finally {
                        _backupBusy.value = false
                    }
            }
        }

        suspend fun notificationPermissionAsked() = repository.notificationPermissionAsked()

        suspend fun markNotificationPermissionAsked() = repository.markNotificationPermissionAsked()

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
            const val TAG = "Settings"
        }
    }
