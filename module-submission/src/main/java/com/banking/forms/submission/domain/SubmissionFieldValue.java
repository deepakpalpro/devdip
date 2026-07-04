package com.banking.forms.submission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A single normalized field value for a KEY_VALUE-stored submission section.
 * The {@code encrypted} flag supports column-level encryption for PII-sensitive fields.
 */
@Entity
@Table(name = "submission_field_value")
public class SubmissionFieldValue {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "section_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID sectionId;

    @Column(name = "field_key", nullable = false, length = 191)
    private String fieldKey;

    @Column(name = "field_value", length = 512)
    private String fieldValue;

    @Column(name = "is_encrypted", nullable = false)
    private boolean encrypted;

    protected SubmissionFieldValue() {}

    public SubmissionFieldValue(UUID id, UUID sectionId, String fieldKey, String fieldValue, boolean encrypted) {
        this.id = id;
        this.sectionId = sectionId;
        this.fieldKey = fieldKey;
        this.fieldValue = fieldValue;
        this.encrypted = encrypted;
    }

    public UUID getSectionId() {
        return sectionId;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public boolean isEncrypted() {
        return encrypted;
    }
}
