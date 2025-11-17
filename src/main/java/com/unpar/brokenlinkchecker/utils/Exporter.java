package com.unpar.brokenlinkchecker.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.unpar.brokenlinkchecker.models.Link;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Exporter {
    /**
     * Mengekspor data broken links ke file Excel (.xlsx) menggunakan Apache POI.
     *
     * @param brokenLinks
     * @param file
     * @throws IOException
     */
    public static void exportToExcel(List<Link> brokenLinks, File file) throws IOException {
        /*
         * Pake try-with-resources biar workbook otomatis ditutup
         * setelah proses selesai, meskipun terjadi exception.
         *
         * XSSFWorkbook adalah implementasi Workbook untuk format .xlsx.
         */
        try (Workbook workbook = new XSSFWorkbook()) {
            // Buat satu sheet bernama Broken Links di dalam workbook
            Sheet sheet = workbook.createSheet("Broken Links");

            // =================== HEADER ===================
            // Baris pertama (indeks 0) dipake buat header tabel
            Row header = sheet.createRow(0);
            // Daftar nama kolom
            String[] headers = { "URL", "Final URL", "Content Type", "Error", "Source Webpage", "Anchor Text" };
            for (int i = 0; i < headers.length; i++) {
                // Buat cell baru di kolom ke-i
                Cell cell = header.createCell(i);

                // Isi teks header sesuai array
                cell.setCellValue(headers[i]);

                // Buat style baru untuk cell header
                CellStyle style = workbook.createCellStyle();

                // Buat font baru dan ubah jadi bold
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);

                // Tambahkan border bawah
                style.setBorderBottom(BorderStyle.THIN);

                // Terapkan style ke cell header
                cell.setCellStyle(style);
            }

            // =================== DATA TABEL ===================
            // mulai dari baris kedua (setelah header)
            int rowIdx = 1;

            for (Link link : brokenLinks) {
                for (Map.Entry<Link, String> entry : link.getConnection().entrySet()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(link.getUrl());
                    row.createCell(1).setCellValue(link.getFinalUrl());
                    row.createCell(2).setCellValue(link.getContentType());
                    row.createCell(3).setCellValue(link.getError());
                    row.createCell(4).setCellValue(entry.getKey().getUrl());
                    row.createCell(5).setCellValue(entry.getValue());
                }
            }

            // auto-fit kolom
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            /*
             * Gunakan try-with-resources kedua untuk FileOutputStream.
             * Ini memastikan stream tertutup dengan aman setelah penulisan selesai.
             */
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos); // tulis seluruh workbook ke file .xlsx
            }
        }
    }


    /**
     * Mengekspor daftar broken link ke file JSON menggunakan Gson.
     * Setiap objek Link akan dikonversi menjadi struktur Map yang urut
     * supaya hasil JSON juga urut dan rapi.
     *
     * @param brokenLinks daftar objek Link yang mengandung error
     * @param file        file tujuan penyimpanan
     * @throws IOException jika ada masalah saat menulis file
     */
    public static void exportToJson(List<Link> brokenLinks, File file) throws IOException {

        // List untuk menampung semua item yang akan ditulis ke JSON
        List<Object> exportData = new ArrayList<>();

        // Loop tiap link rusak satu per satu
        for (Link link : brokenLinks) {

            // -----------------------------
            // Buat list untuk semua koneksi
            // -----------------------------
            List<Map<String, String>> connections = new ArrayList<>();

            /*
             * Loop semua pasangan (webpage → anchor_text)
             * dari link.getConnection()
             */
            for (Map.Entry<Link, String> entry : link.getConnection().entrySet()) {

                // Gunakan LinkedHashMap supaya urutan key tetap
                Map<String, String> conn = new LinkedHashMap<>();

                // URL halaman sumber
                conn.put("webpage_url", entry.getKey().getUrl());

                // Anchor text pada halaman tersebut
                conn.put("anchor_text", entry.getValue());

                // Tambahkan ke daftar connections
                connections.add(conn);
            }

            // --------------------------------------------
            // Buat map utama untuk satu broken link
            // (urutan key sangat diperhatikan)
            // --------------------------------------------
            Map<String, Object> item = new LinkedHashMap<>();

            // URL asli yang ditemukan saat crawling
            item.put("url", link.getUrl());

            // URL final setelah redirect (kalau ada)
            item.put("final_url", link.getFinalUrl());

            // Content-Type hasil HEAD/GET
            item.put("content_type", link.getContentType());

            // Error (misalnya "404 Not Found")
            item.put("error", link.getError());

            // Daftar halaman sumber
            item.put("connections", connections);

            // Masukkan item ini ke list utama
            exportData.add(item);
        }

        // Buat Gson dengan indentasi supaya JSON mudah dibaca
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping() // biar tidak escape simbol HTML
                .create();

        // Tulis hasil ke file (try-with-resources biar otomatis close)
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(exportData, writer);
        }
    }


    /**
     * 
     * @param brokenLinks
     * @param file
     * @throws IOException
     */
    public static void exportToCsv(List<Link> brokenLinks, File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("URL,Final URL,Content Type,Error,Source Webpage,Anchor Text");

            for (Link link : brokenLinks) {
                for (Map.Entry<Link, String> entry : link.getConnection().entrySet()) {
                    String[] values = {
                            escapeCsv(link.getUrl()),
                            escapeCsv(link.getFinalUrl()),
                            escapeCsv(link.getContentType()),
                            escapeCsv(link.getError()),
                            escapeCsv(entry.getKey().getUrl()),
                            escapeCsv(entry.getValue())
                    };
                    writer.println(String.join(",", values));
                }
            }
        }
    }

    /**
     * 
     * @param value
     * @return
     */
    private static String escapeCsv(String value) {
        if (value == null){
            return "";
        }

        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
