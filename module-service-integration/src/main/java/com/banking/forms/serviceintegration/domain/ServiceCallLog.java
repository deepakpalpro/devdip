package com.banking.forms.serviceintegration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_call_log")
public class ServiceCallLog {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID tenantId;

    @Column(name = "submission_id", columnDefinition = "BINARY(16)")
    private UUID submissionId;

    @Column(name = "provider_code", nullable = false, length = 64)
    private String providerCode;

    @Column(name = "adapter_type", nullable = false, length = 32)
    private String adapterType;

    @Column(nullable = false, length = 64)
    private String operation;

    @Column(name = "form_code", length = 64)
    private String formCode;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "provider_ref", length = 256)
    private String providerRef;

    @Lob
    @Column(name = "response_json")
    private String responseJson;

    @Lob
    @Column
    private String error;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ServiceCallLog() {}

    public ServiceCallLog(
            UUID id,
            UUID tenantId,
            UUID submissionId,
            String providerCode,
            String adapterType,
            String operation,
            String formCode,
            ServiceCallStatus status) {
        this.id = id;
        this.tenantId = tenantId;
        this.submissionId = submissionId;
        this.providerCode = providerCode;
        this.adapterType = adapterType;
        this.operation = operation;
        this.formCode = formCode;
        this.status = status.name();
    }

    public void completeSuccess(String providerRef, String responseJson, long durationMs) {
        this.status = ServiceCallStatus.SUCCESS.name();
        this.providerRef = providerRef;
        this.responseJson = responseJson;
        this.durationMs = durationMs;
        this.error = null;
    }

    public void completeFailed(String error, long durationMs) {
        this.status = ServiceCallStatus.FAILED.name();
        this.error = error;
        this.durationMs = durationMs;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getAdapterType() {
        return adapterType;
    }

    public String getOperation() {
        return operation;
    }

    public String getFormCode() {
        return formCode;
    }

    public ServiceCallStatus getStatus() {
        return ServiceCallStatus.valueOf(status);
    }

    public String getProviderRef() {
        return providerRef;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public String getError() {
        return error;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
