package com.launcher.void.helper

object AppLifecycleState {
    @Volatile
    var isAppInForeground: Boolean = false
}
