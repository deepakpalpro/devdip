package com.banking.forms.serviceintegration.spi;

import java.util.Map;

/**
 * Outcome of a {@link ServiceAdapter#execute} attempt. Adapters never throw for expected failures.
 */
public record ServiceResult(
        ServiceOutcome outcome, String providerRef, Map<String, Object> data, String detail) {

    public static ServiceResult success(String providerRef, Map<String, Object> data) {
        return new ServiceResult(ServiceOutcome.SUCCESS, providerRef, data, null);
    }

    public static ServiceResult failed(String detail) {
        return new ServiceResult(ServiceOutcome.FAILED, null, Map.of(), detail);
    }

    public boolean isSuccess() {
        return outcome == ServiceOutcome.SUCCESS;
    }
}
