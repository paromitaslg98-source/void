# VOID Launcher (Compose Edition)

VOID Launcher is a minimalist launcher refactored to a Compose-first architecture.

## Migration Review (Final)

As part of the final Compose migration review, the project was audited for:
- **Security** risks in manifest/configuration
- **Performance** and unnecessary legacy UI artifacts
- **UI/UX** consistency with Material 3 patterns
- **Project hygiene** after XML/Fragment → Compose migration

## What was cleaned up

### 1) Package rename (requested)
- Application package/namespace changed to:
  - `com.launcher.projectvoid`
- Updated across Gradle and Kotlin sources.

### 2) Compose navigation + transition polish
- Compose `NavHost` remains the main UI entry.
- Added direction-aware transitions for Notes/Notifications flows to preserve gesture mental model with Material motion.

### 3) Security hardening
- Removed privileged/inapplicable permission:
  - `android.permission.EXPAND_STATUS_BAR`
- Added cleartext network hardening:
  - `android:usesCleartextTraffic="false"`

### 4) Legacy cleanup
- Removed stale menu resource not used after Compose migration:
  - `res/menu/menu_note_options.xml`

## Feature Refinements (Interactive UI Update)

### 1) Widget Dimensional Rescaling
- Integrated adaptive dimensional rescaling on the **Widgets Screen**.
- Converted primary bounds to a scalable `GridCells.Fixed(4)` matrix.
- Enabled long-press context interface on pinned widgets rendering (2x1, 2x2, 4x2) span overrides dynamically propagating through SharedPreferences.

### 2) Advanced Private Space Integration (Android 15+)
- **Automated Profile Detection**: Engineered a dual-layer profile detection system using `UserManager.userProfiles` and `LauncherApps.getLauncherUserInfo` to reliably identify Hidden Profiles (Private Space) on Android 15.
- **Dynamic Sectioning**: Implemented a dedicated Private Space section at the end of the app library, separated by a visual divider, ensuring clear isolation of sensitive applications.
- **Biometric Synchronization**: Fully synchronized with native OS states using intensified broadcast monitoring for `ACTION_PROFILE_ACCESSIBLE`, `ACTION_MANAGED_PROFILE_UNLOCKED`, and `ACTION_PROFILE_INACCESSIBLE`.
- **Permission & Role Requirements**: Optimized for the `ACCESS_HIDDEN_PROFILES` signature permission; requires VOID to be set as the **Default Launcher** for full kernel-level access to the private profile sandbox.

### 3) Core Dimensional Lockdown
- Removed reactive bounding instructions (`.systemBarsPadding()`) preventing unpredictable resolution stretch or layout fracturing during OS visibility interruptions.
- Engineered solid rigid bounding metrics natively fixing component scales.

### 4) Notes UI Refinement
- Obsoleted legacy dropdown interaction hierarchies and `Delete Note` textural placeholders on swipe.
- Surfaced instant Native Trashcan iconography optimizing interactions seamlessly within layout parameters.

## Current architecture

```text
app/src/main/java/com/launcher/projectvoid
├── MainActivity.kt                # Compose app shell + animated NavHost
├── AppRoutes.kt                   # @Serializable route objects
├── MainUiViewModel.kt             # StateFlow-based UI state
├── ui/
│   ├── theme/Theme.kt             # Material3 theme
│   ├── screen/
│   │   ├── HomeScreen.kt
│   │   ├── AppDrawerScreen.kt
│   │   ├── SettingsScreen.kt
│   │   ├── NotificationsScreen.kt
│   │   ├── NotificationSummaryScreen.kt
│   │   └── NotesScreen.kt
│   └── FakeHomeScreen.kt
├── data/
├── helper/
│   ├── NotificationService.kt
│   └── AiSummarizer.kt
└── listener/
```

## Notification UI + AI summary implementation map

- The notifications UX is Compose-first and implemented in:
  - `ui/screen/NotificationsScreen.kt`
  - `ui/screen/NotificationSummaryScreen.kt`
- Notification collection + dismissal lives in:
  - `helper/NotificationService.kt`
- On-device AI/fallback summarization lives in:
  - `helper/AiSummarizer.kt`
- There is no active Fragment-based notifications screen in the app flow; stale references to `com/voidlauncher/app/ui/NotificationsFragment.kt` should remain removed.

## Build and validation

### Requirements
- Android SDK 35
- JDK 21 (project toolchain requirement)

### Commands
```bash
./gradlew clean :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

## Known environment limitation

In restricted CI/container environments where JDK 21 cannot be installed, Gradle tasks will fail before compilation. In that case, run the commands locally with JDK 21 for full validation.

## License

GPL-3.0
