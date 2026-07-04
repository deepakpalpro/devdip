package com.banking.forms.formimport.infrastructure;

import com.banking.forms.formimport.application.ExtractedField;
import com.banking.forms.formimport.application.ExtractedForm;
import com.banking.forms.formimport.application.FieldKind;
import com.banking.forms.formimport.application.FormImportException;
import com.banking.forms.formimport.spi.FormExtractor;
import com.banking.forms.formimport.spi.FormImportSource;
import com.banking.forms.formimport.spi.ProviderConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * Extracts a form from an HTML page (provider {@code jsoup-html}) — either uploaded HTML bytes or a
 * fetched URL. Reads {@code <input>/<select>/<textarea>} controls, associates {@code <label>}s, maps
 * HTML input types to field kinds, and groups radios by name into a single choice.
 */
@Component
public class HtmlFormExtractor implements FormExtractor {

    private static final int MAX_FIELDS = 200;
    private static final double CONFIDENCE = 0.85;

    @Override
    public String code() {
        return "jsoup-html";
    }

    @Override
    public ExtractedForm extract(FormImportSource source, ProviderConfig config) {
        String baseUri = source.url() == null ? "" : source.url();
        String html;
        if (source.hasContent()) {
            html = new String(source.content(), StandardCharsets.UTF_8);
        } else if (source.url() != null && !source.url().isBlank()) {
            html = fetch(source.url(), config);
        } else {
            throw new FormImportException("No HTML content or URL provided");
        }
        return parse(html, baseUri);
    }

    /** Package-visible so tests can exercise parsing without any network. */
    ExtractedForm parse(String html, String baseUri) {
        Document doc = Jsoup.parse(html, baseUri == null ? "" : baseUri);
        Elements forms = doc.select("form");
        List<Element> scopes = forms.isEmpty() ? List.of((Element) doc) : new ArrayList<>(forms);

        List<ExtractedField> fields = new ArrayList<>();
        for (int fi = 0; fi < scopes.size() && fields.size() < MAX_FIELDS; fi++) {
            Element scope = scopes.get(fi);
            String group = forms.isEmpty() ? null : formTitle(scope, scopes.size(), fi);
            fields.addAll(extractControls(scope, group, MAX_FIELDS - fields.size()));
        }
        return new ExtractedForm(doc.title().isBlank() ? null : doc.title(), "HTML_FORM", fields);
    }

    private List<ExtractedField> extractControls(Element scope, String group, int limit) {
        Map<String, String> labelByFor = new LinkedHashMap<>();
        for (Element label : scope.select("label[for]")) {
            String forId = label.attr("for");
            if (!forId.isBlank() && !label.text().isBlank()) {
                labelByFor.putIfAbsent(forId, label.text().trim());
            }
        }
        Map<String, List<Element>> radiosByName = new LinkedHashMap<>();
        for (Element radio : scope.select("input[type=radio]")) {
            radiosByName.computeIfAbsent(radio.attr("name"), k -> new ArrayList<>()).add(radio);
        }

        List<ExtractedField> fields = new ArrayList<>();
        java.util.Set<String> emittedRadioGroups = new java.util.HashSet<>();
        for (Element control : scope.select("input, select, textarea")) {
            if (fields.size() >= limit) {
                break;
            }
            String tag = control.tagName();
            if ("input".equals(tag)) {
                String type = control.attr("type").toLowerCase();
                if (type.isBlank()) {
                    type = "text";
                }
                if (List.of("hidden", "submit", "button", "reset", "image", "file").contains(type)) {
                    continue;
                }
                if ("radio".equals(type)) {
                    String name = control.attr("name");
                    if (!emittedRadioGroups.add(name)) {
                        continue;
                    }
                    List<String> options = new ArrayList<>();
                    for (Element radio : radiosByName.getOrDefault(name, List.of())) {
                        options.add(radioOptionLabel(radio, labelByFor));
                    }
                    fields.add(field(name, FieldKind.CHOICE, options, anyRequired(radiosByName.get(name)), group));
                } else if ("checkbox".equals(type)) {
                    fields.add(field(
                            controlLabel(control, labelByFor, null),
                            FieldKind.CHECKBOX,
                            List.of(),
                            control.hasAttr("required"),
                            group));
                } else {
                    FieldKind kind =
                            ("number".equals(type) || "range".equals(type)) ? FieldKind.NUMBER : FieldKind.TEXT;
                    fields.add(field(
                            controlLabel(control, labelByFor, null),
                            kind,
                            List.of(),
                            control.hasAttr("required"),
                            group));
                }
            } else if ("select".equals(tag)) {
                List<String> options = new ArrayList<>();
                for (Element option : control.select("option")) {
                    String text = option.text().trim();
                    if (!text.isBlank()) {
                        options.add(text);
                    }
                }
                fields.add(field(
                        controlLabel(control, labelByFor, null),
                        FieldKind.CHOICE,
                        options,
                        control.hasAttr("required"),
                        group));
            } else { // textarea
                fields.add(field(
                        controlLabel(control, labelByFor, null),
                        FieldKind.TEXT,
                        List.of(),
                        control.hasAttr("required"),
                        group));
            }
        }
        return fields;
    }

    private ExtractedField field(String label, FieldKind kind, List<String> options, boolean required, String group) {
        return new ExtractedField(label, kind, options, required, CONFIDENCE, group);
    }

    private boolean anyRequired(List<Element> elements) {
        return elements != null && elements.stream().anyMatch(e -> e.hasAttr("required"));
    }

    /** An individual radio's option label: its associated label / value, never the group name. */
    private String radioOptionLabel(Element radio, Map<String, String> labelByFor) {
        String id = radio.id();
        if (!id.isBlank() && labelByFor.containsKey(id)) {
            return labelByFor.get(id);
        }
        for (Element parent : radio.parents()) {
            if ("label".equals(parent.tagName()) && !parent.text().isBlank()) {
                return parent.text().trim();
            }
        }
        String value = radio.attr("value");
        if (!value.isBlank()) {
            return value;
        }
        return id.isBlank() ? "Option" : id;
    }

    private String controlLabel(Element control, Map<String, String> labelByFor, String fallback) {
        String id = control.id();
        if (!id.isBlank() && labelByFor.containsKey(id)) {
            return labelByFor.get(id);
        }
        for (Element parent : control.parents()) {
            if ("label".equals(parent.tagName()) && !parent.text().isBlank()) {
                return parent.text().trim();
            }
        }
        String placeholder = control.attr("placeholder");
        if (!placeholder.isBlank()) {
            return placeholder.trim();
        }
        String name = control.attr("name");
        if (!name.isBlank()) {
            return name;
        }
        if (!id.isBlank()) {
            return id;
        }
        return fallback == null || fallback.isBlank() ? "Field" : fallback;
    }

    private String formTitle(Element form, int formCount, int index) {
        String name = firstNonBlank(form.attr("name"), form.attr("id"), legendText(form));
        if (name == null && formCount > 1) {
            return "Form " + (index + 1);
        }
        return name;
    }

    private String legendText(Element form) {
        Element legend = form.selectFirst("legend");
        return legend == null ? null : legend.text().trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String fetch(String url, ProviderConfig config) {
        int timeout = config.integer("timeoutSeconds", 10);
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(timeout))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(timeout))
                    .header("User-Agent", "BankingFormsImporter/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new FormImportException("URL returned HTTP " + response.statusCode());
            }
            return response.body();
        } catch (FormImportException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new FormImportException("Could not fetch URL: " + ex.getMessage(), ex);
        }
    }
}
