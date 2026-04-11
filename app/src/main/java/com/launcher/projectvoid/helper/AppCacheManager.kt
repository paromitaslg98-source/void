package com.launcher.projectvoid.helper

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.launcher.projectvoid.data.AppModel
import com.launcher.projectvoid.data.Prefs
import java.io.File

object AppCacheManager {
    private val _appCacheFlow = MutableStateFlow<List<AppModel>>(emptyList())
    val appCacheFlow: StateFlow<List<AppModel>> = _appCacheFlow.asStateFlow()

    private const val CACHE_FILE_NAME = "apps_cache.json"

    suspend fun initializeCache(context: Context) {
        withContext(Dispatchers.IO) {
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            if (cacheFile.exists()) {
                try {
                    val jsonStr = cacheFile.readText()
                    val jsonArray = JSONArray(jsonStr)
                    val list = mutableListOf<AppModel>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val type = obj.getString("type")
                        if (type == "App") {
                            list.add(
                                AppModel.App(
                                    appLabel = obj.getString("appLabel"),
                                    appPackage = obj.getString("appPackage"),
                                    activityClassName = obj.getString("activityClassName"),
                                    user = getUserHandleFromString(context, obj.getString("user")),
                                    key = null,
                                    isNew = obj.optBoolean("isNew", false)
                                )
                            )
                        } else if (type == "PinnedShortcut" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            list.add(
                                AppModel.PinnedShortcut(
                                    appLabel = obj.getString("appLabel"),
                                    appPackage = obj.getString("appPackage"),
                                    shortcutId = obj.getString("shortcutId"),
                                    user = getUserHandleFromString(context, obj.getString("user")),
                                    key = null
                                )
                            )
                        }
                    }
                    _appCacheFlow.value = list
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun syncCache(context: Context) = withContext(Dispatchers.IO) {
        try {
            val prefs = Prefs(context)
            val freshApps: List<AppModel> = getAppsList(context, prefs, includeRegularApps = true, includeHiddenApps = false)
            _appCacheFlow.value = freshApps

            // Serialize to file
            val jsonArray = JSONArray()
            freshApps.forEach { app ->
                val obj = JSONObject()
                obj.put("appLabel", app.appLabel)
                obj.put("appPackage", app.appPackage)
                obj.put("user", app.user.toString())
                when (app) {
                    is AppModel.App -> {
                        obj.put("type", "App")
                        obj.put("activityClassName", app.activityClassName)
                        obj.put("isNew", app.isNew)
                    }
                    is AppModel.PinnedShortcut -> {
                        obj.put("type", "PinnedShortcut")
                        obj.put("shortcutId", app.shortcutId)
                    }
                }
                jsonArray.put(obj)
            }
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            cacheFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
