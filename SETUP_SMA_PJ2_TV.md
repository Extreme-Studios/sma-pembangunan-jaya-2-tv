# SMA Pembangunan Jaya 2 TV

Salinan lokal dari `Extreme-Studios/mugaru-tv`, disesuaikan untuk SMA Pembangunan Jaya 2 Sidoarjo. Repository asal tidak diubah.

## Sumber konten saat ini

- Aplikasi membaca feed publik channel YouTube sekolah (`UCRUYr8IDtd_OSbEOgE-z1Iw`) serta 10 playlist publiknya. Nama playlist menjadi kategori. Video yang masuk beberapa playlist tampil di setiap kategori tersebut. Video yang sama dari Sheet menggunakan metadata Sheet.
- Katalog lokal `SchoolYoutubeCatalog.kt` diambil dari YouTube Data API v3 pada 26 September 2026: 117 video publik, mencakup seluruh 10 playlist. Angka statistik channel adalah 118; satu item tidak tersedia sebagai video publik dalam hasil API. Feed publik tetap melengkapi katalog dengan unggahan baru, termasuk bila katalog belum diperbarui.
- Untuk memperbarui katalog, salin API key YouTube Data API v3 dari Google Cloud ke clipboard lalu jalankan `tools/RefreshSchoolYoutubeCatalog.ps1`. Kunci tidak ditulis ke proyek atau APK. Kategori playlist baru perlu ditambahkan ke `SchoolBackend.kt` sebelum refresh.
- Google Sheet tujuan: `1bB0FVJIzffbhHIAXW7LspDdR1XNvKCPhUbnyC2UYBIs`. Pada pemeriksaan 25 September 2026, `Sheet1` masih kosong.
- Endpoint Web App yang diberikan pemilik proyek sudah dipasang di `SchoolBackend.kt` dan respons GET diverifikasi HTTP 200 dengan JSON `success:true`. Daftar video Sheet pada saat verifikasi masih kosong. Kesiapan fitur tulis, folder Drive, dan admin belum diverifikasi.

## Pemeriksaan backend Sheet

1. Pastikan deployment Apps Script memakai isi terbaru `backend/SchoolTvAppsScript.gs` dan Sheet ID yang tertera di atas.
2. Folder Drive proyek sudah dibuat di bawah `SMA Pembangunan Jaya 2 TV` dan ID-nya sudah diisi pada `FILM_COVER_FOLDER_ID`, `FILM_VIDEO_FOLDER_ID`, dan `PORTAL_POSTER_FOLDER_ID` di script lokal. Pastikan deployment Apps Script menggunakan versi script ini sebelum menguji unggahan.
3. Buat kredensial admin sendiri di tab `admin`. Script lokal tidak membuat akun `admin/admin`.
4. Uji login admin dan unggah dengan akun sekolah sebelum fitur tersebut dianggap siap.

Respons baca backend sudah aktif. Jangan menganggap fitur tulis siap sebelum pengujian tersebut selesai. Aplikasi tetap menampilkan video dari YouTube.

## Folder aset Google Drive

- Induk: `https://drive.google.com/drive/u/0/folders/19denpeUoiy3qMgoYdmKNDuYKeD2ECAD0`
- Gambar Sampul Video: `https://drive.google.com/drive/u/0/folders/1sqBLSCOZw6fnk2EdMAm4wBzw23opaVJf` (`FILM_COVER_FOLDER_ID`).
- Video Sekolah: `https://drive.google.com/drive/u/0/folders/1XV1h-CDoBH3hB3RSTAVxA2K2ttjbA2eq` (`FILM_VIDEO_FOLDER_ID`). Saat ini aplikasi memakai tautan YouTube; folder ini untuk penyimpanan video bila fitur unggah file digunakan.
- Poster Informasi: `https://drive.google.com/drive/u/0/folders/1zFKrk3ShC7OrDIQ4jEBWCxxKLXgYuHm2` (`PORTAL_POSTER_FOLDER_ID`).

## Pemutar YouTube

- Video YouTube dibuka melalui pemutar embed resmi di WebView aplikasi dengan `Referer` yang memakai Application ID Android. Tautan di dalam pemutar tidak membuka aplikasi lain.
- Pemutaran di perangkat Android belum diuji. Video yang pemiliknya menonaktifkan embed tetap dapat ditolak YouTube.
- Google Cloud menyediakan kunci YouTube Data API v3. Kunci dipakai sementara untuk membuat katalog lokal, tanpa disimpan dalam aplikasi. Katalog lengkap memperbarui isi hanya setelah skrip refresh dijalankan lagi; feed publik menangani unggahan terbaru di antaranya.

## Identitas

- Application ID Android: `id.sch.smapjsidoarjo.tv`.
- Logo berasal dari situs resmi Sekolah Pembangunan Jaya: `https://spj.sch.id/uploads/frontend/images/logo9.png`.
- Warna mengikuti logo sekolah: merah, gelap, dan latar terang.

## Splash screen

- Video latar vertikal 4 detik dibuat di Google Flow: `https://flow.google.com/project/d020b588-a64d-4992-be50-a67529f379ad`. Aset lokal `app/src/main/res/raw/school_splash.mp4` diputar tanpa audio.
- Simbol resmi sekolah dipotong tanpa mengubah bentuknya dari `school_official_logo.png` menjadi `app/src/main/res/drawable/school_official_mark.png`. Nama aplikasi muncul sekali sebagai wordmark dua tingkat `SMA PEMBANGUNAN JAYA 2` / `TV` di bawah simbol, tanpa kotak latar putih. Tipografi memakai Barlow Condensed dari Google Fonts dengan lisensi OFL di `licenses/BarlowCondensed-OFL.txt`.
- Bagian bawah tanpa kotak putih menampilkan `Powered By Extreme Studios` dan `Versi` yang membaca `BuildConfig.VERSION_NAME` dari `app/build.gradle.kts`. Splash berakhir saat video selesai, dengan batas waktu cadangan 5,5 detik.

APK belum dibuat. Buka proyek ini di Android Studio untuk sinkronisasi Gradle dan pengujian UI ketika build diminta.
