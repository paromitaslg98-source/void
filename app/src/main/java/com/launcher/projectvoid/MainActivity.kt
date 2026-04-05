package com.launcher.projectvoid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoidAppTheme {
                val navController = rememberNavController()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val notifications by NotificationService.notificationsState
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val allowTopEdgeNotificationExpansion =
                    uiState.showStatusBar && (currentBackStackEntry?.isRoute("HomeRoute") == true)

                LaunchedEffect(uiState.showStatusBar) {
                    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                    if (uiState.showStatusBar) {
                        windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
                    } else {
                        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(allowTopEdgeNotificationExpansion) {
                            awaitEachGesture {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                val isTopEdge = down.position.y < size.height * 0.1f
                                var totalY = 0f
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull() ?: break
                                    val deltaY = change.position.y - change.previousPosition.y
                                    totalY += deltaY
                                    if (allowTopEdgeNotificationExpansion && isTopEdge && totalY > 120f) {
                                        expandNotificationsPanelIfAllowed(
                                            isAllowed = uiState.showStatusBar
                                        )
                                        break
                                    }
                                    if (!change.pressed) break
                                }
                            }
                        }
                ) {
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
                                expandNotificationsPanelIfAllowed(
                                    isAllowed = uiState.showStatusBar
                                )
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
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshFromPrefs()
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

    private fun expandNotificationsPanelIfAllowed(isAllowed: Boolean) {
        if (!isAllowed) return
        try {
            @Suppress("PrivateApi")
            val sbservice = getSystemService("statusbar")
            val statusbarManager = Class.forName("android.app.StatusBarManager")
            val expands = statusbarManager.getMethod("expandNotificationsPanel")
            expands.invoke(sbservice)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
