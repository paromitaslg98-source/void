package com.launcher.void.helper

import com.launcher.void.data.AppModel

interface AppFilterHelper {
    fun onAppFiltered(items:List<AppModel>)
}