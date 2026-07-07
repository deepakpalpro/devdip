package com.banking.forms.pipeline.application;

import com.banking.forms.pipeline.domain.PipelineDefinition;
import com.banking.forms.pipeline.domain.PipelineStep;
import com.banking.forms.pipeline.domain.PipelineTrigger;
import com.banking.forms.pipeline.infrastructure.FormPipelineBindingRepository;
import com.banking.forms.pipeline.infrastructure.PipelineDefinitionRepository;
import com.banking.forms.pipeline.infrastructure.PipelineStepRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FormPipelineResolver {

    private final FormPipelineBindingRepository bindingRepository;
    private final PipelineDefinitionRepository definitionRepository;
    private final PipelineStepRepository stepRepository;
    private final PipeletRegistry pipeletRegistry;

    public FormPipelineResolver(
            FormPipelineBindingRepository bindingRepository,
            PipelineDefinitionRepository definitionRepository,
            PipelineStepRepository stepRepository,
            PipeletRegistry pipeletRegistry) {
        this.bindingRepository = bindingRepository;
        this.definitionRepository = definitionRepository;
        this.stepRepository = stepRepository;
        this.pipeletRegistry = pipeletRegistry;
    }

    @Transactional(readOnly = true)
    public Optional<ResolvedPipeline> resolve(UUID tenantId, UUID formVersionId, PipelineTrigger trigger) {
        return bindingRepository
                .findByFormVersionIdAndTriggerEventAndEnabledTrue(formVersionId, trigger.name())
                .flatMap(binding -> loadPipeline(binding.getPipelineDefinitionId()))
                .or(() -> systemDefault(tenantId, trigger));
    }

    private Optional<ResolvedPipeline> systemDefault(UUID tenantId, PipelineTrigger trigger) {
        if (trigger != PipelineTrigger.ON_SUBMIT) {
            return Optional.empty();
        }
        return definitionRepository
                .findFirstByTenantIdAndSystemDefaultTrueAndStatus(tenantId, "ACTIVE")
                .flatMap(definition -> loadPipeline(definition.getId()));
    }

    @Transactional(readOnly = true)
    public Optional<ResolvedPipeline> loadById(UUID tenantId, UUID pipelineId) {
        return definitionRepository
                .findById(pipelineId)
                .filter(definition -> definition.getTenantId().equals(tenantId))
                .flatMap(definition -> loadPipeline(definition.getId()));
    }

    private Optional<ResolvedPipeline> loadPipeline(UUID pipelineId) {
        return definitionRepository.findById(pipelineId).map(definition -> {
            List<PipelineStep> steps = stepRepository.findByPipelineDefinitionIdOrderByStepOrderAsc(pipelineId);
            validateSteps(steps);
            return new ResolvedPipeline(definition, steps);
        });
    }

    private void validateSteps(List<PipelineStep> steps) {
        if (steps.isEmpty()) {
            throw new PipelineConfigurationException("Pipeline has no steps configured");
        }
        for (PipelineStep step : steps) {
            if (!pipeletRegistry.hasImplementation(step.getPipeletCode())) {
                throw new PipelineConfigurationException("Pipelet not available: " + step.getPipeletCode());
            }
        }
    }
}
