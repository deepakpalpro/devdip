package com.banking.forms.formimport.application;

import java.util.List;

/**
 * The raw result of extracting structure from a PDF.
 *
 * @param suggestedName a human-friendly name guess (document title or file name)
 * @param source        how the structure was obtained (e.g. {@code ACROFORM}, {@code TEXT_HEURISTIC}, {@code NONE})
 * @param fields        the discovered fields, in document order
 */
public record ExtractedForm(String suggestedName, String source, List<ExtractedField> fields) {

    public ExtractedForm {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
