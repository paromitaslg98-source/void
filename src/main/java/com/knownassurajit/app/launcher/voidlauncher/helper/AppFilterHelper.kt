package com.knownassurajit.app.launcher.voidlauncher.helper

import com.knownassurajit.app.launcher.voidlauncher.data.AppModel

interface AppFilterHelper {
    fun onAppFiltered(items:List<AppModel>)
}