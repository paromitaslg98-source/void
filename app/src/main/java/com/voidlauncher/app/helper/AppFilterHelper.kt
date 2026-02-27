package com.voidlauncher.app.helper

import com.voidlauncher.app.data.AppModel

interface AppFilterHelper {
    fun onAppFiltered(items:List<AppModel>)
}