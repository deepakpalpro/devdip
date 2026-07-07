package com.banking.forms.formimport.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.banking.forms.formimport.application.ExtractedForm;
import com.banking.forms.formimport.application.FieldKind;
import com.banking.forms.formimport.application.FormImportException;
import com.banking.forms.formimport.spi.FormImportSource;
import com.banking.forms.formimport.spi.ProviderConfig;
import com.banking.forms.formimport.spi.SourceTypes;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CsvFormExtractorTest {

    private final CsvFormExtractor extractor = new CsvFormExtractor();

    @Test
    void headerBecomesFieldsWithNumericInference() {
        String csv = "Full Name,Annual Income,\"City, State\"\nJane Doe,50000,\"Austin, TX\"\n";

        ExtractedForm form = extractor.extract(source(csv), ProviderConfig.empty());

        assertThat(form.source()).isEqualTo("CSV_HEADER");
        assertThat(form.fields()).hasSize(3);
        assertThat(form.fields().get(0).label()).isEqualTo("Full Name");
        assertThat(form.fields().get(0).kind()).isEqualTo(FieldKind.TEXT);
        assertThat(form.fields().get(1).label()).isEqualTo("Annual Income");
        assertThat(form.fields().get(1).kind()).isEqualTo(FieldKind.NUMBER);
        // quoted header containing a comma stays a single column
        assertThat(form.fields().get(2).label()).isEqualTo("City, State");
    }

    @Test
    void rejectsEmptyCsv() {
        assertThatThrownBy(() -> extractor.extract(source(""), ProviderConfig.empty()))
                .isInstanceOf(FormImportException.class);
    }

    private FormImportSource source(String csv) {
        return FormImportSource.ofFile(
                SourceTypes.CSV, csv.getBytes(StandardCharsets.UTF_8), "data.csv", "text/csv");
    }
}
