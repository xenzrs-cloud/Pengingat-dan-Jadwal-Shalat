# Pengingat Shalat & Imsak

Aplikasi Android (Kotlin & Jetpack Compose) untuk jadwal shalat dan Imsak **100% offline** sesuai standar **Kementerian Agama Republik Indonesia (Kemenag RI)**, dilengkapi alarm presisi (`Exact Alarm`), pengingat H-30 & H-15 menit sebelum waktu habis, Smart Cancel **Sudah Shalat**, dukungan file audio lokal, **tema aplikasi dinamis sesuai jam**, serta **3 pilihan ukuran Widget (2x2, 3x2, dan 4x2)** dengan pilihan tema widget tersendiri.

## Unduh Langsung APK untuk Android

File APK siap pasang tersedia langsung di halaman utama repositori ini (sejajar dengan `README.md`):

- **[Unduh Pengingat-Shalat-Imsak.apk](./Pengingat-Shalat-Imsak.apk)** *(ketuk file lalu pilih **Download / View raw** di browser HP Android, kemudian buka untuk memasang — aktifkan izin "Install unknown apps" bila diminta).*

### Tema Aplikasi Dinamis vs Tema Widget Mandiri
- **Tema Aplikasi (Dinamis sesuai jam):** Latar masjid dan palet warna di dalam aplikasi otomatis berubah mengikuti waktu lokal (Fajar, Siang, Senja, Malam), atau dapat dipilih manual lewat ikon palet di aplikasi.
- **3 Pilihan Widget (2x2, 3x2, 4x2):** Tersedia dalam ukuran **2x2** (Jadwal Mini), **3x2** (Berikutnya + Jadwal), dan **4x2** (Jadwal Lengkap). Penggantian tema pada widget berdiri sendiri (tidak mengubah tema aplikasi).

## Otomatisasi Build & Release di GitHub

1. **Rilis Otomatis (`latest` & tag `v*`):** Workflow `.github/workflows/build-apk.yml` otomatis berjalan setiap kali ada push ke `main`/`master` dan langsung membuat/memperbarui rilis **`latest`** di halaman **Releases** dengan file `Pengingat-Shalat-Imsak.apk`.
2. **Melalui Tab Actions (Artifact):** Anda juga dapat mengunduh `Pengingat-Shalat-Imsak-APK` dari hasil run terbaru di tab **Actions**.

### 3. Opsional: Signing Release APK dengan Keystore Pribadi
Secara bawaan, workflow otomatis menghasilkan APK yang sudah ditandatangani (`Pengingat-Shalat-Imsak.apk`) agar langsung bisa dipasang di perangkat Android tanpa konfigurasi tambahan.
Jika Anda ingin merilis APK bertanda tangan kunci rilis pribadi (`assembleRelease`), tambahkan 3 **Repository Secrets** di **Settings -> Secrets and variables -> Actions**:
- `RELEASE_KEYSTORE_BASE64`: isi base64 dari file `.jks` Anda (`alias: upload`)
- `STORE_PASSWORD`: kata sandi keystore
- `KEY_PASSWORD`: kata sandi alias kunci

## Alternatif Cepat: Ekspor APK Langsung
Anda juga dapat mengunduh APK/AAB langsung dari menu pengaturan (**Settings**) di AI Studio tanpa menunggu proses CI di GitHub.
