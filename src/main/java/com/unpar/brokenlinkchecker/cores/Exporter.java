package com.unpar.brokenlinkchecker.cores;

import com.unpar.brokenlinkchecker.models.Link;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Kelas ini tugasnya khusus buat urusan export hasil broken link ke file Excel (.xlsx).
 * Jadi alurnya: terima daftar link rusak, urutkan, bentuk tabel, atur style,
 * terus disimpan ke file yang dikasih.
 */
public class Exporter { // mulai definisi kelas Exporter

    // ====================== KONSTANTA UMUM TABEL ======================

    /** Nama sheet yang dipakai di workbook Excel. */
    private static final String SHEET_NAME = "Broken Links"; // nama sheet tetap, ga perlu diubah-ubah

    // Header kolom yang bakal dipakai di baris pertama Excel
    private static final String[] HEADERS = {
            "URL",               // kolom 0: URL awal yang dicek
            "Final URL",         // kolom 1: URL akhir setelah redirect
            "Content Type",      // kolom 2: tipe konten dari response
            "Error",             // kolom 3: pesan error / status
            "Source Webpage",    // kolom 4: halaman sumber yang mengarah ke broken link
            "Anchor Text"        // kolom 5: teks anchor di halaman sumber
    };

    // indeks kolom biar ga pakai angka magic di tengah-tengah kode
    private static final int COL_URL = 0;            // index kolom URL
    private static final int COL_FINAL_URL = 1;      // index kolom Final URL
    private static final int COL_CONTENT_TYPE = 2;   // index kolom Content Type
    private static final int COL_ERROR = 3;          // index kolom Error
    private static final int COL_SOURCE = 4;         // index kolom Source Webpage
    private static final int COL_ANCHOR = 5;         // index kolom Anchor Text
    private static final int COL_DUMMY = 6;          // index kolom dummy blocker di paling kanan

    // lebar kolom dalam satuan unit Excel (kurang lebih karakter * 256)
    private static final int WIDTH_URL = 15000;      // lebar kolom URL
    private static final int WIDTH_FINAL_URL = 15000; // lebar kolom Final URL
    private static final int WIDTH_CONTENT_TYPE = 10000; // lebar kolom Content Type
    private static final int WIDTH_ERROR = 10000;    // lebar kolom Error
    private static final int WIDTH_SOURCE = 15000;   // lebar kolom Source Webpage
    private static final int WIDTH_ANCHOR = 10000;   // lebar kolom Anchor Text
    private static final int WIDTH_DUMMY = 2000;     // lebar kolom dummy (kecil aja)

    // ====================== WARNA TEMA APLIKASI ======================

    /** Warna primary yang dipakai untuk baris ganjil. */
    private static final Color COLOR_PRIMARY = Color.decode("#f4ebdb"); // warna background baris ganjil

    /** Warna putih krem untuk baris genap. */
    private static final Color COLOR_WHITE = Color.decode("#f1f0eb"); // warna background baris genap

    /** Warna background header (hijau tua). */
    private static final Color COLOR_HEADER_BG = Color.decode("#2f5d50"); // warna hijau tua buat header

    // ====================== ATRIBUT STYLE YANG BISA DIPAKAI ULANG ======================

    /** Style khusus header tabel. */
    private CellStyle headerStyle; // nyimpen style header setelah di-init

    /** Style buat baris ganjil. */
    private CellStyle oddRowStyle; // nyimpen style baris ganjil

    /** Style buat baris genap. */
    private CellStyle evenRowStyle; // nyimpen style baris genap

    /** Style kosong buat kolom dummy (tanpa border dan fill). */
    private CellStyle emptyStyle; // style sederhana buat kolom dummy (biar ga ganggu tampilan)

