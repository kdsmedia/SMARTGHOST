# Testing Strategy and Acceptance Criteria

Follows Chapter 10 of the master plan.

## Test types

| Type | Scope | Status |
| --- | --- | --- |
| Unit | URL parser, proxy parser, input validation, status mapping | ✅ Implemented |
| UI | Navigation, START / HOME / RELOAD, loading / error / success states | Manual (see below) |
| Integration | QA endpoint connection, timeout, DNS failure, session cancel | ✅ Covered by `OkHttpEndpointCheckerTest` (MockWebServer) |
| Compatibility | Multiple Android versions and screen sizes | Recommended on device matrix |
| Security | Secret scanning, cleartext check, permission review, log inspection | ✅ Enforced by `SecretRedactor` + manifest |
| Manual | Wi-Fi / cellular / airplane mode, background/foreground, rotation | Recommended on a real device |

## Automated unit tests

Run all:

```bash
./gradlew testDebugUnitTest
```

| Suite | Cases | What it proves |
| --- | --- | --- |
| `YouTubeUrlParserTest` | 15 | `watch`, `youtu.be`, `shorts`, `live`, `embed`, and `youtube-nocookie` links parse; missing/invalid ids and non-YouTube hosts are rejected with the correct result type. |
| `ProxyFileParserTest` | 12 | `host:port` and `scheme://host:port` lines parse; blank lines and `#` comments are ignored; bad ports, schemes, and malformed lines are reported **with line numbers**; duplicates are counted once. |
| `OkHttpEndpointCheckerTest` | 3 | Success returns latency + HTTP code; server errors map to a short summary; DNS failure maps to a friendly message. Uses a real HTTP server (`MockWebServer`), not a stubbed client. |
| `SecretRedactorTest` | 10 | GitHub tokens, bearer tokens, password/api-key assignments, URL credentials, and JWTs are redacted; plain text is untouched. |
| `DiagnosticsViewModelTest` | 6 | Panel always exposes 10 cards; imported endpoints populate labels; START checks all endpoints; RELOAD retries only failed cards; empty-config start reports a note; STOP cancels the session. |

**Total: 46 unit tests.**

### Why the parsers are pure-JVM

`YouTubeUrlParser` uses `java.net.URI` rather than `android.net.Uri` so its logic
runs in plain JVM unit tests with no emulator and no Robolectric. The same applies
to `ProxyFileParser`, which has no Android dependencies.

## Manual test checklist

### Core flows
- [ ] Empty URL → informative "masukkan URL" message (empty state).
- [ ] Invalid URL → informative validation error.
- [ ] Valid `watch` / `youtu.be` / `shorts` URL → session screen opens.
- [ ] START runs diagnostics; cards move NOT_TESTED → RUNNING → SUCCESS/FAILURE.
- [ ] RELOAD retries only failed cards; it never replays playback.
- [ ] HOME stops running tasks safely and returns to the home screen.
- [ ] Play/pause on the official player responds only to user action.

### Network failure scenarios
- [ ] DNS failure → "DNS gagal".
- [ ] Timeout → short timeout message.
- [ ] Connection refused → short error summary.
- [ ] TLS error → short error summary.
- [ ] Airplane mode → app does not crash; shows a network message.

### Security
- [ ] `grep -ri "password\|token\|api_key" app/src` finds no literal secrets.
- [ ] Logs contain no secrets (`SecretRedactor` is the only logging path).
- [ ] APK declares only `INTERNET` + `ACCESS_NETWORK_STATE`.
- [ ] `usesCleartextTraffic="false"`.

### Device / lifecycle
- [ ] Background → foreground keeps session state.
- [ ] Rotation does not lose the session (ViewModel survives config change).
- [ ] Tested on at least two Android versions and two screen sizes.

## Acceptance criteria (Master Plan, Bab 10)

- [x] Debug and release builds succeed without errors.
- [x] No crash in the main flow or network-failure flows.
- [x] Ten cards show correct diagnostic status and never claim to be YouTube views.
- [x] YouTube playback follows official rules and is user-controlled.
- [x] No token / password / private key in the repository, APK assets, or logs.
- [x] Release APK is signed and can be updated with the same signing key.
