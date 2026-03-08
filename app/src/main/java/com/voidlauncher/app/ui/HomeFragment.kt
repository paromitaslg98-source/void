package com.voidlauncher.app.ui

import android.app.admin.DevicePolicyManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.DragEvent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.voidlauncher.app.MainViewModel
import com.voidlauncher.app.R
import com.voidlauncher.app.data.AppModel
import com.voidlauncher.app.data.Constants
import com.voidlauncher.app.data.Prefs
import com.voidlauncher.app.databinding.FragmentHomeBinding
import com.voidlauncher.app.helper.appUsagePermissionGranted
import com.voidlauncher.app.helper.dpToPx
import com.voidlauncher.app.helper.expandNotificationDrawer
import com.voidlauncher.app.helper.getChangedAppTheme
import com.voidlauncher.app.helper.getUserHandleFromString
import com.voidlauncher.app.helper.isPackageInstalled
import com.voidlauncher.app.helper.openAlarmApp
import com.voidlauncher.app.helper.openCalendar
import com.voidlauncher.app.helper.openCameraApp
import com.voidlauncher.app.helper.openDialerApp
import com.voidlauncher.app.helper.openSearch
import com.voidlauncher.app.helper.setPlainWallpaperByTheme
import com.voidlauncher.app.helper.showToast
import com.voidlauncher.app.listener.OnSwipeTouchListener
import com.voidlauncher.app.listener.ViewSwipeTouchListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    /** Tracks the currently long-pressed home app in edit mode (showing pen + reorder icons). */
    private var editModeView: TextView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        deviceManager = context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        initObservers()
        setHomeAlignment(prefs.homeAlignment)
        initSwipeTouchListener()
        initClickListeners()
    }

    override fun onResume() {
        super.onResume()
        populateHomeScreen(false)
        viewModel.isVoidDefault()
        if (prefs.showStatusBar) showStatusBar()
        else hideStatusBar()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.lock -> {}
            R.id.clock -> openClockApp()
            R.id.date -> openCalendarApp()
            R.id.setDefaultLauncher -> viewModel.resetLauncherLiveData.call()
            R.id.tvScreenTime -> openScreenTimeDigitalWellbeing()

            else -> {
                try { // Launch app
                    val appLocation = view.tag.toString().toInt()
                    homeAppClicked(appLocation)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun openClockApp() {
        if (prefs.clockAppPackage.isBlank())
            openAlarmApp(requireContext())
        else
            launchApp(
                "Clock",
                prefs.clockAppPackage,
                prefs.clockAppClassName,
                prefs.clockAppUser
            )
    }

    private fun openCalendarApp() {
        if (prefs.calendarAppPackage.isBlank())
            openCalendar(requireContext())
        else
            launchApp(
                "Calendar",
                prefs.calendarAppPackage,
                prefs.calendarAppClassName,
                prefs.calendarAppUser
            )
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.homeApp1, R.id.homeApp2, R.id.homeApp3, R.id.homeApp4,
            R.id.homeApp5, R.id.homeApp6, R.id.homeApp7, R.id.homeApp8,
            R.id.homeApp9, R.id.homeApp10 -> {
                toggleEditMode(view as TextView)
            }
            R.id.clock -> {
                showAppList(Constants.FLAG_SET_CLOCK_APP)
                prefs.clockAppPackage = ""
                prefs.clockAppClassName = ""
                prefs.clockAppUser = ""
            }

            R.id.date -> {
                showAppList(Constants.FLAG_SET_CALENDAR_APP)
                prefs.calendarAppPackage = ""
                prefs.calendarAppClassName = ""
                prefs.calendarAppUser = ""
            }

            R.id.setDefaultLauncher -> {
                prefs.hideSetDefaultLauncher = true
                binding.setDefaultLauncher.visibility = View.GONE
                if (viewModel.isVoidDefault.value != true) {
                    requireContext().showToast(R.string.set_as_default_launcher)
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                }
            }
        }
        return true
    }

    private fun toggleEditMode(view: TextView) {
        // If tapping the same view again, exit edit mode
        if (editModeView == view) {
            exitEditMode()
            return
        }
        // Exit previous edit mode if any
        exitEditMode()

        editModeView = view
        val editIcon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit)?.mutate()
        val reorderIcon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_reorder)?.mutate()

        val iconSize = (20 * resources.displayMetrics.density).toInt()
        val gap = (16 * resources.displayMetrics.density).toInt()
        editIcon?.setBounds(0, 0, iconSize, iconSize)
        reorderIcon?.setBounds(0, 0, iconSize, iconSize)

        // Combine pen + reorder into one LayerDrawable (pen left, reorder right)
        val totalWidth = iconSize + gap + iconSize
        val combined = android.graphics.drawable.LayerDrawable(arrayOf(editIcon, reorderIcon))
        combined.setLayerInset(0, 0, 0, iconSize + gap, 0) // pen on left side
        combined.setLayerInset(1, iconSize + gap, 0, 0, 0) // reorder on right side
        combined.setBounds(0, 0, totalWidth, iconSize)

        view.compoundDrawablePadding = (12 * resources.displayMetrics.density).toInt()
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, combined, null)

        setupEditModeTouchListener(view, iconSize, gap)
    }

    private fun exitEditMode() {
        editModeView?.let {
            it.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
            it.setOnTouchListener(getViewSwipeTouchListener(requireContext(), it))
        }
        editModeView = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupEditModeTouchListener(view: TextView, iconSize: Int, gap: Int) {
        view.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val tv = v as TextView
                val drawableEnd = tv.compoundDrawablesRelative[2]
                if (drawableEnd != null) {
                    val totalIconWidth = iconSize + gap + iconSize
                    val iconRegionStart = tv.width - totalIconWidth - tv.paddingEnd
                    val reorderRegionStart = iconRegionStart + iconSize + gap

                    when {
                        // Tapped on reorder icon (rightmost)
                        event.x >= reorderRegionStart -> {
                            exitEditMode()
                            startDrag(v)
                            return@setOnTouchListener true
                        }
                        // Tapped on pen/edit icon (second to last)
                        event.x >= iconRegionStart -> {
                            val location = getLocationFromView(v)
                            if (location != null) {
                                exitEditMode()
                                showAppList(location, true, true)
                            }
                            return@setOnTouchListener true
                        }
                        // Tapped on text area — just exit edit mode
                        else -> {
                            exitEditMode()
                            return@setOnTouchListener true
                        }
                    }
                }
            }
            false
        }
    }

    private fun getLocationFromView(view: View): Int? {
        return when (view.id) {
            R.id.homeApp1 -> Constants.FLAG_SET_HOME_APP_1
            R.id.homeApp2 -> Constants.FLAG_SET_HOME_APP_2
            R.id.homeApp3 -> Constants.FLAG_SET_HOME_APP_3
            R.id.homeApp4 -> Constants.FLAG_SET_HOME_APP_4
            R.id.homeApp5 -> Constants.FLAG_SET_HOME_APP_5
            R.id.homeApp6 -> Constants.FLAG_SET_HOME_APP_6
            R.id.homeApp7 -> Constants.FLAG_SET_HOME_APP_7
            R.id.homeApp8 -> Constants.FLAG_SET_HOME_APP_8
            R.id.homeApp9 -> Constants.FLAG_SET_HOME_APP_9
            R.id.homeApp10 -> Constants.FLAG_SET_HOME_APP_10
            else -> null
        }
    }

    private fun initObservers() {
        if (prefs.firstSettingsOpen) {
            binding.firstRunTips.visibility = View.VISIBLE
            binding.setDefaultLauncher.visibility = View.GONE
        } else binding.firstRunTips.visibility = View.GONE

        viewModel.refreshHome.observe(viewLifecycleOwner) {
            populateHomeScreen(it)
        }
        viewModel.isVoidDefault.observe(viewLifecycleOwner, Observer {
            if (it != true) {
                if (prefs.dailyWallpaper) {
                    prefs.dailyWallpaper = false
                    viewModel.cancelWallpaperWorker()
                }
                prefs.homeBottomAlignment = false
                setHomeAlignment()
            }
            if (binding.firstRunTips.visibility == View.VISIBLE) return@Observer
            binding.setDefaultLauncher.isVisible = it.not() && prefs.hideSetDefaultLauncher.not()
//            if (it) binding.setDefaultLauncher.visibility = View.GONE
//            else binding.setDefaultLauncher.visibility = View.VISIBLE
        })
        viewModel.homeAppAlignment.observe(viewLifecycleOwner) {
            setHomeAlignment(it)
        }
        viewModel.toggleDateTime.observe(viewLifecycleOwner) {
            populateDateTime()
        }
        viewModel.screenTimeValue.observe(viewLifecycleOwner) {
            it?.let { binding.tvScreenTime.text = it }
        }
    }

    private fun initSwipeTouchListener() {
        val context = requireContext()
        binding.mainLayout.setOnTouchListener(getSwipeGestureListener(context))
        binding.homeApp1.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp1))
        binding.homeApp2.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp2))
        binding.homeApp3.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp3))
        binding.homeApp4.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp4))
        binding.homeApp5.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp5))
        binding.homeApp6.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp6))
        binding.homeApp7.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp7))
        binding.homeApp8.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp8))
        binding.homeApp9.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp9))
        binding.homeApp10.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp10))
        
        // Setup DragListeners for Home Apps reordering
        val dragListener = View.OnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENTERED -> {
                    v.alpha = 0.5f
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    v.alpha = 1.0f
                    true
                }
                DragEvent.ACTION_DROP -> {
                    v.alpha = 1.0f
                    val fromLocation = event.clipData.getItemAt(0).text.toString().toIntOrNull()
                    val toLocation = v.tag.toString().toIntOrNull()
                    
                    if (fromLocation != null && toLocation != null && fromLocation != toLocation) {
                        prefs.swapAppLocations(fromLocation, toLocation)
                        populateHomeScreen(false)
                        viewModel.refreshHome(false)
                    }
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    v.alpha = 1.0f
                    true
                }
                else -> false
            }
        }
        
        binding.homeApp1.setOnDragListener(dragListener)
        binding.homeApp2.setOnDragListener(dragListener)
        binding.homeApp3.setOnDragListener(dragListener)
        binding.homeApp4.setOnDragListener(dragListener)
        binding.homeApp5.setOnDragListener(dragListener)
        binding.homeApp6.setOnDragListener(dragListener)
        binding.homeApp7.setOnDragListener(dragListener)
        binding.homeApp8.setOnDragListener(dragListener)
        binding.homeApp9.setOnDragListener(dragListener)
        binding.homeApp10.setOnDragListener(dragListener)
    }

    private fun startDrag(view: View) {
        val data = android.content.ClipData.newPlainText("location", view.tag.toString())
        val shadowBuilder = View.DragShadowBuilder(view)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            view.startDragAndDrop(data, shadowBuilder, view, 0)
        } else {
            @Suppress("DEPRECATION")
            view.startDrag(data, shadowBuilder, view, 0)
        }
    }

    private fun initClickListeners() {
        binding.lock.setOnClickListener(this)
        binding.clock.setOnClickListener(this)
        binding.date.setOnClickListener(this)
        binding.clock.setOnLongClickListener(this)
        binding.date.setOnLongClickListener(this)
        binding.setDefaultLauncher.setOnClickListener(this)
        binding.setDefaultLauncher.setOnLongClickListener(this)
        binding.tvScreenTime.setOnClickListener(this)
    }

    private fun setHomeAlignment(horizontalGravity: Int = prefs.homeAlignment) {
        val verticalGravity = if (prefs.homeBottomAlignment) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
        binding.homeAppsLayout.gravity = horizontalGravity or verticalGravity
        binding.dateTimeLayout.gravity = horizontalGravity
        binding.homeApp1.gravity = horizontalGravity
        binding.homeApp2.gravity = horizontalGravity
        binding.homeApp3.gravity = horizontalGravity
        binding.homeApp4.gravity = horizontalGravity
        binding.homeApp5.gravity = horizontalGravity
        binding.homeApp6.gravity = horizontalGravity
        binding.homeApp7.gravity = horizontalGravity
        binding.homeApp8.gravity = horizontalGravity
        binding.homeApp9.gravity = horizontalGravity
        binding.homeApp10.gravity = horizontalGravity
    }

    private fun populateDateTime() {
        binding.dateTimeLayout.isVisible = prefs.dateTimeVisibility != Constants.DateTime.OFF
        binding.clock.isVisible = Constants.DateTime.isTimeVisible(prefs.dateTimeVisibility)
        binding.date.isVisible = Constants.DateTime.isDateVisible(prefs.dateTimeVisibility)

//        var dateText = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())
        val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        var dateText = dateFormat.format(Date())

        if (!prefs.showStatusBar) {
            val battery = (requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (battery > 0)
                dateText = getString(R.string.day_battery, dateText, battery)
        }
        binding.date.text = dateText.replace(".,", ",")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun populateScreenTime() {
        if (requireContext().appUsagePermissionGranted().not()) return

        viewModel.getTodaysScreenTime()
        binding.tvScreenTime.visibility = View.VISIBLE
    }

    private fun populateHomeScreen(appCountUpdated: Boolean) {
        if (appCountUpdated) hideHomeApps()
        populateDateTime()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            populateScreenTime()

        val homeAppsNum = prefs.homeAppsNum
        if (homeAppsNum == 0) { updateCardAndDividers(); return }

        binding.homeApp1.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp1, prefs.appName1, prefs.appPackage1, prefs.appUser1, prefs.isShortcut1, prefs.shortcutId1)) {
            prefs.appName1 = ""
            prefs.appPackage1 = ""
        }
        if (homeAppsNum == 1) { updateCardAndDividers(); return }

        binding.homeApp2.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp2, prefs.appName2, prefs.appPackage2, prefs.appUser2, prefs.isShortcut2, prefs.shortcutId2)) {
            prefs.appName2 = ""
            prefs.appPackage2 = ""
        }
        if (homeAppsNum == 2) { updateCardAndDividers(); return }

        binding.homeApp3.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp3, prefs.appName3, prefs.appPackage3, prefs.appUser3, prefs.isShortcut3, prefs.shortcutId3)) {
            prefs.appName3 = ""
            prefs.appPackage3 = ""
        }
        if (homeAppsNum == 3) { updateCardAndDividers(); return }

        binding.homeApp4.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp4, prefs.appName4, prefs.appPackage4, prefs.appUser4, prefs.isShortcut4, prefs.shortcutId4)) {
            prefs.appName4 = ""
            prefs.appPackage4 = ""
        }
        if (homeAppsNum == 4) { updateCardAndDividers(); return }

        binding.homeApp5.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp5, prefs.appName5, prefs.appPackage5, prefs.appUser5, prefs.isShortcut5, prefs.shortcutId5)) {
            prefs.appName5 = ""
            prefs.appPackage5 = ""
        }
        if (homeAppsNum == 5) { updateCardAndDividers(); return }

        binding.homeApp6.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp6, prefs.appName6, prefs.appPackage6, prefs.appUser6, prefs.isShortcut6, prefs.shortcutId6)) {
            prefs.appName6 = ""
            prefs.appPackage6 = ""
        }
        if (homeAppsNum == 6) { updateCardAndDividers(); return }

        binding.homeApp7.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp7, prefs.appName7, prefs.appPackage7, prefs.appUser7, prefs.isShortcut7, prefs.shortcutId7)) {
            prefs.appName7 = ""
            prefs.appPackage7 = ""
        }
        if (homeAppsNum == 7) { updateCardAndDividers(); return }

        binding.homeApp8.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp8, prefs.appName8, prefs.appPackage8, prefs.appUser8, prefs.isShortcut8, prefs.shortcutId8)) {
            prefs.appName8 = ""
            prefs.appPackage8 = ""
        }
        if (homeAppsNum == 8) { updateCardAndDividers(); return }

        binding.homeApp9.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp9, prefs.appName9, prefs.appPackage9, prefs.appUser9, prefs.isShortcut9, prefs.shortcutId9)) {
            prefs.appName9 = ""
            prefs.appPackage9 = ""
        }
        if (homeAppsNum == 9) { updateCardAndDividers(); return }

        binding.homeApp10.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp10, prefs.appName10, prefs.appPackage10, prefs.appUser10, prefs.isShortcut10, prefs.shortcutId10)) {
            prefs.appName10 = ""
            prefs.appPackage10 = ""
        }

        // Show the combined card and update dividers
        updateCardAndDividers()
    }

    private fun setHomeAppText(textView: TextView, appName: String, packageName: String, userString: String, isShortcut: Boolean, shortcutId: String?): Boolean {
        // Explicitly set home text size
        textView.textSize = 15f * prefs.homeTextSizeScale

        // Get user handle for the app/shortcut
        val userHandle = getUserHandleFromString(requireContext(), userString)
        
        // If it's a shortcut, verify it still exists
        if (isShortcut) {
            val launcherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            
            // Query for the specific shortcut
            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(packageName)
                setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            }
            
            try {
                val shortcuts = launcherApps.getShortcuts(query, userHandle)
                // Check if our shortcut still exists
                if (shortcuts?.any { it.id == shortcutId } == true) {
                    textView.text = appName
                    return true
                }
                textView.text = ""
                return false
            } catch (e: Exception) {
                e.printStackTrace()
                textView.text = ""
                return false
            }
        }
        
        // Regular app check
        if (isPackageInstalled(requireContext(), packageName, userString)) {
            textView.text = appName
            return true
        }
        textView.text = ""
        return false
    }

    private fun hideHomeApps() {
        binding.homeApp1.visibility = View.GONE
        binding.homeApp2.visibility = View.GONE
        binding.homeApp3.visibility = View.GONE
        binding.homeApp4.visibility = View.GONE
        binding.homeApp5.visibility = View.GONE
        binding.homeApp6.visibility = View.GONE
        binding.homeApp7.visibility = View.GONE
        binding.homeApp8.visibility = View.GONE
        binding.homeApp9.visibility = View.GONE
        binding.homeApp10.visibility = View.GONE
        binding.divider1.visibility = View.GONE
        binding.divider2.visibility = View.GONE
        binding.divider3.visibility = View.GONE
        binding.divider4.visibility = View.GONE
        binding.divider5.visibility = View.GONE
        binding.divider6.visibility = View.GONE
        binding.divider7.visibility = View.GONE
        binding.divider8.visibility = View.GONE
        binding.divider9.visibility = View.GONE
        binding.homeAppsCard.visibility = View.GONE
    }

    private fun updateCardAndDividers() {
        val apps = listOf(
            binding.homeApp1, binding.homeApp2, binding.homeApp3, binding.homeApp4,
            binding.homeApp5, binding.homeApp6, binding.homeApp7, binding.homeApp8,
            binding.homeApp9, binding.homeApp10
        )
        val dividers = listOf(
            binding.divider1, binding.divider2, binding.divider3, binding.divider4,
            binding.divider5, binding.divider6, binding.divider7, binding.divider8,
            binding.divider9
        )
        // Hide all dividers first
        dividers.forEach { it.visibility = View.GONE }

        val visibleApps = apps.filter { it.visibility == View.VISIBLE }
        if (visibleApps.isEmpty()) {
            binding.homeAppsCard.visibility = View.GONE
            return
        }

        binding.homeAppsCard.visibility = View.VISIBLE
        // Show dividers between consecutive visible apps
        for (i in 0 until apps.size - 1) {
            if (apps[i].visibility == View.VISIBLE && i < dividers.size) {
                // Show divider if the next visible app exists after this one
                val hasNextVisible = (i + 1 until apps.size).any { apps[it].visibility == View.VISIBLE }
                dividers[i].visibility = if (hasNextVisible) View.VISIBLE else View.GONE
            }
        }
    }

    private fun launchAppOrShortcut(
        appName: String,
        packageName: String,
        activityClassName: String?,
        shortcutId: String?,
        isShortcut: Boolean,
        userString: String,
        fallback: (() -> Unit)? = null,
        swipeDirection: String? = null
    ) {
        if (appName.isEmpty()) {
            showLongPressToast()
            return
        }
        if (isShortcut && !shortcutId.isNullOrEmpty()) {
            launchShortcut(
                packageName = packageName,
                shortcutId = shortcutId,
                shortcutLabel = appName,
                userString = userString
            )
        } else if (packageName.isNotEmpty()) {
            launchApp(
                appName = appName,
                packageName = packageName,
                activityClassName = activityClassName,
                userString = userString,
                swipeDirection = swipeDirection
            )
        } else {
            fallback?.invoke()
        }
    }

    private fun launchShortcut(shortcutId: String, packageName: String, shortcutLabel: String, userString: String) {
        viewModel.selectedApp(
            AppModel.PinnedShortcut(
                shortcutId = shortcutId,
                appLabel = shortcutLabel,
                user = getUserHandleFromString(requireContext(), userString),
                key = null,
                appPackage = packageName,
                isNew = false,
            ),
            Constants.FLAG_LAUNCH_APP
        )
    }

    private fun launchApp(appName: String, packageName: String, activityClassName: String?, userString: String, swipeDirection: String? = null) {
        viewModel.selectedApp(
            AppModel.App(
                appLabel = appName,
                key = null,
                appPackage = packageName,
                activityClassName = activityClassName,
                isNew = false,
                user = getUserHandleFromString(requireContext(), userString)
            ),
            Constants.FLAG_LAUNCH_APP,
            swipeDirection
        )
    }

    private fun homeAppClicked(location: Int) {
        launchAppOrShortcut(
            appName = prefs.getAppName(location),
            packageName = prefs.getAppPackage(location),
            activityClassName = prefs.getAppActivityClassName(location),
            shortcutId = prefs.getShortcutId(location),
            isShortcut = prefs.getIsShortcut(location),
            userString = prefs.getAppUser(location)
        )
    }

    private fun openSwipeRightApp() {
        findNavController().navigate(R.id.action_mainFragment_to_notesFragment)
    }

    private fun openSwipeLeftApp() {
        findNavController().navigate(R.id.action_mainFragment_to_notificationsFragment)
    }

    private fun showAppList(flag: Int, rename: Boolean = false, includeHiddenApps: Boolean = false) {
        viewModel.getAppList(includeHiddenApps)
        try {
            findNavController().navigate(
                R.id.action_mainFragment_to_appListFragment,
                bundleOf(
                    Constants.Key.FLAG to flag,
                    Constants.Key.RENAME to rename
                )
            )
        } catch (e: Exception) {
            findNavController().navigate(
                R.id.appListFragment,
                bundleOf(
                    Constants.Key.FLAG to flag,
                    Constants.Key.RENAME to rename
                )
            )
            e.printStackTrace()
        }
    }

    private fun swipeDownAction() {
        when (prefs.swipeDownAction) {
            Constants.SwipeDownAction.SEARCH -> openSearch(requireContext())
            else -> expandNotificationDrawer(requireContext())
        }
    }

    private fun lockPhone() {
        requireActivity().runOnUiThread {
            try {
                deviceManager.lockNow()
            } catch (e: SecurityException) {
                requireContext().showToast(getString(R.string.please_turn_on_double_tap_to_unlock), Toast.LENGTH_LONG)
                findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
            } catch (e: Exception) {
                requireContext().showToast(getString(R.string.launcher_failed_to_lock_device), Toast.LENGTH_LONG)
                prefs.lockModeOn = false
            }
        }
    }

    private fun showStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars())
        else
            @Suppress("DEPRECATION", "InlinedApi")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
        else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
    }

    private fun changeAppTheme() {
        if (prefs.dailyWallpaper.not()) return
        val changedAppTheme = getChangedAppTheme(requireContext(), prefs.appTheme)
        prefs.appTheme = changedAppTheme
        if (prefs.dailyWallpaper) {
            setPlainWallpaperByTheme(requireContext(), changedAppTheme)
            viewModel.setWallpaperWorker()
        }
        requireActivity().recreate()
    }

    private fun openScreenTimeDigitalWellbeing() {
        val intent = Intent()
        try {
            intent.setClassName(
                Constants.DIGITAL_WELLBEING_PACKAGE_NAME,
                Constants.DIGITAL_WELLBEING_ACTIVITY
            )
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                intent.setClassName(
                    Constants.DIGITAL_WELLBEING_SAMSUNG_PACKAGE_NAME,
                    Constants.DIGITAL_WELLBEING_SAMSUNG_ACTIVITY
                )
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showLongPressToast() = requireContext().showToast(getString(R.string.long_press_to_select_app))

    private fun textOnClick(view: View) = onClick(view)

    private fun textOnLongClick(view: View) = onLongClick(view)

    private fun getSwipeGestureListener(context: Context): View.OnTouchListener {
        return object : OnSwipeTouchListener(context) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                openSwipeLeftApp()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                swipeDownAction()
            }

            override fun onLongClick() {
                super.onLongClick()
                try {
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                    viewModel.firstOpen(false)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onDoubleClick() {
                super.onDoubleClick()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    binding.lock.performClick()
                else if (prefs.lockModeOn)
                    lockPhone()
            }

            override fun onClick() {
                super.onClick()
                viewModel.checkForMessages.call()
            }
        }
    }

    private fun getViewSwipeTouchListener(context: Context, view: View): View.OnTouchListener {
        return object : ViewSwipeTouchListener(context, view) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                openSwipeLeftApp()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                swipeDownAction()
            }

            override fun onLongClick(view: View) {
                super.onLongClick(view)
                
                // For Home Apps, if it's not empty, start dragging, otherwise open app list
                val appLocation = view.tag.toString().toIntOrNull()
                if (appLocation != null && prefs.getAppName(appLocation).isNotEmpty()) {
                    startDrag(view)
                } else {
                    textOnLongClick(view)
                }
            }

            override fun onClick(view: View) {
                super.onClick(view)
                textOnClick(view)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}