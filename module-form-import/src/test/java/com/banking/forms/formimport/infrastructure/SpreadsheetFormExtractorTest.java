package com.banking.forms.formimport.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.forms.formimport.application.ExtractedForm;
import com.banking.forms.formimport.application.FieldKind;
import com.banking.forms.formimport.spi.FormImportSource;
import com.banking.forms.formimport.spi.ProviderConfig;
import com.banking.forms.formimport.spi.SourceTypes;
import java.io.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class SpreadsheetFormExtractorTest {

    private final SpreadsheetFormExtractor extractor = new SpreadsheetFormExtractor();

    @Test
    void headerRowBecomesFieldsWithNumericInference() throws Exception {
        byte[] xlsx = workbook();

        ExtractedForm form = extractor.extract(source(xlsx), ProviderConfig.empty());

        assertThat(form.source()).isEqualTo("SPREADSHEET_HEADER");
        assertThat(form.fields()).hasSize(2);
        assertThat(form.fields().get(0).label()).isEqualTo("Full Name");
        assertThat(form.fields().get(0).kind()).isEqualTo(FieldKind.TEXT);
        assertThat(form.fields().get(1).label()).isEqualTo("Balance");
        assertThat(form.fields().get(1).kind()).isEqualTo(FieldKind.NUMBER);
    }

    private FormImportSource source(byte[] xlsx) {
        return FormImportSource.ofFile(
                SourceTypes.SPREADSHEET,
                xlsx,
                "data.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    private byte[] workbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Accounts");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Full Name");
            header.createCell(1).setCellValue("Balance");
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("Jane Doe");
            data.createCell(1).setCellValue(1234.56);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
