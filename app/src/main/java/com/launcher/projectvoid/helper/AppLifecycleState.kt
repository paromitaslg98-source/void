package com.launcher.projectvoid.helper

object AppLifecycleState {
    @Volatile
    var isAppInForeground: Boolean = false
}
