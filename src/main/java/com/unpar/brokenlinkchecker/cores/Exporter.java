package com.unpar.brokenlinkchecker.cores;

import com.unpar.brokenlinkchecker.models.Link;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Kelas Exporter bertugas mengekspor hasil pemeriksaan tautan rusak
 * ke sebuah file Excel (.xlsx). Kelas ini bersifat instance-based
 * agar lebih fleksibel apabila di masa depan ingin ditambah fitur lain.
 */
public class Exporter {

    /**
     * Metode utama untuk menyimpan data broken links ke dalam file Excel.
     *
     * @param brokenLinks daftar link rusak
     * @param file        file tujuan (.xlsx)
     */
    public void save(List<Link> brokenLinks, File file) throws IOException {

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Broken Links");

            // ============= 1. Styling =============
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle oddRowStyle = createRowStyle(workbook, new Color(245, 245, 245));
            CellStyle evenRowStyle = createRowStyle(workbook, Color.WHITE);

            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.setWrapText(true);

            // ============= 2. Header =============
            String[] headers = {
                    "URL",
                    "Final URL",
                    "Content Type",
                    "Error",
                    "Source Webpage",
                    "Anchor Text"
            };

            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ============= 3. Isi Data =============
            int rowIndex = 1;

            for (Link link : brokenLinks) {
                for (Map.Entry<Link, String> entry : link.getConnection().entrySet()) {

                    Row row = sheet.createRow(rowIndex);

                    CellStyle rowStyle = (rowIndex % 2 == 0) ? evenRowStyle : oddRowStyle;

                    createStyledCell(row, 0, link.getUrl(), rowStyle);
                    createStyledCell(row, 1, link.getFinalUrl(), rowStyle);
                    createStyledCell(row, 2, link.getContentType(), rowStyle);
                    createStyledCell(row, 3, link.getError(), rowStyle);
                    createStyledCell(row, 4, entry.getKey().getUrl(), rowStyle);

                    Cell anchorCell = row.createCell(5);
                    anchorCell.setCellValue(entry.getValue());
                    anchorCell.setCellStyle(wrapStyle);

                    rowIndex++;
                }
            }

            // ============= 4. Auto-size kolom =============
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // ============= 5. Tulis ke file =============
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }



    // ============================================================
    // ========================= Utilities =========================
    // ============================================================

    /** Style untuk header */
    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();

        Font font = wb.createFont();
        font.setBold(true);

        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        addBorder(style);
        return style;
    }

    /** Style baris (zebra striping) */
    private CellStyle createRowStyle(Workbook wb, Color awtColor) {
        CellStyle style = wb.createCellStyle();

        style.setFillForegroundColor(new XSSFColor(awtColor, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        addBorder(style);
        return style;
    }

    /** Membuat cell dengan style */
    private void createStyledCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    /** Tambahkan border tipis ke style */
    private void addBorder(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
