package com.launcher.projectvoid.ui.screen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.launcher.projectvoid.R
import com.launcher.projectvoid.data.AppModel
import com.launcher.projectvoid.data.Prefs
import com.launcher.projectvoid.helper.getAppsList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import kotlin.math.abs

@Composable
fun AppDrawerScreen(
    onBack: () -> Unit,
    onAppClick: (AppModel) -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    val allApps = remember { mutableStateListOf<AppModel>() }
    val privateApps = remember { mutableStateListOf<AppModel>() }
    var isPrivateSpaceLocked by remember { mutableStateOf(true) }
    var hasPrivateSpace by remember { mutableStateOf(false) }
    var showAlphabetCategories by remember { mutableStateOf(prefs.showAlphabetCategories) }
    val focusRequester = remember { FocusRequester() }

    // Swipe-down to go back (reverse of swipe-up to open)
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(Unit) {
        scope.launch {
            val apps = getAppsList(context, prefs, includeRegularApps = true, includeHiddenApps = false)
            allApps.clear()
            allApps.addAll(apps)
        }
        if (prefs.privateSpaceEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val profile = getPrivateSpaceProfile(context)
            hasPrivateSpace = profile != null
            if (profile != null) {
                val um = context.getSystemService(Context.USER_SERVICE) as UserManager
                isPrivateSpaceLocked = um.isQuietModeEnabled(profile)
                // KEY FIX: Load private apps immediately if space is already unlocked
                if (!isPrivateSpaceLocked) {
                    scope.launch {
                        val pApps = loadPrivateSpaceApps(context, prefs)
                        privateApps.clear()
                        privateApps.addAll(pApps)
                    }
                }
            }
        } else {
            hasPrivateSpace = false
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (!prefs.privateSpaceEnabled) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    val profile = getPrivateSpaceProfile(context)
                    if (profile != null) {
                        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
                        isPrivateSpaceLocked = um.isQuietModeEnabled(profile)
                        if (!isPrivateSpaceLocked) {
                            scope.launch {
                                val pApps = loadPrivateSpaceApps(context, prefs)
                                privateApps.clear()
                                privateApps.addAll(pApps)
                            }
                        } else {
                            privateApps.clear()
                        }
                    } else {
                        isPrivateSpaceLocked = true
                        privateApps.clear()
                        hasPrivateSpace = false
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            if (Build.VERSION.SDK_INT >= 35) {
                addAction(Intent.ACTION_PROFILE_ACCESSIBLE)
                addAction(Intent.ACTION_PROFILE_INACCESSIBLE)
            }
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // Filter regular apps only (no private apps mixed in)
    val filteredApps = remember(searchQuery, allApps.toList()) {
        val list = allApps.toList()
        val collator = java.text.Collator.getInstance()
        val sorted = list.sortedWith(compareBy(collator) { it.appLabel })
        if (searchQuery.isBlank()) sorted
        else sorted.filter { it.appLabel.contains(searchQuery, ignoreCase = true) }
    }

    // Filter private apps separately
    val filteredPrivateApps = remember(searchQuery, privateApps.toList()) {
        val list = privateApps.toList()
        if (searchQuery.isBlank()) list
        else list.filter { it.appLabel.contains(searchQuery, ignoreCase = true) }
    }

    val showPrivateSpaceInSearch = remember(searchQuery, hasPrivateSpace) {
        hasPrivateSpace && searchQuery.isNotBlank() &&
            "private space".contains(searchQuery, ignoreCase = true)
    }

    val groupedApps = remember(filteredApps) {
        filteredApps.groupBy { it.appLabel.firstOrNull()?.uppercase() ?: "#" }.toSortedMap()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp, bottom = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar row with Settings icon - Responsive and Evenly Spaced
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            stringResource(R.string.search),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Sub-row for icons to maintain consistent distribution
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Private Space lock/unlock toggle (API 35+)
                    if (hasPrivateSpace) {
                        IconButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                                    togglePrivateSpace(context)
                                    isPrivateSpaceLocked = !isPrivateSpaceLocked
                                    if (!isPrivateSpaceLocked) {
                                        scope.launch {
                                            val pApps = loadPrivateSpaceApps(context, prefs)
                                            privateApps.clear()
                                            privateApps.addAll(pApps)
                                        }
                                    } else {
                                        privateApps.clear()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPrivateSpaceLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                                contentDescription = "Private Space",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Settings button (Material Icons)
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            LaunchedEffect(Unit) {
                if (prefs.autoShowKeyboard) focusRequester.requestFocus()
            }

            // App list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Private Space search result — shows when user searches "private"
                if (showPrivateSpaceInSearch) {
                    item(key = "private_search_result") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                                        togglePrivateSpace(context)
                                        isPrivateSpaceLocked = !isPrivateSpaceLocked
                                        if (!isPrivateSpaceLocked) {
                                            scope.launch {
                                                val pApps = loadPrivateSpaceApps(context, prefs)
                                                privateApps.clear()
                                                privateApps.addAll(pApps)
                                            }
                                        } else {
                                            privateApps.clear()
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.private_space),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isPrivateSpaceLocked) "Tap to unlock" else "Tap to lock",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = if (isPrivateSpaceLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                groupedApps.forEach { (letter, apps) ->
                    if (showAlphabetCategories) {
                        item(key = "header_$letter") {
                            Text(
                                text = letter,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
                            )
                        }
                    }
                    items(items = apps, key = { "${it.appPackage}_${it.user}" }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppClick(app) }
                                .padding(vertical = 14.dp),  // 14+14+20sp ≈ 48dp touch target
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = app.appLabel,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * prefs.appDrawerTextSizeScale
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // ── Private Space Section (separate, at end) ──
                if (prefs.privateSpaceEnabled && filteredPrivateApps.isNotEmpty()) {
                    item(key = "private_divider") {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    }
                    item(key = "private_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Private Space",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(items = filteredPrivateApps, key = { "private_${it.appPackage}_${it.user}" }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppClick(app) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = app.appLabel,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * prefs.appDrawerTextSizeScale
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "Private App",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

            }
        }
    }
}

// ── Private Space helpers ──

private fun getPrivateSpaceProfile(context: Context): android.os.UserHandle? {
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

private fun togglePrivateSpace(context: Context) {
    val profile = getPrivateSpaceProfile(context) ?: return
    val um = context.getSystemService(Context.USER_SERVICE) as UserManager
    val locked = um.isQuietModeEnabled(profile)
    
    // On Android 15+, requestQuietModeEnabled handles the biometric/PIN challenge automatically
    um.requestQuietModeEnabled(!locked, profile)
}

private suspend fun loadPrivateSpaceApps(
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
