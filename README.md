# SMARTGHOST

**Brand:** ALTOMEDIA
**Package:** `com.altomedia.smartghost`
**Platform:** Android (Kotlin · Jetpack Compose · Material 3 · MVVM)

SMARTGHOST is a **YouTube URL checker and network diagnostics tool**. It helps you
validate YouTube links, import an authorised `proxy.txt` test configuration, and
run safe endpoint diagnostics — all while playing back a single video through the
**official YouTube IFrame Player API**, controlled entirely by you.

> ### ⚠️ Scope statement — please read first
>
> The original SMARTGHOST concept (running 10 simultaneous players and proxying
> traffic to fabricate 10 fake viewers) is **explicitly rejected**. That behaviour
> is viewbotting, which violates the
> [YouTube Fake Engagement policy](https://support.google.com/youtube/answer/3399767).
>
> This app therefore does **not**, and must never, generate artificial views,
> manipulate watch time, rotate proxies to hide viewer identity, or bypass platform
> limits. The "10 cards" from the original concept are re-implemented as an honest
> **10-card diagnostics panel** that reports endpoint connectivity — never a
> viewer count.

---

## What the app does

| Capability | Description |
| --- | --- |
| YouTube URL validation | Detects `watch`, `youtu.be`, `shorts`, `live`, and `embed` links and validates the video id. |
| Single official playback | One YouTube player via the official IFrame Player API; play/pause follows user action only. |
| `proxy.txt` import | Parses and validates an authorised test-endpoint file (format + line numbers + duplicate detection). |
| 10-card diagnostics panel | START / RELOAD / HOME controls with bounded concurrency, latency and connection status. |
| Secret-safe logging | All logs pass through a redactor; no tokens, passwords, cookies, or keys are ever stored or logged. |

## What the app will never do

- No fake engagement, no view inflation, no watch-time manipulation.
- No multiple simultaneous players.
- No proxy rotation to mask viewers or evade platform limits.
- No storage of passwords, GitHub tokens, YouTube cookies, or private keys.

---

## Module structure

```
SMARTGHOST/
├── app/                  # Application, MainActivity, navigation, signing config
├── core-ui/              # Compose theme (dark navy/charcoal + electric blue/cyan) + shared components
├── data/                 # YouTube URL parser, proxy.txt parser, endpoint checker, DataStore, Hilt DI
├── security/             # SecretRedactor + SafeLogger (log redaction)
├── feature-home/         # URL input, proxy import, START
├── feature-player/       # Single official YouTube player (IFrame Player API)
└── feature-diagnostics/  # 10-card diagnostics panel + session management
```

Dependency direction: `feature-* → data / core-ui / security → app`.
Features never depend on each other.

## Architecture

- **MVVM** with `StateFlow`-backed `ViewModel`s and unidirectional data flow.
- **Hilt** for dependency injection (`@HiltAndroidApp`, `@HiltViewModel`, `@Module`).
- **Jetpack Compose** + **Material 3** UI, dark theme by default.
- Pure-JVM domain logic in `data` (parsers use `java.net.URI`, not `android.net.Uri`)
  so it is fully unit-testable without an emulator.

## Requirements

- JDK 17+ (JDK 21 verified)
- Android SDK Platform 34 + Build-Tools 34.0.0
- Gradle 8.7 (wrapper included)

See [docs/BUILD.md](docs/BUILD.md) for full build and signing instructions.

## Quick start

```bash
export ANDROID_HOME=$HOME/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# Debug build + unit tests
./gradlew testDebugUnitTest
./gradlew :app:assembleDebug

# Signed release (see docs/BUILD.md for keystore env vars)
./gradlew :app:assembleRelease
```

## Testing

```bash
./gradlew testDebugUnitTest
```

Unit tests cover the URL parser, `proxy.txt` parser, endpoint checker, and the
secret redactor. See [docs/TESTING.md](docs/TESTING.md).

## Privacy & security

- No secrets in Git, APK resources, or logs (enforced by `SecretRedactor` + `*.jks`/`local.properties` in `.gitignore`).
- Backups and device-transfer of app data are disabled.
- Cleartext HTTP traffic is disabled (`usesCleartextTraffic="false"`); only HTTPS.
- Only two permissions: `INTERNET` and `ACCESS_NETWORK_STATE`.
- Proxy authentication (if ever needed) must use encrypted storage — never plain text.

## Documentation

- [docs/BUILD.md](docs/BUILD.md) — build, signing, verification
- [docs/TESTING.md](docs/TESTING.md) — test strategy and acceptance criteria
- [CHANGELOG.md](CHANGELOG.md) — version history
- [docs/RELEASE_NOTES.md](docs/RELEASE_NOTES.md) — release notes

## Compliance checklist (Master Plan, page 8–9)

- [x] Package name `com.altomedia.smartghost`
- [x] All main screens with loading / empty / success / error states
- [x] YouTube URLs detected; playback uses official integration
- [x] Ten cards work as an honest diagnostics panel (no artificial views)
- [x] Proxy import restricted to authorised test endpoints
- [x] HOME, RELOAD, START behave per specification
- [x] No secrets in Git, logs, APK resources, or documents
- [x] Release APK passes signature verification
- [x] README, changelog, build instructions, and release notes provided

## Official references

1. YouTube — Fake engagement policy: <https://support.google.com/youtube/answer/3399767>
2. Android — Sign your app: <https://developer.android.com/studio/publish/app-signing>
3. Android — App architecture: <https://developer.android.com/topic/architecture>
4. YouTube — IFrame Player API: <https://developers.google.com/youtube/iframe_api_reference>

---

© ALTOMEDIA. SMARTGHOST.
