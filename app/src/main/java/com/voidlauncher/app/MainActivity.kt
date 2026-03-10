package com.voidlauncher.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.voidlauncher.app.data.Constants
import com.voidlauncher.app.data.Prefs
import com.voidlauncher.app.databinding.ActivityMainBinding
import com.voidlauncher.app.helper.getColorFromAttr
import com.voidlauncher.app.helper.hasBeenDays
import com.voidlauncher.app.helper.hasBeenHours
import com.voidlauncher.app.helper.hasBeenMinutes
import com.voidlauncher.app.helper.isDarkThemeOn
import com.voidlauncher.app.helper.isDaySince
import com.voidlauncher.app.helper.isDefaultLauncher
import com.voidlauncher.app.helper.isEinkDisplay
import com.voidlauncher.app.helper.isVoidDefault
import com.voidlauncher.app.helper.isTablet
import com.voidlauncher.app.helper.openUrl
import com.voidlauncher.app.helper.rateApp
import com.voidlauncher.app.helper.resetLauncherViaFakeActivity
import com.voidlauncher.app.helper.setPlainWallpaper
import com.voidlauncher.app.helper.shareApp
import com.voidlauncher.app.helper.showLauncherSelector
import com.voidlauncher.app.helper.showToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var navController: NavController
    private lateinit var viewModel: MainViewModel
    private lateinit var appSettingsViewModel: AppSettingsViewModel
    private lateinit var binding: ActivityMainBinding
    private var timerJob: Job? = null
    private var predictiveBackCallback: OnBackInvokedCallback? = null

