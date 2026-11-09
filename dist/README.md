# SMARTGHOST build artifacts — ALTOMEDIA

> ⚠️ **Peringatan keamanan.** Folder ini berisi **APK** dan **keystore rilis**.
> Keystore adalah kunci penandatanganan aplikasi. Siapa pun yang memilikinya
> dapat membuat APK palsu yang dianggap Android sebagai aplikasi resmi
> ALTOMEDIA. Karena itu isi folder ini seharusnya **hanya** ada di repo privat.
> Jika repo ini publik, **putar ulang (rotate) keystore** dan hapus file ini.

## APK

| File | Versi | Ukuran | SHA-256 |
| --- | --- | --- | --- |
| `smartghost-1.1.2-release.apk` | 1.1.2 (versionCode 9), bertanda tangan | ~1.4 MB | `1e1136625192f2569764554ac51745de85ba4cdd13d9487290c72d6fa67dee86` |
| `smartghost-1.1.2-debug.apk` | 1.1.2 (versionCode 9), debug | ~17 MB | `6b09c0b28bf0d5b5897ecbb23914a9d27362385f3dbb603a9ebab8f470be846e` |

Verifikasi tanda tangan:

```bash
apksigner verify --verbose smartghost-1.1.2-release.apk
```

## Keystore rilis

| Item | Nilai |
| --- | --- |
| File | `smartghost-release.jks` |
| Alias | `smartghost` |
| Store / key password | `SmartGhost2026!` |
| Algoritma | RSA 4096-bit |
| Signer | `CN=ALTOMEDIA, OU=SMARTGHOST, O=ALTOMEDIA, L=Jakarta, S=DKI Jakarta, C=ID` |

Membangun rilis bertanda tangan:

```bash
export SMARTGHOST_KEYSTORE_PATH=$PWD/dist/smartghost-release.jks
export SMARTGHOST_KEYSTORE_PASSWORD='SmartGhost2026!'
export SMARTGHOST_KEY_ALIAS=smartghost
export SMARTGHOST_KEY_PASSWORD='SmartGhost2026!'
./gradlew :app:assembleRelease
```

## Cara membangun ulang APK

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew testDebugUnitTest          # 55 tes (48 unit + 7 runtime)
./gradlew :app:assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`.
