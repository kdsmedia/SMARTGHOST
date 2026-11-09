# SMARTGHOST build artifacts — ALTOMEDIA

> ⚠️ **Peringatan keamanan.** Folder ini berisi **APK** dan **keystore rilis**.
> Keystore adalah kunci penandatanganan aplikasi. Siapa pun yang memilikinya
> dapat membuat APK palsu yang dianggap Android sebagai aplikasi resmi
> ALTOMEDIA. Karena itu isi folder ini seharusnya **hanya** ada di repo privat.
> Jika repo ini publik, **putar ulang (rotate) keystore** dan hapus file ini.

## APK

| File | Versi | Ukuran | SHA-256 |
| --- | --- | --- | --- |
| `smartghost-1.0.2-release.apk` | 1.0.2 (versionCode 3), bertanda tangan | ~1.4 MB | `b867949092e320f87ade73681a2d990d492c5e62d1a491a69832ea362cf3a12c` |
| `smartghost-1.0.2-debug.apk` | 1.0.2 (versionCode 3), debug | ~17 MB | `704418ce8d1443271b928afc12c9666be574b9cc2b9e3a0b0f53222524577ab2` |

Verifikasi tanda tangan:

```bash
apksigner verify --verbose smartghost-1.0.2-release.apk
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
