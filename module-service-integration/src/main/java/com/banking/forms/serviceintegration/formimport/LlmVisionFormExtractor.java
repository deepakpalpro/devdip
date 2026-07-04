package com.banking.forms.serviceintegration.formimport;

import com.banking.forms.formimport.application.ExtractedForm;
import com.banking.forms.formimport.application.FormImportException;
import com.banking.forms.formimport.spi.FormExtractor;
import com.banking.forms.formimport.spi.FormImportSource;
import com.banking.forms.formimport.spi.ProviderConfig;
import org.springframework.stereotype.Component;

/**
 * External provider seam for turning form <em>images</em> (scans/photos) into fields via a hosted
 * vision LLM. Lives in {@code module-service-integration} because it calls an outside service; it
 * implements the same {@link FormExtractor} SPI as the in-JVM providers, so the router treats it
 * identically once an admin enables + configures it (endpoint, model, {@code secretRef}).
 *
 * <p>Disabled by default (see migration seed). Until a real integration is wired, invoking it
 * surfaces a clear, actionable configuration error rather than silently faking OCR — appropriate for
 * a regulated banking context where extracted data must be trustworthy and auditable.
 */
@Component
public class LlmVisionFormExtractor implements FormExtractor {

    @Override
    public String code() {
        return "llm-vision";
    }

    @Override
    public ExtractedForm extract(FormImportSource source, ProviderConfig config) {
        if (!source.hasContent()) {
            throw new FormImportException("No image content provided");
        }
        String endpoint = config.string("endpoint");
        String apiKey = config.secret("secretRef");
        if (endpoint == null || endpoint.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new FormImportException(
                    "LLM vision provider 'llm-vision' is enabled but not configured: set 'endpoint' and a valid "
                            + "'secretRef' (env var holding the API key) in Settings before importing images.");
        }

        // Real integration (HTTP call to the vision model, PII-guarded, response → ExtractedForm)
        // is intentionally not implemented here; this seam documents where it plugs in.
        throw new FormImportException(
                "LLM vision extraction is configured but not yet available in this build. Endpoint="
                        + endpoint + ", model=" + config.string("model", "(default)"));
    }
}
