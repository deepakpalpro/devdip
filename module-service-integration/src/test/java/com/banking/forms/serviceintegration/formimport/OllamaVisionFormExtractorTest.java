package com.banking.forms.serviceintegration.formimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.banking.forms.formimport.application.ExtractedForm;
import com.banking.forms.formimport.application.FieldKind;
import com.banking.forms.formimport.application.FormImportException;
import com.banking.forms.formimport.spi.FormImportSource;
import com.banking.forms.formimport.spi.ProviderConfig;
import com.banking.forms.formimport.spi.SourceTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class OllamaVisionFormExtractorTest {

    private final OllamaVisionFormExtractor extractor = new OllamaVisionFormExtractor(new ObjectMapper());

    @Test
    void mapsModelJsonToExtractedFields() {
        String modelJson =
                """
                {
                  "title": "Loan Application",
                  "fields": [
                    {"label": "Full Name", "type": "text", "required": true, "options": []},
                    {"label": "Annual Income", "type": "number", "required": false},
                    {"label": "Account Type", "type": "choice", "required": true, "options": ["Savings", "Current"]},
                    {"label": "I consent", "type": "checkbox", "required": false}
                  ]
                }
                """;

        ExtractedForm form = extractor.mapResponse(modelJson);

        assertThat(form.suggestedName()).isEqualTo("Loan Application");
        assertThat(form.source()).isEqualTo("OLLAMA_VISION");
        assertThat(form.fields()).hasSize(4);
        assertThat(form.fields().get(0).kind()).isEqualTo(FieldKind.TEXT);
        assertThat(form.fields().get(0).required()).isTrue();
        assertThat(form.fields().get(1).kind()).isEqualTo(FieldKind.NUMBER);
        assertThat(form.fields().get(2).kind()).isEqualTo(FieldKind.CHOICE);
        assertThat(form.fields().get(2).options()).containsExactly("Savings", "Current");
        assertThat(form.fields().get(3).kind()).isEqualTo(FieldKind.CHECKBOX);
    }

    @Test
    void skipsBlankLabelsAndDefaultsUnknownTypeToText() {
        String modelJson =
                """
                {"fields": [
                  {"label": "", "type": "text"},
                  {"label": "Notes", "type": "paragraph"}
                ]}
                """;

        ExtractedForm form = extractor.mapResponse(modelJson);

        assertThat(form.fields()).hasSize(1);
        assertThat(form.fields().get(0).label()).isEqualTo("Notes");
        assertThat(form.fields().get(0).kind()).isEqualTo(FieldKind.TEXT);
    }

    @Test
    void rejectsInvalidJson() {
        assertThatThrownBy(() -> extractor.mapResponse("not json at all"))
                .isInstanceOf(FormImportException.class);
    }

    @Test
    void rejectsEmptyImage() {
        FormImportSource empty =
                FormImportSource.ofFile(SourceTypes.IMAGE, new byte[0], "scan.png", "image/png");
        assertThatThrownBy(() -> extractor.extract(empty, ProviderConfig.empty()))
                .isInstanceOf(FormImportException.class);
    }

    @Test
    void exposesStableCode() {
        assertThat(extractor.code()).isEqualTo("ollama-vision");
    }
}
