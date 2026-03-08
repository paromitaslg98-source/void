<p align="center">
  <img src="fastlane/metadata/android/en-US/images/icon.png" alt="VOID Launcher" width="100" height="100" style="border-radius: 50%;">
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

**VOID** is not just a launcher; it's a tool for digital minimalism. By stripping away colorful icons, badges, and the traditional grid layout, VOID forces intentionality. We present a hyper-clean, text-based interface where your focus dictates your actions, not the other way around. No ads, no tracking, no distractions.

---

## Core Features

- **Text-Only Home Screen:** Up to 10 of your most important apps are pinned to the home screen as clean, customizable text labels inside a single unified card (`#363636`) with subtle dividers between entries.
- **Notification Grouping:** Swipe left to access a purpose-built notification screen that groups system notifications by app with smart summaries, timestamps, and inline expansion — styled in the launcher's monochrome theme.
- **Quick Notes:** Swipe right to instantly capture thoughts in a monochrome, text-based checklist — native to the launcher with priority ordering, tick-to-complete, swipe-to-delete, and a per-note options menu (edit, remind, delete).
- **Hold to Edit & Reorder:** Long-press a home screen app to reveal inline pen (reassign) and reorder (drag) icons — tap pen to swap the app, drag the reorder icon to rearrange.
- **Directional Animations:** Every gesture transition slides in from the direction you swiped toward: left swipe slides in from left, right from right, up from bottom, down from top.
- **Robust App Launcher:** Advanced component resolution ensures that even when applications update their internal packages or icon labels, VOID will dynamically re-resolve their launch intents.
- **Deep Private Space Integration:** Built for Android 15+. Access your hidden, secure, or work-profile apps directly from the main drawer with biometric unlock.
- **Digital Wellbeing Built-in:** See your actual screen time and unlock count overlaid on the home screen immediately.
- **Separate Text Size Controls:** Independent text size scaling for the home screen and app drawer.
- **Fluid Inline Settings UI:** Configure your launcher entirely within the app with smooth-animated inline cards.
- **Daily Wallpapers:** Automatically fetch and apply fresh, minimalist wallpapers (opt-in).

---

## Interaction & Gestures

```text
Home Screen
├── Swipe Up         → Opens App Drawer (auto-focuses search bar)
├── Swipe Down       → Expands Notification Panel / Web Search (configurable)
├── Swipe Left       → Notification Grouping Screen
├── Swipe Right      → Quick Notes Screen
├── Long Press       → Advanced Inline Settings Panel
├── Double Tap       → Sleep/Lock Screen
└── Hold + Drag      → Reorder home screen apps

App Drawer
├── Type to search   → Instantly filter apps or query the web
├── Type "private"   → Unlock biometric Private Space and reveal hidden apps
└── Long Press App   → Hide App / Open System App Info / Uninstall
```

---

## Technical Architecture

VOID Launcher embraces modern Android development practices, ensuring a tiny memory footprint while remaining highly performant.

- **Stack:** 100% Kotlin
- **UI:** XML Layouts, Material Design 3 guidelines (M3 typography, outlined cards, clean padding), and Android ViewBinding.
- **Architecture:** Single-Activity, Fragment-based navigation powered by a shared `MainViewModel` utilizing `LiveData` and Kotlin Coroutines.
- **Notification System:** A `NotificationListenerService` intercepts system-wide notifications and groups them intelligently by app and conversation.
- **Notes Storage:** Lightweight `SharedPreferences`-backed JSON persistence — no heavy database dependency for a launcher.
- **Background Processes:** Reliable `WorkManager` API to execute low-impact background fetches (e.g., daily wallpaper downloads).
- **Hardware Support:** Fully compatible with specialized hardware like E-Ink arrays.

---

## Building from Source

**Prerequisites:**
- Android Studio Koala (or newer)
- Android SDK API 35
- JDK 17+

```bash
# Clone the repository
git clone https://github.com/knownassurajit/void.git
cd void

# Build the debug APK
./gradlew clean assembleDebug
```

The output APK will be generated at `app/build/outputs/apk/debug/`.

---

## Acknowledgments & License

This project is licensed under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html).

VOID is a heavily restructured, modernized, and refined fork of the original open-source project [Olauncher](https://github.com/knownassurajit/olauncher). Special thanks and credit to the original contributors for laying the foundational concept of a text-only, minimalist interface.
