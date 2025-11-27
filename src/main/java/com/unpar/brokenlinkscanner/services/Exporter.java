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
import java.util.*;

public class Exporter {
    private CellStyle headerStyle;
    private CellStyle oddRowStyle;
    private CellStyle evenRowStyle;
    private CellStyle otherStyle;
    private CellStyle emptyStyle;

    public void save(List<Link> data, Summary summary, File file) throws IOException {

        List<Link> brokenLinkData = new ArrayList<>(data);

        brokenLinkData.sort(Comparator.comparingInt(a -> a.getConnection().size()));

        try (Workbook workbook = new XSSFWorkbook()) {

            this.headerStyle = createRowStyle(workbook, Color.decode("#2f5d50"), true, true, Color.decode("#f1f0eb"), 16);
            this.oddRowStyle = createRowStyle(workbook, Color.decode("#f4ebdb"), false, false, Color.decode("#222222"), 12);
            this.evenRowStyle = createRowStyle(workbook, Color.decode("#b6c5bf"), false, false, Color.decode("#222222"), 12);
            this.otherStyle = createRowStyle(workbook, Color.decode("#efefef"), true, true, Color.decode("#222222"), 12);
            this.emptyStyle = workbook.createCellStyle();

            Sheet summarySheet = workbook.createSheet("Summary");
            writeProcessSummaryTable(summarySheet, summary);
            writeBrokenLinkSummaryTable(summarySheet, brokenLinkData);

            Sheet brokenLinkSheet = workbook.createSheet("Broken Links");
            writeBrokenLinkTable(brokenLinkSheet, brokenLinkData);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    private void writeProcessSummaryTable(Sheet sheet, Summary summary) {
        int rowIndex = 0;

        // ================= HEADER TABLE =================
        Row headerRow = sheet.createRow(rowIndex++);
        headerRow.setHeightInPoints(25);

        Cell h1 = headerRow.createCell(0);
        h1.setCellValue("Process Summary");
        h1.setCellStyle(headerStyle);

        Cell h2 = headerRow.createCell(1);
        h2.setCellValue("");
        h2.setCellStyle(headerStyle);

        sheet.addMergedRegion(new CellRangeAddress(headerRow.getRowNum(), headerRow.getRowNum(), 0, 1));

        // ================= BODY TABLE =================
        long startTimeMs = summary.getStartTime();
        long endTimeMs = summary.getEndTime();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

        String startTimeStr = startTimeMs > 0 ? fmt.format(Instant.ofEpochMilli(startTimeMs)) : "-";
        String endTimeStr = endTimeMs > 0 ? fmt.format(Instant.ofEpochMilli(endTimeMs)) : "-";

        String durationStr = "-";
        if (startTimeMs > 0 && endTimeMs > 0 && endTimeMs >= startTimeMs) {
            Duration d = Duration.ofMillis(endTimeMs - startTimeMs);
            long m = d.toMinutes();
            long s = d.minusMinutes(m).toSeconds();
            durationStr = m + "m " + s + "s";
        }

        Map<String, String> summaryMap = new LinkedHashMap<>(Map.of(
                "Status",        String.valueOf(summary.getStatus()),
                "All Links",     String.valueOf(summary.getTotalLinks()),
                "Webpage Links", String.valueOf(summary.getWebpages()),
                "Broken Links",  String.valueOf(summary.getBrokenLinks()),
                "Start Time",    startTimeStr,
                "End Time",      endTimeStr,
                "Duration",      durationStr
        ));

        for (var entry : summaryMap.entrySet()) {
            Row row = sheet.createRow(rowIndex++);

            CellStyle style = (rowIndex % 2 == 0) ? evenRowStyle : oddRowStyle;

            createTableCell(row, 0, entry.getKey(), style);
            createTableCell(row, 1, entry.getValue(), style);
        }

        // ================= UKURAN KOLOM =================
        sheet.setColumnWidth(0, 7000);
        sheet.setColumnWidth(1, 7000);
        sheet.setColumnWidth(1, 7000);
    }

    private void writeBrokenLinkSummaryTable(Sheet sheet, List<Link> data) {
        int rowIndex = 10;

        // ================= HEADER =================
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

        // ================= SUBHEADER =================
        Row subHeader = sheet.createRow(rowIndex++);
        createTableCell(subHeader, 0, "Category", headerStyle);
        createTableCell(subHeader, 1, "Error", headerStyle);
        createTableCell(subHeader, 2, "Count", headerStyle);

        // ================= PREPARE GROUPS =================
        Map<String, Integer> connectionErrorMap = new HashMap<>();
        Map<String, Integer> clientErrorMap = new HashMap<>();
        Map<String, Integer> serverErrorMap = new HashMap<>();
        Map<String, Integer> nonStandardErrorMap = new HashMap<>();

        int connectionErrorTotal = 0;
        int clientErrorTotal = 0;
        int serverErrorTotal = 0;
        int nonStandardErrorTotal = 0;

        for (Link link : data) {
            int code = link.getStatusCode();
            String err = link.getError();
            boolean isStandard = HTTPHandler.isStandardError(code);

            if (code == 0) {
                connectionErrorMap.merge(err, 1, Integer::sum);
                connectionErrorTotal++;
            } else if (isStandard && code >= 400 && code < 500) {
                clientErrorMap.merge(err, 1, Integer::sum);
                clientErrorTotal++;
            } else if (isStandard && code >= 500 && code < 600) {
                serverErrorMap.merge(err, 1, Integer::sum);
                serverErrorTotal++;
            } else {
                nonStandardErrorMap.merge(err, 1, Integer::sum);
                nonStandardErrorTotal++;
            }
        }

        // ================= GROUP CATEGORY LIST =================
        List<Map<String, ?>> groups = List.of(
                Map.of("name", "Connection Error",   "map", connectionErrorMap,   "total", connectionErrorTotal),
                Map.of("name", "4XX Client Error",   "map", clientErrorMap,       "total", clientErrorTotal),
                Map.of("name", "5XX Server Error",   "map", serverErrorMap,       "total", serverErrorTotal),
                Map.of("name", "Non-Standard Error", "map", nonStandardErrorMap,  "total", nonStandardErrorTotal)
        );

        // ================= WRITE ALL WITH ONE LOOP =================
        for (var group : groups) {

            String categoryName = (String) group.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Integer> map = (Map<String, Integer>) group.get("map");
            int total = (int) group.get("total");

            if (total == 0 || map.isEmpty())
                continue;

            // sorted errors
            List<String> errors = new ArrayList<>(map.keySet());
            errors.sort(String::compareTo);

            int startRow = rowIndex;

            for (String err : errors) {
                Row row = sheet.createRow(rowIndex);

                CellStyle style = (rowIndex % 2 == 0) ? evenRowStyle : oddRowStyle;

                // category (only first row)
                createTableCell(row, 0, (rowIndex == startRow ? categoryName : ""), style);
                createTableCell(row, 1, err, style);

                CellStyle center = sheet.getWorkbook().createCellStyle();
                center.cloneStyleFrom(style);
                center.setAlignment(HorizontalAlignment.CENTER);

                createTableCell(row, 2, String.valueOf(map.get(err)), center);

                rowIndex++;
            }

            int endRow = rowIndex - 1;

            if (endRow > startRow) {
                sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 0, 0));
            }

            Row totalRow = sheet.createRow(rowIndex);
            createTableCell(totalRow, 0, "Total " + categoryName, otherStyle);
            createTableCell(totalRow, 1, "", otherStyle);
            createTableCell(totalRow, 2, String.valueOf(total), otherStyle);

            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 1));
            rowIndex++;
        }
    }

    private void writeBrokenLinkTable(Sheet sheet, List<Link> data) {

        List<String> columnList = List.of("URL", "Final URL", "Content Type", "Error", "Source Webpage", "Anchor Text");

        // ================= HEADER TABLE =================

        Row headerRow = sheet.createRow(0);

        headerRow.setHeightInPoints(25);

        for (int i = 0; i < columnList.size(); i++) {
            createTableCell(headerRow, i, columnList.get(i), headerStyle);
        }

        Cell dummyCellHeader = headerRow.createCell(columnList.size());
        dummyCellHeader.setCellValue("");
        dummyCellHeader.setCellStyle(emptyStyle);

        // ================= BODY TABLE =================

        int rowIndex = 1;

        int groupIndex = 1;

        for (Link link : data) {

            int startRow = rowIndex;

            boolean isFirst = true;

            CellStyle groupStyle = (groupIndex % 2 == 0) ? evenRowStyle : oddRowStyle;

            for (Map.Entry<Link, String> entry : link.getConnection().entrySet()) {

                Row row = sheet.createRow(rowIndex);

                if (isFirst) {
                    createTableCell(row, columnList.indexOf("URL"), link.getUrl(), groupStyle);
                    createTableCell(row, columnList.indexOf("Final URL"), link.getFinalUrl(), groupStyle);
                    createTableCell(row, columnList.indexOf("Content Type"), link.getContentType(), groupStyle);
                    createTableCell(row, columnList.indexOf("Error"), link.getError(), groupStyle);

                    isFirst = false;
                } else {
                    createTableCell(row, columnList.indexOf("URL"), "", groupStyle);
                    createTableCell(row, columnList.indexOf("Final URL"), "", groupStyle);
                    createTableCell(row, columnList.indexOf("Content Type"), "", groupStyle);
                    createTableCell(row, columnList.indexOf("Error"), "", groupStyle);
                }

                createTableCell(row, columnList.indexOf("Source Webpage"), entry.getKey().getUrl(), groupStyle);

                createTableCell(row, columnList.indexOf("Anchor Text"), entry.getValue(), groupStyle);

                Cell dummyCellBody = row.createCell(columnList.size());

                dummyCellBody.setCellValue("");

                dummyCellBody.setCellStyle(emptyStyle);

                rowIndex++;
            }

            int endRow = rowIndex - 1;

            if (endRow > startRow) {

                for (int col = columnList.indexOf("URL"); col <= columnList.indexOf("Error"); col++) {

                    sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, col, col));
                }
            }

            groupIndex++;
        }

        // ================= UKURAN KOLOM =================
        sheet.setColumnWidth(columnList.indexOf("URL"), 15000);
        sheet.setColumnWidth(columnList.indexOf("Final URL"), 15000);
        sheet.setColumnWidth(columnList.indexOf("Content Type"), 10000);
        sheet.setColumnWidth(columnList.indexOf("Error"), 10000);
        sheet.setColumnWidth(columnList.indexOf("Source Webpage"), 15000);
        sheet.setColumnWidth(columnList.indexOf("Anchor Text"), 10000);
        sheet.setColumnWidth(columnList.size(), 20000);
    }

    private CellStyle createRowStyle(Workbook workbook, Color bgColor, Boolean isCenter, Boolean isBold, Color fontColor, int fontSize) {

        CellStyle style = workbook.createCellStyle();

        style.setFillForegroundColor(new XSSFColor(bgColor, null));

        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setAlignment(isCenter ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT);

        style.setVerticalAlignment(isCenter ? VerticalAlignment.CENTER : VerticalAlignment.TOP);

        XSSFFont textFont = (XSSFFont) workbook.createFont();

        textFont.setBold(isBold);

        textFont.setColor(new XSSFColor(fontColor, null));

        textFont.setFontHeightInPoints((short) fontSize);

        style.setFont(textFont);

        createBorder(style);

        return style;
    }

    private void createTableCell(Row row, int col, String value, CellStyle style) {

        Cell cell = row.createCell(col);

        cell.setCellValue(value != null ? value : "");

        cell.setCellStyle(style);
    }

    private void createBorder(CellStyle style) {
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.MEDIUM);
        style.setBorderRight(BorderStyle.MEDIUM);
    }

}
