# 🚀 VOID Launcher

**VOID Launcher** is a radically minimalist, ad-free Android launcher designed to combat digital addiction and promote digital well-being. It eschews colorful icons and grids in favor of a clean, text-based interface that minimizes distractions and helps you use your phone purposefully. 

## 📖 Table of Contents
- [🎯 Purpose & Philosophy](#-purpose--philosophy)
- [✨ Key Features](#-key-features)
- [📱 Screen Flow & User Manual](#-screen-flow--user-manual)
- [🏗 Project Structure & Components](#-project-structure--components)
- [🛠 Tech Stack & Dependencies](#-tech-stack--dependencies)
- [⚙️ Building the Source](#️-building-the-source)
- [📄 License & Credits](#-license--credits)

---

## 🎯 Purpose & Philosophy

The modern smartphone is filled with bright, attention-grabbing app icons meant to hook the user. VOID Launcher was built from the ground up to counter this by replacing the traditional grid of apps with a simple, elegant text list. It bridges the gap between aesthetics and productivity by offering just enough utility (like Private Space and daily wallpapers) without the overhead of tracking and ads.

---

## ✨ Key Features

Despite its minimal footprint (< 2MB), VOID Launcher is highly functional. To maintain simplicity, most features are accessed via gestures and long-presses:

- **Text-based Home Screen**: Clean, text-only shortcuts to up to 10 of your most essential apps.
- **Fast App Drawer & Search**: Swipe up to instantly access all apps. A smooth, unified search bar lets you find apps or search the web directly.
- **Private Space Integration (Android 15+)**: Securely unlock and view hidden or work-profile apps directly from the search bar.
- **Digital Wellbeing Tracking**: View your real-time screen time and daily screen unlocks right on your home screen.
- **Quick Swipe Gestures**: Swipe left or right on the home screen to instantly launch designated apps.
- **Double Tap to Lock**: Easily lock your device simply by double-tapping empty space (Requires Accessibility or Device Admin permissions).
- **Hidden Apps**: Keep your app drawer clutter-free by long-pressing an app to hide it.
- **Visual Customizations**:
  - Fine-grained Text Size control with interactive pop-ups.
  - Home screen app alignment (Left, Center, Right, Bottom).
  - Hide/Show system Status Bar and Date/Time.
  - Light, Dark, or System default themes.
- **Daily Wallpapers**: Fresh, high-quality minimalistic wallpapers fetched and updated daily in the background.

---

## 📱 Screen Flow & User Manual

The launcher is designed to be completely fluid, relying heavily on gestures and an uncluttered screen.

### 1. Home Screen
- **Overview**: The default entry point. Shows date, time, screen usage metrics (time & unlocks), and up to 10 pinned apps.
- **Gestures**:
  - **Swipe Up**: Opens the App Drawer.
  - **Swipe Down**: Expands the Notification Panel or opens Search (customizable).
  - **Swipe Left/Right**: Quick-launch user-defined apps.
  - **Long Press Empty Space**: Opens the Settings Screen.
  - **Double Tap Empty Space**: Locks the screen.

### 2. App Drawer / App Library
- **Overview**: A highly responsive list of all installed applications, accessible via a swipe-up from the home screen.
- **Search Bar**: Centered and padded perfectly. Type to filter apps. If no apps match, it allows searching the web securely.
- **Private Space**: Typing "private" reveals locked profile apps, prompting the system biometric dialog for access.
- **Long Press an App**: Opens a context menu to Hide the app, check App Info, or Uninstall it.

### 3. Settings Screen
- **Overview**: Invoked by long-pressing the home screen. Allows configuring the launcher's appearance and behavior.
- **Pop-up Controls**: Changing options like Text Size, Number of Apps, or Layout Alignment uses floating translucent pop-up dialogues to keep context alive in the background.

---

## 🏗 Project Structure & Components

VOID Launcher uses Android's modern native development tools and Single Activity Architecture (`MainActivity.kt` orchestrating Fragments via Jetpack Navigation).

### Core Directories (`app/src/main/java/com/voidlauncher/app/`)
- `ui/`: Contains heavily cohesive view components logic.
  - `HomeFragment.kt`: Renders time, updates screen-time/unlock metrics, and binds pinned apps.
  - `AppDrawerFragment.kt` & `AppDrawerAdapter.kt`: Handles querying the package manager, filtering, Private Space unlocking (`UserManager` API), and rendering `item_app_drawer` rows.
  - `SettingsFragment.kt`: A comprehensive configuration panel utilizing shared preferences to instantly trigger architectural changes (Text Size recreated, alignments updated).
- `helper/`: Utility files defining broad functionality.
  - `Utils.kt`: Core mapping for app resolving, launching intents, and permission checking.
  - `usageStats/EventLogWrapper.kt`: Interacts with `UsageStatsManager` to continuously calculate active Screen Time and device Unlocks.
- `data/`: Core constants and SharedPreferences delegates (`Prefs.kt`) handling the user's customized text-alignments, app limits, and states.
- `worker/`: `WallpaperWorker.kt` implements Android `WorkManager` for fetching daily abstract wallpapers smoothly in the background without draining battery.
- `listener/`: Touch layer controllers translating raw coordinates into semantic gestures (e.g., `ViewSwipeTouchListener`).

---

## 🛠 Tech Stack & Dependencies

- **Language:** Kotlin
- **Minimum SDK:** 26 (Android 8.0) | **Target SDK:** 35 (Android 15)
- **UI Toolkit:** Android ViewBinding, XML Layouts, Material Design Components (MD3).
- **Architecture Components:**
  - ViewModels & LiveData
  - Navigation Graph (`res/navigation/nav_graph.xml`)
  - WorkManager
- **Build System:** Gradle (Kotlin DSL ready) utilizing Version Catalogs (`libs.versions.toml`).

---

## ⚙️ Building the Source

1. Clone the repository to your local machine:
   ```bash
   git clone https://github.com/knownassurajit/void.git
   ```
2. Open the project in **Android Studio** (Koala or newer recommended).
3. Ensure you have the Android SDK (API 35) installed via the SDK Manager.
4. Run the Gradle wrapper to build and install:
   ```bash
   ./gradlew clean build assembleDebug
   ```

---

## 📄 License & Credits

License: [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html)

App renamed and restructured from the original open-source base "Olauncher". Special thanks to the original contributors for laying the foundation of this minimalist experience.
