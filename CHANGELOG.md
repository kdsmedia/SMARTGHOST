# Changelog

All notable changes to SMARTGHOST are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
[Semantic Versioning](https://semver.org/).

## [1.0.4] — 2026-11-09

Session console rework plus two layout/robustness fixes requested by the user.

### Added
- **Random ten-endpoint sampling on START.** Every START now draws ten endpoints
  at random from the imported `proxy.txt` (`SessionRepository.sampleForSession`)
  and checks those; the console updates live. With a pool of hundreds, each
  session shows a different ten. Endpoints are still only probed from the device
  — this remains a connectivity diagnostic, never a YouTube viewer count.
- **`ProxyConsole`** — a terminal-style panel: monospace green text on black,
  one line per endpoint with a live status marker, a `● LIVE`/`○ IDLE` header, an
  `n/10 proxy bertugas` counter and an mm:ss session clock that ticks while a
  session runs.

### Fixed
- **Bottom bar no longer hidden by the system navigation bar.** The HOME/RELOAD
  row now applies `navigationBarsPadding()` under edge-to-edge.
- The session screen scrolls, so the player block and the console block both
  stay reachable on short screens.

### Tests
- `SessionRepositoryTest` (6) for sampling; console tests (format + runtime);
  sampling test in `DiagnosticsViewModelTest`. 74 tests total, 0 failures.

## [1.0.3] — 2026-11-09

Two user-reported defects fixed: the player failed with IFrame error 153 after
START, and proxy `.txt` import was fragile.

### Fixed
- **Player error 153 ("Video player configuration error").** The host page was
  loaded with `loadDataWithBaseURL`, giving it a `data:` origin, so the WebView
  sent no `Referer`/`Origin` to YouTube and the IFrame player refused to start.
  The page now ships as an asset and is served from a real https origin
  (`https://appassets.androidplatform.net`) via `WebViewAssetLoader`, with
  `<meta name="referrer" content="strict-origin-when-cross-origin">`. Error 153
  also has its own explanatory message now.
- **Proxy `.txt` reading hardened.** A UTF-8 BOM is stripped (Windows editors)
  so the first endpoint is no longer misread; CRLF line endings are handled; the
  picked file's real display name is shown (Android providers often return an
  opaque id); and an unreadable file now reports "File '…' tidak dapat dibaca"
  instead of silently loading zero endpoints.

### Added
- `feature-player/src/main/assets/player.html` — the bundled IFrame host page.
- `HomeViewModelTest` (4 tests) and `PlayerAssetRuntimeTest` (4 runtime tests),
  including a real `WebViewAssetLoader` check that the page is served over https.

### Tests
- 65 tests total (57 unit + 8 runtime), 0 failures; 0 lint errors.

## [1.0.2] — 2026-11-09

Runtime verification on the JVM (no emulator available in this environment).
The build no longer merely compiles: real Compose, a real HTTP server, a real
DataStore, and the real Hilt/Application boot are all exercised at runtime.

### Added
- **Robolectric runtime tests** (7 new). They render the real Compose diagnostics
  panel, click the real START button, drive the real ViewModel, hit a real HTTP
  server via OkHttp, persist to a real DataStore, and boot the real `Application`.
- Settings screen now lists the **authorised test endpoints** imported from
  `proxy.txt` (Bab 4 "endpoint uji yang diizinkan").

### Fixed
- Plain-HTTP endpoints now return a clear message ("HTTP polos tidak diizinkan —
  gunakan https:// pada proxy.txt") instead of a raw I/O error, since cleartext
  traffic is disabled. Connection-refused is reported as "Koneksi ditolak".

### Tests
- 55 tests total (48 unit + 7 runtime), 0 failures.

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
