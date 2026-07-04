package com.banking.forms.submission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Section header for a submission. For JSON_BLOB forms the full section payload lives in
 * {@code sectionDataJson}; for KEY_VALUE forms this row is a parent for
 * {@link SubmissionFieldValue} rows and {@code sectionDataJson} stays {@code null}.
 */
@Entity
@Table(name = "submission_section")
public class SubmissionSection {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "submission_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID submissionId;

    @Column(name = "section_key", nullable = false, length = 100)
    private String sectionKey;

    @Lob
    @Column(name = "section_data_json")
    private String sectionDataJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected SubmissionSection() {}

    public SubmissionSection(UUID id, UUID submissionId, String sectionKey) {
        this.id = id;
        this.submissionId = submissionId;
        this.sectionKey = sectionKey;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public String getSectionKey() {
        return sectionKey;
    }

    public String getSectionDataJson() {
        return sectionDataJson;
    }

    public void setSectionDataJson(String sectionDataJson) {
        this.sectionDataJson = sectionDataJson;
        this.updatedAt = Instant.now();
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
