package com.banking.forms.formdefinition.domain;

/**
 * Determines how submission section data is persisted for a form.
 *
 * <ul>
 *   <li>{@code JSON_BLOB} — each section is stored as a single JSON document. Flexible and cheap,
 *       ideal for simple, low-regulation forms.</li>
 *   <li>{@code KEY_VALUE} — each field is normalized into its own row, enabling per-field indexing,
 *       column-level encryption, and richer query/audit capabilities for regulated forms.</li>
 * </ul>
 */
public enum StorageStrategy {
    JSON_BLOB,
    KEY_VALUE
}
