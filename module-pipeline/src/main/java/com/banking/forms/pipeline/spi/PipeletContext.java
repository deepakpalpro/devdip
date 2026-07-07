package com.banking.forms.pipeline.spi;

import com.banking.forms.formdefinition.application.PublishedFormView;
import com.banking.forms.pipeline.domain.PipelineTrigger;
import com.banking.forms.submission.domain.Submission;
import com.banking.forms.transformation.application.ScrubResult;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Mutable execution context shared across pipelets in one pipeline run. */
public class PipeletContext {

    private final UUID tenantId;
    private final UUID submissionId;
    private final PipelineTrigger trigger;
    private final Submission submission;
    private final PublishedFormView form;
    private final Map<String, Map<String, Object>> sectionData;
    private final Map<String, Object> attributes = new HashMap<>();

    private ScrubResult scrubResult;
    private Double riskScore;
    private String riskRecommendation;
    private UUID pipelineDefinitionId;
    private UUID pipelineStepId;

    public PipeletContext(
            UUID tenantId,
            UUID submissionId,
            PipelineTrigger trigger,
            Submission submission,
            PublishedFormView form,
            Map<String, Map<String, Object>> sectionData) {
        this.tenantId = tenantId;
        this.submissionId = submissionId;
        this.trigger = trigger;
        this.submission = submission;
        this.form = form;
        this.sectionData = sectionData;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public UUID submissionId() {
        return submissionId;
    }

    public UUID formVersionId() {
        return submission.getFormVersionId();
    }

    public UUID pipelineDefinitionId() {
        return pipelineDefinitionId;
    }

    public void setPipelineDefinitionId(UUID pipelineDefinitionId) {
        this.pipelineDefinitionId = pipelineDefinitionId;
    }

    public UUID pipelineStepId() {
        return pipelineStepId;
    }

    public void setPipelineStepId(UUID pipelineStepId) {
        this.pipelineStepId = pipelineStepId;
    }

    public PipelineTrigger trigger() {
        return trigger;
    }

    public Submission submission() {
        return submission;
    }

    public PublishedFormView form() {
        return form;
    }

    public Map<String, Map<String, Object>> sectionData() {
        return sectionData;
    }

    public ScrubResult scrubResult() {
        return scrubResult;
    }

    public void setScrubResult(ScrubResult scrubResult) {
        this.scrubResult = scrubResult;
    }

    public Double riskScore() {
        return riskScore;
    }

    public String riskRecommendation() {
        return riskRecommendation;
    }

    public void setRiskEvaluation(Double riskScore, String riskRecommendation) {
        this.riskScore = riskScore;
        this.riskRecommendation = riskRecommendation;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }
}
