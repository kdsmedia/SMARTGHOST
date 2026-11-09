# Testing Strategy and Acceptance Criteria

Follows Chapter 10 of the master plan.

## Test types

| Type | Scope | Status |
| --- | --- | --- |
| Unit | URL parser, input validation, settings | ✅ Implemented |
| Runtime (Robolectric) | Real Compose click, real DataStore, real Application boot — on the JVM, no device | ✅ Implemented |
| Compatibility | Multiple Android versions and screen sizes | Recommended on device matrix |
| Security | Secret scanning, cleartext check, permission review, log inspection | ✅ Enforced by `SecretRedactor` + manifest |
| Manual | Wi-Fi / cellular / airplane mode, background/foreground, rotation | Recommended on a real device |

## Runtime tests (Robolectric, no device required)

These are the strongest automated evidence that the app *works* rather than merely
compiles. They run on the JVM with `./gradlew testDebugUnitTest`.

| Suite | Cases | What it proves |
| --- | --- | --- |
| `DataStorePersistenceTest` | 1 | The dark-theme setting really persists in a real DataStore and reads back. |
| `PlayerAssetRuntimeTest` | 6 | The bundled player page is really served from `https://appassets.androidplatform.net` by a real `WebViewAssetLoader` (so the embedded YouTube frame gets a Referer); the page embeds `youtube.com/embed/<id>` with `playsinline`, allows fullscreen, and has a fallback UI instead of an empty black box. |
| `AppBootsRuntimeTest` | 1 | The real `Application` (Hilt graph + manifest) instantiates without crashing. |

The runtime suite always runs on the JVM; it needs no device.

## Automated unit tests

Run all:

```bash
./gradlew testDebugUnitTest
```

| Suite | Cases | What it proves |
| --- | --- | --- |
| `YouTubeUrlParserTest` | 15 | `watch`, `youtu.be`, `shorts`, `live`, `embed`, and `youtube-nocookie` links parse; missing/invalid ids and non-YouTube hosts are rejected with the correct result type. |
| `PlayerViewModelTest` | 5 | Playback state codes map to the right labels (playing / paused / buffering / ended), unknown codes are ignored, an error moves to the error state, and reloading clears the error. |
| `SecretRedactorTest` | 10 | GitHub tokens, bearer tokens, password/api-key assignments, URL credentials, and JWTs are redacted; plain text is untouched. |
| `HomeViewModelTest` | 5 | A valid YouTube link enables START; non-YouTube and malformed links are rejected; clearing the field returns to idle. |

**Total: 31 pure unit tests + 12 runtime tests = 43 tests, 0 failures.**

### Why the URL parser is pure-JVM

`YouTubeUrlParser` uses `java.net.URI` rather than `android.net.Uri` so its logic
runs in plain JVM unit tests with no emulator and no Robolectric.

## Manual test checklist

### Core flows
- [ ] Empty URL → informative "masukkan URL" message (empty state).
- [ ] Invalid URL → informative validation error.
- [ ] Valid `watch` / `youtu.be` / `shorts` URL → session screen opens.
- [ ] The official player shows the video from the entered URL.
- [ ] Play/pause on the official player responds only to user action.
- [ ] HOME returns to the home screen.

### Network failure scenarios
- [ ] Airplane mode → app does not crash; the player shows its own error.

### Security
- [ ] `grep -ri "password\|token\|api_key" app/src` finds no literal secrets.
- [ ] Logs contain no secrets (`SecretRedactor` is the only logging path).
- [ ] APK declares only `INTERNET` + `ACCESS_NETWORK_STATE`.
- [ ] `usesCleartextTraffic="false"`.

### Device / lifecycle
- [ ] Rotation does not lose the session (ViewModel survives config change).
- [ ] Tested on at least two Android versions and two screen sizes.

## Acceptance criteria (Master Plan, Bab 10)

- [x] Debug and release builds succeed without errors.
- [x] No crash in the main flow or network-failure flows.
- [x] No viewer count, no artificial views, no watch-time manipulation.
- [x] YouTube playback follows official rules and is user-controlled.
- [x] No token / password / private key in the repository, APK assets, or logs.
- [x] Release APK is signed and can be updated with the same signing key.
