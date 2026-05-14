## Summary

- **Build stability** — fix `compileSdk`/`targetSdk` mismatch and add ProGuard keep rules that prevented MLKit GenAI classes from being stripped during release minification, causing corrupt/crashing packages.
- **Widget screen** — replace static grayscale preview images with live, interactive `AppWidgetHostView` instances. Widgets are now real system widgets rendered in a scrollable list; tapping them fires their native PendingIntents (redirects to the app).
- **MLKit fallback** — when on-device AI is unavailable (Tier 3), produce compact minimum-token summaries (`Title: body[0..70]`) instead of verbatim full-text dumps.

---

## Changes

### `build.gradle.kts`
- `compileSdk 35 → 36` to match `targetSdk = 36`. Having `targetSdk > compileSdk` is invalid per AGP 8.x and caused lint/build failures.

### `gradle.properties`
- `android.suppressUnsupportedCompileSdk=35 → 36` to match the updated `compileSdk`.

### `proguard-rules.pro`
- Added `-keep` rules for:
  - `com.google.mlkit.genai.**` — MLKit GenAI uses reflection-based class loading for on-device feature delivery; without keeps, R8 strips these, producing a corrupt release build.
  - `com.google.android.gms.internal.mlkit_genai_*` — GMS internals for prompt and summarization.
  - `com.google.common.util.concurrent.ListenableFuture` — Guava futures adapter used by the GenAI async API.
  - `kotlinx.coroutines` volatile fields and dispatcher factories — required for coroutine machinery.

### `src/main/AndroidManifest.xml`
- Added `android.permission.BIND_APPWIDGET` — required for a launcher to bind live widgets via `AppWidgetManager.bindAppWidgetIdIfAllowed()`.

### `src/main/java/.../data/Prefs.kt`
- Added `widgetAllocatedIds: Set<String>` property (key `WIDGET_ALLOCATED_IDS`) storing `"providerFlatName|appWidgetId"` pairs, persisting bound widget IDs across launches.

### `src/integrated/java/.../ui/screen/WidgetsScreen.kt` _(full rewrite)_
- `WidgetsViewModel` now owns an `AppWidgetHost` (host ID `1024`).
  - `pinWidget()` calls `bindAppWidgetIdIfAllowed()`, stores the allocated ID, and falls back with a toast if the launcher does not have the permission yet.
  - `unpinWidget()` calls `deleteAppWidgetId()` to release the system resource.
  - `onCleared()` calls `stopListening()` as a safety net for ViewModel teardown.
  - `refreshPinnedAndIds()` auto-migrates installs from the old version by dropping pinned entries that have no corresponding allocated ID (no crash, no orphaned UI).
- `WidgetsScreen` composable:
  - `DisposableEffect` manages `startListening()` / `stopListening()` tied to screen visibility.
  - `LazyColumn` replaces the old `LazyVerticalGrid` — widgets display as first-class, full-width content.
  - Explicit **Add** button in the header replaces the long-press-to-add flow.
  - Per-widget `x` remove button replaces the long-press resize/remove dialog. Widget dimensions now follow `AppWidgetProviderInfo.minHeight` directly.
- `LiveWidgetItem` composable:
  - `AndroidView` embeds a real `AppWidgetHostView`; touch events pass through unblocked so PendingIntents fire natively (clock widgets open alarms, weather/calendar widgets open the app, etc.).
  - Widget height clamped to `max(provider.minHeight, 100).dp`.
  - `createView()` wrapped in try-catch; degrades to a blank `FrameLayout` if the provider is unavailable rather than crashing.
- `WidgetPickerSheet` is now add-only; `groupedWidgets` `remember()` moved outside the `ModalBottomSheet` content lambda to avoid recomputation on every recomposition.
- Removed: `WidgetCard`, all Bitmap/Canvas/ColorMatrix imports, `LazyVerticalGrid`, `widgetSpans` state, `updateWidgetSpan()`.

