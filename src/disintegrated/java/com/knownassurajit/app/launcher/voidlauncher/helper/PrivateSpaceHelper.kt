package com.knownassurajit.app.launcher.voidlauncher.helper

import android.content.Context
import com.knownassurajit.app.launcher.voidlauncher.data.AppModel
import com.knownassurajit.app.launcher.voidlauncher.data.Prefs

object PrivateSpaceHelper {
    fun getPrivateSpaceProfile(context: Context): android.os.UserHandle? {
        return null
    }

    fun togglePrivateSpace(context: Context) {
        // Do nothing in disintegrated build
    }

    suspend fun loadPrivateSpaceApps(
        context: Context, prefs: Prefs
    ): List<AppModel> {
        return emptyList()
    }
}
