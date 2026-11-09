# Changelog

All notable changes to SMARTGHOST are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
[Semantic Versioning](https://semver.org/).

## [1.0.1] — 2026-11-09

Functional audit follow-up. The 1.0.0 build compiled, passed its tests and was
signed, but a full audit found features that were unreachable from the UI. This
release makes every advertised control actually work.

### Fixed
- **Diagnostics START was unreachable.** `DiagnosticsViewModel.start()` had no UI
  caller, so the ten-card panel could never run a check. A START button is now in
  the panel, and the ViewModel is hoisted to `SessionScreen`.
- **RELOAD duplicated the session screen** instead of retrying. It now calls
  `reloadFailed()` on the same ViewModel, retrying only failed cards.
- **HOME did not stop diagnostics.** It now calls `stop()` before navigating, as
  Bab 8 requires.
- **Player reported nothing.** A JavaScript bridge now relays the IFrame Player
  API's `onError`/`onStateChange` (and `onReady`) back to `PlayerViewModel`;
  `domStorageEnabled` is enabled and the video id is sanitised. Player errors are
  now shown to the user.
- **Test timeout had no effect.** `EndpointChecker.check` takes a timeout and the
  panel applies the value from Settings.
- **History toggle was decorative.** A real, redacted, capped diagnostic history
  is now stored when enabled, with a "Hapus riwayat" action.
- `SafeLogger` is now actually used on the diagnostics path.

### Added
- Instrumented UI test (`feature-diagnostics/src/androidTest`) that drives the
  real Compose panel and asserts START triggers checks.

### Tests
- 48 unit tests (was 46), 0 failures. New coverage: settings timeout propagation,
  optional history record/clear.

## [1.0.0] — 2026-11-09

First release. SMARTGHOST ships as a **YouTube URL checker and network diagnostics
tool**, not a view-generation tool. See the scope statement in the README.

### Added
- **YouTube URL validation** (`data/youtube/YouTubeUrlParser`): supports `watch`,
  `youtu.be`, `shorts`, `live`, and `embed` links, plus `youtube-nocookie`; validates
  the 11-character video id and returns a canonical URL.
- **Single official player** (`feature-player`): one video played through the
  official YouTube IFrame Player API, with play/pause driven only by user action.
  An "open in YouTube" action is provided.
- **`proxy.txt` import** (`data/proxy/ProxyFileParser`): parses `host:port` and
  `scheme://host:port` lines, ignores blank lines and `#` comments, validates ports
  and schemes, reports errors with line numbers, and detects duplicates.
- **10-card diagnostics panel** (`feature-diagnostics`): START runs one diagnostic
  pass with bounded concurrency (max 3); RELOAD retries only failed cards; HOME
  cancels safely. Cards show status colour, latency, and a short error summary —
  never a viewer count.
- **Endpoint checker** (`data/diagnostics/OkHttpEndpointChecker`): HTTPS endpoint
  checks with timeouts, mapping DNS/timeout/connection/TLS failures to short,
  user-friendly messages.
- **Secret-safe logging** (`security/SecretRedactor`, `security/SafeLogger`):
  redacts GitHub tokens, bearer tokens, password/api-key assignments, URL
  credentials, and JWTs.
- **Settings screen**: theme, test timeout, optional diagnostics history, privacy
  summary, and version info.
- **Design system** (`core-ui`): dark navy/charcoal palette with electric
  blue/cyan accents, Material 3, shared cards/pills/buttons.
- **DataStore preferences** (`data/prefs/SettingsRepository`) via Hilt DI.

### Security
- Only two permissions: `INTERNET`, `ACCESS_NETWORK_STATE`.
- Cleartext traffic disabled (`usesCleartextTraffic="false"`).
- Backups and device-to-device transfer of app data disabled.
- No secrets committed; signing values supplied via environment variables.
- Release build signed with APK Signature Scheme v2.

### Tests
- 46 unit tests: URL parser (15), proxy parser (12), endpoint checker (3, real
  MockWebServer), secret redactor (10), diagnostics view model (6).

### Notes
- The original concept of 10 simultaneous players with fake-viewer proxying is
  **rejected** on YouTube Fake Engagement policy grounds and is not implemented.
- `minSdk` 24, `targetSdk` 34, `compileSdk` 34.
