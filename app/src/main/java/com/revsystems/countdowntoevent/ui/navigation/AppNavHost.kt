package com.revsystems.countdowntoevent.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.revsystems.countdowntoevent.ui.screens.detail.EventDetailScreen
import com.revsystems.countdowntoevent.ui.screens.editor.EventEditorScreen
import com.revsystems.countdowntoevent.ui.screens.home.HomeScreen
import com.revsystems.countdowntoevent.ui.screens.settings.SettingsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    launchRequest: LaunchRequest? = null,
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onAddEvent = { navController.navigate(EventEditorRoute()) },
                onOpenEvent = { id -> navController.navigate(EventDetailRoute(id)) },
                onEditEvent = { id -> navController.navigate(EventEditorRoute(id)) },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen()
        }
        // ViewModels read their route arguments from SavedStateHandle.
        composable<EventEditorRoute> { entry ->
            EventEditorScreen(onClose = { navController.popIfCurrent(entry) })
        }
        composable<EventDetailRoute> { entry ->
            EventDetailScreen(
                onBack = { navController.popIfCurrent(entry) },
                onEdit = { id -> navController.navigate(EventEditorRoute(id)) },
            )
        }
    }

    // Runs after NavHost has set its graph.
    LaunchedEffect(launchRequest) {
        when (launchRequest) {
            is LaunchRequest.OpenEvent -> navController.navigate(EventDetailRoute(launchRequest.eventId))
            LaunchRequest.NewEvent -> navController.navigate(EventEditorRoute())
            null -> Unit
        }
    }
}

/** Ignores repeated close requests (e.g. "saved" and a back tap at once) so Home is never popped. */
private fun NavHostController.popIfCurrent(entry: NavBackStackEntry) {
    if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) popBackStack()
}
