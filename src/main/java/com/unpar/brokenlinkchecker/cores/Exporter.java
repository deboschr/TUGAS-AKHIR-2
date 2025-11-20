package com.unpar.brokenlinkchecker.cores;

import com.unpar.brokenlinkchecker.models.Link;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Exporter {

    public void save(List<Link> brokenLinks, File file) throws IOException {

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Broken Links");

            // ============================================================
            //         FIX: Make a modifiable list for sorting
            // ============================================================
            List<Link> sortedBrokenLinks = new ArrayList<>(brokenLinks);

            sortedBrokenLinks.sort((a, b) ->
                    Integer.compare(a.getConnection().size(), b.getConnection().size())
            );

            // ============================================================
            //                       THEME COLORS
            // ============================================================
            Color primary = Color.decode("#f4ebdb");
            Color white   = Color.decode("#f1f0eb");
            Color headerBg = Color.decode("#2f5d50");

            // ============================================================
            //                       STYLES
            // ============================================================
            CellStyle headerStyle = createHeaderStyle(workbook, headerBg);

            CellStyle oddRowStyle = createRowStyle(workbook, primary);
            CellStyle evenRowStyle = createRowStyle(workbook, white);

            // Anchor text wrap + zebra + no overflow
            CellStyle wrapOddStyle = workbook.createCellStyle();
            wrapOddStyle.setWrapText(true);
            wrapOddStyle.setFillForegroundColor(new XSSFColor(primary, null));
            wrapOddStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            wrapOddStyle.setVerticalAlignment(VerticalAlignment.TOP);
            wrapOddStyle.setAlignment(HorizontalAlignment.LEFT);
            createBorder(wrapOddStyle);

            CellStyle wrapEvenStyle = workbook.createCellStyle();
            wrapEvenStyle.setWrapText(true);
            wrapEvenStyle.setFillForegroundColor(new XSSFColor(white, null));
            wrapEvenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            wrapEvenStyle.setVerticalAlignment(VerticalAlignment.TOP);
            wrapEvenStyle.setAlignment(HorizontalAlignment.LEFT);
            createBorder(wrapEvenStyle);

            // STYLE TANPA BORDER UNTUK DUMMY BLOCKER COLUMN (header + semua row)
            CellStyle emptyStyle = workbook.createCellStyle();
            emptyStyle.setWrapText(false);

            // ============================================================
            //                       HEADER
            // ============================================================
            String[] headers = {
                    "URL",
                    "Final URL",
                    "Content Type",
                    "Error",
                    "Source Webpage",
                    "Anchor Text"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(25);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // DUMMY HEADER FOR BLOCKER COLUMN
            Cell dummyHeader = headerRow.createCell(6);
            dummyHeader.setCellValue("");
            dummyHeader.setCellStyle(emptyStyle);

            // ============================================================
            //                       ISI DATA
            // ============================================================
            int rowIndex = 1;

            for (Link link : sortedBrokenLinks) {

                int startRow = rowIndex;
                boolean first = true;

                for (Map.Entry<Link, String> entry : link.getConnection().entrySet()) {

                    Row row = sheet.createRow(rowIndex);

                    boolean isEven = (rowIndex % 2 == 0);
                    CellStyle rowStyle = isEven ? evenRowStyle : oddRowStyle;
                    CellStyle anchorStyle = isEven ? wrapEvenStyle : wrapOddStyle;

                    // ================ BROKEN LINK COLUMNS 0–3 =================
                    if (first) {
                        createStyledCell(row, 0, link.getUrl(), rowStyle);
                        createStyledCell(row, 1, link.getFinalUrl(), rowStyle);
                        createStyledCell(row, 2, link.getContentType(), rowStyle);
                        createStyledCell(row, 3, link.getError(), rowStyle);
                        first = false;
                    } else {
                        createStyledCell(row, 0, "", rowStyle);
                        createStyledCell(row, 1, "", rowStyle);
                        createStyledCell(row, 2, "", rowStyle);
                        createStyledCell(row, 3, "", rowStyle);
                    }

                    // ================ SOURCE WEBPAGE =================
                    createStyledCell(row, 4, entry.getKey().getUrl(), rowStyle);

                    // ================ ANCHOR TEXT =================
                    Cell anchorCell = row.createCell(5);
                    anchorCell.setCellValue(entry.getValue());
                    anchorCell.setCellStyle(anchorStyle);

                    // ================ BLOCKER COLUMN =================
                    Cell blocker = row.createCell(6);
                    blocker.setCellValue("");
                    blocker.setCellStyle(emptyStyle);

                    rowIndex++;
                }

                // ============================================================
                //                       MERGE BROKEN LINK COLUMNS
                // ============================================================
                int endRow = rowIndex - 1;

                if (endRow > startRow) {
                    for (int col = 0; col <= 3; col++) {
                        sheet.addMergedRegion(
                                new CellRangeAddress(startRow, endRow, col, col)
                        );
                    }
                }
            }

            // ============================================================
            //                       FIXED WIDTH COLUMNS
            // ============================================================
            sheet.setColumnWidth(0, 15000);
            sheet.setColumnWidth(1, 15000);
            sheet.setColumnWidth(2, 10000);
            sheet.setColumnWidth(3, 10000);
            sheet.setColumnWidth(4, 15000);

            // Anchor Text → auto width
            sheet.autoSizeColumn(5);

            // Dummy blocker column
            sheet.setColumnWidth(6, 2000);

            // ============================================================
            //                       WRITE FILE
            // ============================================================
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    // ============================================================
    //                       STYLE HELPERS
    // ============================================================

    private CellStyle createHeaderStyle(Workbook wb, Color bgColor) {
        CellStyle style = wb.createCellStyle();

        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(new XSSFColor(Color.decode("#f1f0eb"), null).getIndex());
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setFillForegroundColor(new XSSFColor(bgColor, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        createBorder(style);
        return style;
    }

    private CellStyle createRowStyle(Workbook wb, Color awtColor) {
        CellStyle style = wb.createCellStyle();

        style.setFillForegroundColor(new XSSFColor(awtColor, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setAlignment(HorizontalAlignment.LEFT);

        createBorder(style);
        return style;
    }

    private void createStyledCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createBorder(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
