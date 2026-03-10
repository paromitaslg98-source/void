package com.launcher.void.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppDrawerScreen(onBack: () -> Unit) {
    val apps = listOf("Camera", "Clock", "Settings", "Phone", "Messages")
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("App Drawer")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items = apps, key = { it }) { appName ->
                Text(appName)
            }
        }
        Button(onClick = onBack) { Text("Back") }
    }
}
