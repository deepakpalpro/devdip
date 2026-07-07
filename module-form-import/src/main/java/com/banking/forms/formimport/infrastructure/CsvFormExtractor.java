package com.banking.forms.formimport.infrastructure;

import com.banking.forms.formimport.application.ExtractedField;
import com.banking.forms.formimport.application.ExtractedForm;
import com.banking.forms.formimport.application.FieldKind;
import com.banking.forms.formimport.application.FormImportException;
import com.banking.forms.formimport.spi.FormExtractor;
import com.banking.forms.formimport.spi.FormImportSource;
import com.banking.forms.formimport.spi.ProviderConfig;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Extracts a form from a CSV (provider {@code csv}). The header row becomes the field labels; the
 * first data row (if any) is used to infer numeric vs text. All fields land in one section.
 */
@Component
public class CsvFormExtractor implements FormExtractor {

    private static final int MAX_FIELDS = 200;
    private static final double CONFIDENCE = 0.7;

    @Override
    public String code() {
        return "csv";
    }

    @Override
    public ExtractedForm extract(FormImportSource source, ProviderConfig config) {
        if (!source.hasContent()) {
            throw new FormImportException("Uploaded CSV is empty");
        }
        String text = new String(source.content(), StandardCharsets.UTF_8);
        String[] lines = text.split("\\r?\\n");
        if (lines.length == 0 || lines[0].isBlank()) {
            throw new FormImportException("CSV has no header row");
        }

        List<String> headers = parseLine(lines[0]);
        List<String> sample = lines.length > 1 ? parseLine(lines[1]) : List.of();

        List<ExtractedField> fields = new ArrayList<>();
        for (int i = 0; i < headers.size() && fields.size() < MAX_FIELDS; i++) {
            String header = headers.get(i).trim();
            if (header.isEmpty()) {
                continue;
            }
            String sampleValue = i < sample.size() ? sample.get(i).trim() : "";
            FieldKind kind = isNumeric(sampleValue) ? FieldKind.NUMBER : FieldKind.TEXT;
            fields.add(new ExtractedField(header, kind, List.of(), false, CONFIDENCE, null));
        }
        return new ExtractedForm(null, "CSV_HEADER", fields);
    }

    /** Minimal CSV line parser handling double-quoted values (incl. commas and escaped quotes). */
    private List<String> parseLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cells.add(current.toString());
        return cells;
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.replace(",", "").matches("-?\\d+(\\.\\d+)?");
    }
}
