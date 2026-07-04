package com.banking.forms.formimport.infrastructure;

import com.banking.forms.formimport.application.ExtractedField;
import com.banking.forms.formimport.application.ExtractedForm;
import com.banking.forms.formimport.application.FieldKind;
import com.banking.forms.formimport.application.FormImportException;
import com.banking.forms.formimport.spi.FormExtractor;
import com.banking.forms.formimport.spi.FormImportSource;
import com.banking.forms.formimport.spi.ProviderConfig;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

/**
 * Extracts a form from an XLS/XLSX spreadsheet (provider {@code poi-spreadsheet}) via Apache POI.
 * The first sheet's header row becomes field labels; the first data row infers numeric vs text.
 */
@Component
public class SpreadsheetFormExtractor implements FormExtractor {

    private static final int MAX_FIELDS = 200;
    private static final double CONFIDENCE = 0.75;

    @Override
    public String code() {
        return "poi-spreadsheet";
    }

    @Override
    public ExtractedForm extract(FormImportSource source, ProviderConfig config) {
        if (!source.hasContent()) {
            throw new FormImportException("Uploaded spreadsheet is empty");
        }
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(source.content()))) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new FormImportException("Spreadsheet has no sheets");
            }
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                throw new FormImportException("Spreadsheet has no header row");
            }
            Row sample = sheet.getRow(sheet.getFirstRowNum() + 1);

            List<ExtractedField> fields = new ArrayList<>();
            for (int c = header.getFirstCellNum(); c < header.getLastCellNum() && fields.size() < MAX_FIELDS; c++) {
                Cell headerCell = header.getCell(c);
                String label = headerCell == null ? "" : cellText(headerCell).trim();
                if (label.isEmpty()) {
                    continue;
                }
                Cell sampleCell = sample == null ? null : sample.getCell(c);
                boolean numeric = sampleCell != null && sampleCell.getCellType() == CellType.NUMERIC;
                fields.add(new ExtractedField(
                        label, numeric ? FieldKind.NUMBER : FieldKind.TEXT, List.of(), false, CONFIDENCE, null));
            }
            String name = sheet.getSheetName();
            return new ExtractedForm(name, "SPREADSHEET_HEADER", fields);
        } catch (FormImportException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new FormImportException("Unable to read spreadsheet: " + ex.getMessage(), ex);
        }
    }

    private String cellText(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> cell.toString();
        };
    }
}
