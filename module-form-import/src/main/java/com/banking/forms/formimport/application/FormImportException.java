package com.banking.forms.formimport.application;

/** Raised when an import cannot proceed (bad/empty file, unreadable PDF, invalid accept request). */
public class FormImportException extends RuntimeException {

    public FormImportException(String message) {
        super(message);
    }

    public FormImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
