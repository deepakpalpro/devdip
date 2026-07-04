package com.banking.forms.formimport.application;

import java.util.List;

/**
 * A single field discovered in a PDF, before it is mapped to the form schema. {@code group} is an
 * optional section hint (e.g. derived from an AcroForm field-name prefix); {@code confidence} is a
 * 0..1 signal used to flag low-confidence fields for human review.
 */
public record ExtractedField(
        String label, FieldKind kind, List<String> options, boolean required, double confidence, String group) {

    public ExtractedField {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
