package com.launcher.void.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var darkThemeEnabled by remember { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings")
        Switch(checked = darkThemeEnabled, onCheckedChange = { darkThemeEnabled = it })
        Button(onClick = onBack) { Text("Back") }
    }
}
