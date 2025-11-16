package com.unpar.brokenlinkchecker.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.unpar.brokenlinkchecker.models.Link;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Exporter {
    // ================================================================
    // ============ EXCEL EXPORT (Apache POI) =========================
    // ================================================================
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

    // ================================================================
    // ============ JSON EXPORT (Gson) ================================
    // ================================================================
    /**
     * Mengekspor data broken link ke file JSON menggunakan pustaka Gson.
     * <p>
     * Format JSON yang dihasilkan bersifat terstruktur (nested),
     * di mana setiap link berisi daftar halaman sumber (connections)
     * yang memuat tautan tersebut.
     *
     * Contoh struktur output:
     *
     * <pre>
     * [
     *   {
     *     "url": "https://example.com/image.png",
     *     "final_url": "https://example.com/image.png",
     *     "content_type": "image/png",
     *     "error": "404 Not Found",
     *     "connections": [
     *       { "webpage_url": "https://example.com/home", "anchor_text": "logo" },
     *       { "webpage_url": "https://example.com/about", "anchor_text": "company logo" }
     *     ]
     *   }
     * ]
     * </pre>
     *
     * @param brokenLinks daftar objek Link yang mengandung error
     * @param file        file tujuan untuk menyimpan hasil ekspor (.json)
     * @throws IOException jika terjadi kesalahan saat menulis file
     */
    public static void exportToJson(List<Link> brokenLinks, File file) throws IOException {
        /*
         * Buat list kosong untuk menampung seluruh data ekspor.
         * Tipe List<Object> digunakan karena setiap elemen nanti akan berupa Map
         * yang mewakili satu link rusak beserta daftar connections-nya.
         */
        List<Object> exportData = new ArrayList<>();

        /*
         * Loop semua link rusak yang akan diekspor.
         * Setiap Link akan dikonversi menjadi satu Map<String, Object>
         * dengan struktur key-value yang mudah dikonversi oleh Gson.
         */
        for (Link link : brokenLinks) {

            /*
             * Buat list untuk menyimpan semua connections dari link ini.
             * Setiap connection berisi dua informasi:
             * - webpage_url → alamat halaman sumber
             * - anchor_text → teks tautan di halaman tersebut
             */
            List<Map<String, String>> connections = new ArrayList<>();

            // Loop semua pasangan (webpage → anchor_text) dari link saat ini
            for (Map.Entry<Link, String> entry : link.getConnection().entrySet()) {

                /*
                 * Gunakan Map.of() (immutable map) untuk membuat satu objek JSON sederhana
                 * yang mewakili satu connection.
                 * Contoh hasil sementara:
                 * { "webpage_url": "https://example.com/home", "anchor_text": "logo" }
                 */
                connections.add(Map.of(
                        "webpage_url", entry.getKey().getUrl(),
                        "anchor_text", entry.getValue()));
            }

            /*
             * Tambahkan data utama link ke list exportData.
             * Tiap link disimpan dalam bentuk Map dengan key:
             * - url
             * - final_url
             * - content_type
             * - error
             * - connections (list berisi Map yang baru dibuat di atas)
             *
             * Gson akan otomatis mengubah Map dan List ini menjadi objek JSON bersarang.
             */
            exportData.add(Map.of(
                    "url", link.getUrl(),
                    "final_url", link.getFinalUrl(),
                    "content_type", link.getContentType(),
                    "error", link.getError(),
                    "connections", connections));
        }

        /*
         * Buat instance Gson dengan opsi pretty printing.
         * Pretty printing membuat JSON lebih mudah dibaca manusia
         * (tiap level indentasi diberi spasi dan baris baru).
         */
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        /*
         * Tulis hasil konversi ke file tujuan menggunakan try-with-resources.
         * FileWriter otomatis menutup file setelah selesai menulis.
         */
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(exportData, writer); // konversi dan tulis JSON ke file
        }
    }

    // ================================================================
    // ============ CSV EXPORT ========================================
    // ================================================================
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