    /**
     * Method utama yang dipanggil dari luar buat export data ke Excel.
     *
     * @param brokenLinks daftar link yang berstatus rusak yang mau diexport
     * @param file        file tujuan output (.xlsx) yang mau diisi data
     * @throws IOException kalau ada masalah waktu nulis file fisik
     */
    public void save(List<Link> brokenLinks, File file) throws IOException { // method utama export

        // bikin list baru biar ga ngoprek list original yang dikirim
        List<Link> sortedBrokenLinks = new ArrayList<>(brokenLinks); // copy konten brokenLinks ke list baru

        // urutin list berdasarkan jumlah connection (source halaman) dari kecil ke besar
        sortedBrokenLinks.sort(Comparator.comparingInt(a -> a.getConnection().size())); // sort ascending by connection size

        // bikin workbook baru dan pastikan nanti otomatis ke-close pakai try-with-resources
        try (Workbook workbook = new XSSFWorkbook()) { // mulai blok try pakai XSSFWorkbook (xlsx)

            // init semua style yang bakal dipakai (header, row ganjil/genap, dummy)
            initStyles(workbook); // panggil helper buat set isi headerStyle, oddRowStyle, evenRowStyle, emptyStyle

            // bikin sheet baru dengan nama yang sudah didefinisikan
            Sheet sheet = workbook.createSheet(SHEET_NAME); // buat sheet Excel dengan nama "Broken Links"

            // tulis baris header di paling atas
            writeHeaderRow(sheet); // bikin baris header lengkap dengan style

            // tulis body / isi data ke bawah, mulai dari baris index 1
            writeBodyRows(sheet, sortedBrokenLinks); // isi baris data dan merge kolom yang perlu

            // atur lebar tiap kolom supaya tampilan lebih rapi
            setColumnWidths(sheet); // set lebar kolom-kolom tabel

            // terakhir, tulis workbook ke file tujuan di disk
            writeWorkbookToFile(workbook, file); // flush isi workbook ke file .xlsx
        } // di sini workbook otomatis di-close karena try-with-resources
    } // tutup method save

    /**
     * Inisialisasi semua style yang akan dipakai di workbook ini.
     * Style di sini bergantung ke workbook, jadi harus dipanggil setelah workbook dibuat.
     *
     * @param workbook objek workbook yang lagi aktif
     */
    private void initStyles(Workbook workbook) { // method buat nyiapin semua style

        // bikin style header pakai warna dan font yang sudah ditentukan
        this.headerStyle = createHeaderStyle(workbook, COLOR_HEADER_BG); // set field headerStyle

        // bikin style baris ganjil pakai warna primary
        this.oddRowStyle = createRowStyle(workbook, COLOR_PRIMARY); // set field oddRowStyle

        // bikin style baris genap pakai warna putih krem
        this.evenRowStyle = createRowStyle(workbook, COLOR_WHITE); // set field evenRowStyle

        // bikin style kosong tanpa border dan fill buat kolom dummy
        this.emptyStyle = workbook.createCellStyle(); // style default (tanpa border, tanpa fill) untuk dummy
    } // tutup method initStyles

    /**
     * Menulis baris header (judul kolom) di baris paling atas sheet.
     *
     * @param sheet sheet Excel yang lagi dipakai
     */
    private void writeHeaderRow(Sheet sheet) { // method buat nulis header tabel

        // bikin baris baru di index 0 (baris pertama)
        Row headerRow = sheet.createRow(0); // baris header ada di row 0

        // atur tinggi baris biar sedikit lebih besar dari baris biasa
        headerRow.setHeightInPoints(25); // set tinggi baris header (dalam satuan point)

        // loop semua item di array HEADERS buat diisi ke kolom-kolom
        for (int i = 0; i < HEADERS.length; i++) { // looping dari kolom 0 sampai kolom terakhir header

            // bikin cell untuk kolom ke-i di baris header
            Cell cell = headerRow.createCell(i); // buat cell di posisi kolom i

            // set teks header sesuai value di array HEADERS
            cell.setCellValue(HEADERS[i]); // isi teks header

            // pasang style header ke cell ini
            cell.setCellStyle(headerStyle); // pasang style header (warna, bold, center)
        } // selesai mengisi semua header kolom

        // bikin satu cell dummy di kolom paling kanan (kolom dummy)
        Cell dummyHeader = headerRow.createCell(COL_DUMMY); // buat cell dummy di kolom 6

        // isi dummy dengan string kosong (biar Excel anggap cell-nya ada)
        dummyHeader.setCellValue(""); // isi kosong supaya ga kelihatan apa-apa

        // style dummy pakai emptyStyle (tanpa border, tanpa background)
        dummyHeader.setCellStyle(emptyStyle); // pasang style kosong ke dummy
    } // tutup method writeHeaderRow

