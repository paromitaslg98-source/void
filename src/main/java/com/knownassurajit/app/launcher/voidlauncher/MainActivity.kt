package com.knownassurajit.app.launcher.voidlauncher

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.knownassurajit.app.launcher.voidlauncher.data.AppModel
import com.knownassurajit.app.launcher.voidlauncher.helper.NotificationService
import com.knownassurajit.app.launcher.voidlauncher.helper.getUserHandleFromString
import com.knownassurajit.app.launcher.voidlauncher.ui.screen.AppDrawerScreen
import com.knownassurajit.app.launcher.voidlauncher.ui.screen.HomeScreen
import com.knownassurajit.app.launcher.voidlauncher.ui.screen.NotesScreen
import com.knownassurajit.app.launcher.voidlauncher.ui.screen.NotificationSummaryScreen
import com.knownassurajit.app.launcher.voidlauncher.ui.screen.NotificationsScreen
import com.knownassurajit.app.launcher.voidlauncher.ui.screen.SettingsScreen
import com.knownassurajit.app.launcher.voidlauncher.ui.screen.WidgetsScreen
import com.knownassurajit.app.launcher.voidlauncher.ui.theme.VoidAppTheme

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

/**
 * Best-effort access to the hidden `StatusBarManager.expandNotificationsPanel` method.
 * The class and method are on Android's non-SDK greylist and may be blocked on
 * `targetSdk` 28+. We cache the result of the first lookup so we don't repeatedly
 * pay the reflection cost — and so a single failure permanently disables the path
 * instead of spamming the log on every gesture.
 */
private object StatusBarPanelOpener {
    private const val TAG = "StatusBarPanelOpener"

    @Volatile private var resolved = false
    @Volatile private var method: java.lang.reflect.Method? = null

    fun expandNotificationsPanel(context: Context) {
        if (!resolved) synchronized(this) {
            if (!resolved) {
                method = try {
                    @Suppress("PrivateApi")
                    Class.forName("android.app.StatusBarManager")
                        .getMethod("expandNotificationsPanel")
                } catch (t: Throwable) {
                    Log.w(TAG, "StatusBarManager.expandNotificationsPanel unavailable: ${t.javaClass.simpleName}")
                    null
                }
                resolved = true
            }
        }
        val m = method ?: return
        try {
            m.invoke(context.getSystemService("statusbar"))
        } catch (t: Throwable) {
            Log.w(TAG, "expandNotificationsPanel invoke failed: ${t.javaClass.simpleName}")
        }
    }
}

/** Physical status bar height, available to all screens regardless of bar visibility. */
val LocalFixedStatusBarHeight = compositionLocalOf<Dp> { 24.dp }

class MainActivity : ComponentActivity() {
    private val appReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            context?.let { ctx ->
                lifecycleScope.launch { com.knownassurajit.app.launcher.voidlauncher.helper.AppCacheManager.syncCache(ctx) }
            }
        }
    }

    private val viewModel: MainUiViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            com.knownassurajit.app.launcher.voidlauncher.helper.AppCacheManager.initializeCache(this@MainActivity)
        }

        val pkgFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        registerReceiver(appReceiver, pkgFilter)

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            VoidAppTheme(appFont = uiState.appFont) {
                // Capture the physical status bar height — this returns the
                // hardware inset even before the bar is programmatically hidden.
                val density = LocalDensity.current
                val statusBarHeightDp = with(density) {
                    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
                    val heightPx = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
                    if (heightPx > 0) (heightPx / density.density).dp else 24.dp
                }

                CompositionLocalProvider(LocalFixedStatusBarHeight provides statusBarHeightDp) {
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val notifications by NotificationService.notificationsState
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val allowTopEdgeNotificationExpansion =
                    uiState.showStatusBar && (currentBackStackEntry?.isRoute("HomeRoute") == true)

                LaunchedEffect(uiState.showStatusBar) {
                    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                    if (uiState.showStatusBar) {
                        windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
                        windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                    } else {
                        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
                        windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(uiState.showStatusBar) {
                            awaitEachGesture {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                val isTopEdge = down.position.y < size.height * 0.1f
                                var totalY = 0f
                                var totalX = 0f
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull() ?: break
                                    val deltaX = change.position.x - change.previousPosition.x
                                    val deltaY = change.position.y - change.previousPosition.y
                                    totalX += deltaX
                                    totalY += deltaY
                                    if (uiState.showStatusBar && isTopEdge && totalY > 120f) {
                                        StatusBarPanelOpener.expandNotificationsPanel(this@MainActivity)
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
                            onOpenApps = { navController.navigate(AppDrawerRoute) { launchSingleTop = true } },
                            onOpenSettings = { navController.navigate(SettingsRoute) { launchSingleTop = true } },
                            onOpenNotifications = {
                                if (uiState.showStatusBar) {
                                    StatusBarPanelOpener.expandNotificationsPanel(this@MainActivity)
                                }
                            },
                            onOpenNotificationSummary = { navController.navigate(NotificationSummaryRoute) { launchSingleTop = true } },
                            onOpenWidgets = { navController.navigate(WidgetsRoute) { launchSingleTop = true } },
                            onOpenNotes = { navController.navigate(NotesRoute) { launchSingleTop = true } },
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
                                mainViewModel.selectedApp(app, com.knownassurajit.app.launcher.voidlauncher.data.Constants.FLAG_LAUNCH_APP)
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
                            notifications = notifications,
                            onBack = { navController.popBackStack() }
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
            } // end CompositionLocalProvider
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshFromPrefs()
        lifecycleScope.launch { com.knownassurajit.app.launcher.voidlauncher.helper.AppCacheManager.syncCache(this@MainActivity) }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(appReceiver)
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
        mainViewModel.selectedApp(appModel, com.knownassurajit.app.launcher.voidlauncher.data.Constants.FLAG_LAUNCH_APP)
    }

}

private fun navigateHome(navController: NavHostController) {
    navController.navigate(HomeRoute) {
        launchSingleTop = true
        popUpTo(HomeRoute) { inclusive = false }
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
        // We only evaluate swipe-driven horizontal transitions when the swipe action actually maps
        // to a supported route. This avoids accidentally matching everything when the action is unsupported.
        leftRoute != null && targetState.isRoute(leftRoute) ->
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) + fadeIn(tween(220))
        rightRoute != null && targetState.isRoute(rightRoute) ->
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
        leftRoute != null && initialState.isRoute(leftRoute) ->
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) + fadeOut(tween(200))
        rightRoute != null && initialState.isRoute(rightRoute) ->
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) + fadeOut(tween(200))
        else -> fadeOut(animationSpec = tween(200))
    }
}

private fun actionToRouteName(action: String): String? = when (action) {
    com.knownassurajit.app.launcher.voidlauncher.data.Prefs.SwipeAction.NOTIFICATION_SUMMARY -> "NotificationSummaryRoute"
    com.knownassurajit.app.launcher.voidlauncher.data.Prefs.SwipeAction.WIDGETS -> "WidgetsRoute"
    com.knownassurajit.app.launcher.voidlauncher.data.Prefs.SwipeAction.NOTES -> "NotesRoute"
    else -> null
}

private fun NavBackStackEntry.isRoute(name: String?): Boolean {
    if (name.isNullOrBlank()) return false
    return destination.route?.contains(name) == true
}
