package com.launcher.projectvoid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.launcher.projectvoid.data.AppModel
import com.launcher.projectvoid.helper.NotificationService
import com.launcher.projectvoid.helper.getUserHandleFromString
import com.launcher.projectvoid.ui.screen.AppDrawerScreen
import com.launcher.projectvoid.ui.screen.HomeScreen
import com.launcher.projectvoid.ui.screen.NotesScreen
import com.launcher.projectvoid.ui.screen.NotificationSummaryScreen
import com.launcher.projectvoid.ui.screen.NotificationsScreen
import com.launcher.projectvoid.ui.screen.SettingsScreen
import com.launcher.projectvoid.ui.screen.WidgetsScreen
import com.launcher.projectvoid.ui.theme.VoidAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainUiViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoidAppTheme {
                val navController = rememberNavController()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val notifications by NotificationService.notificationsState
                    .collectAsStateWithLifecycle(initialValue = emptyList())

                LaunchedEffect(uiState.showStatusBar) {
                    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                    if (uiState.showStatusBar) {
                        windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
                    } else {
                        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    enterTransition = { directionEnter(initialState, targetState, uiState) },
                    exitTransition = { fadeOut(animationSpec = tween(160)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(160)) },
                    popExitTransition = { directionExit(initialState, targetState, uiState) }
                ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            state = uiState,
                            onOpenApps = { navController.navigate(AppDrawerRoute) },
                            onOpenSettings = { navController.navigate(SettingsRoute) },
                            onOpenNotifications = { 
                                try {
                                    @Suppress("PrivateApi")
                                    val sbservice = getSystemService("statusbar")
                                    val statusbarManager = Class.forName("android.app.StatusBarManager")
                                    val expands = statusbarManager.getMethod("expandNotificationsPanel")
                                    expands.invoke(sbservice)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            onOpenNotificationSummary = { navController.navigate(NotificationSummaryRoute) },
                            onOpenWidgets = { navController.navigate(WidgetsRoute) },
                            onOpenNotes = { navController.navigate(NotesRoute) },
                            onAppClick = { app -> launchHomeApp(app) },
                            onClockClick = { mainViewModel.setDefaultClockApp() },
                            onDateClick = { /* open calendar */ },
                            onHomeAppsChanged = { viewModel.refreshFromPrefs() }
                        )
                    }
                    composable<AppDrawerRoute> {
                        AppDrawerScreen(
                            onBack = { navController.popBackStack() },
                            onAppClick = { app ->
                                mainViewModel.selectedApp(app, com.launcher.projectvoid.data.Constants.FLAG_LAUNCH_APP)
                            },
                            onOpenSettings = { navController.navigate(SettingsRoute) }
                        )
                    }
                    composable<SettingsRoute> {
                        SettingsScreen(onBack = {
                            viewModel.refreshFromPrefs()
                            navController.popBackStack()
                        })
                    }
                    composable<NotificationPanelRoute> {
                        NotificationsScreen(
                            onBack = { navController.popBackStack() },
                            notifications = notifications
                        )
                    }
                    composable<NotificationSummaryRoute> {
                        NotificationSummaryScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<WidgetsRoute> {
                        WidgetsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<NotesRoute> {
                        NotesScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshFromPrefs()
        mainViewModel.getTodaysScreenTime()
        mainViewModel.screenTimeValue.observe(this) { text ->
            viewModel.updateScreenTime(text)
        }
    }

    private fun launchHomeApp(app: HomeApp) {
        val userHandle = getUserHandleFromString(this, app.userString)
        val appModel = if (app.isShortcut) {
            AppModel.PinnedShortcut(
                appLabel = app.label,
                key = null,
                appPackage = app.packageName,
                shortcutId = app.shortcutId,
                user = userHandle
            )
        } else {
            AppModel.App(
                appLabel = app.label,
                key = null,
                appPackage = app.packageName,
                activityClassName = app.activityClassName,
                user = userHandle
            )
        }
        mainViewModel.selectedApp(appModel, com.launcher.projectvoid.data.Constants.FLAG_LAUNCH_APP)
    }
}

// ── Direction-aware transitions ──

private fun AnimatedContentTransitionScope<NavBackStackEntry>.directionEnter(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry,
    uiState: MainUiState
): androidx.compose.animation.EnterTransition {
    val leftRoute = actionToRouteName(uiState.leftSwipeAction)
    val rightRoute = actionToRouteName(uiState.rightSwipeAction)

    return when {
        targetState.isRoute("AppDrawerRoute") ->
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(280)) + fadeIn(tween(220))
        targetState.isRoute("NotificationPanelRoute") ->
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(280)) + fadeIn(tween(220))
        targetState.isRoute(leftRoute) ->
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) + fadeIn(tween(220))
        targetState.isRoute(rightRoute) ->
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) + fadeIn(tween(220))
        else -> fadeIn(animationSpec = tween(200))
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.directionExit(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry,
    uiState: MainUiState
): androidx.compose.animation.ExitTransition {
    val leftRoute = actionToRouteName(uiState.leftSwipeAction)
    val rightRoute = actionToRouteName(uiState.rightSwipeAction)

    return when {
        initialState.isRoute("AppDrawerRoute") ->
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(260)) + fadeOut(tween(200))
        initialState.isRoute("NotificationPanelRoute") ->
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(260)) + fadeOut(tween(200))
        initialState.isRoute(leftRoute) ->
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) + fadeOut(tween(200))
        initialState.isRoute(rightRoute) ->
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) + fadeOut(tween(200))
        else -> fadeOut(animationSpec = tween(200))
    }
}

private fun actionToRouteName(action: String): String = when (action) {
    com.launcher.projectvoid.data.Prefs.SwipeAction.NOTIFICATION_SUMMARY -> "NotificationSummaryRoute"
    com.launcher.projectvoid.data.Prefs.SwipeAction.WIDGETS -> "WidgetsRoute"
    com.launcher.projectvoid.data.Prefs.SwipeAction.NOTES -> "NotesRoute"
    else -> ""
}

private fun NavBackStackEntry.isRoute(name: String): Boolean {
    return destination.route?.contains(name) == true
}
