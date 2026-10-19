# Sistem Absensi Presensi Wajah Cerdas & Web Pelaporan Realtime (SmartFace Attendance)

Aplikasi presensi kehadiran pintar berbasis Android dengan pengenalan wajah (Face Recognition) otomatis untuk sekolah/madrasah, dilengkapi sistem pelaporan otomatis berbasis web yang dapat diakses langsung oleh PC/laptop melalui jaringan kabel Ethernet, serta sinkronisasi data realtime melalui Firebase.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Keputusan Utama Berdasarkan Preferensi Anda:**
> - **Kategori Organisasi**: Disesuaikan khusus untuk institusi **Sekolah / Madrasah** (kategori Guru, Tenaga Kependidikan/Staf, dan Siswa per Kelas).
> - **Metode Presensi**: **Pendaftaran foto wajah + deteksi kamera otomatis**. Saat wajah terdeteksi di kamera depan/belakang, sistem memverifikasi kesesuaian data, mencatat status kehadiran (Tepat Waktu/Terlambat/Pulang), dan memberikan feedback visual/audio instan.
> - **Akses Pelaporan Web & Ethernet**: Android menjalankan **Local Web Server (Port 8080)** yang melayani Web Dashboard responsif yang dapat dibuka dari browser laptop/komputer admin di jaringan LAN/Ethernet yang sama (`http://<IP-Ethernet>:8080`), sekaligus menyediakan **Admin Portal terintegrasi di dalam aplikasi Android**.
> - **Sinkronisasi Realtime Firebase**: Semua data master (pengguna/siswa/guru) dan riwayat absensi harian disinkronkan secara realtime ke Firebase Firestore.

## 1. Overview & Core Concept

- **Apa yang Dibangun**:
  1. **Mode Kios Presensi Android**: Tampilan kamera responsif dengan overlay panduan wajah, deteksi liveness & kecocokan profil wajah otomatis, indikator jam digital presisi, status koneksi Ethernet, dan konfirmasi presensi bersuara/bergetar.
  2. **Manajemen Pendaftaran (Enrollment)**: Pendaftaran profil guru & siswa dengan foto wajah, NIS/NIP, nama lengkap, kelas/bagian, serta jam kerja/sekolah.
  3. **Backend Pelaporan Web & In-App Admin**: Dashboard statistik kehadiran harian, mingguan, dan bulanan, rekapitulasi persentase kehadiran, filter per kelas/guru, ekspor laporan data (CSV/Excel siap cetak), dan manajemen izin/sakit.
  4. **Konektivitas Ethernet & Realtime Cloud**: Menampilkan status IP Ethernet aktif pada perangkat dan sinkronisasi instan ke Firebase Firestore.

- **Target Pengguna**:
  - Siswa dan Guru/Staf Sekolah sebagai pengguna absensi mandiri di kiosk Android.
  - Petugas Tata Usaha (TU), Guru Piket, dan Kepala Sekolah sebagai admin pemantau laporan melalui browser web di PC.

## 2. User Experience & Visual Design

### Key User Flows

1. **Alur Absensi Wajah (Kiosk Flow)**:
   - Layar menampilkan viewfinder kamera dengan panduan bingkai wajah beranimasi.
   - User berdiri di depan kamera Android; sistem mendeteksi wajah secara real-time.
   - Bila wajah terdaftar cocok: sistem otomatis mencatat absensi (Masuk/Pulang), menampilkan kartu profil dengan ucapan selamat datang, indikator "Tepat Waktu" atau "Terlambat", dan membunyikan nada konfirmasi.
   - Layar otomatis kembali ke kondisi siap pindai dalam 2,5 detik tanpa perlu sentuhan tangan (touchless).

2. **Alur Pendaftaran Wajah (Enrollment Flow)**:
   - Admin membuka menu pendaftaran terproteksi PIN/keamanan.
   - Mengambil sampel foto wajah melalui kamera atau galeri, mengisi NIP/NIS, nama, jabatan/kelas, dan kontak.
   - Data langsung disimpan dan diindeks untuk pengenalan instan.

3. **Alur Pelaporan Web (Web Dashboard Flow via Ethernet)**:
   - Admin membuka browser di laptop/PC yang terhubung ke jaringan kabel Ethernet yang sama, mengetikkan alamat `http://<IP-Ethernet>:8080`.
   - Menampilkan halaman dasbor HTML5/Tailwind modern: kartu ringkasan kehadiran hari ini (Hadir, Izin, Sakit, Terlambat, Alfa), grafik tren kehadiran, dan tabel log kehadiran live update.
   - Tombol "Export Laporan CSV / Cetak Rekap" untuk laporan dinas atau arsip sekolah.

### Visual Identity & Theme (Material 3)

- **Aesthetic**: Modern Institutional Minimalis — bersih, andal, ramah untuk lingkungan pendidikan.
- **Color Palette**:
  - Primary: Deep Teal / Emerald (`#0F766E` / `#14B8A6`) melambangkan profesionalitas, akurasi, dan ketenangan.
  - Secondary Accent: Energetic Cyan & Amber (`#0284C7` & `#F59E0B` untuk status Terlambat/Peringatan).
  - Background: Clean Slate Neutral (`#F8FAFC` untuk light theme, `#0F172A` untuk dark/kiosk mode kontras tinggi).
  - Success/On-Time: Vivid Green (`#10B981`).
- **Typography**: Huruf sans-serif tegas dengan hierarki skala teks jelas untuk jam digital besar dan pembacaan nama yang mudah dari jarak 1 meter.

## 3. Key Product Decisions & Trade-Offs

