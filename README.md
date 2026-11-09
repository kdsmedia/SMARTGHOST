# SMARTGHOST

**Brand:** ALTOMEDIA
**Package:** `com.altomedia.smartghost`
**Platform:** Android (Kotlin · Jetpack Compose · Material 3 · MVVM)

SMARTGHOST is a **YouTube link checker and player**. It validates a YouTube URL and
opens that video through YouTube's **official embed player**, controlled
entirely by you.

> ### ⚠️ Scope statement — please read first
>
> The original SMARTGHOST concept (running 10 simultaneous players and proxying
> traffic to fabricate 10 fake viewers) is **explicitly rejected**. That behaviour
> is viewbotting, which violates the
> [YouTube Fake Engagement policy](https://support.google.com/youtube/answer/3399767).
>
> This app therefore does **not**, and must never, generate artificial views,
> manipulate watch time, rotate proxies to hide viewer identity, or bypass platform
> limits. It plays **one** video, through YouTube's official player, driven only by
> your own play/pause actions.

---

## What the app does

| Capability | Description |
| --- | --- |
| YouTube URL validation | Detects `watch`, `youtu.be`, `shorts`, `live`, and `embed` links and validates the video id. |
| Single official playback | One embedded YouTube player (`youtube.com/embed/<id>`); play/pause follows user action only. |
| Ten-card monitor | A 5 x 2 wall that previews the same video (thumbnail + live status). It is a monitor, not ten players and not a viewer count. |
| Clear playback status | Reports the player's own ready / playing / error state so you can see whether the video runs. |
| Open in YouTube | One tap to open the same video in the official YouTube app or site. |
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
├── app/              # Application, MainActivity, navigation, settings, signing config
├── core-ui/          # Compose theme (dark navy/charcoal + electric blue/cyan) + shared components
├── data/             # YouTube URL parser + settings DataStore (pure-JVM where possible)
├── security/         # SecretRedactor + SafeLogger (log redaction)
├── feature-home/     # URL input + START
└── feature-player/   # Single official YouTube embed player
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

Unit tests cover the URL parser and the secret redactor. See
[docs/TESTING.md](docs/TESTING.md).

## Privacy & security

- No secrets in Git, APK resources, or logs (enforced by `SecretRedactor` + `*.jks`/`local.properties` in `.gitignore`).
- Backups and device-transfer of app data are disabled.
- Cleartext HTTP traffic is disabled (`usesCleartextTraffic="false"`); only HTTPS.
- Only two permissions: `INTERNET` and `ACCESS_NETWORK_STATE`.

## Documentation

- [docs/BUILD.md](docs/BUILD.md) — build, signing, verification
- [docs/TESTING.md](docs/TESTING.md) — test strategy and acceptance criteria
- [CHANGELOG.md](CHANGELOG.md) — version history
- [docs/RELEASE_NOTES.md](docs/RELEASE_NOTES.md) — release notes

## Compliance checklist (Master Plan, page 8–9)

- [x] Package name `com.altomedia.smartghost`
- [x] All main screens with loading / empty / success / error states
- [x] YouTube URLs detected; playback uses official integration
- [x] No artificial views, watch time, or viewer claims
- [x] START / HOME behave per specification
- [x] No secrets in Git, logs, APK resources, or documents
- [x] Release APK passes signature verification
- [x] README, changelog, build instructions, and release notes provided

## Official references

1. YouTube — Fake engagement policy: <https://support.google.com/youtube/answer/3399767>
2. Android — Sign your app: <https://developer.android.com/studio/publish/app-signing>
3. Android — App architecture: <https://developer.android.com/topic/architecture>
4. YouTube — Embed a video: <https://developers.google.com/youtube/player_parameters>

---

© ALTOMEDIA. SMARTGHOST.
