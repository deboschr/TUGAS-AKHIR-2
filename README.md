# Broken Link Checker

## Deskripsi

**Broken Link Checker** adalah aplikasi desktop berbasis **JavaFX** yang dikembangkan untuk membantu pengguna memeriksa tautan rusak (*broken links*) yang ada pada sebuah situs web. Aplikasi ini menerima *seed URL* sebagai titik awal, lalu melakukan proses **crawling** terhadap seluruh halaman yang berada dalam domain yang sama.  

Setiap URL yang ditemukan akan diakses menggunakan **OkHttp** untuk melakukan *fetching* konten dan memeriksa **status HTTP**-nya. Apabila tautan tersebut mengarah ke halaman HTML dan memiliki domain yang sama dengan seed URL, maka kontennya akan parse menggunakan **Jsoup** untuk mengekstraksi elemen `<a href>` dan menemukan tautan baru yang akan diperiksa lebih lanjut.  


## Teknologi Pengembangan

| Komponen | Versi | Keterangan |
|-----------|--------|-------------|
| **Java** | 21 | Bahasa pemrograman utama |
| **JavaFX** | 21 | Framework antarmuka pengguna |
| **Gradle** | 8.8 | Sistem build dan manajemen dependensi |
| **OkHttp** | 5.2.1 | HTTP client untuk melakukan fetching dan pemeriksaan HTTP status code tautan |
| **Jsoup** | 1.17.2 | Parser HTML untuk ekstraksi tautan |


## Fitur

- **Form Input URL**  
    Pengguna dapat memasukkan *seed URL* secara lengkap, termasuk skema `http://` atau `https://`. Aplikasi tidak menambahkan skema secara otomatis untuk menghindari kesalahan akses pada situs yang hanya mendukung salah satu protokol.

- **Tombol Kontrol**  
    Tersedia tombol **Start** dan **Stop** untuk memulai serta menghentikan proses pemeriksaan tautan kapan saja, sehingga pengguna memiliki kendali penuh atas jalannya proses.

- **Ringkasan Hasil**  
    Menampilkan informasi umum seperti jumlah total tautan yang diperiksa, jumlah halaman yang berhasil di-crawling (*webpage link*), jumlah tautan rusak (*broken link*), serta status proses pengecekan (IDLE, CHECKING, COMPLETED, STOPPED) yang diperbarui secara **real-time** selama proses berjalan.

- **Filter Hasil**  
    Pengguna dapat memfilter hasil yang tampil di tabel berdasarkan **URL** (dengan opsi *equals*, *contains*, *starts with*, dan *ends with*) maupun **kode status HTTP** (dengan opsi *equals*, *greater than*, dan *less than*), untuk memudahkan pencarian tautan tertentu.

- **Tabel Hasil**  
    Semua hasil pemeriksaan ditampilkan pada satu tabel utama yang berisi daftar tautan rusak. Tabel ini diperbarui secara **real-time** menggunakan mekanisme *data binding* JavaFX.

- **Ekspor Hasil**  
    Pengguna dapat mengekspor seluruh hasil pemeriksaan ke **file Excel (.xlsx)** untuk kebutuhan dokumentasi atau analisis lebih lanjut.

- **Detail Broken Link**  
    Setiap entri pada tabel hasil dapat diperluas untuk melihat informasi lengkap dari sebuah link.

## Cara Menjalankan

### Clone repository
```bash
git clone https://github.com/deboschr/TUGAS-AKHIR-2
```

### Build proyek
```bash
./gradlew build
```

### Jalankan aplikasi
```bash
./gradlew run
```

### Masukan URL
```bash
https://unpar.ac.id
```