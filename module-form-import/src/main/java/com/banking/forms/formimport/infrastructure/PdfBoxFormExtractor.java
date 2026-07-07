package com.banking.forms.formimport.infrastructure;

import com.banking.forms.formimport.application.ExtractedField;
import com.banking.forms.formimport.application.ExtractedForm;
import com.banking.forms.formimport.application.FieldKind;
import com.banking.forms.formimport.application.FormImportException;
import com.banking.forms.formimport.spi.FormExtractor;
import com.banking.forms.formimport.spi.FormImportSource;
import com.banking.forms.formimport.spi.ProviderConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDChoice;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDPushButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * Deterministic, in-JVM PDF extractor (provider {@code pdfbox}). Two strategies, best-first:
 * <ol>
 *   <li><b>AcroForm</b> — if the PDF has real form fields, read them directly (names, types,
 *       required flags, choice options).</li>
 *   <li><b>Text heuristic</b> — otherwise scan extracted text for label-like lines (ending in ':').</li>
 * </ol>
 * No bytes leave the JVM. Scanned/handwritten PDFs are better served by an external OCR provider.
 */
@Component
public class PdfBoxFormExtractor implements FormExtractor {

    private static final int MAX_FIELDS = 200;
    private static final double ACROFORM_CONFIDENCE = 0.92;
    private static final double TEXT_HEURISTIC_CONFIDENCE = 0.4;
    private static final Pattern NUMBER_HINT =
            Pattern.compile("(?i).*(amount|income|salary|balance|\\bage\\b|quantity|total).*");
    private static final Pattern LABEL_LINE = Pattern.compile("^\\s*([A-Za-z][A-Za-z0-9 /&'()\\-]{1,60}):\\s*$");

    @Override
    public String code() {
        return "pdfbox";
    }

    @Override
    public ExtractedForm extract(FormImportSource source, ProviderConfig config) {
        if (!source.hasContent()) {
            throw new FormImportException("Uploaded PDF is empty");
        }
        try (PDDocument document = Loader.loadPDF(source.content())) {
            String title = document.getDocumentInformation() == null
                    ? null
                    : document.getDocumentInformation().getTitle();

            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm != null && !acroForm.getFields().isEmpty()) {
                List<ExtractedField> fields = fromAcroForm(acroForm);
                if (!fields.isEmpty()) {
                    return new ExtractedForm(title, "ACROFORM", fields);
                }
            }

            List<ExtractedField> textFields = fromText(document);
            return new ExtractedForm(title, textFields.isEmpty() ? "NONE" : "TEXT_HEURISTIC", textFields);
        } catch (FormImportException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new FormImportException("Unable to read PDF: " + ex.getMessage(), ex);
        }
    }

    private List<ExtractedField> fromAcroForm(PDAcroForm acroForm) {
        List<ExtractedField> fields = new ArrayList<>();
        Iterator<PDField> iterator = acroForm.getFieldIterator();
        while (iterator.hasNext() && fields.size() < MAX_FIELDS) {
            PDField field = iterator.next();
            if (field instanceof PDPushButton || field instanceof PDSignatureField) {
                continue;
            }
            String name = field.getFullyQualifiedName();
            if (name == null || name.isBlank()) {
                continue;
            }

            String group = null;
            String label = name;
            int firstDot = name.indexOf('.');
            if (firstDot > 0 && firstDot < name.length() - 1) {
                group = name.substring(0, firstDot);
                label = name.substring(firstDot + 1);
            }

            FieldKind kind;
            List<String> options = List.of();
            if (field instanceof PDCheckBox) {
                kind = FieldKind.CHECKBOX;
            } else if (field instanceof PDRadioButton radio) {
                kind = FieldKind.CHOICE;
                options = safeList(radio.getExportValues());
            } else if (field instanceof PDChoice choice) {
                kind = FieldKind.CHOICE;
                options = safeList(choice.getOptions());
            } else if (NUMBER_HINT.matcher(name).matches()) {
                kind = FieldKind.NUMBER;
            } else {
                kind = FieldKind.TEXT;
            }

            fields.add(new ExtractedField(label, kind, options, field.isRequired(), ACROFORM_CONFIDENCE, group));
        }
        return fields;
    }

    private List<ExtractedField> fromText(PDDocument document) throws java.io.IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        List<ExtractedField> fields = new ArrayList<>();
        for (String rawLine : text.split("\\r?\\n")) {
            if (fields.size() >= MAX_FIELDS) {
                break;
            }
            var matcher = LABEL_LINE.matcher(rawLine);
            if (matcher.matches()) {
                String label = matcher.group(1).trim();
                FieldKind kind = NUMBER_HINT.matcher(label).matches() ? FieldKind.NUMBER : FieldKind.TEXT;
                fields.add(new ExtractedField(label, kind, List.of(), false, TEXT_HEURISTIC_CONFIDENCE, null));
            }
        }
        return fields;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : new ArrayList<>(values);
    }
}
