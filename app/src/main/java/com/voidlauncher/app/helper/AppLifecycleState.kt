package com.voidlauncher.app.helper

object AppLifecycleState {
    @Volatile
    var isAppInForeground: Boolean = false
}
