package com.banking.forms.transformation.domain;

/**
 * How a sensitive field value is transformed before it leaves the system of record (for AI
 * evaluation, analytics, or downstream delivery).
 */
public enum PiiStrategy {
    /** Leave the value untouched (e.g. names a reviewer must see). */
    NONE,
    /** Keep only the last few characters visible, replace the rest with {@code *}. */
    MASK,
    /** Replace with a deterministic one-way token so records can still be correlated. */
    HASH,
    /** Replace the whole value with a fixed marker. */
    REDACT,
    /** Drop the field entirely from the sanitized output. */
    REMOVE
}
