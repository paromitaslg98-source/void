<p align="center">
  <img src="fastlane/metadata/android/en-US/images/icon.png" alt="VOID Launcher" width="100" height="100" style="border-radius: 50%;">
</p>

<h1 align="center">VOID Launcher</h1>

<p align="center">
  <em>A minimalist Android launcher, now refactored to a Compose-first architecture.</em>
</p>

---

## Project Status

The UI stack has been fully refactored away from XML layouts, Fragments, and ViewBinding to a **Jetpack Compose-first** setup.

### What changed in this refactor

- `MainActivity` now uses `ComponentActivity` + `setContent {}`.
- Navigation is now provided by `navigation-compose` with type-safe `@Serializable` route objects.
- The app theme is defined in Kotlin (`ui/theme/Theme.kt`) via `MaterialTheme` and `ColorScheme`.
- All screen entry points are Composable functions under `ui/screen/`.
- Legacy XML screen layouts and XML navigation graph were removed.
- Legacy Fragment-based UI classes and RecyclerView adapters were removed from the primary UI path.

---

## Compose Architecture (Current)

### UI

- **Jetpack Compose** (Material 3)
- **Compose Navigation** (`NavHost`)
- **StateFlow-first UI state collection** (`collectAsStateWithLifecycle`)

### Key files

- `app/src/main/java/com/voidlauncher/app/MainActivity.kt`
- `app/src/main/java/com/voidlauncher/app/AppRoutes.kt`
- `app/src/main/java/com/voidlauncher/app/MainUiViewModel.kt`
- `app/src/main/java/com/voidlauncher/app/ui/theme/Theme.kt`
- `app/src/main/java/com/voidlauncher/app/ui/screen/*`

### Package structure

```text
app/src/main/java/com/voidlauncher/app
├── MainActivity.kt                # Compose app shell + NavHost
├── AppRoutes.kt                   # @Serializable route objects
├── MainUiViewModel.kt             # StateFlow-based UI state
├── ui/
│   ├── theme/Theme.kt             # MaterialTheme configuration
│   ├── screen/
│   │   ├── HomeScreen.kt
│   │   ├── AppDrawerScreen.kt
│   │   ├── SettingsScreen.kt
│   │   ├── NotificationsScreen.kt
│   │   └── NotesScreen.kt
│   └── FakeHomeScreen.kt
├── data/                          # persistence/domain models
├── helper/                        # services/workers/system integration
└── listener/                      # admin/gesture listeners
```

---

## Build & Test

### Prerequisites

- Android Studio Koala+ (or equivalent Gradle/SDK setup)
- Android SDK API 35
- **JDK 21** (required by `gradle-daemon-jvm.properties`)

### Build

```bash
./gradlew clean :app:assembleDebug
```

### E2E (instrumentation) test flow

Run on a connected device or emulator:

```bash
# 1) Build + install debug APK
./gradlew :app:installDebug

# 2) Run instrumentation tests (when androidTest cases exist)
./gradlew :app:connectedDebugAndroidTest
```

If no emulator/device is attached, start one first from Android Studio Device Manager.

---

## Refactor Validation Checklist

- [x] No `res/layout/` directory
- [x] No `res/navigation/` XML graph
- [x] No Fragment/ViewBinding-based main UI entrypoint
- [x] Compose BOM and Material3 configured in Gradle
- [x] Compose `NavHost` wired from `MainActivity`

---

## License

This project is licensed under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html).

VOID is a fork/rework inspired by [Olauncher](https://github.com/knownassurajit/olauncher).
