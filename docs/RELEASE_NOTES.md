# SMARTGHOST 1.0.0 — Release Notes

**Brand:** ALTOMEDIA
**Date:** 2026-11-09
**Package:** `com.altomedia.smartghost`
**versionCode / versionName:** `1` / `1.0.0`
**Min / Target / Compile SDK:** 24 / 34 / 34

---

## Release artifact

| Item | Value |
| --- | --- |
| File | `app/build/outputs/apk/release/app-release.apk` |
| Size | ~1.4 MB |
| SHA-256 | `5b28867dd1943facc6178844fba20b1b833adf1a2c04074c2d4922ffc22ca673` |
| Signature | APK Signature Scheme v2 — verified |
| Signer | `CN=ALTOMEDIA, OU=SMARTGHOST, O=ALTOMEDIA, L=Jakarta, S=DKI Jakarta, C=ID` |
| Key | RSA 4096-bit |

> Recompute the SHA-256 after any rebuild: `sha256sum app/build/outputs/apk/release/app-release.apk`.

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

1. `./gradlew testDebugUnitTest` — 46 tests, 0 failures.
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
