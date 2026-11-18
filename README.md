# Pengingat Shalat & Imsak

Aplikasi Android (Kotlin & Jetpack Compose) untuk jadwal shalat dan Imsak **100% offline** sesuai standar **Kementerian Agama Republik Indonesia (Kemenag RI)**, dilengkapi alarm presisi (`Exact Alarm`), pengingat H-30 & H-15 menit sebelum waktu habis, Smart Cancel **Sudah Shalat**, serta dukungan file audio lokal.

## Cara Mengunduh & Memasang di Android melalui GitHub

### 1. Push Proyek ke GitHub
Dari menu **Settings / GitHub** di Google AI Studio, hubungkan atau push proyek ini ke repositori GitHub Anda.

### 2. Bangun & Publikasikan APK Secara Otomatis
Repositori ini sudah dilengkapi workflow GitHub Actions (`.github/workflows/build-apk.yml`) yang otomatis berjalan setiap kali Anda push ke branch `main`/`master` atau membuat tag versi (`v1.0.0`, dst.):

- **Melalui Halaman Releases (Direkomendasikan untuk pengguna Android):**
  1. Buka repositori Anda di GitHub.
  2. Buat rilis baru via **Releases -> Draft a new release**, isi tag misalnya `v1.0.0`, atau jalankan workflow secara manual dari tab **Actions -> Build & Release Android APK -> Run workflow** dengan mengisi `release_tag` (misal `v1.0.0`).
  3. Setelah workflow selesai (~2–3 menit), buka menu **Releases** di GitHub.
  4. Di HP Android, ketuk file **`Pengingat-Shalat-Imsak.apk`** untuk mengunduh dan memasangnya (izinkan *"Install unknown apps"* jika diminta Android).

- **Melalui Tab Actions (Artifact):**
  1. Buka tab **Actions** pada repositori GitHub Anda.
  2. Pilih run terbaru dari **Build & Release Android APK**.
  3. Unduh artifact **`Pengingat-Shalat-Imsak-APK`**, ekstrak ZIP-nya, lalu instal file `.apk` di HP Android.

### 3. Opsional: Signing Release APK dengan Keystore Pribadi
Secara bawaan, workflow otomatis menghasilkan APK yang sudah ditandatangani (`Pengingat-Shalat-Imsak.apk`) agar langsung bisa dipasang di perangkat Android tanpa konfigurasi tambahan.
Jika Anda ingin merilis APK bertanda tangan kunci rilis pribadi (`assembleRelease`), tambahkan 3 **Repository Secrets** di **Settings -> Secrets and variables -> Actions**:
- `RELEASE_KEYSTORE_BASE64`: isi base64 dari file `.jks` Anda (`alias: upload`)
- `STORE_PASSWORD`: kata sandi keystore
- `KEY_PASSWORD`: kata sandi alias kunci

## Alternatif Cepat: Ekspor APK Langsung
Anda juga dapat mengunduh APK/AAB langsung dari menu pengaturan (**Settings**) di AI Studio tanpa menunggu proses CI di GitHub.
