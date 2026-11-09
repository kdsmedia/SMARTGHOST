# Build, Signing, and Verification

This document covers building SMARTGHOST, producing a **signed release APK**, and
verifying it — following Chapter 11 of the master plan.

## 1. Environment

| Tool | Version used | Notes |
| --- | --- | --- |
| JDK | 21 (`openjdk-21-jdk-headless`) | Set `JAVA_HOME`. |
| Android SDK Platform | android-34 | `ANDROID_HOME` must point at the SDK. |
| Android Build-Tools | 34.0.0 | Provides `apksigner`, `aapt2`. |
| Gradle | 8.7 | The wrapper is committed; do not install globally. |

```bash
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH
```

`local.properties` (git-ignored) must contain:

```properties
sdk.dir=/home/openhands/android-sdk
```

## 2. Debug build

```bash
./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

## 3. Unit tests

```bash
./gradlew testDebugUnitTest
# HTML report: <module>/build/reports/tests/testDebugUnitTest/index.html
```

## 4. Release signing (secrets stay out of Git)

Release signing reads four **environment variables**; nothing is written into
`build.gradle.kts` and no keystore is committed.

| Variable | Purpose |
| --- | --- |
| `SMARTGHOST_KEYSTORE_PATH` | Absolute path to the `.jks` keystore (stored **outside** the repo). |
| `SMARTGHOST_KEYSTORE_PASSWORD` | Keystore password. |
| `SMARTGHOST_KEY_ALIAS` | Key alias. |
| `SMARTGHOST_KEY_PASSWORD` | Key password. |

If `SMARTGHOST_KEYSTORE_PATH` is unset, the release build is produced **unsigned**
(useful for CI compile checks). Set all four for a signed, installable APK.

### 4.1 Create a keystore (once)

```bash
keytool -genkeypair -v \
  -keystore /secure/location/smartghost-release.jks \
  -alias smartghost -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=ALTOMEDIA, OU=SMARTGHOST, O=ALTOMEDIA, L=Jakarta, S=DKI Jakarta, C=ID"
```

Back this keystore up **encrypted** in a separate location. If it is lost you can
never ship an update that installs over the previous version (same package +
signing key is required).

### 4.2 Build the signed release

```bash
export SMARTGHOST_KEYSTORE_PATH=/secure/location/smartghost-release.jks
export SMARTGHOST_KEYSTORE_PASSWORD='********'
export SMARTGHOST_KEY_ALIAS=smartghost
export SMARTGHOST_KEY_PASSWORD='********'

./gradlew :app:assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

The release build enables R8 minification and resource shrinking.

## 5. Verify the signed APK

```bash
$ANDROID_HOME/build-tools/34.0.0/apksigner verify --verbose --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

Expected output includes:

```
Verifies
Verified using v2 scheme (APK Signature Scheme v2): true
Signer #1 certificate DN: CN=ALTOMEDIA, OU=SMARTGHOST, ...
```

Inspect package metadata:

```bash
$ANDROID_HOME/build-tools/34.0.0/aapt2 dump badging \
  app/build/outputs/apk/release/app-release.apk | head
```

Expected:

```
package: name='com.altomedia.smartghost' versionCode='1' versionName='1.0.0'
sdkVersion:'24' targetSdkVersion:'34'
uses-permission: name='android.permission.INTERNET'
uses-permission: name='android.permission.ACCESS_NETWORK_STATE'
```

## 6. Record the artifact

```bash
sha256sum app/build/outputs/apk/release/app-release.apk
```

Archive the APK, its SHA-256, `versionCode`, and `versionName` together. See
[docs/RELEASE_NOTES.md](RELEASE_NOTES.md) for the current values.

## 7. Install on a device

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

## 8. CI recommendations (Master Plan, Bab 11)

- **On every pull request:** `./gradlew compileDebugKotlin testDebugUnitTest lint`
  plus a secret scan (e.g. `gitleaks`).
- **On a version tag:** build the release through a trusted runner with restricted
  secrets; never auto-push without owner review.
- Never commit `.jks`, `local.properties`, `keystore.properties`, tokens, or any
  secret configuration file.
