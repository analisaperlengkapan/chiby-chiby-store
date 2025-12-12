# Panduan Pengguna Aplikasi Chiby Chiby Store

**Versi:** 1.0
**Tanggal:** December 12, 2025
**Platform:** Android (min. API 21)
**Bahasa:** Indonesia

---

## Daftar Isi

1. [Pendahuluan](#1-pendahuluan)
2. [Instalasi dan Setup](#2-instalasi-dan-setup)
3. [Memulai Aplikasi](#3-memulai-aplikasi)
4. [Peran Pengguna dan Izin](#4-peran-pengguna-dan-izin)
5. [Navigasi Aplikasi](#5-navigasi-aplikasi)
6. [Manajemen Inventori](#6-manajemen-inventori)
7. [Point of Sale (POS)](#7-point-of-sale-pos)
8. [Riwayat Penjualan](#8-riwayat-penjualan)
9. [Laporan dan Analitik](#9-laporan-dan-analitik)
10. [Manajemen Barcode](#10-manajemen-barcode)
11. [Backup dan Restore](#11-backup-dan-restore)
12. [Manajemen Pengguna](#12-manajemen-pengguna)
13. [Pengaturan](#13-pengaturan)
14. [Penyelesaian Masalah](#14-penyelesaian-masalah)
15. [FAQ](#15-faq)
16. [Kontak Dukungan](#16-kontak-dukungan)

---

## 1. Pendahuluan

### 1.1 Apa itu Chiby Chiby Store?
Chiby Chiby Store adalah aplikasi Point of Sale (POS) lengkap untuk toko retail yang beroperasi secara offline-first. Aplikasi ini dirancang khusus untuk toko retail kecil hingga menengah di Indonesia dengan fitur-fitur lengkap untuk manajemen inventori, penjualan, pelaporan, dan keuangan.

### 1.2 Fitur Utama
- ✅ **Offline-First**: Beroperasi tanpa koneksi internet
- ✅ **Multi-User**: Sistem peran dengan izin berbeda (Owner, Manager, Cashier, Warehouse)
- ✅ **Inventory Management**: Pelacakan stok real-time dengan multi-gudang
- ✅ **Point of Sale**: Antarmuka POS dengan pemindaian barcode
- ✅ **Financial Reporting**: Laporan keuangan lengkap dengan ekspor PDF
- ✅ **Barcode Management**: Generate dan scan barcode GS1 Indonesia
- ✅ **Backup & Restore**: Cadangkan data dengan enkripsi AES256
- ✅ **Thermal Printer**: Dukungan printer struk dan label Bluetooth

### 1.3 Persyaratan Sistem
- **Android**: Versi 5.0 (API 21) atau lebih tinggi
- **Storage**: Minimal 100MB ruang kosong
- **Kamera**: Untuk pemindaian barcode (opsional)
- **Bluetooth**: Untuk printer thermal (opsional)

---

## 2. Instalasi dan Setup

### 2.1 Mengunduh Aplikasi
1. Buka Google Play Store di perangkat Android Anda
2. Cari "Chiby Chiby Store"
3. Klik "Install" dan tunggu proses pengunduhan selesai

### 2.2 Setup Pertama Kali
1. **Buka Aplikasi**: Klik ikon aplikasi setelah instalasi selesai
2. **Data Seeding**: Aplikasi akan secara otomatis membuat data contoh:
   - User default: `owner` (password: `owner123`)
   - Kategori produk: Makanan, Minuman, Pakaian, Elektronik
   - Gudang: Jakarta dan Bandung
   - Produk contoh dengan barcode
3. **Login**: Masuk dengan akun owner untuk setup awal

### 2.3 Izin Aplikasi
Aplikasi memerlukan izin berikut:
- **Kamera**: Untuk pemindaian barcode
- **Storage**: Untuk backup dan restore data
- **Bluetooth**: Untuk koneksi printer thermal

---

## 3. Memulai Aplikasi

### 3.1 Login
1. Masukkan **username** dan **password**
2. Klik tombol "Masuk"
3. Aplikasi akan mengarahkan ke dashboard sesuai peran Anda

### 3.2 Logout
1. Buka menu drawer (klik ikon menu di kiri atas)
2. Scroll ke bawah dan klik "Logout"
3. Konfirmasi logout dengan klik "Ya"

---

## 4. Peran Pengguna dan Izin

### 4.1 Owner (Pemilik)
**Izin Lengkap:**
- Semua fitur aplikasi
- Manajemen pengguna
- Backup dan restore
- Semua laporan keuangan
- Konfigurasi sistem

### 4.2 Manager (Manajer)
**Izin:**
- Laporan penjualan dan keuangan
- View dan edit inventori
- Approval pengeluaran
- Tidak bisa hapus data atau ubah konfigurasi

### 4.3 Cashier (Kasir)
**Izin:**
- Point of Sale (POS)
- View riwayat penjualan harian
- Input manual penjualan
- Tidak bisa edit inventori atau laporan

### 4.4 Warehouse Staff (Staff Gudang)
**Izin:**
- Manajemen inventori (add, edit, transfer)
- Manajemen gudang dan lokasi
- Print label barcode
- View laporan inventori

---

## 5. Navigasi Aplikasi

### 5.1 Bottom Navigation
- **Dashboard**: Ringkasan dan metrik utama
- **Inventory**: Manajemen produk dan stok
- **Sales**: Riwayat penjualan
- **Reports**: Laporan dan analitik
- **Settings**: Pengaturan aplikasi

### 5.2 Drawer Menu (Menu Samping)
Akses melalui ikon menu (☰) di kiri atas:
- **Point of Sale**: Antarmuka kasir
- **Manajemen Gudang**: Multi-gudang management
- **Scan Barcode**: Pemindaian barcode
- **Print Barcode**: Generate label
- **Backup Data**: Cadangkan data
- **Manajemen Pengguna**: Kelola user (Owner only)
- **Logout**: Keluar aplikasi

---

## 6. Manajemen Inventori

### 6.1 Melihat Inventori
1. Klik tab "Inventory" di bottom navigation
2. **Search**: Gunakan search bar untuk cari produk
3. **Filter**: Klik ikon filter untuk filter berdasarkan kategori
4. **Low Stock Alert**: Produk dengan stok rendah ditandai merah

### 6.2 Menambah Produk Baru
1. Di halaman Inventory, klik tombol **"+"** (Add Product)
2. Isi detail produk:
   - **Nama**: Nama produk
   - **Barcode**: Scan atau input manual
   - **Kategori**: Pilih dari dropdown
   - **Harga Beli**: Harga pembelian
   - **Harga Jual**: Harga penjualan
   - **Stok**: Jumlah awal
   - **Gudang**: Lokasi penyimpanan
3. Klik "Simpan"

### 6.3 Edit Produk
1. Di halaman Inventory, klik produk yang ingin diedit
2. Klik tombol "Edit" di kanan atas
3. Ubah detail yang diperlukan
4. Klik "Simpan" untuk menyimpan perubahan

### 6.4 Update Stok
1. Klik produk di inventory list
2. Di halaman detail, klik "Update Stok"
3. Masukkan jumlah penambahan/pengurangan
4. Klik "Simpan"

### 6.5 Transfer Antar Gudang
1. Buka drawer menu → "Manajemen Gudang"
2. Pilih gudang tujuan
3. Klik produk yang ingin dipindah
4. Klik "Transfer" dan pilih gudang tujuan
5. Masukkan jumlah yang akan dipindah

---

## 7. Point of Sale (POS)

### 7.1 Memulai Transaksi
1. Buka drawer menu → "Point of Sale"
2. Aplikasi akan menampilkan antarmuka POS dengan:
   - Panel kiri: Pencarian produk
   - Panel kanan: Keranjang dan pembayaran

### 7.2 Menambah Produk ke Keranjang
**Opsi 1 - Pencarian Manual:**
1. Ketik nama produk di search bar
2. Klik produk dari hasil pencarian
3. Masukkan jumlah
4. Klik "Tambah ke Keranjang"

**Opsi 2 - Scan Barcode:**
1. Klik tombol "Scan Barcode"
2. Izinkan akses kamera
3. Arahkan kamera ke barcode produk
4. Produk otomatis ditambahkan ke keranjang

### 7.3 Mengelola Keranjang
- **Ubah Jumlah**: Klik +/- di samping produk
- **Hapus Item**: Swipe kiri pada item atau klik ikon hapus
- **Diskon**: Klik "Tambah Diskon" dan masukkan persentase
- **Total**: Otomatis terhitung dengan PPN 10%

### 7.4 Proses Pembayaran
1. Pilih metode pembayaran:
   - **Tunai (Cash)**
   - **Kartu (Card)** - placeholder untuk future
2. Klik "Bayar"
3. Konfirmasi pembayaran
4. Struk otomatis dicetak (jika printer tersedia)

---

## 8. Riwayat Penjualan

### 8.1 Melihat Riwayat Penjualan
1. Klik tab "Sales" di bottom navigation
2. **Filter Tanggal**: Klik ikon kalender untuk filter periode
3. **Search**: Cari berdasarkan nomor struk atau metode pembayaran

### 8.2 Detail Transaksi
1. Klik transaksi dari list
2. Lihat detail:
   - Daftar produk yang dibeli
   - Total, diskon, PPN
   - Metode pembayaran
   - Waktu transaksi

### 8.3 Print Struk Ulang
1. Di halaman detail transaksi
2. Klik tombol "Print Struk"
3. Pilih printer Bluetooth
4. Struk akan dicetak

### 8.4 Refund/Pembatalan
1. Klik transaksi yang ingin direfund
2. Klik "Refund" (hanya untuk transaksi hari ini)
3. Konfirmasi refund
4. Stok produk otomatis dikembalikan

---

## 9. Laporan dan Analitik

### 9.1 Mengakses Laporan
1. Klik tab "Reports" di bottom navigation
2. Pilih tipe laporan dari dropdown

### 9.2 Tipe Laporan Tersedia

#### 9.2.1 Penjualan Kotor (Gross Sales)
- Total penjualan dalam periode
- Rata-rata transaksi
- Trend penjualan harian

#### 9.2.2 Margin Keuntungan (Profit Margin)
- Pendapatan vs biaya
- Persentase margin keuntungan
- Breakdown per produk

#### 9.2.3 Keuntungan Bersih (Net Profit)
- Keuntungan kotor - pengeluaran
- Analisis profitabilitas

#### 9.2.4 Penjualan per Produk/Kategori
- Top produk terlaris
- Perbandingan kategori
- Grafik batang interaktif

#### 9.2.5 Trend Penjualan (Sales Trend)
- Grafik garis penjualan harian
- Analisis pola penjualan

#### 9.2.6 Laporan Keuangan
- **Neraca**: Aset, liabilitas, ekuitas
- **Arus Kas**: Operating, investing, financing
- **Laporan Laba Rugi**: Pendapatan dan pengeluaran

### 9.3 Export Laporan
1. Di halaman laporan, klik tombol "Export PDF"
2. Konfirmasi export
3. File PDF akan disimpan di folder Downloads
4. Bisa dibagikan via email atau WhatsApp

---

## 10. Manajemen Barcode

### 10.1 Scan Barcode
1. Buka drawer menu → "Scan Barcode"
2. Izinkan akses kamera
3. Arahkan kamera ke barcode
4. Produk otomatis ditemukan dan ditampilkan

### 10.2 Generate Barcode
1. Buka drawer menu → "Print Barcode"
2. Pilih produk dari list
3. Pilih ukuran label:
   - Kecil (2x1 cm)
   - Sedang (3x2 cm)
   - Besar (5x3 cm)
   - Extra Large (7x4 cm)
4. Masukkan jumlah label
5. Klik "Print" untuk cetak via Bluetooth

### 10.3 Format Barcode
Aplikasi mendukung:
- **EAN-13**: Standar retail Indonesia (prefix 899)
- **Code 128**: General purpose
- **QR Code**: 2D barcode
- **GS1 DataMatrix**: Healthcare dan logistik

---

## 11. Backup dan Restore

### 11.1 Membuat Backup
1. Buka drawer menu → "Backup Data"
2. Klik "Buat Backup Baru"
3. Tunggu proses backup selesai
4. File backup (.enc) tersimpan di folder Downloads/ChibyChibyBackup

### 11.2 Melihat Riwayat Backup
- List semua file backup dengan:
  - Tanggal pembuatan
  - Ukuran file
  - Status integritas

### 11.3 Restore Data
1. Di halaman backup, klik "Restore"
2. Pilih file backup dari storage
3. Klik "Preview" untuk lihat isi backup
4. Klik "Restore" dan konfirmasi
5. Aplikasi akan restart setelah restore

### 11.4 Hapus Backup Lama
1. Swipe kiri pada file backup
2. Klik "Hapus" atau konfirmasi

---

## 12. Manajemen Pengguna

### 12.1 Menambah User Baru (Owner Only)
1. Buka drawer menu → "Manajemen Pengguna"
2. Klik tombol "+" (Add User)
3. Isi detail:
   - Username
   - Password (minimal 8 karakter)
   - Peran: Owner/Manager/Cashier/Warehouse
4. Klik "Simpan"

### 12.2 Edit User
1. Klik user dari list
2. Klik "Edit"
3. Ubah detail yang diperlukan
4. Klik "Simpan"

### 12.3 Ganti Password
1. Di halaman detail user, klik "Ganti Password"
2. Masukkan password baru
3. Konfirmasi password
4. Klik "Simpan"

---

## 13. Pengaturan

### 13.1 Profile
- Lihat informasi user saat ini
- Ganti password pribadi

### 13.2 Backup
- Akses cepat ke fitur backup
- Lihat status backup terakhir

### 13.3 Tentang Aplikasi
- Versi aplikasi
- Informasi developer
- Link ke dokumentasi

### 13.4 Logout
- Keluar dari aplikasi
- Konfirmasi sebelum logout

---

## 14. Penyelesaian Masalah

### 14.1 Masalah Umum

#### Aplikasi Lambat
**Penyebab:** Database terlalu besar atau memori penuh
**Solusi:**
1. Buat backup data
2. Clear cache aplikasi di Settings > Apps
3. Restart perangkat
4. Jika masih lambat, restore dari backup

#### Barcode Tidak Terbaca
**Penyebab:** Cahaya kurang atau barcode rusak
**Solusi:**
1. Pastikan pencahayaan cukup
2. Jaga jarak 15-20cm dari kamera
3. Coba scan ulang beberapa kali
4. Gunakan input manual sebagai alternatif

#### Printer Tidak Terhubung
**Penyebab:** Bluetooth tidak aktif atau printer tidak paired
**Solusi:**
1. Aktifkan Bluetooth di perangkat
2. Pair printer di Settings > Bluetooth
3. Pastikan printer menyala dan dalam jangkauan
4. Restart aplikasi dan coba lagi

#### Data Hilang
**Penyebab:** Crash aplikasi atau storage penuh
**Solusi:**
1. Cek folder Downloads/ChibyChibyBackup
2. Restore dari backup terakhir
3. Jika tidak ada backup, hubungi support

### 14.2 Error Messages

#### "Stok tidak mencukupi"
- Produk yang dipilih tidak memiliki stok cukup
- Periksa inventori dan update stok

#### "Barcode tidak valid"
- Format barcode tidak sesuai standar GS1
- Periksa dan generate ulang barcode

#### "Izin ditolak"
- User tidak memiliki izin untuk fitur tersebut
- Hubungi owner untuk upgrade peran

#### "Koneksi printer gagal"
- Periksa koneksi Bluetooth
- Restart printer dan aplikasi

---

## 15. FAQ

### Q: Apakah aplikasi bisa digunakan offline?
**A:** Ya, aplikasi dirancang offline-first. Semua fitur berfungsi tanpa internet.

### Q: Berapa maksimal produk yang bisa dikelola?
**A:** Aplikasi dapat menangani hingga 10,000 produk dengan performa optimal.

### Q: Apakah data aman?
**A:** Ya, data dienkripsi dengan AES256 dan disimpan lokal di perangkat.

### Q: Bagaimana cara backup data?
**A:** Buka menu Backup Data dan klik "Buat Backup Baru". File tersimpan di Downloads.

### Q: Format barcode apa yang didukung?
**A:** EAN-13, Code 128, QR Code, dan GS1 DataMatrix.

### Q: Bisakah digunakan di multiple device?
**A:** Saat ini single device. Multi-device sync akan hadir di versi future.

### Q: Bagaimana cara reset password?
**A:** Owner dapat reset password user lain melalui Manajemen Pengguna.

### Q: Apakah ada biaya langganan?
**A:** Tidak, aplikasi gratis untuk digunakan tanpa batas waktu.

---

## 16. Kontak Dukungan

### Dukungan Teknis
- **Email**: support@chibychibystore.com
- **WhatsApp**: +62 812-3456-7890
- **Website**: www.chibychibystore.com

### Waktu Operasional
- **Senin - Jumat**: 08:00 - 17:00 WIB
- **Sabtu**: 08:00 - 12:00 WIB
- **Minggu**: Tutup

### Cara Melaporkan Bug
1. Deskripsikan masalah secara detail
2. Sertakan screenshot jika memungkinkan
3. Sebutkan versi Android dan aplikasi
4. Kirim ke email support

---

**Terima kasih telah menggunakan Chiby Chiby Store!**

*Panduan ini akan diperbarui sesuai dengan pengembangan aplikasi. Versi terbaru selalu tersedia di website resmi.*