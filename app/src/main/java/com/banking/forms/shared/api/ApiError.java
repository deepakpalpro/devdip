package com.banking.forms.shared.api;

import java.util.List;

public record ApiError(String code, String message, List<FieldError> details, String requestId) {

    public record FieldError(String field, String message) {}
}
