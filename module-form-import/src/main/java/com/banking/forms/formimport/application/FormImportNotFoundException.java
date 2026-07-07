package com.banking.forms.formimport.application;

import java.util.UUID;

/** Raised when an import job does not exist for the tenant. */
public class FormImportNotFoundException extends RuntimeException {

    public FormImportNotFoundException(UUID jobId) {
        super("Form import job not found: " + jobId);
    }
}
