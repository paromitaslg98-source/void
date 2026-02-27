# 🚀 VOID Launcher (formerly Olauncher)

**VOID Launcher** is a radically minimalist, ad-free (AF) Android launcher designed to combat digital addiction and promote digital well-being. It eschews colorful icons and grids in favor of a clean, text-based interface that minimizes distractions and helps you use your phone purposefully. 

## 📖 Table of Contents
- [📱 Screenshots & Design](#-screenshots--design)
- [✨ Key Features](#-key-features)
- [🏗 Project Structure & Architecture](#-project-structure--architecture)
- [🛣 Screen Flow & Navigation](#-screen-flow--navigation)
- [🛠 Tech Stack & Dependencies](#-tech-stack--dependencies)
- [⚙️ Building the Source](#️-building-the-source)
- [📄 License & Credits](#-license--credits)

---

## 📱 Screenshots & Design

VOID Launcher features an ultra-clean, text-based minimalist design.

<div align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/01.jpg" width="200"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/02.jpg" width="200"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/03.jpg" width="200"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/04.jpg" width="200"/>
</div>

---

## ✨ Key Features

Despite its minimal footprint (< 2MB), VOID Launcher is highly functional. To maintain simplicity, most features are accessed via gestures and long-presses:

- **Text-based Home Screen**: Clean, text-only shortcuts to up to 8 of your most essential apps.
- **Fast App Drawer**: Swipe up to instantly access all apps.
- **Auto-keyboard Search**: Optionally deploy the keyboard automatically upon opening the drawer for immediate search queries.
- **Quick Swipe Gestures**: Swipe left or right on the home screen to instantly launch designated apps (e.g., Camera, Phone).
- **Double Tap to Lock**: Easily lock your device simply by double-tapping empty space (Requires Accessibility or Device Admin permissions).
- **Hidden Apps**: Keep your app drawer clutter-free by long-pressing an app to hide it.
- **Visual Customizations**:
  - App list alignment (Left, Center, Right).
  - Hide/Show system Status Bar.
  - Hide/Show Date/Time.
  - Light, Dark, or System default theme syncing.
- **Daily Wallpapers**: Fresh, high-quality minimalistic wallpapers fetched and updated daily.

---

## 🏗 Project Structure & Architecture

VOID Launcher uses Android's modern native development tools and architectures:

- `app/src/main/java/com/voidlauncher/app/`
  - `ui/`: Contains heavily cohesive view components like `HomeFragment`, `AppListFragment`, and `SettingsFragment`.
  - `helper/`: Utility functions spanning bitmap handling, intent wrappers, display metrics, and Kotlin extension functions (`Extensions.kt`, `Utils.kt`).
  - `data/`: Core constants and SharedPreferences delegates (`Prefs.kt`) handling the user's customized text-alignments, app limits, and states.
  - `worker/`: `WallpaperWorker.kt` implements Android `WorkManager` for fetching daily abstract wallpapers smoothly in the background without draining the battery.
  - `listener/`: Touch layer controllers translating raw coordinates into semantic gestures (e.g., `ViewSwipeTouchListener`).

---

## 🛣 Screen Flow & Navigation

The Application operates through a Single Activity Architecture (`MainActivity.kt`) orchestrated via Android's Jetpack Navigation Component.

1. **Home Screen (`HomeFragment`)**: The default entry point. Rendered dynamically. Shows date/time and up to 8 pinned apps. Listens to swipe gestures.
2. **App Drawer (`AppListFragment`)**: Invoked by swiping up on the Home screen. Contains a fast scrollable list of all installed packages utilizing a performant `RecyclerView`.
3. **Settings Panel (`SettingsFragment`)**: Invoked by a long-press on the Home Screen. Provides a scrolling toggle interface for adjusting Text Size, Accessibility permissions, daily wallpapers, and more.

---

## 🛠 Tech Stack & Dependencies

- **Language:** Kotlin
- **Minimum SDK:** 26 (Android 8.0) | **Target SDK:** 35
- **UI Toolkit:** Android ViewBinding, XML Layouts, Google Material Design Components.
- **Architecture Components:**
  - ViewModels & LiveData
  - Navigation Graph (`res/navigation/nav_graph.xml`)
  - WorkManager
- **Build System:** Gradle (Kotlin DSL ready) utilizing Version Catalogs (`libs.versions.toml`).

---

## ⚙️ Building the Source

1. Clone the repository.
2. Ensure you have the Android SDK (API 35) installed.
3. Because the package was renamed to `com.voidlauncher.app`, ensure there are no remnants of the old `app.olauncher` package.
4. Run the Gradle wrapper:
   ```bash
   ./gradlew clean build assembleDebug
   ```

---

## 📄 License & Credits

License: [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html)

App renamed and restructured from the original open-source base "Olauncher".
Dev for original base: [X/twitter](https://x.com/tanujnotes) • [Bluesky](https://bsky.app/profile/tanujnotes.bsky.social)
