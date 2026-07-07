package com.banking.forms.formimport.spi;

/**
 * Canonical source-type tokens used to detect an upload's kind and to key configurable providers.
 *
 * <p>These are intentionally plain strings (not an enum): a provider row in the database ties a
 * token to a concrete extractor implementation, so new source types/providers can be introduced as
 * data + a bean without changing this contract.
 */
public final class SourceTypes {

    public static final String PDF = "PDF";
    public static final String CSV = "CSV";
    public static final String SPREADSHEET = "SPREADSHEET";
    public static final String IMAGE = "IMAGE";
    public static final String HTML = "HTML";

    private SourceTypes() {}
}
