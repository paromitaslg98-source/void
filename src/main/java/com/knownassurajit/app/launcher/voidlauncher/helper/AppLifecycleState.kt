package com.knownassurajit.app.launcher.voidlauncher.helper

object AppLifecycleState {
    @Volatile
    var isAppInForeground: Boolean = false
}
