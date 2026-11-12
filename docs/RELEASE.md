# Rilis SAVR v0.2

## 1. Kunci penandatanganan (sekali saja)
```
keytool -genkeypair -v -keystore savr-release.jks -alias savr \
        -keyalg RSA -keysize 2048 -validity 10000
```
Buat `keystore.properties` di root proyek (sudah di-.gitignore):
```
storeFile=savr-release.jks
storePassword=...
keyAlias=savr
keyPassword=...
```
**Simpan `savr-release.jks` dan passwordnya di tempat aman (di luar GitHub).** Kehilangan kunci = tidak bisa memperbarui aplikasi yang sudah terpasang.

## 2. Build
```
./gradlew testDebugUnitTest lintRelease assembleRelease
```
Hasil: `app/build/outputs/apk/release/app-release.apk` (tertandatangani bila `keystore.properties` ada).

Verifikasi tanda tangan & ukuran (Android SDK build-tools):
```
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
apkanalyzer apk file-size app/build/outputs/apk/release/app-release.apk
apkanalyzer apk download-size app/build/outputs/apk/release/app-release.apk
```

## 3. PENTING — memperbarui dari v0.1
Android hanya mau memperbarui aplikasi bila **tanda tangannya sama**. Jika v0.1 terpasang dengan kunci *debug*
dan v0.2 ditandatangani kunci rilis, Android akan meminta uninstall dan **data v0.1 hilang** (v0.1 belum punya backup).
Jika data v0.1 penting, build v0.2 dengan kunci yang sama dengan v0.1 untuk pemasangan pertama.
Mulai v0.2 data bisa diamankan lewat Profile > Backup.

## 4. GitHub Actions (opsional)
Simpan keystore sebagai secret base64 (`KEYSTORE_B64`) beserta `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, lalu sebelum build:
```
echo "$KEYSTORE_B64" | base64 -d > savr-release.jks
printf 'storeFile=savr-release.jks\nstorePassword=%s\nkeyAlias=%s\nkeyPassword=%s\n' \
  "$KEYSTORE_PASSWORD" "$KEY_ALIAS" "$KEY_PASSWORD" > keystore.properties
./gradlew assembleRelease
```

## 5. Pemeriksaan dependensi
```
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
```
Tinjau tiap versi di halaman rilis/advisory AndroidX & Kotlin sebelum rilis publik. Naikkan versi satu per satu dan build ulang.

## 6. Play Protect
Aplikasi yang dipasang manual (sideload) bisa memicu pemindaian Play Protect, terutama bila tidak tertandatangani atau memakai
kunci debug. Pemicu risiko diminimalkan: tanpa izin apa pun, tanpa internet, tanpa library iklan/analitik, APK rilis tertandatangani.
Hasil akhir tetap harus diuji di perangkat sebenarnya.
