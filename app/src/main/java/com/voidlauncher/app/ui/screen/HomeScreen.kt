package com.voidlauncher.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.voidlauncher.app.MainUiState

@Composable
fun HomeScreen(
    state: MainUiState,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenNotes: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(state.title)
        Text(state.subtitle)
        Button(onClick = onOpenApps) { Text("App Drawer") }
        Button(onClick = onOpenSettings) { Text("Settings") }
        Button(onClick = onOpenNotifications) { Text("Notifications") }
        Button(onClick = onOpenNotes) { Text("Notes") }
    }
}
