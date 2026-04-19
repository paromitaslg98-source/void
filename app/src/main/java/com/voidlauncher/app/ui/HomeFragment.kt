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
import android.os.Handler
import android.os.Looper
import android.view.DragEvent
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.ViewConfiguration
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.voidlauncher.app.ui.navigation.NavTransitionPolicy.Direction
import com.voidlauncher.app.ui.navigation.NavTransitionPolicy.applyDestinationTransitions
import com.voidlauncher.app.ui.navigation.NavTransitionPolicy.applyExitFor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val mainHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null

    /** Tracks the currently selected home app in contextual edit mode. */
    private var editModeView: TextView? = null
    private var longPressTriggered = false
    private var lastTouchDownX = 0f
    private var lastTouchDownY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyDestinationTransitions(Direction.FADE)
    }

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
        observeHomescreenPreferences()
        setHomeAlignment(prefs.homeAlignment)
        initSwipeTouchListener()
        initClickListeners()
    }

    override fun onResume() {
        super.onResume()
        populateHomeScreen(false)
        viewModel.isVoidDefault()
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
                    applyExitFor(Direction.FADE)
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                }
            }
        }
        return true
    }

    private fun enterEditMode(view: TextView) {
        exitEditMode()
        editModeView = view
        view.alpha = 0.75f
        view.translationX = 8.dpToPx().toFloat()
        view.paint.isFakeBoldText = true
    }

    private fun exitEditMode() {
        editModeView?.let {
            it.alpha = 1f
            it.translationX = 0f
            it.paint.isFakeBoldText = false
            it.invalidate()
        }
        editModeView = null
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
                setHomeAlignment()
            }
            if (binding.firstRunTips.visibility == View.VISIBLE) return@Observer
            binding.setDefaultLauncher.isVisible = it.not() && prefs.hideSetDefaultLauncher.not()
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

    private fun observeHomescreenPreferences() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                prefs.homescreenPreferences.collect {
                    setHomeAlignment(it.horizontalAlignment)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        populateScreenTime()
                    } else {
                        binding.tvScreenTime.visibility = View.GONE
                    }
                    populateDateTime()
                }
            }
        }
    }

    private fun initSwipeTouchListener() {
        val context = requireContext()
        binding.mainLayout.setOnTouchListener(getSwipeGestureListener(context))
        binding.homeApp1.setOnTouchListener(getHomeEntryTouchListener(context, binding.homeApp1))
        binding.homeApp2.setOnTouchListener(getHomeEntryTouchListener(context, binding.homeApp2))
        binding.homeApp3.setOnTouchListener(getHomeEntryTouchListener(context, binding.homeApp3))
        binding.homeApp4.setOnTouchListener(getHomeEntryTouchListener(context, binding.homeApp4))
        binding.homeApp5.setOnTouchListener(getHomeEntryTouchListener(context, binding.homeApp5))
        binding.homeApp6.setOnTouchListener(getHomeEntryTouchListener(context, binding.homeApp6))
        binding.homeApp7.setOnTouchListener(getHomeEntryTouchListener(context, binding.homeApp7))
        binding.homeApp8.setOnTouchListener(getHomeEntryTouchListener(context, binding.homeApp8))
        binding.homeApp9.setOnTouchListener(getHomeEntryTouchListener(context, binding.homeApp9))
        binding.homeApp10.setOnTouchListener(getHomeEntryTouchListener(context, binding.homeApp10))
        
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
                    
                    if (fromLocation != null && toLocation != null && fromLocation != toLocation)
                        performHomeAppReorder(fromLocation, toLocation)
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    v.alpha = 1.0f
                    exitEditMode()
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

    private fun performHomeAppReorder(fromLocation: Int, toLocation: Int) {
        prefs.swapAppLocations(fromLocation, toLocation)
        exitEditMode()
        viewModel.refreshHome(false)
    }


    private fun showHomeAppContextMenu(view: TextView) {
        val location = view.tag.toString().toIntOrNull() ?: return
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menu.add(0, 1, 0, getString(R.string.change_or_add_app))
        popupMenu.menu.add(0, 2, 1, getString(R.string.reorder_apps))

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    exitEditMode()
                    showAppList(location)
                }
                2 -> startDrag(view)
            }
            true
        }
        popupMenu.setOnDismissListener {
            if (editModeView == view) exitEditMode()
        }
        popupMenu.show()
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
        val verticalGravity = prefs.homeVerticalAlignment
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
        binding.clock.isVisible = prefs.showClockWidget
        binding.date.isVisible = prefs.showDateWidget
        binding.dateTimeLayout.isVisible = binding.clock.isVisible || binding.date.isVisible || binding.tvScreenTime.isVisible

        val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        val dateText = dateFormat.format(Date())
        binding.date.text = dateText.replace(".,", ",")

        // Handle battery separately
        val battery = (requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        
        if (battery > 0 && !prefs.showStatusBar) {
            binding.tvBattery.text = "$battery%"
            binding.tvBattery.visibility = View.VISIBLE
        } else {
            binding.tvBattery.visibility = View.GONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun populateScreenTime() {
        if (!prefs.showScreenTimeWidget) {
            binding.tvScreenTime.visibility = View.GONE
            return
        }
        if (requireContext().appUsagePermissionGranted().not()) return

        viewModel.getTodaysScreenTime()
        binding.tvScreenTime.visibility = View.VISIBLE

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val horizontalMargin = if (isLandscape) 64.dpToPx() else 10.dpToPx()
        val marginTop = if (isLandscape) {
            if (prefs.dateTimeVisibility == Constants.DateTime.DATE_ONLY) 36.dpToPx() else 56.dpToPx()
        } else {
            if (prefs.dateTimeVisibility == Constants.DateTime.DATE_ONLY) 45.dpToPx() else 72.dpToPx()
        }
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = marginTop
            marginStart = horizontalMargin
            marginEnd = horizontalMargin
            gravity = if (prefs.homeAlignment == Gravity.END) Gravity.START else Gravity.END
        }
        binding.tvScreenTime.layoutParams = params
        binding.tvScreenTime.setPadding(10.dpToPx())
    }

    private fun populateHomeScreen(appCountUpdated: Boolean) {
        if (appCountUpdated) hideHomeApps()
        populateDateTime()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            populateScreenTime()

        val homeAppsNum = prefs.homeAppsNum
        if (homeAppsNum == 0) return

        binding.homeApp1.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp1, prefs.appName1, prefs.appPackage1, prefs.appUser1, prefs.isShortcut1, prefs.shortcutId1)) {
            prefs.appName1 = ""
            prefs.appPackage1 = ""
        }
        if (homeAppsNum == 1) return

        binding.homeApp2.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp2, prefs.appName2, prefs.appPackage2, prefs.appUser2, prefs.isShortcut2, prefs.shortcutId2)) {
            prefs.appName2 = ""
            prefs.appPackage2 = ""
        }
        if (homeAppsNum == 2) return

        binding.homeApp3.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp3, prefs.appName3, prefs.appPackage3, prefs.appUser3, prefs.isShortcut3, prefs.shortcutId3)) {
            prefs.appName3 = ""
            prefs.appPackage3 = ""
        }
        if (homeAppsNum == 3) return

        binding.homeApp4.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp4, prefs.appName4, prefs.appPackage4, prefs.appUser4, prefs.isShortcut4, prefs.shortcutId4)) {
            prefs.appName4 = ""
            prefs.appPackage4 = ""
        }
        if (homeAppsNum == 4) return

        binding.homeApp5.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp5, prefs.appName5, prefs.appPackage5, prefs.appUser5, prefs.isShortcut5, prefs.shortcutId5)) {
            prefs.appName5 = ""
            prefs.appPackage5 = ""
        }
        if (homeAppsNum == 5) return

        binding.homeApp6.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp6, prefs.appName6, prefs.appPackage6, prefs.appUser6, prefs.isShortcut6, prefs.shortcutId6)) {
            prefs.appName6 = ""
            prefs.appPackage6 = ""
        }
        if (homeAppsNum == 6) return

        binding.homeApp7.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp7, prefs.appName7, prefs.appPackage7, prefs.appUser7, prefs.isShortcut7, prefs.shortcutId7)) {
            prefs.appName7 = ""
            prefs.appPackage7 = ""
        }
        if (homeAppsNum == 7) return

        binding.homeApp8.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp8, prefs.appName8, prefs.appPackage8, prefs.appUser8, prefs.isShortcut8, prefs.shortcutId8)) {
            prefs.appName8 = ""
            prefs.appPackage8 = ""
        }
        if (homeAppsNum == 8) return

        binding.homeApp9.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp9, prefs.appName9, prefs.appPackage9, prefs.appUser9, prefs.isShortcut9, prefs.shortcutId9)) {
            prefs.appName9 = ""
            prefs.appPackage9 = ""
        }
        if (homeAppsNum == 9) return

        binding.homeApp10.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp10, prefs.appName10, prefs.appPackage10, prefs.appUser10, prefs.isShortcut10, prefs.shortcutId10)) {
            prefs.appName10 = ""
            prefs.appPackage10 = ""
        }
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
        applyExitFor(Direction.RIGHT)
        findNavController().navigate(R.id.action_mainFragment_to_notesFragment)
    }

    private fun openSwipeLeftApp() {
        applyExitFor(Direction.LEFT)
        findNavController().navigate(R.id.action_mainFragment_to_notificationsFragment)
    }

    private fun showAppList(flag: Int, rename: Boolean = false, includeHiddenApps: Boolean = false) {
        viewModel.getAppList(includeHiddenApps)
        applyExitFor(Direction.UP)
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
                applyExitFor(Direction.FADE)
                findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
            } catch (e: Exception) {
                requireContext().showToast(getString(R.string.launcher_failed_to_lock_device), Toast.LENGTH_LONG)
                prefs.lockModeOn = false
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
                if (prefs.swipeLeftEnabled) openSwipeLeftApp()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                if (prefs.swipeRightEnabled) openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                if (prefs.swipeUpEnabled) showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                swipeDownAction()
            }

            override fun onLongClick() {
                super.onLongClick()
                try {
                    applyExitFor(Direction.FADE)
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
                if (prefs.swipeLeftEnabled) openSwipeLeftApp()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                if (prefs.swipeRightEnabled) openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                if (prefs.swipeUpEnabled) showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                swipeDownAction()
            }

            override fun onLongClick(view: View) {
                super.onLongClick(view)
                // Long press handling for home entries is explicitly handled via timeout + handler.
            }

            override fun onClick(view: View) {
                super.onClick(view)
                textOnClick(view)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun getHomeEntryTouchListener(context: Context, view: TextView): View.OnTouchListener {
        val swipeTouchListener = getViewSwipeTouchListener(context, view)
        val longPressTimeoutMs = maxOf(400L, ViewConfiguration.getLongPressTimeout().toLong())

        return View.OnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (editModeView != null && editModeView != view) exitEditMode()
                    longPressTriggered = false
                    lastTouchDownX = event.x
                    lastTouchDownY = event.y
                    longPressRunnable?.let(mainHandler::removeCallbacks)
                    longPressRunnable = Runnable {
                        longPressTriggered = true
                        enterEditMode(view)
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        showHomeAppContextMenu(view)
                    }
                    mainHandler.postDelayed(longPressRunnable!!, longPressTimeoutMs)
                }

                MotionEvent.ACTION_MOVE -> {
                    val slop = ViewConfiguration.get(context).scaledTouchSlop
                    if (kotlin.math.abs(event.x - lastTouchDownX) > slop || kotlin.math.abs(event.y - lastTouchDownY) > slop) {
                        longPressRunnable?.let(mainHandler::removeCallbacks)
                    }
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let(mainHandler::removeCallbacks)
                    if (longPressTriggered) return@OnTouchListener true
                    if (editModeView == view && event.actionMasked == MotionEvent.ACTION_UP) {
                        startDrag(view)
                        return@OnTouchListener true
                    }
                }
            }
            swipeTouchListener.onTouch(v, event)
        }
    }

    override fun onDestroyView() {
        longPressRunnable?.let(mainHandler::removeCallbacks)
        exitEditMode()
        super.onDestroyView()
        _binding = null
    }
}
