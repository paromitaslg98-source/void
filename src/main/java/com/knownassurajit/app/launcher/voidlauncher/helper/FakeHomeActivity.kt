package com.knownassurajit.app.launcher.voidlauncher.helper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.knownassurajit.app.launcher.voidlauncher.ui.FakeHomeScreen

class FakeHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FakeHomeScreen()
        }
    }
}
