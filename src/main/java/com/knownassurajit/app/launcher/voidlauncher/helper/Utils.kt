package com.knownassurajit.app.launcher.voidlauncher.helper

import android.annotation.SuppressLint
import android.app.SearchManager
import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.knownassurajit.app.launcher.voidlauncher.BuildConfig
import com.knownassurajit.app.launcher.voidlauncher.R
import com.knownassurajit.app.launcher.voidlauncher.data.AppModel
import com.knownassurajit.app.launcher.voidlauncher.data.Constants
import com.knownassurajit.app.launcher.voidlauncher.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.Collator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Scanner
import kotlin.math.pow
import kotlin.math.sqrt

fun Context.showToast(message: String?, duration: Int = Toast.LENGTH_SHORT) {
    if (message.isNullOrBlank()) return
    Toast.makeText(this, message, duration).show()
}

fun Context.showToast(stringResource: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, getString(stringResource), duration).show()
}

suspend fun getAppsList(
    context: Context,
    prefs: Prefs,
    includeRegularApps: Boolean = true,
    includeHiddenApps: Boolean = false,
): MutableList<AppModel> {
    return withContext(Dispatchers.IO) {
        val appList: MutableList<AppModel> = mutableListOf()

        try {
            if (!Prefs(context).hiddenAppsUpdated) upgradeHiddenApps(Prefs(context))
            val hiddenApps = Prefs(context).hiddenApps

            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            val launcherApps =
                context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val collator = Collator.getInstance()

            for (profile in userManager.userProfiles) {
                // If the profile is a Private Space, skip adding its apps to the regular mixed list.
                // We handle these separately via explicit querying when the Private Space section is unlocked.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    val userInfo = launcherApps.getLauncherUserInfo(profile)
                    if (userInfo != null && userInfo.userType == UserManager.USER_TYPE_PROFILE_PRIVATE) {
                        continue
                    }
                }

                for (app in launcherApps.getActivityList(null, profile)) {
                    val appLabelShown = prefs.getAppRenameLabel(app.applicationInfo.packageName)
                        .ifBlank { app.label.toString() }
                    val appModel = AppModel.App(
                        appLabel = appLabelShown,
                        key = collator.getCollationKey(app.label.toString()),
                        appPackage = app.applicationInfo.packageName,
                        activityClassName = app.componentName.className,
                        isNew = (System.currentTimeMillis() - app.firstInstallTime) < Constants.ONE_HOUR_IN_MILLIS,
                        user = profile
                    )

                    // if the current app is not VOID Launcher
                    if (app.applicationInfo.packageName != BuildConfig.APPLICATION_ID) {
                        // is this a hidden app?
                        if (hiddenApps.contains(app.applicationInfo.packageName + "|" + profile.toString())) {
                            if (includeHiddenApps) {
                                appList.add(appModel)
                            }
                        } else {
                            // this is a regular app
                            if (includeRegularApps) {
                                appList.add(appModel)
                            }
                        }
                    }
                }
            }

            // Add shortcuts if we're getting regular apps
            if (includeRegularApps && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pinned = try {
                    getPinnedShortcuts(context, prefs, collator)
                } catch (e: Exception) {
                    emptyList()
                }
                appList.addAll(pinned)
            }

            appList.sortWith(compareBy(collator) { it.appLabel })
        } catch (e: Exception) {
            e.printStackTrace()
        }
        appList
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private suspend fun getPinnedShortcuts(
    context: Context,
    prefs: Prefs,
    collator: Collator,
): List<AppModel.PinnedShortcut> =
    withContext(Dispatchers.IO) {
        val pinnedShortcuts = mutableListOf<AppModel.PinnedShortcut>()
        val shortcuts = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        if (shortcuts?.hasShortcutHostPermission() == true) {
            val query = LauncherApps.ShortcutQuery().apply {
                setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            }
            shortcuts.profiles.forEach { profile ->
                try {
                    shortcuts.getShortcuts(query, profile)?.forEach { shortcut ->
                        if (shortcut.isPinned && pinnedShortcuts.none { it.shortcutId == shortcut.id }) {
                            val label = prefs.getAppRenameLabel(shortcut.id)
                                .takeIf { it.isNotBlank() }
                                ?: shortcut.shortLabel?.toString()
                                ?: shortcut.longLabel?.toString().orEmpty()
                            pinnedShortcuts.add(
                                AppModel.PinnedShortcut(
                                    appLabel = label,
                                    key = collator.getCollationKey(label),
                                    appPackage = shortcut.`package`,
                                    shortcutId = shortcut.id,
                                    isNew = false,
                                    user = profile
                                )
                            )
                        }
                    }
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        }
        pinnedShortcuts
    }

// This is to ensure backward compatibility with older app versions
// which did not support multiple user profiles
private fun upgradeHiddenApps(prefs: Prefs) {
    val hiddenAppsSet = prefs.hiddenApps
    val newHiddenAppsSet = mutableSetOf<String>()
    for (hiddenPackage in hiddenAppsSet) {
        if (hiddenPackage.contains("|")) newHiddenAppsSet.add(hiddenPackage)
        else newHiddenAppsSet.add(hiddenPackage + android.os.Process.myUserHandle().toString())
    }
    prefs.hiddenApps = newHiddenAppsSet
    prefs.hiddenAppsUpdated = true
}

fun isPackageInstalled(context: Context, packageName: String, userString: String): Boolean {
    val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val activityInfo = launcher.getActivityList(packageName, getUserHandleFromString(context, userString))
    if (activityInfo.size > 0) return true
    return false
}

fun getUserHandleFromString(context: Context, userHandleString: String): UserHandle {
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    for (userHandle in userManager.userProfiles) {
        if (userHandle.toString() == userHandleString) {
            return userHandle
        }
    }
    return android.os.Process.myUserHandle()
}

fun isVoidDefault(context: Context): Boolean {
    val launcherPackageName = getDefaultLauncherPackage(context)
    return BuildConfig.APPLICATION_ID == launcherPackageName
}

fun getDefaultLauncherPackage(context: Context): String {
    val intent = Intent()
    intent.action = Intent.ACTION_MAIN
    intent.addCategory(Intent.CATEGORY_HOME)
    val packageManager = context.packageManager
    val result = packageManager.resolveActivity(intent, 0)
    return if (result?.activityInfo != null) {
        result.activityInfo.packageName
    } else "android"
}


fun openSearch(context: Context) {
    val intent = Intent(Intent.ACTION_WEB_SEARCH)
    intent.putExtra(SearchManager.QUERY, "")
    context.startActivity(intent)
}

@SuppressLint("WrongConstant", "PrivateApi")
fun expandNotificationDrawer(context: Context) {
    // Source: https://stackoverflow.com/a/51132142
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandNotificationsPanel")
        method.invoke(statusBarService)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun openDialerApp(context: Context) {
    try {
        val sendIntent = Intent(Intent.ACTION_DIAL)
        context.startActivity(sendIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun openCameraApp(context: Context) {
    try {
        val sendIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        context.startActivity(sendIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun openAlarmApp(context: Context) {
    try {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.d("TAG", e.toString())
    }
}

fun openCalendar(context: Context) {
    try {
        val calendarUri = CalendarContract.CONTENT_URI
            .buildUpon()
            .appendPath("time")
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, calendarUri))
    } catch (e: Exception) {
        try {
            @SuppressLint("UnsafeImplicitIntentLaunch")
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_CALENDAR)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun isAccessServiceEnabled(context: Context): Boolean {
    val enabled = try {
        Settings.Secure.getInt(context.applicationContext.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
    } catch (e: Exception) {
        0
    }
    if (enabled == 1) {
        val enabledServicesString: String? = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServicesString?.contains(context.packageName + "/" + MyAccessibilityService::class.java.name) ?: false
    }
    return false
}

fun isTablet(context: Context): Boolean {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(metrics)
    val widthInches = metrics.widthPixels / metrics.xdpi
    val heightInches = metrics.heightPixels / metrics.ydpi
    val diagonalInches = sqrt(widthInches.toDouble().pow(2.0) + heightInches.toDouble().pow(2.0))
    if (diagonalInches >= 7.0) return true
    return false
}

fun Context.isDarkThemeOn(): Boolean {
    return resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
}

fun Context.openUrl(url: String) {
    if (url.isEmpty()) return
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(url)
    startActivity(intent)
}

fun Context.isSystemApp(packageName: String): Boolean {
    if (packageName.isBlank()) return true
    return try {
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        ((applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0)
                || (applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0))
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun Context.uninstall(packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE)
    intent.data = Uri.parse("package:$packageName")
    startActivity(intent)
}

@ColorInt
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true,
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

fun Context.shareApp() {
    val message = getString(R.string.are_you_using_your_phone_or_is_your_phone_using_you) +
            "\n" + Constants.URL_VOID_PLAY_STORE
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, message)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    startActivity(shareIntent)
}

fun Context.rateApp() {
    val intent = Intent(
        Intent.ACTION_VIEW,
        Constants.URL_VOID_PLAY_STORE.toUri()
    )
    var flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
    flags = flags or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
    intent.addFlags(flags)
    startActivity(intent)
}

@RequiresApi(Build.VERSION_CODES.N_MR1)
fun Context.deletePinnedShortcut(packageName: String, shortcutIdToDelete: String, user: UserHandle) {
    val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    // 1. Query for existing pinned shortcuts for the package
    val query = LauncherApps.ShortcutQuery().apply {
        setPackage(packageName)
        // Query only for pinned shortcuts
        setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
    }

    try {
        val pinnedShortcuts = launcherApps.getShortcuts(query, user)

        if (pinnedShortcuts != null) {
            // 2. Filter out the shortcut to be deleted
            val updatedPinnedIds = pinnedShortcuts
                .filter { it.id != shortcutIdToDelete }
                .map { it.id }

            // 3. Re-pin the remaining shortcuts
            // This replaces the existing set of pinned shortcuts for this package
            launcherApps.pinShortcuts(packageName, updatedPinnedIds, user)
        }
    } catch (e: SecurityException) {
        // Handle cases where the app doesn't have permission
        // (e.g., not the default launcher or active voice interaction service)
        Log.e("ShortcutHelper", "Permission denied to modify pinned shortcuts for $packageName", e)
    } catch (e: IllegalStateException) {
        // Handle cases where the user profile is locked or not running
        Log.e("ShortcutHelper", "User profile unavailable for modifying pinned shortcuts for $packageName", e)
    } catch (e: Exception) {
        // Handle other potential exceptions (like RemoteException wrapped)
        Log.e("ShortcutHelper", "Failed to modify pinned shortcuts for $packageName", e)
    }
}