//    override fun onBackPressed() {
//        if (navController.currentDestination?.id != R.id.mainFragment)
//            super.onBackPressed()
//    }

    override fun attachBaseContext(context: Context) {
        val newConfig = Configuration(context.resources.configuration)
        newConfig.fontScale = Prefs(context).textSizeScale
        applyOverrideConfiguration(newConfig)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs(this)
        // VOID Launcher is always dark — never follow system or allow light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)  
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = this.findNavController(R.id.nav_host_fragment)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        appSettingsViewModel = ViewModelProvider(this)[AppSettingsViewModel::class.java]

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = handleNavigationBack()
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        registerPredictiveBackCallback()

        if (prefs.firstOpen) {
            viewModel.firstOpen(true)
            prefs.firstOpen = false
            prefs.firstOpenTime = System.currentTimeMillis()
            viewModel.setDefaultClockApp()
            viewModel.resetLauncherLiveData.call()
        }

        initClickListeners()
        initObservers(viewModel)
        viewModel.getAppList()
        setupOrientation()
        observeStatusBarVisibility()

        window.addFlags(FLAG_LAYOUT_NO_LIMITS)
    }

    override fun onStart() {
        super.onStart()
        restartLauncherOrCheckTheme()
    }

    override fun onStop() {
        backToHomeScreen()
        super.onStop()
    }

    override fun onUserLeaveHint() {
        backToHomeScreen()
        super.onUserLeaveHint()
    }

    override fun onNewIntent(intent: Intent?) {
        setIntent(intent)
        backToHomeScreen()
        navigateToReminderIfNeeded(intent)
        super.onNewIntent(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppCompatDelegate.setDefaultNightMode(prefs.appTheme)
        if (prefs.dailyWallpaper && AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            setPlainWallpaper()
            viewModel.setWallpaperWorker()
            recreate()
        }
    }

    private fun initClickListeners() {
        binding.ivClose.setOnClickListener {
            binding.messageLayout.visibility = View.GONE
        }
    }

    private fun initObservers(viewModel: MainViewModel) {
        viewModel.launcherResetFailed.observe(this) {
            openLauncherChooser(it)
        }
        viewModel.resetLauncherLiveData.observe(this) {
            if (isDefaultLauncher() || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                resetLauncherViaFakeActivity()
            else
                showLauncherSelector(Constants.REQUEST_CODE_LAUNCHER_SELECTOR)
        }
        viewModel.checkForMessages.observe(this) {
            checkForMessages()
        }
        viewModel.showDialog.observe(this) {
            when (it) {
                Constants.Dialog.ABOUT -> {
                    showMessageDialog(R.string.app_name, R.string.welcome_to_void_settings, R.string.okay) {
                        binding.messageLayout.visibility = View.GONE
                    }
                }

                Constants.Dialog.HIDDEN -> {
                    showMessageDialog(R.string.hidden_apps, R.string.hidden_apps_message, R.string.okay) {}
                }

                Constants.Dialog.KEYBOARD -> {
                    showMessageDialog(R.string.app_name, R.string.keyboard_message, R.string.okay) {}
                }

                Constants.Dialog.DIGITAL_WELLBEING -> {
                    showMessageDialog(R.string.screen_time, R.string.app_usage_message, R.string.permission) {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                }

                // PRO_MESSAGE, WALLPAPER, REVIEW, RATE, SHARE — removed (external links)
                else -> { /* no-op */ }
            }
        }
    }

    private fun showMessageDialog(title: Int, message: Int, action: Int, clickListener: () -> Unit) {
        binding.tvTitle.text = getString(title)
        binding.tvMessage.text = getString(message)
        binding.tvAction.text = getString(action)
        binding.tvAction.setOnClickListener {
            clickListener()
            binding.messageLayout.visibility = View.GONE
        }
        binding.messageLayout.visibility = View.VISIBLE
    }

    private fun checkForMessages() {
        if (prefs.firstOpenTime == 0L)
            prefs.firstOpenTime = System.currentTimeMillis()

        when (prefs.userState) {
            Constants.UserState.START -> {
                if (prefs.firstOpenTime.hasBeenMinutes(10))
                    prefs.userState = Constants.UserState.WALLPAPER
            }

            Constants.UserState.WALLPAPER -> {
                if (prefs.wallpaperMsgShown || prefs.dailyWallpaper) {
                    // Transition straight to end states since rate/share are removed
                    prefs.userState = Constants.UserState.SHARE 
                } else if (isVoidDefault(this)) {
                    viewModel.showDialog.postValue(Constants.Dialog.WALLPAPER)
                }
            }
            else -> { /* Rate, Review, and Share popups removed */ }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupOrientation() {
        if (isTablet(this) || Build.VERSION.SDK_INT == Build.VERSION_CODES.O)
            return
        // In Android 8.0, windowIsTranslucent cannot be used with screenOrientation=portrait
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun backToHomeScreen() {
        binding.messageLayout.visibility = View.GONE
        if (navController.currentDestination?.id != R.id.mainFragment)
            navController.popBackStack(R.id.mainFragment, false)
    }

    private fun navigateToReminderIfNeeded(incomingIntent: Intent?) {
        val noteId = incomingIntent?.getLongExtra(NoteReminderReceiver.EXTRA_NOTE_ID, -1L) ?: -1L
        if (noteId <= 0L) return
        if (navController.currentDestination?.id != R.id.notesFragment) {
            navController.navigate(R.id.action_mainFragment_to_notesFragment)
        }
    }

    private fun setPlainWallpaper() {
        if (this.isDarkThemeOn())
            setPlainWallpaper(this, android.R.color.black)
        else setPlainWallpaper(this, android.R.color.white)
    }

    private fun openLauncherChooser(resetFailed: Boolean) {
        if (resetFailed) {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            startActivity(intent)
        }
    }

    private fun restartLauncherOrCheckTheme(forceRestart: Boolean = false) {
        if (forceRestart || prefs.launcherRestartTimestamp.hasBeenHours(4)) {
            prefs.launcherRestartTimestamp = System.currentTimeMillis()
            cacheDir.deleteRecursively()
            recreate()
        } else
            checkTheme()
    }

    private fun checkTheme() {
        timerJob?.cancel()
        timerJob = lifecycleScope.launch {
            delay(200)
            if ((prefs.appTheme == AppCompatDelegate.MODE_NIGHT_YES && getColorFromAttr(R.attr.primaryColor) != getColor(R.color.white))
                || (prefs.appTheme == AppCompatDelegate.MODE_NIGHT_NO && getColorFromAttr(R.attr.primaryColor) != getColor(R.color.black))
            )
                restartLauncherOrCheckTheme(true)
        }
    }


    private fun observeStatusBarVisibility() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                appSettingsViewModel.isStatusBarVisible.collect { isVisible ->
                    if (isVisible) showStatusBar() else hideStatusBar()
                }
            }
        }
    }

    private fun showStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            window.insetsController?.show(WindowInsets.Type.statusBars())
        else
            @Suppress("DEPRECATION", "InlinedApi")
            window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        else {
            @Suppress("DEPRECATION")
            window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Constants.REQUEST_CODE_ENABLE_ADMIN -> {
                if (resultCode == Activity.RESULT_OK)
                    prefs.lockModeOn = true
            }

            Constants.REQUEST_CODE_LAUNCHER_SELECTOR -> {
                if (resultCode == Activity.RESULT_OK)
                    resetLauncherViaFakeActivity()
            }
        }
    }

    override fun onDestroy() {
        unregisterPredictiveBackCallback()
        super.onDestroy()
    }

    private fun handleNavigationBack() {
        if (navController.currentDestination?.id != R.id.mainFragment) {
            navController.popBackStack()
        } else {
            binding.messageLayout.visibility = View.GONE
        }
    }

    private fun registerPredictiveBackCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val callback = OnBackInvokedCallback { handleNavigationBack() }
        predictiveBackCallback = callback
        onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            callback
        )
    }

    private fun unregisterPredictiveBackCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        predictiveBackCallback?.let { onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it) }
        predictiveBackCallback = null
    }
}
