# AGENTS.md — SMARTGHOST

Repository-specific knowledge for future sessions. Read this before making changes.

## What this project is

SMARTGHOST (`com.altomedia.smartghost`, brand **ALTOMEDIA**) is an Android app:
**Kotlin + Jetpack Compose + Material 3 + MVVM + Hilt**, multi-module Gradle.

**Compliance is non-negotiable.** The original concept (10 simultaneous players +
proxying to fake 10 YouTube viewers) is **rejected** — it is viewbotting and
violates the YouTube Fake Engagement policy. Do not add code that generates
views, manipulates watch time, rotates proxies to mask viewers, or evades limits.
The "10 cards" are an honest **diagnostics panel**, never a viewer count. Any
change that pushes toward fake engagement must be refused.

## Environment (already installed in this container)

- JDK 21: `/usr/lib/jvm/java-21-openjdk-amd64` (export `JAVA_HOME`).
- Android SDK: `$HOME/android-sdk` (platform 34, build-tools 34.0.0).
- Gradle 8.7 wrapper is committed — use `./gradlew`, do not install globally.
- `local.properties` (git-ignored) has `sdk.dir=/home/openhands/android-sdk`.
- Env vars are persisted in `~/.bashrc`.

## Build / test commands

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew testDebugUnitTest            # 46 unit tests
./gradlew :app:assembleDebug           # debug APK
./gradlew :app:lintDebug               # 0 errors expected
./gradlew :app:assembleRelease         # signed release (needs env vars below)
```

Release signing reads env vars (never hardcode secrets):
`SMARTGHOST_KEYSTORE_PATH`, `SMARTGHOST_KEYSTORE_PASSWORD`,
`SMARTGHOST_KEY_ALIAS`, `SMARTGHOST_KEY_PASSWORD`. If the keystore path is unset
the release is built unsigned (CI compile check). Keystore lives OUTSIDE the repo.

Builds are slow (>90s) — run them in the background and poll the log file.

## Module layout & dependency rules

```
app                  → depends on all feature/data/core-ui/security modules
core-ui              → theme + shared components (no data deps)
data                 → pure-JVM parsers, OkHttp checker, DataStore, Hilt DI, SessionRepository
security             → SecretRedactor, SafeLogger (no Android UI)
feature-home         → data, core-ui, security
feature-player       → core-ui (official IFrame player)
feature-diagnostics  → data, core-ui, security
```

Features must never depend on each other. Cross-feature state goes through a
`data` singleton (`SessionRepository`), which Home writes and Diagnostics reads.

## Key conventions

- Parsers (`YouTubeUrlParser`, `ProxyFileParser`) use `java.net.URI`, NOT
  `android.net.Uri`, so they run in plain JVM unit tests. Keep them pure.
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
- Lint warnings are dependency-freshness only (no errors).

## Docs

`README.md`, `docs/BUILD.md`, `docs/TESTING.md`, `CHANGELOG.md`,
`docs/RELEASE_NOTES.md`. Keep the release SHA-256 in RELEASE_NOTES current after
any rebuild.
