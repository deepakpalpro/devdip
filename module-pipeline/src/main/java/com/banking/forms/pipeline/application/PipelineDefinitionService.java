package com.banking.forms.pipeline.application;

import com.banking.forms.pipeline.domain.FormPipelineBinding;
import com.banking.forms.pipeline.domain.PipelineDefinition;
import com.banking.forms.pipeline.domain.PipelineStep;
import com.banking.forms.pipeline.domain.PipelineTrigger;
import com.banking.forms.pipeline.domain.PipeletDefinition;
import com.banking.forms.pipeline.infrastructure.FormPipelineBindingRepository;
import com.banking.forms.pipeline.infrastructure.PipeletDefinitionRepository;
import com.banking.forms.pipeline.infrastructure.PipelineDefinitionRepository;
import com.banking.forms.pipeline.infrastructure.PipelineStepRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PipelineDefinitionService {

    private final PipeletDefinitionRepository pipeletRepository;
    private final PipelineDefinitionRepository definitionRepository;
    private final PipelineStepRepository stepRepository;
    private final FormPipelineBindingRepository bindingRepository;
    private final PipeletRegistry pipeletRegistry;
    private final ObjectMapper objectMapper;

    public PipelineDefinitionService(
            PipeletDefinitionRepository pipeletRepository,
            PipelineDefinitionRepository definitionRepository,
            PipelineStepRepository stepRepository,
            FormPipelineBindingRepository bindingRepository,
            PipeletRegistry pipeletRegistry,
            ObjectMapper objectMapper) {
        this.pipeletRepository = pipeletRepository;
        this.definitionRepository = definitionRepository;
        this.stepRepository = stepRepository;
        this.bindingRepository = bindingRepository;
        this.pipeletRegistry = pipeletRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PipeletDefinitionView> listPipelets() {
        return pipeletRepository.findByEnabledTrueOrderByCodeAsc().stream()
                .map(this::toPipeletView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PipelineDefinitionView> listPipelines(UUID tenantId) {
        return definitionRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(this::toDefinitionView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PipelineDefinitionDetailView getPipeline(UUID tenantId, UUID pipelineId) {
        PipelineDefinition definition = loadOwned(tenantId, pipelineId);
        List<PipelineStepView> steps = stepRepository.findByPipelineDefinitionIdOrderByStepOrderAsc(pipelineId).stream()
                .map(this::toStepView)
                .toList();
        return new PipelineDefinitionDetailView(toDefinitionView(definition), steps);
    }

    public PipelineDefinitionDetailView createPipeline(UUID tenantId, CreatePipelineRequest request) {
        if (definitionRepository.findByTenantIdAndCode(tenantId, request.code()).isPresent()) {
            throw new PipelineConfigurationException("Pipeline code already exists: " + request.code());
        }
        PipelineDefinition definition = definitionRepository.save(new PipelineDefinition(
                UUID.randomUUID(), tenantId, request.code(), request.name(), request.description(), 1, false));
        saveSteps(definition.getId(), request.steps());
        return getPipeline(tenantId, definition.getId());
    }

    public PipelineDefinitionDetailView updateSteps(UUID tenantId, UUID pipelineId, List<PipelineStepRequest> steps) {
        PipelineDefinition definition = loadOwned(tenantId, pipelineId);
        if (definition.isSystemDefault()) {
            throw new PipelineConfigurationException("System default pipeline cannot be modified");
        }
        stepRepository.deleteByPipelineDefinitionId(pipelineId);
        saveSteps(pipelineId, steps);
        return getPipeline(tenantId, pipelineId);
    }

    @Transactional(readOnly = true)
    public List<FormPipelineBindingView> listBindings(UUID tenantId, UUID formVersionId) {
        return bindingRepository.findByTenantIdAndFormVersionId(tenantId, formVersionId).stream()
                .map(this::toBindingView)
                .toList();
    }

    public FormPipelineBindingView upsertBinding(UUID tenantId, UUID formVersionId, UpsertBindingRequest request) {
        FormPipelineBinding binding = bindingRepository
                .findByFormVersionIdAndTriggerEvent(formVersionId, request.trigger().name())
                .orElseGet(() -> new FormPipelineBinding(
                        UUID.randomUUID(), tenantId, formVersionId, request.pipelineId(), request.trigger(), true));
        loadOwned(tenantId, request.pipelineId());
        binding.update(request.pipelineId(), request.enabled());
        return toBindingView(bindingRepository.save(binding));
    }

    private void saveSteps(UUID pipelineId, List<PipelineStepRequest> steps) {
        int order = 1;
        for (PipelineStepRequest step : steps) {
            if (!pipeletRegistry.hasImplementation(step.pipeletCode())) {
                throw new PipelineConfigurationException("Pipelet not available: " + step.pipeletCode());
            }
            stepRepository.save(new PipelineStep(
                    UUID.randomUUID(),
                    pipelineId,
                    order++,
                    step.stepKey(),
                    step.pipeletCode(),
                    writeJson(step.properties())));
        }
    }

    private PipelineDefinition loadOwned(UUID tenantId, UUID pipelineId) {
        return definitionRepository
                .findById(pipelineId)
                .filter(def -> def.getTenantId().equals(tenantId))
                .orElseThrow(() -> new PipelineConfigurationException("Pipeline not found: " + pipelineId));
    }

    private PipeletDefinitionView toPipeletView(PipeletDefinition pipelet) {
        return new PipeletDefinitionView(
                pipelet.getCode(),
                pipelet.getName(),
                pipelet.getDescription(),
                readJson(pipelet.getConfigSchemaJson()),
                pipelet.isEnabled(),
                pipeletRegistry.hasImplementation(pipelet.getCode()));
    }

    private PipelineDefinitionView toDefinitionView(PipelineDefinition definition) {
        return new PipelineDefinitionView(
                definition.getId(),
                definition.getCode(),
                definition.getName(),
                definition.getDescription(),
                definition.getVersion(),
                definition.getStatus(),
                definition.isSystemDefault());
    }

    private PipelineStepView toStepView(PipelineStep step) {
        return new PipelineStepView(
                step.getId(),
                step.getStepOrder(),
                step.getStepKey(),
                step.getPipeletCode(),
                readJson(step.getPropertiesJson()));
    }

    private FormPipelineBindingView toBindingView(FormPipelineBinding binding) {
        return new FormPipelineBindingView(
                binding.getId(),
                binding.getFormVersionId(),
                binding.getPipelineDefinitionId(),
                binding.getTrigger(),
                binding.isEnabled());
    }

    private JsonNode readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return null;
        }
    }

    private String writeJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            throw new PipelineConfigurationException("Invalid step properties JSON");
        }
    }

    public record PipeletDefinitionView(
            String code, String name, String description, JsonNode configSchema, boolean enabled, boolean available) {}

    public record PipelineDefinitionView(
            UUID id, String code, String name, String description, int version, String status, boolean systemDefault) {}

    public record PipelineStepView(UUID id, int stepOrder, String stepKey, String pipeletCode, JsonNode properties) {}

    public record PipelineDefinitionDetailView(PipelineDefinitionView definition, List<PipelineStepView> steps) {}

    public record PipelineStepRequest(String stepKey, String pipeletCode, JsonNode properties) {}

    public record CreatePipelineRequest(
            String code, String name, String description, List<PipelineStepRequest> steps) {}

    public record FormPipelineBindingView(
            UUID id, UUID formVersionId, UUID pipelineDefinitionId, PipelineTrigger trigger, boolean enabled) {}

    public record UpsertBindingRequest(UUID pipelineId, PipelineTrigger trigger, boolean enabled) {}
}
