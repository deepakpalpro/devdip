package com.banking.forms.formimport.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.forms.formimport.application.ExtractedField;
import com.banking.forms.formimport.application.ExtractedForm;
import com.banking.forms.formimport.application.FieldKind;
import java.util.List;
import org.junit.jupiter.api.Test;

class HtmlFormExtractorTest {

    private final HtmlFormExtractor extractor = new HtmlFormExtractor();

    @Test
    void parsesInputsLabelsSelectsAndRadioGroups() {
        String html =
                """
                <html><body>
                <form name="account">
                  <label for="fullName">Full Name</label>
                  <input id="fullName" name="fullName" type="text" required />

                  <label for="income">Annual Income</label>
                  <input id="income" name="income" type="number" />

                  <label for="accountType">Account Type</label>
                  <select id="accountType" name="accountType">
                    <option value="">Choose…</option>
                    <option value="savings">Savings</option>
                    <option value="current">Current</option>
                  </select>

                  <label for="consent">I consent</label>
                  <input id="consent" name="consent" type="checkbox" />

                  <input type="radio" name="contact" value="email" /> Email
                  <input type="radio" name="contact" value="phone" /> Phone

                  <input type="hidden" name="csrf" value="x" />
                  <button type="submit">Apply</button>
                </form>
                </body></html>
                """;

        ExtractedForm form = extractor.parse(html, "");
        List<ExtractedField> fields = form.fields();

        assertThat(form.source()).isEqualTo("HTML_FORM");
        // text, number, select, checkbox, one radio group (hidden + submit excluded)
        assertThat(fields).hasSize(5);

        ExtractedField name = fields.get(0);
        assertThat(name.label()).isEqualTo("Full Name");
        assertThat(name.kind()).isEqualTo(FieldKind.TEXT);
        assertThat(name.required()).isTrue();
        assertThat(name.group()).isEqualTo("account");

        assertThat(fields.get(1).kind()).isEqualTo(FieldKind.NUMBER);

        ExtractedField select = fields.get(2);
        assertThat(select.kind()).isEqualTo(FieldKind.CHOICE);
        assertThat(select.options()).contains("Savings", "Current");

        assertThat(fields.get(3).kind()).isEqualTo(FieldKind.CHECKBOX);

        ExtractedField radio = fields.get(4);
        assertThat(radio.kind()).isEqualTo(FieldKind.CHOICE);
        assertThat(radio.options()).containsExactly("email", "phone");
    }

    @Test
    void exposesStableCode() {
        assertThat(extractor.code()).isEqualTo("jsoup-html");
    }
}
