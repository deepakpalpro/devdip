package com.banking.forms.formimport.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.banking.forms.formimport.application.ExtractedForm;
import com.banking.forms.formimport.application.FieldKind;
import com.banking.forms.formimport.application.FormImportException;
import com.banking.forms.formimport.spi.FormImportSource;
import com.banking.forms.formimport.spi.ProviderConfig;
import com.banking.forms.formimport.spi.SourceTypes;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class PdfBoxFormExtractorTest {

    private final PdfBoxFormExtractor extractor = new PdfBoxFormExtractor();

    @Test
    void extractsLabelLinesFromTextPdf() throws Exception {
        byte[] pdf = textPdf(List.of("First Name:", "Annual Income:", "Thank you for applying"));

        ExtractedForm form = extractor.extract(pdfSource(pdf), ProviderConfig.empty());

        assertThat(form.source()).isEqualTo("TEXT_HEURISTIC");
        assertThat(form.fields()).hasSize(2);
        assertThat(form.fields().get(0).label()).isEqualTo("First Name");
        assertThat(form.fields().get(0).kind()).isEqualTo(FieldKind.TEXT);
        assertThat(form.fields().get(1).label()).isEqualTo("Annual Income");
        assertThat(form.fields().get(1).kind()).isEqualTo(FieldKind.NUMBER);
    }

    @Test
    void rejectsEmptyUpload() {
        assertThatThrownBy(() -> extractor.extract(pdfSource(new byte[0]), ProviderConfig.empty()))
                .isInstanceOf(FormImportException.class);
    }

    @Test
    void exposesStableCode() {
        assertThat(extractor.code()).isEqualTo("pdfbox");
    }

    private FormImportSource pdfSource(byte[] bytes) {
        return FormImportSource.ofFile(SourceTypes.PDF, bytes, "sample.pdf", "application/pdf");
    }

    private byte[] textPdf(List<String> lines) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.setLeading(16f);
                content.newLineAtOffset(50, 700);
                for (String line : lines) {
                    content.showText(line);
                    content.newLine();
                }
                content.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
