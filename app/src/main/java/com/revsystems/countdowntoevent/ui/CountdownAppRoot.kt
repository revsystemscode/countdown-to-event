package com.revsystems.countdowntoevent.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.revsystems.countdowntoevent.ui.components.AppBottomBar
import com.revsystems.countdowntoevent.ui.navigation.AppNavHost
import com.revsystems.countdowntoevent.ui.navigation.EventEditorRoute
import com.revsystems.countdowntoevent.ui.navigation.HomeRoute
import com.revsystems.countdowntoevent.ui.navigation.LaunchRequest
import com.revsystems.countdowntoevent.ui.navigation.SettingsRoute

@Composable
fun CountdownAppRoot(launchRequest: LaunchRequest? = null) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    val isHome = destination?.hierarchy?.any { it.hasRoute<HomeRoute>() } == true
    val isSettings = destination?.hierarchy?.any { it.hasRoute<SettingsRoute>() } == true

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Each screen handles the status bar itself; this Scaffold only reserves room for the bottom bar.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isHome || isSettings) {
                AppBottomBar(
                    homeSelected = isHome,
                    settingsSelected = isSettings,
                    onHome = { navigateTopLevel(HomeRoute) },
                    onAdd = { navController.navigate(EventEditorRoute()) },
                    onSettings = { navigateTopLevel(SettingsRoute) },
                )
            }
        },
    ) { padding ->
        AppNavHost(
            navController = navController,
            launchRequest = launchRequest,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        )
    }
}
