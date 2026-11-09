# SMARTGHOST build artifacts — ALTOMEDIA

> ⚠️ **Peringatan keamanan.** Folder ini berisi **APK** dan **keystore rilis**.
> Keystore adalah kunci penandatanganan aplikasi. Siapa pun yang memilikinya
> dapat membuat APK palsu yang dianggap Android sebagai aplikasi resmi
> ALTOMEDIA. Karena itu isi folder ini seharusnya **hanya** ada di repo privat.
> Jika repo ini publik, **putar ulang (rotate) keystore** dan hapus file ini.

## APK

| File | Versi | Ukuran | SHA-256 |
| --- | --- | --- | --- |
| `smartghost-1.0.4-release.apk` | 1.0.4 (versionCode 5), bertanda tangan | ~1.4 MB | `f36a4ff5b04b4e5de57356404288e771e156d14eb1acadf9e04ff7083a42a0da` |
| `smartghost-1.0.4-debug.apk` | 1.0.4 (versionCode 5), debug | ~17 MB | `7d19bf9c3ea288bc8260ee13f836ff0105b5a592ce818736ae6dfd024b064065` |

Verifikasi tanda tangan:

```bash
apksigner verify --verbose smartghost-1.0.4-release.apk
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
