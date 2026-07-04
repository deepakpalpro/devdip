package com.banking.forms.serviceintegration.spi;

/**
 * Logical service adapter types. A type may be served by any of several configurable providers,
 * selected at runtime by priority from the {@code service_provider} registry.
 */
public final class AdapterTypes {

    public static final String LOG = "log";
    public static final String REST = "rest";
    public static final String CREDIT = "credit";
    public static final String IDENTITY = "identity";

    private AdapterTypes() {}
}
