package com.banking.forms.discovery.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A persisted record of a user's preliminary questionnaire answers plus the top recommended form.
 * Referenced when starting the real application so answers can be pre-populated into it.
 */
@Entity
@Table(name = "discovery_session")
public class DiscoverySession {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID tenantId;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "questionnaire_code", nullable = false, length = 64)
    private String questionnaireCode;

    @Lob
    @Column(name = "answers_json", nullable = false)
    private String answersJson;

    @Column(name = "recommended_form_code", length = 64)
    private String recommendedFormCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected DiscoverySession() {}

    public DiscoverySession(
            UUID id,
            UUID tenantId,
            UUID userId,
            String questionnaireCode,
            String answersJson,
            String recommendedFormCode) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.questionnaireCode = questionnaireCode;
        this.answersJson = answersJson;
        this.recommendedFormCode = recommendedFormCode;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getQuestionnaireCode() {
        return questionnaireCode;
    }

    public String getAnswersJson() {
        return answersJson;
    }

    public String getRecommendedFormCode() {
        return recommendedFormCode;
    }
}
