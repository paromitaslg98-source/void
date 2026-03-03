<p align="center">
  <img src="fastlane/metadata/android/en-US/images/icon.png" alt="VOID Launcher" width="100" height="100">
</p>

<h1 align="center">VOID Launcher</h1>

<p align="center">
  <em>A radically minimalist, ad-free Android launcher designed to combat digital addiction.</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=flat-square&logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/Min%20SDK-26%20(Oreo)-blue?style=flat-square" alt="Min SDK">
  <img src="https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-blue?style=flat-square" alt="Target SDK">
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?style=flat-square&logo=kotlin" alt="Language">
  <img src="https://img.shields.io/badge/License-GPLv3-red?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/Size-%3C%202MB-brightgreen?style=flat-square" alt="Size">
</p>

---

## Philosophy

VOID replaces the traditional grid of attention-grabbing app icons with a clean, text-based interface. No ads, no tracking, no distractions — just the essentials you need, accessible through intuitive gestures.

---

## Features

| Feature | Description |
|---|---|
| **Text-Only Home Screen** | Up to 10 pinned apps displayed as clean text shortcuts |
| **Fast Search & App Drawer** | Swipe up for all apps; unified search bar for apps and web |
| **Private Space** *(Android 15+)* | Biometric-secured access to hidden/work-profile apps |
| **Digital Wellbeing** | Real-time screen time and unlock count on the home screen |
| **Swipe Gestures** | Left/right swipe to quick-launch designated apps |
| **Double Tap to Lock** | Lock device by double-tapping empty space |
| **Hidden Apps** | Long-press any app to hide it from the drawer |
| **Daily Wallpapers** | Fresh minimalist wallpapers fetched in the background |
| **Theme Support** | Light, Dark, or System default themes |
| **Text & Layout Controls** | Adjustable text size and home screen alignment |

---

## Gestures

```
Home Screen
├── Swipe Up         → App Drawer
├── Swipe Down       → Notification Panel / Search (configurable)
├── Swipe Left/Right → Quick-launch app
├── Long Press        → Settings
└── Double Tap       → Lock Screen

App Drawer
├── Type to search   → Filter apps or search the web
├── Type "private"   → Reveal Private Space apps
└── Long Press App   → Hide / App Info / Uninstall
```

---

## Project Structure

```
app/src/main/java/com/voidlauncher/app/
├── MainActivity.kt              # Single-activity entry point
├── MainViewModel.kt             # Shared ViewModel
├── ui/
│   ├── HomeFragment.kt          # Home screen with time, metrics, pinned apps
│   ├── AppDrawerFragment.kt     # App list, search, Private Space
│   ├── AppDrawerAdapter.kt      # RecyclerView adapter for app rows
│   └── SettingsFragment.kt      # Configuration panel with pop-up dialogs
├── helper/
│   ├── Utils.kt                 # App resolving, intents, permissions
│   └── usageStats/              # Screen time & unlock tracking
├── data/
│   └── Prefs.kt                 # SharedPreferences delegates
├── worker/
│   └── WallpaperWorker.kt       # Daily wallpaper fetch via WorkManager
└── listener/
    ├── OnSwipeTouchListener.kt  # Swipe gesture detection
    └── ViewSwipeTouchListener.kt
```

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Android ViewBinding + XML Layouts + Material Design 3
- **Architecture:** Single Activity, Fragments, ViewModels, LiveData, Navigation Component
- **Background Work:** WorkManager
- **Build System:** Gradle with Version Catalogs (`libs.versions.toml`)

---

## Building from Source

**Prerequisites:** Android Studio (Koala+), Android SDK API 35

```bash
# Clone
git clone https://github.com/knownassurajit/void.git
cd void

# Build debug APK
./gradlew clean assembleDebug
```

The output APK will be at `app/build/outputs/apk/debug/`.

---

## License

This project is licensed under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html).

Restructured and renamed from the original open-source project [Olauncher](https://github.com/tanujnotes/olauncher). Special thanks to the original contributors.
