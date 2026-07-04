package com.banking.forms.formdefinition.application;

/**
 * Raised when a form authoring action conflicts with current state — e.g. a duplicate form code or
 * an illegal version transition (editing/publishing a non-draft version).
 */
public class FormConflictException extends RuntimeException {

    public FormConflictException(String message) {
        super(message);
    }
}
