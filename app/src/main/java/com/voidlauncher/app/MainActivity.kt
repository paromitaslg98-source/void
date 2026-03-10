package com.voidlauncher.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.voidlauncher.app.ui.screen.AppDrawerScreen
import com.voidlauncher.app.ui.screen.HomeScreen
import com.voidlauncher.app.ui.screen.NotesScreen
import com.voidlauncher.app.ui.screen.NotificationsScreen
import com.voidlauncher.app.ui.screen.SettingsScreen
import com.voidlauncher.app.ui.theme.VoidAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainUiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoidAppTheme {
                val navController = rememberNavController()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                NavHost(navController = navController, startDestination = HomeRoute) {
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
