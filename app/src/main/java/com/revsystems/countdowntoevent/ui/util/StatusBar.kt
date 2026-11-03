package com.revsystems.countdowntoevent.ui.util

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat

/** Forces light status bar icons while this screen is shown (for screens over dark images). */
@Composable
fun LightStatusBarIcons() {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}
