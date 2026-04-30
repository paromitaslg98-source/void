package com.knownassurajit.app.launcher.voidlauncher

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.knownassurajit.app.launcher.voidlauncher.helper.AppLifecycleState

class VOIDApp : Application(), DefaultLifecycleObserver {

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        AppLifecycleState.isAppInForeground = true
    }

    override fun onStop(owner: LifecycleOwner) {
        AppLifecycleState.isAppInForeground = false
    }
}
