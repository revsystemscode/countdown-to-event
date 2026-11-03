package com.revsystems.countdowntoevent

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revsystems.countdowntoevent.domain.settings.SettingsRepository
import com.revsystems.countdowntoevent.domain.settings.ThemeMode
import com.revsystems.countdowntoevent.ui.CountdownAppRoot
import com.revsystems.countdowntoevent.ui.navigation.LaunchRequest
import com.revsystems.countdowntoevent.ui.theme.CountdownTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Only on a fresh start: after rotation the back stack is already restored.
        val launchRequest = if (savedInstanceState == null) LaunchRequest.from(intent) else null
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val systemDark = isSystemInDarkTheme()
            val dark =
                when (settings?.themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM, null -> systemDark
                }
            // Status bar icons must follow the app theme, not the system one.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose { }
            }
            CountdownTheme(darkTheme = dark) {
                CountdownAppRoot(launchRequest)
            }
        }
    }
}
