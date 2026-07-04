package com.banking.forms.formdefinition.application;

/** Raised when a form definition or version cannot be found within the tenant scope. */
public class FormNotFoundException extends RuntimeException {

    public FormNotFoundException(String message) {
        super(message);
    }
}
