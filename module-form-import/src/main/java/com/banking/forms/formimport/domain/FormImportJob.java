package com.banking.forms.formimport.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A record of an admin uploading a PDF to be turned into a draft form. The uploaded bytes are
 * deliberately NOT persisted (only a hash + metadata) — a PDF may contain PII, and the platform only
 * needs the generated proposal for human review. The raw file lives no longer than the extraction call.
 */
@Entity
@Table(name = "form_import_job")
public class FormImportJob {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID tenantId;

    @Column(name = "actor_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID actorId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "file_size")
    private long fileSize;

    @Column(name = "source_type", length = 32)
    private String sourceType;

    @Column(name = "provider_code", length = 64)
    private String providerCode;

    @Column(name = "source", length = 32)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FormImportStatus status;

    @Column(name = "suggested_name", length = 255)
    private String suggestedName;

    @Lob
    @Column(name = "proposed_schema")
    private String proposedSchema;

    @Lob
    @Column(name = "confidence_json")
    private String confidenceJson;

    @Column(name = "error_details", length = 1024)
    private String errorDetails;

    @Column(name = "form_id", columnDefinition = "BINARY(16)")
    private UUID formId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected FormImportJob() {}

    public FormImportJob(
            UUID id, UUID tenantId, UUID actorId, String sourceType, String fileName, String fileHash, long fileSize) {
        this.id = id;
        this.tenantId = tenantId;
        this.actorId = actorId;
        this.sourceType = sourceType;
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.fileSize = fileSize;
        this.status = FormImportStatus.PENDING;
    }

    /** Marks the job as processing (extraction in progress) by the resolved provider. */
    public void markExtracting(String providerCode) {
        this.providerCode = providerCode;
        this.status = FormImportStatus.EXTRACTING;
        this.updatedAt = Instant.now();
    }

    /** Stores the generated proposal and moves the job to human review. */
    public void completeReview(String source, String suggestedName, String proposedSchema, String confidenceJson) {
        this.source = source;
        this.suggestedName = suggestedName;
        this.proposedSchema = proposedSchema;
        this.confidenceJson = confidenceJson;
        this.status = FormImportStatus.NEEDS_REVIEW;
        this.updatedAt = Instant.now();
    }

    public void fail(String errorDetails) {
        this.status = FormImportStatus.FAILED;
        this.errorDetails = errorDetails == null ? null : errorDetails.substring(0, Math.min(errorDetails.length(), 1024));
        this.updatedAt = Instant.now();
    }

    /** Links the created draft form and closes the job. */
    public void accept(UUID formId) {
        this.formId = formId;
        this.status = FormImportStatus.ACCEPTED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileHash() {
        return fileHash;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getSource() {
        return source;
    }

    public FormImportStatus getStatus() {
        return status;
    }

    public String getSuggestedName() {
        return suggestedName;
    }

    public String getProposedSchema() {
        return proposedSchema;
    }

    public String getConfidenceJson() {
        return confidenceJson;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public UUID getFormId() {
        return formId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
