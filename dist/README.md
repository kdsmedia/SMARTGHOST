# SMARTGHOST build artifacts — ALTOMEDIA

> ⚠️ **Peringatan keamanan.** Folder ini berisi **APK** dan **keystore rilis**.
> Keystore adalah kunci penandatanganan aplikasi. Siapa pun yang memilikinya
> dapat membuat APK palsu yang dianggap Android sebagai aplikasi resmi
> ALTOMEDIA. Karena itu isi folder ini seharusnya **hanya** ada di repo privat.
> Jika repo ini publik, **putar ulang (rotate) keystore** dan hapus file ini.

## APK

| File | Versi | Ukuran | SHA-256 |
| --- | --- | --- | --- |
| `smartghost-1.0.3-release.apk` | 1.0.3 (versionCode 4), bertanda tangan | ~1.4 MB | `83b30f68b943c20464de0c5af62fb56915b169e4beb6b8cdb19526e67ec66523` |
| `smartghost-1.0.3-debug.apk` | 1.0.3 (versionCode 4), debug | ~17 MB | `2b82c86c7a36cceb3f95e85a298b457503c37eaeb280784b5095583819724a91` |

Verifikasi tanda tangan:

```bash
apksigner verify --verbose smartghost-1.0.3-release.apk
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
