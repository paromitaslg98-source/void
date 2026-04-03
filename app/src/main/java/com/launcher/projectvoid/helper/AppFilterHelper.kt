package com.launcher.projectvoid.helper

import com.launcher.projectvoid.data.AppModel

interface AppFilterHelper {
    fun onAppFiltered(items:List<AppModel>)
}