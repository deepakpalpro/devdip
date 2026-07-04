package com.banking.forms.serviceintegration.spi;

/**
 * Service Provider Interface for an external API adapter (credit bureau, identity check, REST hook, …).
 * Implementations are Spring beans discovered by {@link #adapterId()}, which matches a
 * {@code service_provider} row's {@code code}.
 */
public interface ServiceAdapter {

    String adapterId();

    String adapterType();

    ServiceResult execute(ServiceRequest request, AdapterConfig config);
}
