package com.banking.forms.formdefinition.application;

/** Raised when a submitted form schema is malformed or structurally invalid. */
public class FormSchemaException extends RuntimeException {

    public FormSchemaException(String message) {
        super(message);
    }
}