- **Pengenalan Wajah On-Device Cepat**:
  - *Pendekatan*: Deteksi wajah real-time pada frame kamera menggunakan detektor geometri & feature matching lokal di Android, digabungkan dengan validasi profil master.
  - *Alasan*: Presensi harus beroperasi instan (<500ms) tanpa jeda internet, serta tetap berfungsi mulus bila koneksi internet sesaat terputus melalui kabel Ethernet lokal.
- **Dual Dashboard (Web Server Lokal + In-App Admin)**:
  - *Pendekatan*: Aplikasi Android menyertakan micro HTTP server bawaan (Ktor / NanoHttpd engine) yang menyajikan dashboard web responsif ke browser PC di jaringan Ethernet, ditambah layar Admin Dashboard native langsung di aplikasi Android.
  - *Alasan*: Memberikan kepraktisan maksimal: admin TU bisa membuka di layar PC monitor besar lewat Ethernet tanpa perlu install aplikasi apapun di komputer.
- **Firebase Firestore Realtime Layer**:
  - *Pendekatan*: Database cloud Firestore dengan listener real-time untuk replikasi data presensi, riwayat absensi, dan master pengguna.
  - *Alasan*: Memastikan data tidak hilang saat perangkat berganti dan memungkinkan pemantauan multi-perangkat.

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                    PERANGKAT ANDROID                         │
│                                                              │
│  ┌───────────────────────┐        ┌───────────────────────┐  │
│  │  CameraX Viewfinder   │        │   Admin & Settings    │  │
│  │  + Face Mesh Scanner  │        │   - Master Data Guru  │  │
│  │  - Deteksi Otomatis   │        │   - Master Data Siswa │  │
│  │  - Status Masuk/Pulang│        │   - Rekap Presensi    │  │
│  └───────────┬───────────┘        └───────────┬───────────┘  │
│              │                                │              │
│              ▼                                ▼              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │               Attendance Engine & Repository            │  │
│  └───────────┬────────────────────────────────┬───────────┘  │
│              │                                │              │
│              ▼                                ▼              │
│  ┌───────────────────────┐        ┌───────────────────────┐  │
│  │   Embedded Web Server │        │   Firebase Firestore  │  │
│  │   (Port 8080 LAN)     │        │   (Cloud Sync API)    │  │
│  └───────────┬───────────┘        └───────────┬───────────┘  │
└──────────────┼────────────────────────────────┼──────────────┘
               │ (Kabel Ethernet / LAN)         │ (Internet)
               ▼                                ▼
┌──────────────────────────────┐    ┌──────────────────────────┐
│   Web Browser di PC / Laptop │    │   Firebase Cloud Backend │
│   - Live Dashboard Monitoring│    │   - Firestore Collections│
│   - Filter & Export Laporan  │    │   - Secure Cloud Backup  │
└──────────────────────────────┘    └──────────────────────────┘
```

### Data Model Entity Structure

1. **User Profile (`User` / `Member`)**:
   - `id`: String (UUID / NIP / NIS)
   - `name`: String
   - `role`: Enum (`TEACHER`, `STAFF`, `STUDENT`)
   - `departmentOrClass`: String (e.g. "Kelas 7A", "Guru IPA", "Tata Usaha")
   - `photoUri` / `faceFeatureData`: String (referensi foto & vektor karakteristik wajah)
   - `registeredAt`: Long

2. **Attendance Log (`AttendanceRecord`)**:
   - `id`: String
   - `userId`: String
   - `userName`: String
   - `role`: String
   - `departmentOrClass`: String
   - `timestamp`: Long
   - `date`: String (`YYYY-MM-DD`)
   - `time`: String (`HH:mm:ss`)
   - `type`: Enum (`CHECK_IN`, `CHECK_OUT`)
   - `status`: Enum (`ON_TIME`, `LATE`, `EARLY_DEPARTURE`)
   - `verificationMethod`: String ("FACE_RECOGNITION")
   - `confidence`: Float

3. **Attendance Rules / Settings (`SchoolSchedule`)**:
   - `checkInStart`: String ("06:30")
   - `checkInDeadline`: String ("07:15") -> setelah ini terhitung Terlambat
   - `checkOutStart`: String ("14:00")
   - `webServerPort`: Int (8080)
   - `ethernetIp`: String (Alamat IP LAN yang aktif)

### Roadmap Implementasi

1. **Konfigurasi Dependensi & Izin**: Mengaktifkan CameraX, Firebase Firestore & Auth, Coroutines, dan izin kamera & network di `AndroidManifest.xml` & `build.gradle.kts`.
2. **Setup Firebase Backend**: Menyiapkan `firebase-blueprint.json` dan `firestore.rules` serta konfigurasi database ID.
3. **Engine Deteksi Wajah & Kiosk Camera UI**: Mengimplementasikan antarmuka kamera pintar dengan overlay penanda deteksi wajah, animasi pemindaian, dan feedback audio/visual.
4. **Local Web Server Engine & Web Dashboard HTML5**: Membangun server HTTP internal perangkat yang menyajikan antarmuka web modern responsif dengan tabel presensi live, ringkasan kartu statistik, dan export laporan CSV.
5. **In-App Management & Reporting UI**: Layar navigasi M3 untuk:
   - Layar Presensi Kios (Face Camera)
   - Layar Pendaftaran Wajah (Enrollment Guru/Siswa)
   - Layar Laporan & Rekapitulasi Presensi
   - Layar Status Jaringan Ethernet & Web Server
6. **Verifikasi & Pengujian Kompilasi**: Menjalankan build via `compile_applet` untuk memastikan seluruh komponen berjalan sempurna tanpa error.
