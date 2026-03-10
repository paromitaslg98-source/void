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
  - `com.launcher.void`
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

## Current architecture

```text
app/src/main/java/com/launcher/void
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
