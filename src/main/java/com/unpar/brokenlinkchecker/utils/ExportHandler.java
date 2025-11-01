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

public class ExportHandler {
    // ================================================================
    // ============ EXCEL EXPORT (Apache POI) ==========================
    // ================================================================
    public static void exportToExcel(List<Link> brokenLinks, File file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Broken Links");

            // buat header
            Row header = sheet.createRow(0);
            String[] headers = {"URL", "Final URL", "Content Type", "Error", "Source Webpage", "Anchor Text"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                style.setBorderBottom(BorderStyle.THIN);
                cell.setCellStyle(style);
            }

            // isi data flatten
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

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    // ================================================================
    // ============ JSON EXPORT (Gson) ================================
    // ================================================================
    public static void exportToJson(List<Link> brokenLinks, File file) throws IOException {
        List<Object> exportData = new ArrayList<>();

        for (Link link : brokenLinks) {
            List<Map<String, String>> connections = new ArrayList<>();
            for (Map.Entry<Link, String> entry : link.getConnection().entrySet()) {
                connections.add(Map.of(
                        "webpage_url", entry.getKey().getUrl(),
                        "anchor_text", entry.getValue()
                ));
            }

            exportData.add(Map.of(
                    "url", link.getUrl(),
                    "final_url", link.getFinalUrl(),
                    "content_type", link.getContentType(),
                    "error", link.getError(),
                    "connections", connections
            ));
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(exportData, writer);
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
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }


}
