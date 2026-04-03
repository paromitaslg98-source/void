package com.launcher.projectvoid.helper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.launcher.projectvoid.ui.FakeHomeScreen

class FakeHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FakeHomeScreen()
        }
    }
}
