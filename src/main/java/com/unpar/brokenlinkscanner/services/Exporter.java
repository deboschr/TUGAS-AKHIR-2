package com.unpar.brokenlinkscanner.services;

import com.unpar.brokenlinkscanner.models.Link;
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

public class Exporter {
    // Nama sheet yang dipakai di workbook Excel
    private static final String SHEET_NAME = "Broken Links";

    // Daftar nama kolom header
    private static final List<String> HEADERS = List.of(
            "URL",
            "Final URL",
            "Content Type",
            "Error",
            "Source Webpage",
            "Anchor Text"
    );

    private CellStyle headerStyle; // Style khusus header tabel
    private CellStyle oddRowStyle; // Style buat body tabel baris ganjil
    private CellStyle evenRowStyle; // Style buat body tabel baris genap
    private CellStyle emptyStyle; // Style buat kolom dummy


    /**
     * Method utama yang dipanggil dari luar buat export data ke Excel.
     *
     * @param data daftar link yang mau diexport
     * @param file file tujuan output (.xlsx) yang mau diisi datanya
     * @throws IOException kalau ada masalah waktu nulis file fisik
     */
    public void save(List<Link> data, File file) throws IOException {
        /**
         * Bikin list baru dari list asli karena nanti bakal di sort biar list asli ga berubah. List asli dipake di GUI, kalau urutannya berubah maka urutan di tabel GUI juga bakal berubah.
         */
        List<Link> sortedData = new ArrayList<>(data);

        /**
         * Urutin list berdasarkan ukuran connection dari kecil ke besar. Jadi disini kita mau mengurutkan list broken links dari kecil ke besar berdasarkan jumlah kemunculannya di website yang lagi diperiksa, jadi semakin banyak sebuah broken link ditemukan di berbagai halaman maka makin besar ukuran connectionsnya.
         */
        sortedData.sort(Comparator.comparingInt(a -> a.getConnection().size()));


        /**
         * Bikin objek workbook baru pake kelas XSSFWorkbook biar format Excel yang dihasilkan adalah .xlsx.
         * Pake try-with-resources biar workbook lansung ketutup kalau udah selesai.
         */
        try (Workbook workbook = new XSSFWorkbook()) {
            // bikin style header tabel
            this.headerStyle = createRowStyle(workbook, Color.decode("#2f5d50"), true);
            // bikin style body tabel baris ganjil
            this.oddRowStyle = createRowStyle(workbook, Color.decode("#f4ebdb"), false);
            // bikin style body tabel baris genap
            this.evenRowStyle = createRowStyle(workbook, Color.decode("#b6c5bf"), false);
            // bikin style buat baris kosong (tanpa border dan warna)
            this.emptyStyle = workbook.createCellStyle();

            // bikin sheet baru pake nama yang sudah didefinisikan
            Sheet sheet = workbook.createSheet(SHEET_NAME);

            // tulis isi header tabel
            writeHeaderRow(sheet);

            // tulis isi body tabel
            writeBodyRows(sheet, sortedData);

            sheet.setColumnWidth(HEADERS.indexOf("URL"), 15000); // atur lebar kolom URL
            sheet.setColumnWidth(HEADERS.indexOf("Final URL"), 15000); // atur lebar kolom Final URL
            sheet.setColumnWidth(HEADERS.indexOf("Content Type"), 10000); // atur lebar kolom Content Type
            sheet.setColumnWidth(HEADERS.indexOf("Error"), 10000); // atur lebar kolom Error
            sheet.setColumnWidth(HEADERS.indexOf("Source Webpage"), 15000); // atur lebar kolom Source Webpage
            sheet.setColumnWidth(HEADERS.indexOf("Anchor Text"), 10000); // atur lebar kolom Anchor Text
            sheet.setColumnWidth(HEADERS.size(), 20000); // atur lebar kolom dummy (kecil saja, cuma buat nahan overflow)

            /**
             * Bikin FileOutputStream ke file tujuan.
             * Pake try-with-resources biar stream-nya lansung di tutup kalau udah selesai.
             */
            try (FileOutputStream fos = new FileOutputStream(file)) {
                // tulis isi workbook ke stream terus stream ke file
                workbook.write(fos);
            }
        }
    }

