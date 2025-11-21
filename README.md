# Broken Link Scanner

## Deskripsi

**Broken Link Scanner** adalah aplikasi desktop berbasis **JavaFX** yang dikembangkan untuk membantu pengguna untuk memeriksa tautan rusak (*broken links*) yang ada pada sebuah situs web. Aplikasi ini menerima sebuah URL (*Seed URL*) sebagai titik awal pemeriksaan, lalu melakukan proses **Web Crawling** terhadap seluruh halaman situs web tersebut.

Pada setiap halaman situs web yang ditemukan akan dilakukan *fetching* lalu *response body*-nya akan di-*parse* ke dokumen HTML. Selanjutnya ekstraksi tautan akan dilakukan pada dokumen HTML ini yang diambil berdasarkan elemen `<a href>`. Setiap tautan hasil ekstraksi akan dilakukan pemeriksaan dengan melakukan *fetching*. Tautan yang gagal diperiksa (*connection error*) atau mengembalikan kode status HTTP 4XX - 5XX maka akan dianggap tautan rusak.


## Teknologi Pengembangan

| Komponen            | Versi | Keterangan                                                                     |
|---------------------|--------|--------------------------------------------------------------------------------|
| **Java**            | 21 | Bahasa pemrograman utama                                                       |
| **JavaFX**          | 21 | Framework antarmuka pengguna                                                   |
| **Gradle**          | 8.8 | Sistem build dan manajemen dependensi                                          |
| **Jsoup**           | 1.17.2 | Parser HTML untuk ekstraksi tautan                                             |
| **Java HttpClient** |  | HTTP client untuk melakukan fetching dan pemeriksaan kode status tautan tautan |


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
    Semua hasil pemeriksaan ditampilkan pada satu tabel yang berisi daftar tautan rusak. Tabel ini diperbarui secara **real-time** menggunakan mekanisme *data binding* JavaFX.

- **Ekspor Hasil**  
    Pengguna dapat mengekspor seluruh hasil pemeriksaan ke **file Excel (.xlsx)** untuk kebutuhan dokumentasi atau analisis lebih lanjut.

- **Detail Broken Link**  
    Setiap entri pada tabel hasil dapat diperluas untuk melihat informasi lengkap dari sebuah tautan.

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
https://informatika.unpar.ac.id
```