package com.banking.forms.pipeline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.util.UUID;

/** One ordered pipelet binding within a pipeline definition. */
@Entity
@Table(name = "pipeline_step")
public class PipelineStep {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "pipeline_definition_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID pipelineDefinitionId;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "step_key", nullable = false, length = 64)
    private String stepKey;

    @Column(name = "pipelet_code", nullable = false, length = 64)
    private String pipeletCode;

    @Lob
    @Column(name = "properties_json")
    private String propertiesJson;

    protected PipelineStep() {}

    public PipelineStep(
            UUID id, UUID pipelineDefinitionId, int stepOrder, String stepKey, String pipeletCode, String propertiesJson) {
        this.id = id;
        this.pipelineDefinitionId = pipelineDefinitionId;
        this.stepOrder = stepOrder;
        this.stepKey = stepKey;
        this.pipeletCode = pipeletCode;
        this.propertiesJson = propertiesJson;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPipelineDefinitionId() {
        return pipelineDefinitionId;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public String getStepKey() {
        return stepKey;
    }

    public String getPipeletCode() {
        return pipeletCode;
    }

    public String getPropertiesJson() {
        return propertiesJson;
    }
}
