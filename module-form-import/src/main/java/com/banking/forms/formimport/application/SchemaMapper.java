package com.banking.forms.formimport.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Maps a raw {@link ExtractedForm} onto the platform's form schema
 * ({@code {sections:[{key,title,fields:[{key,type,label,required,options?}]}]}}), the shape the
 * renderer consumes and {@code FormCommandService.validateSchema} enforces.
 *
 * <p>Guarantees the schema's structural invariants up front: section/field keys are slugified,
 * de-duplicated, and free of the reserved '.' separator, so the generated draft passes validation.
 */
@Component
public class SchemaMapper {

    private static final String DEFAULT_SECTION_TITLE = "Imported Fields";

    private final ObjectMapper objectMapper;

    public SchemaMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MappedSchema map(ExtractedForm extracted) {
        ObjectNode schema = objectMapper.createObjectNode();
        ArrayNode sections = schema.putArray("sections");

        ObjectNode confidence = objectMapper.createObjectNode();
        confidence.put("source", extracted.source());
        ObjectNode sectionConfidences = confidence.putObject("fields");

        // Preserve document order while grouping fields into sections.
        Map<String, List<ExtractedField>> grouped = new LinkedHashMap<>();
        for (ExtractedField field : extracted.fields()) {
            String groupTitle = (field.group() == null || field.group().isBlank())
                    ? DEFAULT_SECTION_TITLE
                    : field.group();
            grouped.computeIfAbsent(groupTitle, key -> new ArrayList<>()).add(field);
        }

        List<Double> allConfidences = new ArrayList<>();
        UniqueKeys sectionKeys = new UniqueKeys("section");
        for (Map.Entry<String, List<ExtractedField>> entry : grouped.entrySet()) {
            String sectionTitle = humanize(entry.getKey());
            String sectionKey = sectionKeys.next(entry.getKey());

            ObjectNode section = sections.addObject();
            section.put("key", sectionKey);
            section.put("title", sectionTitle);
            ArrayNode fields = section.putArray("fields");
            ObjectNode fieldConfidences = sectionConfidences.putObject(sectionKey);

            UniqueKeys fieldKeys = new UniqueKeys("field");
            for (ExtractedField extractedField : entry.getValue()) {
                String fieldKey = fieldKeys.next(extractedField.label());
                ObjectNode field = fields.addObject();
                field.put("key", fieldKey);
                field.put("type", schemaType(extractedField.kind()));
                field.put("label", humanize(extractedField.label()));
                if (extractedField.required()) {
                    field.put("required", true);
                }
                List<String> options = optionsFor(extractedField);
                if (!options.isEmpty()) {
                    ArrayNode optionsNode = field.putArray("options");
                    options.forEach(optionsNode::add);
                }
                fieldConfidences.put(fieldKey, round(extractedField.confidence()));
                allConfidences.add(extractedField.confidence());
            }
        }

        double overall = allConfidences.isEmpty()
                ? 0.0
                : round(allConfidences.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        confidence.put("overall", overall);
        return new MappedSchema(schema, confidence, overall);
    }

    private List<String> optionsFor(ExtractedField field) {
        if (field.kind() == FieldKind.CHECKBOX && field.options().isEmpty()) {
            return List.of("Yes", "No");
        }
        return field.options();
    }

    private String schemaType(FieldKind kind) {
        return switch (kind) {
            case NUMBER -> "number";
            case CHOICE, CHECKBOX -> "select";
            case TEXT -> "text";
        };
    }

    /** Turns raw labels/keys (e.g. "first_name", "applicant.dob") into a readable Title Case label. */
    private String humanize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Untitled";
        }
        String spaced = raw.replaceAll("[._\\-]+", " ")
                .replaceAll("(?<=[a-z])(?=[A-Z])", " ")
                .trim();
        if (spaced.isBlank()) {
            return "Untitled";
        }
        String[] words = spaced.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                sb.append(word.substring(1));
            }
            sb.append(' ');
        }
        return sb.toString().trim();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Generates unique, dot-free, non-blank slug keys within a scope. */
    private static final class UniqueKeys {

        private final String fallbackPrefix;
        private final Map<String, Integer> seen = new LinkedHashMap<>();
        private int counter = 0;

        UniqueKeys(String fallbackPrefix) {
            this.fallbackPrefix = fallbackPrefix;
        }

        String next(String raw) {
            counter++;
            String base = slugify(raw);
            if (base.isBlank()) {
                base = fallbackPrefix + "-" + counter;
            }
            Integer count = seen.get(base);
            if (count == null) {
                seen.put(base, 1);
                return base;
            }
            int nextCount = count + 1;
            seen.put(base, nextCount);
            return base + "-" + nextCount;
        }

        private String slugify(String raw) {
            if (raw == null) {
                return "";
            }
            String slug = raw.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "");
            return slug.length() > 60 ? slug.substring(0, 60).replaceAll("-$", "") : slug;
        }
    }
}
