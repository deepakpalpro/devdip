package com.banking.forms.bff.admin.api;

import com.banking.forms.pipeline.application.PipelineDefinitionService;
import com.banking.forms.pipeline.application.PipelineDefinitionService.CreatePipelineRequest;
import com.banking.forms.pipeline.application.PipelineDefinitionService.PipelineDefinitionDetailView;
import com.banking.forms.pipeline.application.PipelineDefinitionService.PipelineDefinitionView;
import com.banking.forms.pipeline.application.PipelineDefinitionService.PipelineStepRequest;
import com.banking.forms.pipeline.application.PipelineDefinitionService.PipeletDefinitionView;
import com.banking.forms.pipeline.domain.PipelineTrigger;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/v1")
public class AdminPipelineDefinitionController {

    private final PipelineDefinitionService pipelineService;

    public AdminPipelineDefinitionController(PipelineDefinitionService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @GetMapping("/pipelets")
    public List<PipeletDefinitionView> listPipelets() {
        return pipelineService.listPipelets();
    }

    @GetMapping("/pipelines")
    public List<PipelineDefinitionView> listPipelines(@RequestHeader("X-Tenant-Id") UUID tenantId) {
        return pipelineService.listPipelines(tenantId);
    }

    @GetMapping("/pipelines/{id}")
    public PipelineDefinitionDetailView getPipeline(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable("id") UUID id) {
        return pipelineService.getPipeline(tenantId, id);
    }

    @PostMapping("/pipelines")
    public ResponseEntity<PipelineDefinitionDetailView> createPipeline(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @Valid @RequestBody CreatePipelineApiRequest request) {
        PipelineDefinitionDetailView created = pipelineService.createPipeline(
                tenantId,
                new CreatePipelineRequest(request.code(), request.name(), request.description(), request.steps()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/pipelines/{id}/steps")
    public PipelineDefinitionDetailView updateSteps(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateStepsRequest request) {
        return pipelineService.updateSteps(tenantId, id, request.steps());
    }

    @GetMapping("/forms/{formId}/versions/{versionId}/pipeline-bindings")
    public List<PipelineDefinitionService.FormPipelineBindingView> listBindings(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable("versionId") UUID versionId) {
        return pipelineService.listBindings(tenantId, versionId);
    }

    @PutMapping("/forms/{formId}/versions/{versionId}/pipeline-bindings")
    public PipelineDefinitionService.FormPipelineBindingView upsertBinding(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody UpsertBindingApiRequest request) {
        return pipelineService.upsertBinding(
                tenantId,
                versionId,
                new PipelineDefinitionService.UpsertBindingRequest(
                        request.pipelineId(), request.trigger(), request.enabled()));
    }

    public record CreatePipelineApiRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            @NotNull List<PipelineStepRequest> steps) {}

    public record UpdateStepsRequest(@NotNull List<PipelineStepRequest> steps) {}

    public record UpsertBindingApiRequest(
            @NotNull UUID pipelineId, @NotNull PipelineTrigger trigger, boolean enabled) {}
}
