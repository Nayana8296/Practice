package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * The run's data source: one .xlsx sheet, one row per cabinet + equipment to create.
 *
 * <p>The column headings are deliberately identical to the labels AMS prints next to the
 * fields, so a heading can be handed straight to {@code fillField} / {@code fillGridField}
 * - nothing has to be mapped by hand when a column is added.
 */
public final class ExcelData {

    // ---- Add Cabinet ----
    public static final String COMPLEX = "Complex";
    public static final String SERVICE = "Service";
    public static final String CABINET_TYPE = "Cabinet Type";
    public static final String CABINET_MODEL = "Cabinet Model";
    public static final String SERVICE_TYPE = "Service Type";

    /**
     * Optional. Leave it blank and a cabinet is built the first time a Complex is seen,
     * then reused by every later row with that Complex. Fill it in to file the equipment
     * into a cabinet that is already in AMS - nothing new is created.
     */
    public static final String CABINET_NAME = "Cabinet Name";

    // ---- New Inventory Definition ----
    public static final String EQUIP_TYPE = "Equip Type";
    public static final String EQUIP_MODEL = "Equip Model";
    public static final String TEMPLATE_NAME = "Template Name";

    // ---- Add Equipment Search Results grid ----
    public static final String SHELF = "Shelf";
    public static final String LOGICAL_NODE = "Logical Node";
    public static final String CRITICAL_SERVICE = "Critical Service";
    public static final String FIRSTNET_INDICATOR = "FirstNet Indicator";
    public static final String EQUIP_CLLI = "Equip. CLLI";
    public static final String LOCAL_CLLI = "Local CLLI";
    public static final String ASSOC_CLLI = "Assoc CLLI";
    public static final String VOAVPN = "VoAVPN";

    // ---- Inv Options Details popup ----
    public static final String VAN_RELEASE = "VAN Release #";

    /** Column order used when a template workbook is written. */
    public static final List<String> COLUMNS = List.of(
            COMPLEX, SERVICE, CABINET_TYPE, CABINET_MODEL, SERVICE_TYPE, CABINET_NAME,
            EQUIP_TYPE, EQUIP_MODEL, TEMPLATE_NAME,
            SHELF, LOGICAL_NODE, CRITICAL_SERVICE, FIRSTNET_INDICATOR,
            EQUIP_CLLI, LOCAL_CLLI, ASSOC_CLLI, VOAVPN,
            VAN_RELEASE);

    /** The values the script used to have hard-coded - a known-good example row. */
    private static final List<String> SAMPLE_ROW = List.of(
            "nw1ny", "UVP", "Border Element", "Generic Border Element Cabnet", "U-SIP-IC", "",
            "SESSION DIRECTR", "rbc", "Oracle/SBC-HA-6350",
            "1", "1", "N", "NA",
            "NYCMNY54718", "NYCMNY54718", "NYCMNY54718", "B",
            "VAN 4.0");
    static String cabType;

    private ExcelData() {
    }

    /**
     * Reads every filled row of the sheet. The first row is the heading row; blank rows
     * are skipped so trailing empties left behind in Excel do not start a run.
     */
    public static List<DataRow> read(Path file, String sheetName) {
        try (InputStream in = Files.newInputStream(file);
                Workbook workbook = WorkbookFactory.create(in)) {

            Sheet sheet = sheetName == null ? null : workbook.getSheet(sheetName);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalStateException("Sheet '" + sheet.getSheetName() + "' is empty");
            }

            DataFormatter formatter = new DataFormatter();
            Map<Integer, String> headings = new LinkedHashMap<>();
            for (int c = headerRow.getFirstCellNum(); c < headerRow.getLastCellNum(); c++) {
                String heading = cellText(headerRow.getCell(c), formatter);
                if (!heading.isEmpty()) {
                    headings.put(c, heading);
                }
            }
            if (headings.isEmpty()) {
                throw new IllegalStateException(
                        "No column headings in row " + (sheet.getFirstRowNum() + 1)
                        + " of sheet '" + sheet.getSheetName() + "'");
            }

            List<DataRow> rows = new ArrayList<>();
            for (int r = headerRow.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }

                Map<String, String> values = new LinkedHashMap<>();
                boolean blank = true;
                for (Map.Entry<Integer, String> heading : headings.entrySet()) {
                    String value = cellText(row.getCell(heading.getKey()), formatter);
                    values.put(key(heading.getValue()), value);
                    blank &= value.isEmpty();
                }

                if (!blank) {
                    rows.add(new DataRow(r + 1, headings.values(), values));
                }
            }
            return rows;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file.toAbsolutePath(), e);
        }
    }

    /**
     * Writes a workbook holding the heading row plus one filled-in example row, so the
     * file can be opened, copied down and edited rather than built from scratch.
     */
    public static void writeTemplate(Path file, String sheetName) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            Font bold = workbook.createFont();
            bold.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(bold);

            Row header = sheet.createRow(0);
            Row sample = sheet.createRow(1);
            for (int c = 0; c < COLUMNS.size(); c++) {
                Cell headerCell = header.createCell(c);
                headerCell.setCellValue(COLUMNS.get(c));
                headerCell.setCellStyle(headerStyle);
                sample.createCell(c).setCellValue(SAMPLE_ROW.get(c));
            }

            // Keep the headings on screen while the rows below are filled in.
            sheet.createFreezePane(0, 1);
            for (int c = 0; c < COLUMNS.size(); c++) {
                sheet.autoSizeColumn(c);
            }

            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + file.toAbsolutePath(), e);
        }
    }

    /** Cell as the text Excel shows - so a numeric 1 arrives as "1", never "1.0". */
    private static String cellText(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell).replace('\u00A0', ' ').trim();
    }

    /** Headings are matched loosely: case, padding and the legacy ':' / '*' are ignored. */
    private static String key(String heading) {
        return heading.replace('\u00A0', ' ')
                .replaceAll("[:*]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    /** One spreadsheet row, looked up by column heading. */
    public static final class DataRow {

        private final int excelRow;
        private final List<String> headings;
        private final Map<String, String> values;

        private DataRow(int excelRow, Iterable<String> headings, Map<String, String> values) {
            this.excelRow = excelRow;
            this.headings = new ArrayList<>();
            headings.forEach(this.headings::add);
            this.values = values;
        }

        /** 1-based row number as shown in Excel, for error messages. */
        public int excelRow() {
            return excelRow;
        }

        /** Value of a column; fails loudly when the column is missing or the cell is blank. */
        public String get(String column) {
            String value = values.get(key(column));
            if (value == null) {
                throw new IllegalStateException("No column '" + column + "' in the sheet."
                        + " Columns found: " + headings);
            }
            if (value.isEmpty()) {
                throw new IllegalStateException(
                        "Column '" + column + "' is blank on row " + excelRow);
            }
            return value;
        }

        /** Value of an optional column - the fallback is used when it is missing or blank. */
        public String getOrDefault(String column, String fallback) {
            String value = values.get(key(column));
            return value == null || value.isEmpty() ? fallback : value;
        }

        @Override
        public String toString() {
            return "row " + excelRow + " " + values;
        }
    }
}
