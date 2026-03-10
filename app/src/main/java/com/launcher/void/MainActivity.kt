package com.launcher.void

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.launcher.void.ui.screen.AppDrawerScreen
import com.launcher.void.ui.screen.HomeScreen
import com.launcher.void.ui.screen.NotesScreen
import com.launcher.void.ui.screen.NotificationsScreen
import com.launcher.void.ui.screen.SettingsScreen
import com.launcher.void.ui.theme.VoidAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainUiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoidAppTheme {
                val navController = rememberNavController()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    enterTransition = { forwardEnterTransition(initialState, targetState) },
                    exitTransition = { fadeOut(animationSpec = tween(160)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(160)) },
                    popExitTransition = { backwardExitTransition(initialState, targetState) }
                ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            state = uiState,
                            onOpenApps = { navController.navigate(AppDrawerRoute) },
                            onOpenSettings = { navController.navigate(SettingsRoute) },
                            onOpenNotifications = { navController.navigate(NotificationsRoute) },
                            onOpenNotes = { navController.navigate(NotesRoute) }
                        )
                    }
                    composable<AppDrawerRoute> {
                        AppDrawerScreen(onBack = { navController.popBackStack() })
                    }
                    composable<SettingsRoute> {
                        SettingsScreen(onBack = { navController.popBackStack() })
                    }
                    composable<NotificationsRoute> {
                        NotificationsScreen(onBack = { navController.popBackStack() })
                    }
                    composable<NotesRoute> {
                        NotesScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardEnterTransition(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry
) = when {
    initialState.destination.route?.contains("HomeRoute") == true &&
        targetState.destination.route?.contains("NotesRoute") == true -> {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) + fadeIn(tween(220))
    }

    initialState.destination.route?.contains("HomeRoute") == true &&
        targetState.destination.route?.contains("NotificationsRoute") == true -> {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) + fadeIn(tween(220))
    }

    else -> fadeIn(animationSpec = tween(180))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.backwardExitTransition(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry
) = when {
    initialState.destination.route?.contains("NotesRoute") == true &&
        targetState.destination.route?.contains("HomeRoute") == true -> {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(240)) + fadeOut(tween(180))
    }

    initialState.destination.route?.contains("NotificationsRoute") == true &&
        targetState.destination.route?.contains("HomeRoute") == true -> {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(240)) + fadeOut(tween(180))
    }

    else -> fadeOut(animationSpec = tween(180))
}
