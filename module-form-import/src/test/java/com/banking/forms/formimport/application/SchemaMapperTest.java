package com.banking.forms.formimport.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchemaMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SchemaMapper mapper = new SchemaMapper(objectMapper);

    @Test
    void mapsFieldsIntoValidGroupedSchema() {
        ExtractedForm form = new ExtractedForm(
                "Loan Application",
                "ACROFORM",
                List.of(
                        new ExtractedField("First Name", FieldKind.TEXT, List.of(), true, 0.9, "applicant"),
                        new ExtractedField("Annual Income", FieldKind.NUMBER, List.of(), false, 0.9, "applicant"),
                        new ExtractedField(
                                "Account Type", FieldKind.CHOICE, List.of("Savings", "Current"), true, 0.9, null)));

        MappedSchema mapped = mapper.map(form);
        JsonNode sections = mapped.schema().get("sections");

        assertThat(sections).hasSize(2);
        JsonNode applicant = sections.get(0);
        assertThat(applicant.get("key").asText()).isEqualTo("applicant");
        assertThat(applicant.get("title").asText()).isEqualTo("Applicant");
        assertThat(applicant.get("fields")).hasSize(2);

        JsonNode firstName = applicant.get("fields").get(0);
        assertThat(firstName.get("key").asText()).isEqualTo("first-name");
        assertThat(firstName.get("type").asText()).isEqualTo("text");
        assertThat(firstName.get("required").asBoolean()).isTrue();

        JsonNode income = applicant.get("fields").get(1);
        assertThat(income.get("type").asText()).isEqualTo("number");
        assertThat(income.has("required")).isFalse();

        JsonNode importedSection = sections.get(1);
        JsonNode accountType = importedSection.get("fields").get(0);
        assertThat(accountType.get("type").asText()).isEqualTo("select");
        assertThat(accountType.get("options")).hasSize(2);
        assertThat(mapped.overallConfidence()).isEqualTo(0.9);
    }

    @Test
    void deduplicatesKeysAndStripsReservedDot() {
        ExtractedForm form = new ExtractedForm(
                null,
                "ACROFORM",
                List.of(
                        new ExtractedField("First Name", FieldKind.TEXT, List.of(), false, 0.5, null),
                        new ExtractedField("First.Name", FieldKind.TEXT, List.of(), false, 0.5, null)));

        JsonNode fields = mapper.map(form).schema().get("sections").get(0).get("fields");

        assertThat(fields.get(0).get("key").asText()).isEqualTo("first-name");
        assertThat(fields.get(1).get("key").asText()).isEqualTo("first-name-2");
        assertThat(fields.get(0).get("key").asText()).doesNotContain(".");
        assertThat(fields.get(1).get("key").asText()).doesNotContain(".");
    }

    @Test
    void checkboxWithoutOptionsBecomesYesNoSelect() {
        ExtractedForm form = new ExtractedForm(
                null,
                "ACROFORM",
                List.of(new ExtractedField("Consent", FieldKind.CHECKBOX, List.of(), true, 0.9, null)));

        JsonNode field = mapper.map(form).schema().get("sections").get(0).get("fields").get(0);
        assertThat(field.get("type").asText()).isEqualTo("select");
        assertThat(field.get("options")).hasSize(2);
        assertThat(field.get("options").get(0).asText()).isEqualTo("Yes");
    }

    @Test
    void emptyExtractionProducesEmptySectionsAndZeroConfidence() {
        MappedSchema mapped = mapper.map(new ExtractedForm(null, "NONE", List.of()));
        assertThat(mapped.schema().get("sections")).isEmpty();
        assertThat(mapped.overallConfidence()).isEqualTo(0.0);
    }
}
