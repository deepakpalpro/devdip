package com.banking.forms.formimport.spi;

/**
 * The raw input for an import, independent of how it will be extracted. Either {@code content}
 * (uploaded bytes) or {@code url} (a remote form) is present, depending on {@code sourceType}.
 *
 * @param sourceType detected source token (see {@link SourceTypes})
 * @param content    uploaded file bytes, or {@code null} for URL sources
 * @param url        remote form URL, or {@code null} for uploads
 * @param fileName   original file name (uploads), else {@code null}
 * @param contentType original MIME type (uploads), else {@code null}
 */
public record FormImportSource(
        String sourceType, byte[] content, String url, String fileName, String contentType) {

    public static FormImportSource ofFile(String sourceType, byte[] content, String fileName, String contentType) {
        return new FormImportSource(sourceType, content, null, fileName, contentType);
    }

    public static FormImportSource ofUrl(String sourceType, String url) {
        return new FormImportSource(sourceType, null, url, null, null);
    }

    public boolean hasContent() {
        return content != null && content.length > 0;
    }
}
