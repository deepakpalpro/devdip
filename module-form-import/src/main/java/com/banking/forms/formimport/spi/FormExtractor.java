package com.banking.forms.formimport.spi;

import com.banking.forms.formimport.application.ExtractedForm;

/**
 * Service Provider Interface for turning an imported source into an {@link ExtractedForm}.
 *
 * <p>Each implementation is a <em>provider</em> identified by a stable {@link #code()} that matches a
 * configurable {@code form_import_provider} row. The {@link FormExtractorRouter} chooses which
 * provider handles a given source type (from DB config), then invokes it with the stored
 * {@link ProviderConfig}. Implementations may live in any module (in-JVM parsers here, external
 * OCR/LLM adapters in {@code module-service-integration}).
 */
public interface FormExtractor {

    /** Stable provider id, e.g. {@code "pdfbox"}, {@code "csv"}, {@code "llm-vision"}. */
    String code();

    ExtractedForm extract(FormImportSource source, ProviderConfig config);
}