    /**
     * Menulis semua baris data (body) ke sheet.
     * Di sini termasuk logika merge kolom 0–3 kalau satu broken link punya banyak source.
     *
     * @param sheet            sheet Excel tujuan penulisan
     * @param sortedBrokenLinks list link rusak yang sudah di-sort dari paling sedikit connection
     */
    private void writeBodyRows(Sheet sheet, List<Link> sortedBrokenLinks) { // method buat nulis isi tabel

        // mulai dari baris index 1 karena 0 sudah dipakai header
        int rowIndex = 1; // baris awal untuk data

        // loop semua broken link yang sudah diurutkan
        for (Link link : sortedBrokenLinks) { // looping tiap link rusak

            // catat baris awal untuk link ini, nanti dipakai buat merge
            int startRow = rowIndex; // simpan index baris pertama untuk broken link ini

            // flag untuk nandain baris pertama dari sekelompok source untuk link ini
            boolean first = true; // true kalau lagi di baris pertama dari group link ini

            // loop semua pasangan (sourceLink, anchorText) dari connection link ini
            for (Map.Entry<Link, String> entry : link.getConnection().entrySet()) { // looping semua connection/source

                // bikin baris baru di sheet untuk data ini
                Row row = sheet.createRow(rowIndex); // buat row baru di posisi rowIndex

                // cek apakah baris ini genap atau ganjil buat nentuin warna zebra
                boolean isEvenRow = (rowIndex % 2 == 0); // true kalau baris genap

                // pilih style berdasarkan ganjil/genap
                CellStyle rowStyle = isEvenRow ? evenRowStyle : oddRowStyle; // pilih style row yang sesuai

                // kalau ini baris pertama untuk link ini, kolom 0–3 diisi data
                if (first) { // kalau baris pertama untuk broken link ini

                    // isi kolom URL
                    createStyledCell(row, COL_URL, link.getUrl(), rowStyle); // isi URL pertama kali

                    // isi kolom Final URL
                    createStyledCell(row, COL_FINAL_URL, link.getFinalUrl(), rowStyle); // isi Final URL

                    // isi kolom Content Type
                    createStyledCell(row, COL_CONTENT_TYPE, link.getContentType(), rowStyle); // isi content type

                    // isi kolom Error
                    createStyledCell(row, COL_ERROR, link.getError(), rowStyle); // isi pesan error

                    // setelah ini, baris berikutnya untuk link yang sama ga perlu isi kolom 0–3 lagi
                    first = false; // set flag jadi false karena baris pertama sudah lewat
                } else { // kalau bukan baris pertama untuk broken link ini

                    // isi kolom 0–3 dengan string kosong supaya keliatan merge-nya nanti
                    createStyledCell(row, COL_URL, "", rowStyle); // kosongkan URL

                    createStyledCell(row, COL_FINAL_URL, "", rowStyle); // kosongkan Final URL

                    createStyledCell(row, COL_CONTENT_TYPE, "", rowStyle); // kosongkan Content Type

                    createStyledCell(row, COL_ERROR, "", rowStyle); // kosongkan Error
                } // selesai handle kolom 0–3

                // isi kolom Source Webpage dengan URL dari link sumber
                createStyledCell(row, COL_SOURCE, entry.getKey().getUrl(), rowStyle); // isi URL sumber halaman

                // isi kolom Anchor Text dengan teks anchor yang ditemukan di halaman sumber
                createStyledCell(row, COL_ANCHOR, entry.getValue(), rowStyle); // isi anchor text

                // buat cell dummy di kolom paling kanan buat ngeblok overflow teks anchor
                Cell blocker = row.createCell(COL_DUMMY); // bikin cell dummy di kolom dummy

                // isi dummy dengan string kosong supaya dianggap ada tapi ga kelihatan
                blocker.setCellValue(""); // isi kosong

                // pakai style emptyStyle biar ga muncul border atau warna
                blocker.setCellStyle(emptyStyle); // pasang style kosong

                // pindah ke baris berikutnya setelah selesai isi data baris ini
                rowIndex++; // increment row index
            } // selesai loop semua connection untuk link ini

            // hitung baris terakhir yang dipakai untuk broken link ini
            int endRow = rowIndex - 1; // rowIndex sudah maju satu, jadi row terakhir = rowIndex - 1

            // kalau ada lebih dari satu baris untuk link ini, baru merge kolom 0–3
            if (endRow > startRow) { // cuma merge kalau minimal ada 2 baris

                // merge untuk kolom 0 sampai 3
                for (int col = COL_URL; col <= COL_ERROR; col++) { // looping dari kolom URL sampai Error

                    // tambahkan region merge dari startRow sampai endRow untuk kolom ini
                    sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, col, col)); // merge vertikal di kolom tsb
                } // selesai merge semua kolom 0–3
            } // selesai cek perlu merge atau engga
        } // selesai loop semua brokenLinks
    } // tutup method writeBodyRows

    /**
     * Mengatur lebar semua kolom di sheet supaya tampilan rapi dan konsisten.
     *
     * @param sheet sheet Excel yang lagi kita atur
     */
    private void setColumnWidths(Sheet sheet) { // method buat set lebar kolom

        // atur lebar kolom URL
        sheet.setColumnWidth(COL_URL, WIDTH_URL); // set width kolom URL

        // atur lebar kolom Final URL
        sheet.setColumnWidth(COL_FINAL_URL, WIDTH_FINAL_URL); // set width kolom Final URL

        // atur lebar kolom Content Type
        sheet.setColumnWidth(COL_CONTENT_TYPE, WIDTH_CONTENT_TYPE); // set width kolom Content Type

        // atur lebar kolom Error
        sheet.setColumnWidth(COL_ERROR, WIDTH_ERROR); // set width kolom Error

        // atur lebar kolom Source Webpage
        sheet.setColumnWidth(COL_SOURCE, WIDTH_SOURCE); // set width kolom Source Webpage

        // atur lebar kolom Anchor Text
        sheet.setColumnWidth(COL_ANCHOR, WIDTH_ANCHOR); // set width kolom Anchor Text

        // atur lebar kolom dummy (kecil saja, cuma buat nahan overflow)
        sheet.setColumnWidth(COL_DUMMY, WIDTH_DUMMY); // set width kolom dummy
    } // tutup method setColumnWidths

    /**
     * Nulis seluruh isi workbook ke file output di disk.
     *
     * @param workbook workbook yang sudah terisi data
     * @param file     file tujuan yang mau diisi workbook ini
     * @throws IOException kalau ada masalah IO saat nulis file
     */
    private void writeWorkbookToFile(Workbook workbook, File file) throws IOException { // method buat simpan workbook ke file

        // bikin FileOutputStream ke file tujuan
        try (FileOutputStream fos = new FileOutputStream(file)) { // try-with-resources buat stream file

            // tulis konten workbook ke stream (dan akhirnya ke file)
            workbook.write(fos); // write semua isi workbook ke file
        } // stream otomatis di-close di sini
    } // tutup method writeWorkbookToFile

    /**
     * Bikin style khusus untuk header (warna hijau gelap, teks putih, bold, center).
     *
     * @param workbook workbook tempat style ini dipakai
     * @param bgColor  warna background header
     * @return CellStyle yang sudah siap dipakai untuk header
     */
    private CellStyle createHeaderStyle(Workbook workbook, Color bgColor) { // method bikin style header

        // bikin font khusus untuk header
        XSSFFont textFont = (XSSFFont) workbook.createFont(); // cast ke XSSFFont karena pakai XSSFColor

        // set tulisan header jadi bold
        textFont.setBold(true); // header pakai huruf tebal

        // set warna teks header jadi putih krem
        textFont.setColor(new XSSFColor(COLOR_WHITE, null)); // warna teks header pakai constant COLOR_WHITE

        // bikin objek style baru buat header
        CellStyle style = workbook.createCellStyle(); // buat style baru

        // pasang font yang sudah disiapkan ke style
        style.setFont(textFont); // set font ke style

        // atur teks header di-center horizontal
        style.setAlignment(HorizontalAlignment.CENTER); // rata tengah horizontal

        // atur teks header di-center vertical
        style.setVerticalAlignment(VerticalAlignment.CENTER); // rata tengah vertikal

        // set warna background header pakai bgColor
        style.setFillForegroundColor(new XSSFColor(bgColor, null)); // set warna background header

        // isi pattern fill supaya warna background kepakai
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND); // pakai fill solid

        // kasih border di semua sisi header
        createBorder(style); // panggil helper buat atur border

        // balikin objek style yang sudah siap dipakai
        return style; // return style header
    } // tutup method createHeaderStyle

    /**
     * Bikin style untuk baris biasa (isi tabel), dipakai untuk zebra striping.
     *
     * @param workbook workbook tempat style ini akan dipakai
     * @param awtColor warna background untuk baris (primary atau white)
     * @return CellStyle yang siap dipasang ke baris
     */
    private CellStyle createRowStyle(Workbook workbook, Color awtColor) { // method bikin style baris biasa

        // bikin style baru untuk baris
        CellStyle style = workbook.createCellStyle(); // buat objek CellStyle baru

        // set warna background cell sesuai warna yang dikirim
        style.setFillForegroundColor(new XSSFColor(awtColor, null)); // set warna background baris

        // pakai pattern solid biar warna kelihatan
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND); // pakai fill solid

        // atur teks rata kiri
        style.setAlignment(HorizontalAlignment.LEFT); // teks rata kiri

        // atur teks di bagian atas cell
        style.setVerticalAlignment(VerticalAlignment.TOP); // teks nempel di atas

        // tambahkan border di semua sisi
        createBorder(style); // panggil helper buat border

        // kembalikan style yang sudah jadi
        return style; // return style baris
    } // tutup method createRowStyle

    /**
     * Helper kecil buat bikin cell, ngisi nilai, dan pasang style sekaligus.
     *
     * @param row   baris tempat cell ini berada
     * @param col   index kolom tempat cell
     * @param value nilai teks yang mau dimasukin
     * @param style style yang mau dipasang ke cell
     */
    private void createStyledCell(Row row, int col, String value, CellStyle style) { // method helper bikin cell dengan style

        // bikin cell baru di kolom yang diminta
        Cell cell = row.createCell(col); // buat cell di kolom col

        // isi teks cell, kalau null diganti jadi string kosong
        cell.setCellValue(value != null ? value : ""); // set isi cell, antisipasi null

        // pasang style ke cell
        cell.setCellStyle(style); // set style ke cell
    } // tutup method createStyledCell

    /**
     * Helper buat atur border di semua sisi cell style dengan tingkat ketebalan medium.
     *
     * @param style style yang mau ditambahi border
     */
    private void createBorder(CellStyle style) { // method helper buat set border ke style

        // set border bawah jadi medium (sedang)
        style.setBorderBottom(BorderStyle.MEDIUM); // border bawah

        // set border atas jadi medium
        style.setBorderTop(BorderStyle.MEDIUM); // border atas

        // set border kiri jadi medium
        style.setBorderLeft(BorderStyle.MEDIUM); // border kiri

        // set border kanan jadi medium
        style.setBorderRight(BorderStyle.MEDIUM); // border kanan
    } // tutup method createBorder
} // tutup kelas Exporter
