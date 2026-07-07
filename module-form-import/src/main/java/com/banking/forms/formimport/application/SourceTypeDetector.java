package com.banking.forms.formimport.application;

import com.banking.forms.formimport.spi.SourceTypes;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Detects the source-type token for an upload (from file name / MIME type) or a URL. This is only
 * about <em>what kind</em> of artifact it is; which provider extracts it is resolved from DB config.
 */
@Component
public class SourceTypeDetector {

    public Optional<String> detectFile(String fileName, String contentType) {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);

        if (name.endsWith(".pdf") || type.contains("pdf")) {
            return Optional.of(SourceTypes.PDF);
        }
        if (name.endsWith(".csv") || type.contains("csv")) {
            return Optional.of(SourceTypes.CSV);
        }
        if (name.endsWith(".xlsx") || name.endsWith(".xls") || type.contains("excel") || type.contains("spreadsheet")) {
            return Optional.of(SourceTypes.SPREADSHEET);
        }
        if (name.endsWith(".html") || name.endsWith(".htm") || type.contains("html")) {
            return Optional.of(SourceTypes.HTML);
        }
        if (name.matches(".*\\.(png|jpe?g|gif|webp|tiff?|bmp)$") || type.startsWith("image/")) {
            return Optional.of(SourceTypes.IMAGE);
        }
        return Optional.empty();
    }

    /** Remote form URLs are treated as HTML. */
    public String detectUrl(String url) {
        return SourceTypes.HTML;
    }
}