### `src/integrated/java/.../helper/AiSummarizer.kt`
- `fallbackSummarize()` (Tier 3, no on-device AI) now extracts `"Title: body[0..70]"` for colon-delimited notifications, or truncates at 80 characters otherwise. Compact bullets instead of full-text verbatim dumps.

---

## Test checklist

### Build verification
- [ ] `./gradlew assembleIntegratedDebug` completes without errors
- [ ] `./gradlew assembleIntegratedRelease` completes — R8/ProGuard must not strip MLKit classes
- [ ] `./gradlew assembleDisintegratedRelease` completes without errors
- [ ] Installed release APK does not crash on first launch (validates ProGuard keep rules)
- [ ] `./gradlew testIntegratedDebugUnitTest` passes

### Widget screen — live widgets
- [ ] Set VOID Launcher as the default launcher (required for `BIND_APPWIDGET`)
- [ ] Open Widgets screen — empty state shows "Tap Add to pin widgets from your installed apps"
- [ ] Tap **Add** — picker sheet lists all installed widget providers grouped by app
- [ ] Pin a simple widget (e.g., Clock, Calculator) — appears in the scrollable list as a live widget, not a static image
- [ ] Tap the live widget — widget's own action fires (opens the associated app)
- [ ] Scroll down when multiple widgets are pinned — LazyColumn scrolls smoothly
- [ ] Tap the `x` button on a widget — confirmation dialog appears; confirm removes it from list
- [ ] Kill and relaunch the app — previously pinned widgets are restored from `WIDGET_ALLOCATED_IDS`
- [ ] Upgrade path: old `PINNED_WIDGETS` entries without `WIDGET_ALLOCATED_IDS` counterparts are silently dropped (no crash)
- [ ] On a device where `BIND_APPWIDGET` is denied — toast "Cannot bind widget. Set VOID Launcher as default launcher and try again." is shown

### Widget screen — edge cases
- [ ] Widget provider that requires a configuration activity — widget is added without crash; shows default/blank state (configuration activity not auto-launched — known limitation)
- [ ] Uninstall an app that had a pinned widget while the launcher is running — no crash; widget absent on next screen open
- [ ] Dark/light theme toggle — widgets re-render correctly inside AndroidView

### MLKit fallback (Tier 3)
- [ ] Device without `com.google.android.aicore` — `isAvailable()` returns `false`; notification summary screen shows the "On-device AI unavailable" banner
- [ ] Short notification `"OTP: 482910 is your one-time password"` — fallback produces `• OTP: 482910 is your one-time password` (no truncation, under 80 chars)
- [ ] Long notification `"PayApp: You have received 1500 from Rahul Sharma. Your updated balance is 23450. Tap to view full transaction history."` — produces `• PayApp: You have received 1500 from Rahul Sharma. Your updated balance is 234` (title + 70-char body)
- [ ] Plain long text with no colon in first 50 chars — truncated to 80 chars + ellipsis
- [ ] Empty / blank notification texts — filtered out; no empty bullets appear

### Regression checks
- [ ] Home screen swipe gestures still navigate to the widget screen
- [ ] Disintegrated flavor: widget screen still shows "Feature Unavailable" stub
- [ ] Disintegrated flavor: notification summary screen still shows "Feature Unavailable" stub
- [ ] Settings, App Drawer, Notes, Notification Summary screens (integrated) unaffected

---

## Known limitations

- **Widget configuration activities**: Widgets declaring `android:configure` in their provider XML require a post-bind config step. This is not yet implemented; affected widgets display in default/blank state.
- **Scroll conflict with scrollable widgets**: Widgets that are internally scrollable (ListView-based) may conflict with the outer `LazyColumn`. `nestedScrollInterop` was intentionally omitted to keep implementation simple.
- **`BIND_APPWIDGET` on non-default launchers**: The permission is auto-granted only when VOID is the default launcher. Users who have not set it as default will see the actionable toast.

---

Generated with [Claude Code](https://claude.com/claude-code)
