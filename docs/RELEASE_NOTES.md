# SMARTGHOST 1.0.4 — Release Notes

**Brand:** ALTOMEDIA
**Date:** 2026-11-09
**Package:** `com.altomedia.smartghost`
**versionCode / versionName:** `5` / `1.0.4`
**Min / Target / Compile SDK:** 24 / 34 / 34

---

## Release artifact

| Item | Value |
| --- | --- |
| File | `app/build/outputs/apk/release/app-release.apk` |
| Size | ~1.4 MB |
| SHA-256 | `f36a4ff5b04b4e5de57356404288e771e156d14eb1acadf9e04ff7083a42a0da` |
| Signature | APK Signature Scheme v2 — verified |
| Signer | `CN=ALTOMEDIA, OU=SMARTGHOST, O=ALTOMEDIA, L=Jakarta, S=DKI Jakarta, C=ID` |
| Key | RSA 4096-bit |

> Recompute the SHA-256 after any rebuild: `sha256sum app/build/outputs/apk/release/app-release.apk`.

## What changed in 1.0.4

- START now draws **ten endpoints at random** from the imported `proxy.txt` on
  every run and checks those (connectivity diagnostic, not a viewer count).
- New terminal-style **ProxyConsole**: monospace green-on-black, one line per
  endpoint with a live marker, a `● LIVE` header, `n/10 proxy bertugas` counter
  and an mm:ss session clock.
- The bottom HOME/RELOAD bar now clears the system navigation bar
  (`navigationBarsPadding`); the session screen scrolls.
- 77 tests, 0 failures; lint 0 errors.

## What changed in 1.0.3

Two reported defects fixed.

- **Player error 153 after START is fixed.** The player page used to be injected
  with `loadDataWithBaseURL`, so it had a `data:` origin and the WebView sent no
  `Referer`/`Origin`; YouTube's IFrame player then refused to start with error
  153. The page is now a bundled asset served over a real https origin
  (`https://appassets.androidplatform.net`) with a referrer policy meta tag.
- **Proxy `.txt` import hardened.** UTF-8 BOM is stripped (so the first line is
  not misread), CRLF endings work, the picked file shows its real name, and an
  unreadable file reports a clear message instead of loading zero endpoints.
- 65 tests total (57 unit + 8 runtime), 0 failures; lint 0 errors.

## What changed in 1.0.2

Verification moved from "it compiles" to "it runs". No emulator is available in
this environment, so runtime evidence is produced on the JVM with **Robolectric**:

- Seven runtime tests now render real Compose, click the real START button, call
  the real OkHttp checker against a real HTTP server, persist to a real DataStore,
  and boot the real `Application` (Hilt) — all without a device.
- Settings now lists the **authorised test endpoints** imported from `proxy.txt`.
- Clearer messages: plain-HTTP endpoints explain that `https://` is required;
  connection-refused is reported as "Koneksi ditolak".
- 55 tests total (48 unit + 7 runtime), 0 failures; lint 0 errors.

## What changed in 1.0.1

1.0.0 compiled, passed tests and was signed, but a functional audit found controls
that were not reachable from the UI. 1.0.1 fixes them:

- Diagnostics **START** is now a real button and runs checks.
- **RELOAD** retries failed checks (it no longer opens a duplicate screen).
- **HOME** stops the running session before navigating.
- The player now reports **errors and play state** via a JavaScript bridge.
- The **test timeout** setting is applied; **history** is really stored and
  clearable.
- Added an instrumented UI test; unit tests now 48, 0 failures.

## Highlights

- **URL checker & diagnostics tool** — validates YouTube links and runs safe
  endpoint diagnostics.
- **Single official player** — one video via the official YouTube IFrame Player
  API, user-controlled playback only.
- **Honest 10-card panel** — reports endpoint connectivity, latency, and errors.
  It does **not** represent or create YouTube views.
- **Secret-safe by design** — redacted logging, no stored credentials, no secrets
  in the repository or APK.

## Compliance statement

The original SMARTGHOST concept — 10 simultaneous players plus proxy traffic to
fake 10 viewers — is **explicitly rejected**. It is viewbotting and violates the
YouTube Fake Engagement policy. This release implements the compliant alternative
described in the master plan (Bab 6–8): a URL checker, an authorised `proxy.txt`
test configuration, endpoint diagnostics, and official IFrame playback only.

## Permissions

| Permission | Reason |
| --- | --- |
| `android.permission.INTERNET` | Load the official player and run endpoint checks. |
| `android.permission.ACCESS_NETWORK_STATE` | Detect connectivity for diagnostics. |

No storage, location, camera, microphone, contacts, or background permissions.

## Installation

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Requires Android 7.0 (API 24) or newer.

## Known limitations

- The diagnostics panel tests only endpoints the operator owns or is authorised to
  test. It performs no YouTube-specific load.
- Playback depends on the device's installed WebView and the official YouTube
  IFrame Player API being reachable.
- UI and compatibility tests should still be run on a real device matrix before a
  public store release (Bab 10, 12).

## Upgrade note

Keep the release keystore backed up **encrypted** in a separate location. Losing it
prevents shipping future updates that install over this version.

## Verification steps performed

1. `./gradlew testDebugUnitTest` — 48 tests, 0 failures.
2. `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL.
3. `./gradlew :app:assembleRelease` (with keystore env vars) — BUILD SUCCESSFUL.
4. `apksigner verify --verbose --print-certs` — Verifies; v2 scheme true.
5. `aapt2 dump badging` — package/version/SDK/permissions confirmed.

## References

1. YouTube Fake engagement policy — <https://support.google.com/youtube/answer/3399767>
2. Android — Sign your app — <https://developer.android.com/studio/publish/app-signing>
3. Android — App architecture — <https://developer.android.com/topic/architecture>
4. YouTube — IFrame Player API — <https://developers.google.com/youtube/iframe_api_reference>

---

© ALTOMEDIA. SMARTGHOST.
