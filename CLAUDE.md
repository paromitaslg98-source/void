# CLAUDE.md

## VOID Launcher SOP

Minimalist Android launcher. Kotlin + Compose + Material 3. Single-activity.  
SDK: min 26 · target/compile 36 · JDK 17.  
Package: `com.knownassurajit.app.launcher.voidlauncher` · App ID: `com.voidlauncher.app`.

---

## Operating Rules

- Enforce **small, explicit, reviewable diffs**.
- Prefer **Kotlin-first, Compose-only** for new UI.
- Preserve **flavor parity**: `integrated` and `disintegrated` expose identical public APIs.
- Treat `src/main` as shared core; do not ship legacy `app/` code.
- Never commit secrets, generated artifacts, local env files, or debug-only shims.
- Every change must end with:
  - build passes
  - lint passes
  - tests added/updated
  - logs reviewed
  - README updated
  - traceable commit message

---

## Repo Layout

```text
src/main/           shared runtime
src/integrated/     restricted features enabled
src/disintegrated/  Play-safe no-op stubs
app/                legacy fragment code; do not extend
.github/workflows/  CI/CD
docs/               design / ops / decisions
```

Rules:
- Keep feature code co-located.
- Isolate platform/flavor code behind stable interfaces.
- Mirror restricted features across flavors with identical FQNs.
- Prefer abstraction seams over conditionals in `src/main`.

---

## Build / Test / Verify

Run from repo root only.

```bash
./gradlew assembleIntegratedDebug
./gradlew assembleDisintegratedDebug
./gradlew assembleIntegratedRelease
./gradlew bundleDisintegratedRelease

./gradlew testIntegratedDebugUnitTest
./gradlew testDisintegratedDebugUnitTest

./gradlew lintIntegratedDebug
./gradlew lintDisintegratedDebug
```

Single test:
```bash
./gradlew testIntegratedDebugUnitTest --tests "fully.qualified.TestClass"
```

Before merge:
- build both flavors
- lint both flavors
- run affected tests
- run E2E/instrumentation
- verify release artifacts
- verify version bump

---

## Architecture

### App Model

- Single Activity architecture.
- Compose Navigation only.
- Type-safe `@Serializable` routes.
- Separate:
  - UI state
  - side effects
  - persistence
  - system interaction

### Data Flow

```text
UI → ViewModel → StateFlow → Repository → System/Data Layer
```

Rules:
- Enforce unidirectional flow.
- Never mutate state in composables.
- Never place business logic in UI.
- Prefer `StateFlow`.
- Use `LiveData` only for legacy integration.

### Persistence

- SharedPreferences access only through `Prefs.kt`.
- Serialize complex types with `kotlinx.serialization`.
- Treat key renames as migrations.
- Validate corrupted/missing values safely.

---

## Flavor Policy

### integrated

Enable:
- ML Kit GenAI
- NotificationListenerService
- widgets
- Private Space support
- restricted APIs/permissions

### disintegrated

- Ship Play-safe no-op behavior.
- Keep package/class/signature parity with `integrated`.
- Return defaults instead of exceptions.
- Avoid flavor branching in shared code.

Rules:
- Real impl → `src/integrated`
- Stub impl → `src/disintegrated`
- Shared abstractions → `src/main`
- Always degrade gracefully.

---

## Process / Service Boundaries

- Accessibility service runs in isolated process.
- Do not rely on shared statics.
- Treat reflection/private APIs as unstable.
- Add null-safe fallback behavior.
- Preserve process-safe communication boundaries.

---

## UI / Compose Standards

- Compose-only for all new UI.
- No new Fragments/XML Views.
- One composable = one responsibility.
- Prefer stateless composables.
- Extract reusable primitives.
- Avoid unnecessary recomposition.
- Use:
  - `remember`
  - `derivedStateOf`
  - immutable params
  only when justified.

Never hardcode:
- dimensions
- colors
- typography
- spacing

Use design tokens only.

---

## Kotlin Standards

Prefer:
- explicit types
- `val`
- sealed models
- immutable collections
- extension functions
- expression bodies when concise

Avoid:
- `Any`
- deep nesting
- hidden side effects
- unsafe casts
- global mutable state
- dead code

Rules:
- strict null safety
- coroutine-first async
- never block main thread
- small testable functions
- semantic naming only

---

## Logging / Observability

Use structured logging only.

Log:
- failures
- recoveries
- state transitions
- service/process boundaries
- fallback execution
- migration failures

Never log:
- secrets
- tokens
- identifiers
- personal data

Required coverage:
- app startup
- restore flow
- navigation failures
- settings persistence
- launcher actions
- permission denial
- ML/service fallback paths

---

## QA / Testing

### Unit

Cover:
- ViewModels
- serialization
- prefs
- helpers
- routes
- fallback logic

### Instrumentation / E2E

Cover:
- app launch
- drawer/navigation
- swipe actions
- persistence
- service interaction
- process restore
- restricted-feature fallback

Rules:
- add regression tests for all bug fixes
- prefer focused fakes over excessive mocks
- test behavior, not implementation

Minimum:
- critical path coverage
- no untested modified branch

---

## Lint / Static Analysis

Mandatory:
- ktlint
- detekt
- Android Lint

CI must fail on:
- lint issues
- formatting drift
- test failure
- type/build failure

---

## CI / CD

Pipeline order:

```text
install
→ format
→ lint
→ unit-test
→ instrumentation/E2E
→ build
→ sign
→ release
```

Requirements:
- deterministic workflows
- artifact retention
- signed releases
- rollback-ready artifacts
- version tagging
- reproducible builds

Rules:
- fail fast
- never bypass CI
- verify output naming
- verify signing
- verify flavor parity

---

## Git / Repo Hygiene

### Branches

```text
main
develop
feature/*
fix/*
hotfix/*
release/*
```

### Commit Format

```text
feat:
fix:
refactor:
perf:
test:
docs:
build:
ci:
chore:
```

### .gitignore

Ignore:
```text
/build
/.gradle
/local.properties
.idea/
*.keystore
*.jks
*.apk
*.aab
*.log
/reports
```

### .gitkeep

- Use only for intentionally empty directories.
- Remove stale placeholders.
- Never use as fake structure filler.

### PR Rules

- no direct push to main
- squash merge preferred
- CI required
- review required
- docs updated before merge

---

## Documentation

Treat `README.md` as release-critical.

Update after every:
- feature
- refactor
- architecture change
- dependency change
- CI/CD change
- config/env change
- workflow change

README must include:
- setup
- architecture
- flavor matrix
- build/test commands
- env/config
- troubleshooting
- release flow
- known limitations

Maintain:
```text
README.md
CHANGELOG.md
docs/
```

---

## Security

- Never hardcode credentials.
- Use least-privilege permissions.
- Validate all external input.
- Guard reflection/private APIs.
- Sanitize logs/crashes.
- Fail closed on sensitive operations.

---

## Performance

- Avoid main-thread blocking.
- Minimize startup work.
- Lazy-load heavy paths.
- Reduce recomposition.
- Cache only with invalidation.
- Measure before optimization.

---

## Release Checklist

Before release:
- both flavors build
- tests pass
- lint clean
- docs updated
- changelog updated
- version verified
- signing verified
- fallback paths tested
- artifact names verified
- rollback path documented

---

## Done Definition

Complete only when:
- implementation correct
- tests added
- logs updated
- docs updated
- CI green
- release-safe
- repo hygiene preserved