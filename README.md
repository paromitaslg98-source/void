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

### 2) Seamless Private Space Merge
- Synchronized with native device biometric states listening securely for `ACTION_MANAGED_PROFILE_UNLOCKED`.
- Deprecated strict segregated visual compartments for explicit private profile isolation.
- Homogenized Private App instances safely within the primary AppDrawer registry list rendering with inline Lock indicators autonomously upon authorization.

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
│   │   └── NotesScreen.kt
│   └── FakeHomeScreen.kt
├── data/
├── helper/
└── listener/
```

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
