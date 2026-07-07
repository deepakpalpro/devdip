package com.banking.forms.downstream.spi;

/**
 * Logical downstream connector types. A type (e.g. {@code rest}) may be served by any of several
 * configurable providers, selected at runtime by priority from the {@code downstream_provider}
 * registry — mirroring the notification-channel and form-import extractor patterns.
 *
 * <p>{@code log}, {@code rest}, and {@code kafka} ship with in-JVM implementations. {@code s3} is a
 * configured-but-unavailable seam until an adapter is added.
 */
public final class ConnectorTypes {

    public static final String LOG = "log";
    public static final String REST = "rest";
    public static final String KAFKA = "kafka";
    public static final String S3 = "s3";

    private ConnectorTypes() {}
}