    /**
     * Method buat nambahin style ke baris isi tabel.
     *
     * @param workbook workbook tempat style ini akan dipakai
     * @param bgColor warna background buat baris (primary atau white)
     * @return CellStyle yang siap dipasang ke baris
     */
    private CellStyle createRowStyle(Workbook workbook, Color bgColor, Boolean isHeader) {
        // bikin objek style baru buat baris
        CellStyle style = workbook.createCellStyle();

        // set warna background cell sesuai warna yang dikirim
        style.setFillForegroundColor(new XSSFColor(bgColor, null));

        // pakai pattern solid biar warna kelihatan
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // atur teks di tengah kalau header atau di kiri kalau bukan
        style.setAlignment(isHeader ? HorizontalAlignment.CENTER: HorizontalAlignment.LEFT);

        // atur teks di tengah kalau header atau di atas kalau bukan
        style.setVerticalAlignment(isHeader ? VerticalAlignment.CENTER : VerticalAlignment.TOP);

        if (isHeader) {
            // bikin font khusus untuk header
            XSSFFont textFont = (XSSFFont) workbook.createFont();

            // set tulisan header jadi bold
            textFont.setBold(true);

            // set warna teks header jadi putih krem
            textFont.setColor(new XSSFColor(Color.decode("#f1f0eb"), null));

            // pasang font yang sudah disiapkan ke style
            style.setFont(textFont);
        }

        // tambahkan border di semua sisi
        createBorder(style);

        return style;
    }

    /**
     * Method buat nulis header tabel
     *
     * @param sheet sheet Excel yang lagi dipakai
     */
    private void writeHeaderRow(Sheet sheet) {
        // bikin baris baru di index 0 (baris pertama)
        Row headerRow = sheet.createRow(0);

        // atur tinggi baris biar sedikit lebih besar dari baris biasa (dalam satuan point)
        headerRow.setHeightInPoints(25);

        // loop semua item di array HEADERS buat diisi ke kolom-kolom
        for (int i = 0; i < HEADERS.size(); i++) {
            // bikin cell untuk kolom ke-i di baris header
            Cell cell = headerRow.createCell(i);

            // set teks header sesuai value di array HEADERS
            cell.setCellValue(HEADERS.get(i));

            // pasang style header ke cell ini
            cell.setCellStyle(headerStyle);
        }

        // bikin satu cell di kolom paling kanan (kolom dummy)
        Cell dummyHeader = headerRow.createCell(HEADERS.size());

        // isi dummy dengan string kosong biar ga kelihatan apa-apa tapi Excel anggap cell-nya ada
        dummyHeader.setCellValue("");

        // style dummy pakai emptyStyle (tanpa border, tanpa background)
        dummyHeader.setCellStyle(emptyStyle);
    }

