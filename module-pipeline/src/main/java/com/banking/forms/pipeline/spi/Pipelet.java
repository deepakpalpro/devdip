package com.banking.forms.pipeline.spi;

/**
 * Service Provider Interface for a configurable pipeline step. Implementations are Spring beans
 * discovered by {@link #code()}, matching rows in {@code pipelet_definition} and bindings in
 * {@code pipeline_step}.
 */
public interface Pipelet {

    String code();

    PipeletResult execute(PipeletContext context, PipeletConfig config);
}
