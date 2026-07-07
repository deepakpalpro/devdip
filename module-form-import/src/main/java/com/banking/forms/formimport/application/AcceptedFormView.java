package com.banking.forms.formimport.application;

import java.util.UUID;

/** Result of accepting an import: the newly created DRAFT form/version to open in the builder. */
public record AcceptedFormView(UUID jobId, UUID formId, UUID versionId) {}