    /**
     * Method buat nulis isi tabel
     *
     * @param sheet sheet Excel tujuan penulisan
     * @param data  data yang mau dimasukan ke tabel
     */
    private void writeBodyRows(Sheet sheet, List<Link> data) {
        // mulai dari baris index 1 karena 0 sudah dipakai buat header
        int rowIndex = 1;

        // index baris untuk kolom URL, Final URL, Content Type dan Error (kolom yang di merge)
        int groupIndex = 1;

        // Loop semua data
        for (Link link : data) {
            // catat baris awal untuk link ini, nanti dipakai buat merge
            int startRow = rowIndex;

            // Buat nandain baris pertama dari sekelompok source webpage untuk link ini
            boolean isFirst = true; // true kalau lagi di baris pertama dari group link ini

            // cek apakah baris ini genap atau ganjil buat nentuin warna baris.
            CellStyle groupStyle = (groupIndex % 2 == 0) ? evenRowStyle : oddRowStyle;

            // loop semua pasangan (webpageLink, anchorText) dari connection link ini
            for (Map.Entry<Link, String> entry : link.getConnection().entrySet()) {
                // bikin baris baru di sheet untuk data ini
                Row row = sheet.createRow(rowIndex);

                /**
                 * kalau ini baris pertama untuk link ini, maka kolom index ke 0-3 (URL, Final URL, Content Type, Error) kita isi dengan data dari objek link
                 */
                if (isFirst) {
                    createTableCell(row, HEADERS.indexOf("URL"), link.getUrl(), groupStyle); // isi kolom URL
                    createTableCell(row, HEADERS.indexOf("Final URL"), link.getFinalUrl(), groupStyle); // isi kolom Final URL
                    createTableCell(row, HEADERS.indexOf("Content Type"), link.getContentType(), groupStyle); // isi kolom Content Type
                    createTableCell(row, HEADERS.indexOf("Error"), link.getError(), groupStyle); // isi kolom Error

                    // set flag jadi false karena baris pertama sudah lewat
                    isFirst = false;
                }
                /**
                 * kalau bukan baris pertama untuk link ini, maka kolom index ke 0-3 (URL, Final URL, Content Type, Error) kita isi string kosong biar keliatan merge-nya nanti
                 */
                else {
                    createTableCell(row, HEADERS.indexOf("URL"), "", groupStyle); // kosongkan URL
                    createTableCell(row, HEADERS.indexOf("Final URL"), "", groupStyle); // kosongkan Final URL
                    createTableCell(row, HEADERS.indexOf("Content Type"), "", groupStyle); // kosongkan Content Type
                    createTableCell(row, HEADERS.indexOf("Error"), "", groupStyle); // kosongkan Error
                }

                // isi kolom Source Webpage dengan key dari entry (ambil URL dari Webpage Link)
                createTableCell(row, HEADERS.indexOf("Source Webpage"), entry.getKey().getUrl(), groupStyle);

                // isi kolom Anchor Text dengan value dari entry
                createTableCell(row, HEADERS.indexOf("Anchor Text"), entry.getValue(), groupStyle);

                // buat cell dummy di kolom paling kanan buat ngeblok overflow teks anchor
                Cell blocker = row.createCell(HEADERS.size());

                // isi dummy dengan string kosong supaya dianggap ada tapi ga kelihatan
                blocker.setCellValue("");

                // pakai style emptyStyle biar ga muncul border atau warna
                blocker.setCellStyle(emptyStyle);

                rowIndex++;
            }

            // hitung baris terakhir yang dipakai untuk link ini
            int endRow = rowIndex - 1; // rowIndex sudah maju satu, jadi row terakhir = rowIndex - 1

            // kalau ada lebih dari satu baris untuk link ini, baru merge kolom 0–3
            if (endRow > startRow) {
                // merge baris hanya untuk kolom indeks 0 sampai 3 (kolom URL, Final URL, Content Type dan Error)
                for (int col = HEADERS.indexOf("URL"); col <= HEADERS.indexOf("Error"); col++) {
                    // merge secara vertikal (kolomnya sama)
                    sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, col, col));
                }
            }

            groupIndex++;
        }
    }

    /**
     * Method buat bikin cell didalam sebuah kolom.
     *
     * @param row   baris tempat cell ini berada
     * @param col   index kolom tempat cell
     * @param value nilai teks yang mau dimasukin
     * @param style style yang mau dipasang ke cell
     */
    private void createTableCell(Row row, int col, String value, CellStyle style) {
        // bikin cell baru di kolom yang diminta
        Cell cell = row.createCell(col);

        // isi teks cell, kalau null diganti jadi string kosong
        cell.setCellValue(value != null ? value : "");

        // pasang style ke cell
        cell.setCellStyle(style);
    }

    /**
     * Method buat bikin cell jadi punya border di kiri kanan atas bawah dengan
     * ketebalan medium.
     *
     * @param style style yang mau ditambahin border
     */
    private void createBorder(CellStyle style) {
        style.setBorderBottom(BorderStyle.MEDIUM); // border bawah
        style.setBorderTop(BorderStyle.MEDIUM); // border atas
        style.setBorderLeft(BorderStyle.MEDIUM); // border kiri
        style.setBorderRight(BorderStyle.MEDIUM); // border kanan
    }
}
