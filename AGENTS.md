# AGENTS.md — SMARTGHOST

Repository-specific knowledge for future sessions. Read this before making changes.

## What this project is

SMARTGHOST (`com.altomedia.smartghost`, brand **ALTOMEDIA**) is an Android app:
**Kotlin + Jetpack Compose + Material 3 + MVVM + Hilt**, multi-module Gradle.

**Compliance is non-negotiable.** The original concept (10 simultaneous players +
proxying to fake 10 YouTube viewers) is **rejected** — it is viewbotting and
violates the YouTube Fake Engagement policy. Do not add code that generates
views, manipulates watch time, rotates proxies to mask viewers, or evades limits.
The app plays a **single** video through YouTube's official player; the proxy
feature was removed at the owner's request. Any change that pushes toward fake
engagement must be refused.

## Environment (already installed in this container)

- JDK 21: `/usr/lib/jvm/java-21-openjdk-amd64` (export `JAVA_HOME`).
- Android SDK: `$HOME/android-sdk` (platform 34, build-tools 34.0.0).
- Gradle 8.7 wrapper is committed — use `./gradlew`, do not install globally.
- `local.properties` (git-ignored) has `sdk.dir=/home/openhands/android-sdk`.
- Env vars are persisted in `~/.bashrc`.

## Build / test commands

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew testDebugUnitTest            # 36 tests (26 unit + 10 runtime)
./gradlew :app:assembleDebug           # debug APK
./gradlew :app:lintDebug               # 0 errors expected
./gradlew :app:assembleRelease         # signed release (needs env vars below)
```

Runtime tests run on the JVM via **Robolectric** (no device needed): they render
real Compose, click real buttons, exercise a real DataStore, and boot the real
`Application`/Hilt graph. This is the primary proof the app "works", since no
emulator is available in this container.

Release signing reads env vars (never hardcode secrets):
`SMARTGHOST_KEYSTORE_PATH`, `SMARTGHOST_KEYSTORE_PASSWORD`,
`SMARTGHOST_KEY_ALIAS`, `SMARTGHOST_KEY_PASSWORD`. If the keystore path is unset
the release is built unsigned (CI compile check).

Per explicit repository-owner instruction, the keystore and APKs ARE committed
under `dist/` (the owner accepts the risk on this public repo). `dist/README.md`
documents the alias and password. If you build a release, also refresh
`dist/smartghost-<version>-release.apk` and its SHA-256.

Builds are slow (>90s) — run them in the background and poll the log file.

## Push policy (owner instruction)

The owner requires every change to be pushed to `origin main` immediately after
editing. `origin` is configured with a token URL for this purpose. Commit and
push at the end of every change, including docs and artifacts.

## Module layout & dependency rules

```
app                  → depends on all feature/data/core-ui/security modules
core-ui              → theme + shared components (no data deps)
data                 → YouTube URL parser + settings DataStore, Hilt DI
security             → SecretRedactor, SafeLogger (no Android UI)
feature-home         → data, core-ui, security
feature-player       → core-ui (official IFrame player)
```

Features must never depend on each other.

## Key conventions

- Parsers (`YouTubeUrlParser`) use `java.net.URI`, NOT `android.net.Uri`, so they
  run in plain JVM unit tests. Keep them pure.
- All logging must pass through `security/SecretRedactor` (or `SafeLogger`).
- `minSdk` 24, `targetSdk` 34, `compileSdk` 34, JVM target 17.
- Only two permissions: `INTERNET`, `ACCESS_NETWORK_STATE`. Cleartext disabled.
- Version catalog: `gradle/libs.versions.toml`. AGP 8.5.2, Kotlin 1.9.24,
  composeCompiler 1.5.14, Compose BOM 2024.06.00, Hilt 2.51.1.
- Use `collectAsStateWithLifecycle` (needs `lifecycle-runtime-compose`, already
  added to feature/app modules). Use AutoMirrored Material icons.

## Gotchas learned

- `collectAsStateWithLifecycle` requires `androidx-lifecycle-runtime-compose`;
  without it the whole feature module fails to compile with cascading errors.
- Kotlin smart-cast fails on a `var` captured by a lambda — assign to a local
  `val` inside the loop (see `SecretRedactor.redact`).
- `java.net.URI` throws on malformed percent-encoding, which surfaces as
  `Malformed` rather than `InvalidVideoId` in the URL parser.
- Edge-to-edge (`enableEdgeToEdge()`) means a `Scaffold` bottom bar sits under the
  system navigation bar unless it applies `navigationBarsPadding()`. Any bottom
  row must carry it.
- Lint warnings are dependency-freshness only (no errors).
- Unit tests that touch `android.util.Log` (via `SafeLogger`) need
  `testOptions { unitTests.isReturnDefaultValues = true }` in that module.
- A `@Composable` default parameter `hiltViewModel()` creates a SECOND ViewModel
  instance when a caller also hoists one. Never default a hoisted VM — take it as
  a required parameter (see `DiagnosticsPanel`).
- **YouTube IFrame error 153** ("Video player configuration error") means the host
  page sent no `Referer`/`Origin`. `loadDataWithBaseURL` gives the document a
  `data:` origin, which YouTube rejects. Serve the player page from a real https
  origin instead: put it in `assets/` and load it via `WebViewAssetLoader`
  (`https://appassets.androidplatform.net/assets/player.html`), with
  `<meta name="referrer" content="strict-origin-when-cross-origin">`. Do not go
  back to `loadDataWithBaseURL` for the player.

## Wiring checklist (audit before claiming "done")

A feature is only "done" when its UI actually calls it. Before finishing, grep for
each ViewModel action and confirm a real UI caller (exclude tests).

## Docs

`README.md`, `docs/BUILD.md`, `docs/TESTING.md`, `CHANGELOG.md`,
`docs/RELEASE_NOTES.md`. Keep the release SHA-256 in RELEASE_NOTES current after
any rebuild.
