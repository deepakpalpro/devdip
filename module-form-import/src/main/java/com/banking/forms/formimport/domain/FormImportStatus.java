package com.banking.forms.formimport.domain;

/**
 * Lifecycle of a PDF-to-form import job.
 *
 * <p>Extraction is currently synchronous (deterministic in-JVM PDF parsing), so a job typically
 * lands in {@link #NEEDS_REVIEW} or {@link #FAILED} on upload. The intermediate states exist so the
 * lifecycle stays stable once slower external OCR/LLM extraction is introduced behind the same
 * extractor seam and moved off the request thread.
 */
public enum FormImportStatus {
    PENDING,
    EXTRACTING,
    NEEDS_REVIEW,
    ACCEPTED,
    FAILED
}
