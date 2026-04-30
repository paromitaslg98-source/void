package com.knownassurajit.app.launcher.voidlauncher.helper

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserManager
import com.knownassurajit.app.launcher.voidlauncher.data.AppModel
import com.knownassurajit.app.launcher.voidlauncher.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

object PrivateSpaceHelper {
    fun getPrivateSpaceProfile(context: Context): android.os.UserHandle? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return null
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        val la = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        // Use UserManager.userProfiles instead of la.profiles for better detection of hidden profiles
        return um.userProfiles.firstOrNull { profile ->
            try {
                val info = la.getLauncherUserInfo(profile)
                info?.userType == UserManager.USER_TYPE_PROFILE_PRIVATE
            } catch (_: Exception) { false }
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun togglePrivateSpace(context: Context) {
        val profile = getPrivateSpaceProfile(context) ?: return
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        val locked = um.isQuietModeEnabled(profile)
        
        // On Android 15+, requestQuietModeEnabled handles the biometric/PIN challenge automatically
        um.requestQuietModeEnabled(!locked, profile)
    }

    suspend fun loadPrivateSpaceApps(
        context: Context, prefs: Prefs
    ): List<AppModel> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return@withContext emptyList()
        val pApps = mutableListOf<AppModel>()
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        val la = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val collator = Collator.getInstance()
        // Use UserManager.userProfiles for finding the hidden profile
        for (profile in um.userProfiles) {
            try {
                val info = la.getLauncherUserInfo(profile)
                if (info?.userType != UserManager.USER_TYPE_PROFILE_PRIVATE) continue
                // Only load apps if the profile is NOT in quiet mode (unlocked)
                if (!um.isQuietModeEnabled(profile)) {
                    for (app in la.getActivityList(null, profile)) {
                        val label = prefs.getAppRenameLabel(app.applicationInfo.packageName)
                            .ifBlank { app.label.toString() }
                        pApps.add(
                            AppModel.App(
                                appLabel = label,
                                key = collator.getCollationKey(label),
                                appPackage = app.applicationInfo.packageName,
                                activityClassName = app.componentName.className,
                                isNew = false,
                                user = profile
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }
        pApps.sortWith(compareBy(collator) { it.appLabel })
        pApps
    }
}
