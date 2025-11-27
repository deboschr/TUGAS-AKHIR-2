package com.unpar.brokenlinkscanner.services;

import com.unpar.brokenlinkscanner.models.Link;
import com.unpar.brokenlinkscanner.models.Summary;
import com.unpar.brokenlinkscanner.utils.HTTPHandler;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Exporter {
    // Daftar nama kolom header
    private static final List<String> HEADERS = List.of("URL", "Final URL", "Content Type", "Error", "Source Webpage", "Anchor Text");

    private CellStyle headerStyle; // Style buat header tabel
    private CellStyle oddRowStyle; // Style buat body tabel baris ganjil
    private CellStyle evenRowStyle; // Style buat body tabel baris genap
    private CellStyle otherStyle; // Style buat cell lain
    private CellStyle emptyStyle; // Style buat cell dummy

    /**
     * Method utama yang dipanggil dari luar buat export data ke Excel.
     *
     * @param data    daftar link yang mau diexport
     * @param summary objek Summary yang mau diexport
     * @param file    file tujuan output (.xlsx) yang mau diisi datanya
     * @throws IOException kalau ada masalah waktu nulis file fisik
     */
    public void save(List<Link> data, Summary summary, File file) throws IOException {
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

            this.headerStyle = createRowStyle(workbook, Color.decode("#2f5d50"), true, true, Color.decode("#f1f0eb"), 16);
            this.oddRowStyle = createRowStyle(workbook, Color.decode("#f4ebdb"), false, false, Color.decode("#222222"), 12);
            this.evenRowStyle = createRowStyle(workbook, Color.decode("#b6c5bf"), false, false , Color.decode("#222222"), 12);
            this.otherStyle = createRowStyle(workbook, Color.decode("#efefef"), true, true, Color.decode("#222222"), 12);
            this.emptyStyle = workbook.createCellStyle();


            // ==== SUMMARY SHEET ====
            Sheet summarySheet = workbook.createSheet("Summary");
            writeSummarySheet(summarySheet, summary, sortedData);

            // ==== RESULT SHEET ====
            Sheet resultSheet = workbook.createSheet("Broken Links");
            writeHeaderRow(resultSheet);
            writeBodyRows(resultSheet, sortedData);


            resultSheet.setColumnWidth(HEADERS.indexOf("URL"), 15000); // atur lebar kolom URL
            resultSheet.setColumnWidth(HEADERS.indexOf("Final URL"), 15000); // atur lebar kolom Final URL
            resultSheet.setColumnWidth(HEADERS.indexOf("Content Type"), 10000); // atur lebar kolom Content Type
            resultSheet.setColumnWidth(HEADERS.indexOf("Error"), 10000); // atur lebar kolom Error
            resultSheet.setColumnWidth(HEADERS.indexOf("Source Webpage"), 15000); // atur lebar kolom Source Webpage
            resultSheet.setColumnWidth(HEADERS.indexOf("Anchor Text"), 10000); // atur lebar kolom Anchor Text
            resultSheet.setColumnWidth(HEADERS.size(), 20000); // atur lebar kolom dummy (kecil saja, cuma buat nahan overflow)

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
     * @param bgColor  warna background buat baris (primary atau white)
     * @return CellStyle yang siap dipasang ke baris
     */
    private CellStyle createRowStyle(Workbook workbook, Color bgColor, Boolean isCenter, Boolean isBold, Color fontColor, int fontSize) {
        // bikin objek style baru buat baris
        CellStyle style = workbook.createCellStyle();

        // set warna background cell sesuai warna yang dikirim
        style.setFillForegroundColor(new XSSFColor(bgColor, null));

        // pakai pattern solid biar warna kelihatan
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // atur teks di tengah kalau header atau di kiri kalau bukan
        style.setAlignment(isCenter ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT);

        // atur teks di tengah kalau header atau di atas kalau bukan
        style.setVerticalAlignment(isCenter ? VerticalAlignment.CENTER : VerticalAlignment.TOP);

        // bikin font khusus untuk header
        XSSFFont textFont = (XSSFFont) workbook.createFont();

        // set tulisan header jadi bold
        textFont.setBold(isBold);

        // set warna teks header jadi putih krem
        textFont.setColor(new XSSFColor(fontColor, null));

        // set ukuran font
        textFont.setFontHeightInPoints((short) fontSize);

        // pasang font yang sudah disiapkan ke style
        style.setFont(textFont);

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


    // ====================== SUMMARY ======================

    // ====================== RESULT ======================
    private void writeSummarySheet(Sheet sheet, Summary summary, List<Link> brokenLinks) {
        int rowIndex = 0;

        // ===== HEADER =====
        Row headerRow = sheet.createRow(rowIndex++);
        headerRow.setHeightInPoints(25);

        Cell h1 = headerRow.createCell(0);
        h1.setCellValue("Process Summary");
        h1.setCellStyle(headerStyle);

        Cell h2 = headerRow.createCell(1);
        h2.setCellValue("");
        h2.setCellStyle(headerStyle);

        sheet.addMergedRegion(new CellRangeAddress(headerRow.getRowNum(), headerRow.getRowNum(), 0, 1));

        sheet.setColumnWidth(0, 7000);
        sheet.setColumnWidth(1, 7000);
        sheet.setColumnWidth(1, 7000);

        // Baris isi process summary
        rowIndex = writeSummaryRow(sheet, rowIndex, "Status", String.valueOf(summary.getStatus()));
        rowIndex = writeSummaryRow(sheet, rowIndex, "All Links", String.valueOf(summary.getTotalLinks()));
        rowIndex = writeSummaryRow(sheet, rowIndex, "Webpage Links", String.valueOf(summary.getWebpages()));
        rowIndex = writeSummaryRow(sheet, rowIndex, "Broken Links", String.valueOf(summary.getBrokenLinks()));

        // Format waktu
        long startMs = summary.getStartTime();
        long endMs = summary.getEndTime();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

        String startStr = startMs > 0 ? fmt.format(Instant.ofEpochMilli(startMs)) : "-";
        String endStr = endMs > 0 ? fmt.format(Instant.ofEpochMilli(endMs)) : "-";

        rowIndex = writeSummaryRow(sheet, rowIndex, "Start Time", startStr);
        rowIndex = writeSummaryRow(sheet, rowIndex, "End Time", endStr);

        // Durasi
        String durationStr = "-";
        if (startMs > 0 && endMs > 0 && endMs >= startMs) {
            Duration d = Duration.ofMillis(endMs - startMs);
            long m = d.toMinutes();
            long s = d.minusMinutes(m).toSeconds();
            durationStr = m + "m " + s + "s";
        }
        rowIndex = writeSummaryRow(sheet, rowIndex, "Duration", durationStr);

        // ==== JARAK 2 BARIS ANTAR TABEL ====
        sheet.createRow(rowIndex++);
        sheet.createRow(rowIndex++);

        // ======= BROKEN LINK SUMMARY (TABEL KEDUA) =======
        writeBrokenLinkSummary(sheet, rowIndex, brokenLinks);
    }

    private int writeSummaryRow(Sheet sheet, int rowIndex, String key, String value) {
        Row row = sheet.createRow(rowIndex);

        CellStyle style = (rowIndex % 2 == 0) ? evenRowStyle : oddRowStyle;

        Cell c1 = row.createCell(0);
        c1.setCellValue(key);
        c1.setCellStyle(style);

        Cell c2 = row.createCell(1);
        c2.setCellValue(value != null ? value : "");
        c2.setCellStyle(style);

        return rowIndex + 1;
    }

    /**
     * Tabel Broken Link Summary
     * Header: merge 3 kolom "Broken Link Summary"
     * Sub-header: Category | Error | Count
     * Isi: dikelompokkan per kategori & error, ditambah Total per kategori.
     */
    private void writeBrokenLinkSummary(Sheet sheet, int startRow, List<Link> brokenLinks) {
        int rowIndex = startRow;

        // ===== HEADER UTAMA (merge 3 kolom) =====
        Row headerRow = sheet.createRow(rowIndex++);
        headerRow.setHeightInPoints(25);

        Cell h1 = headerRow.createCell(0);
        h1.setCellValue("Broken Link Summary");
        h1.setCellStyle(headerStyle);

        Cell h2 = headerRow.createCell(1);
        h2.setCellValue("");
        h2.setCellStyle(headerStyle);

        Cell h3 = headerRow.createCell(2);
        h3.setCellValue("");
        h3.setCellStyle(headerStyle);

        sheet.addMergedRegion(new CellRangeAddress(headerRow.getRowNum(), headerRow.getRowNum(), 0, 2));

        // ===== SUB-HEADER =====
        Row subHeader = sheet.createRow(rowIndex++);
        CellStyle subHeaderStyle = headerStyle; // pakai headerStyle yang sama

        Cell sh1 = subHeader.createCell(0);
        sh1.setCellValue("Category");
        sh1.setCellStyle(subHeaderStyle);

        Cell sh2 = subHeader.createCell(1);
        sh2.setCellValue("Error");
        sh2.setCellStyle(subHeaderStyle);

        Cell sh3 = subHeader.createCell(2);
        sh3.setCellValue("Count");
        sh3.setCellStyle(subHeaderStyle);

        // ===== GROUPING PER KATEGORI & ERROR =====
        // Map per kategori: key = error (String), value = count
        java.util.Map<String, Integer> connMap = new java.util.HashMap<>();
        java.util.Map<String, Integer> client4xxMap = new java.util.HashMap<>();
        java.util.Map<String, Integer> server5xxMap = new java.util.HashMap<>();
        java.util.Map<String, Integer> nonStdMap = new java.util.HashMap<>();

        int connTotal = 0;
        int clientTotal = 0;
        int serverTotal = 0;
        int nonStdTotal = 0;

        for (Link link : brokenLinks) {
            Integer statusCode = link.getStatusCode();
            int code = statusCode != null ? statusCode : 0;
            String error = link.getError() != null ? link.getError() : "Unknown";

            String category;
            java.util.Map<String, Integer> targetMap;

            if (code == 0) {
                category = "Connection Error";
                targetMap = connMap;
                connTotal++;
            } else {
                boolean isStandard = HTTPHandler.isStandardError(code);

                if (isStandard && code >= 400 && code < 500) {
                    category = "4XX Client Error";
                    targetMap = client4xxMap;
                    clientTotal++;
                } else if (isStandard && code >= 500 && code < 600) {
                    category = "5XX Server Error";
                    targetMap = server5xxMap;
                    serverTotal++;
                } else {
                    category = "Non-Standard Error";
                    targetMap = nonStdMap;
                    nonStdTotal++;
                }
            }

            targetMap.merge(error, 1, Integer::sum);
        }

        // tulis kategori secara berurutan
        rowIndex = writeBrokenLinkCategoryBlock(sheet, rowIndex, "Connection Error", connMap, connTotal);
        rowIndex = writeBrokenLinkCategoryBlock(sheet, rowIndex, "4XX Client Error", client4xxMap, clientTotal);
        rowIndex = writeBrokenLinkCategoryBlock(sheet, rowIndex, "5XX Server Error", server5xxMap, serverTotal);
        rowIndex = writeBrokenLinkCategoryBlock(sheet, rowIndex, "Non-Standard Error", nonStdMap, nonStdTotal);
    }

    /**
     * Menulis blok baris untuk satu kategori:
     * - Beberapa baris error
     * - Category di-merge vertikal di kolom 0
     * - Baris Total di bawahnya (merge kolom 0–1)
     */
    private int writeBrokenLinkCategoryBlock(Sheet sheet, int rowIndex, String categoryName, java.util.Map<String, Integer> errorMap, int totalCount) {
        if (totalCount == 0 || errorMap.isEmpty()) {
            return rowIndex;
        }

        // Sort error berdasarkan nama error biar rapi
        java.util.List<String> errors = new java.util.ArrayList<>(errorMap.keySet());
        errors.sort(String::compareTo);

        int blockStartRow = rowIndex;

        for (String error : errors) {
            Row row = sheet.createRow(rowIndex);
            CellStyle style = (rowIndex % 2 == 0) ? evenRowStyle : oddRowStyle;

            // Category hanya muncul di baris pertama kategori
            if (rowIndex == blockStartRow) {
                createTableCell(row, 0, categoryName, style);
            } else {
                createTableCell(row, 0, "", style);
            }

            CellStyle countStyle = sheet.getWorkbook().createCellStyle();
            countStyle.cloneStyleFrom(style);
            countStyle.setAlignment(HorizontalAlignment.CENTER);

            createTableCell(row, 1, error, style);
            createTableCell(row, 2, String.valueOf(errorMap.get(error)), countStyle);

            rowIndex++;
        }

        int blockEndRow = rowIndex - 1;

        // Merge category secara vertikal di kolom 0
        if (blockEndRow > blockStartRow) {
            sheet.addMergedRegion(new CellRangeAddress(blockStartRow, blockEndRow, 0, 0));
        }

        // Baris Total
        Row totalRow = sheet.createRow(rowIndex);

        createTableCell(totalRow, 0, "Total " + categoryName, otherStyle);
        createTableCell(totalRow, 1, "", otherStyle);
        createTableCell(totalRow, 2, String.valueOf(totalCount), otherStyle);

        // Merge kolom 0–1 untuk tulisan "Total"
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 1));

        return rowIndex + 1;
    }
}
