package com.banking.forms.bff.admin.api;

import com.banking.forms.formdefinition.application.FormAdminSummaryView;
import com.banking.forms.formdefinition.application.FormCommandService;
import com.banking.forms.formdefinition.application.FormDetailView;
import com.banking.forms.formdefinition.application.FormVersionView;
import com.banking.forms.formdefinition.domain.StorageStrategy;
import com.fasterxml.jackson.databind.JsonNode;
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

/** Admin form authoring: create definitions, add/edit draft versions, and publish. */
@RestController
@RequestMapping("/api/admin/v1/forms")
public class AdminFormsController {

    private final FormCommandService formCommandService;

    public AdminFormsController(FormCommandService formCommandService) {
        this.formCommandService = formCommandService;
    }

    @GetMapping
    public List<FormAdminSummaryView> listForms(@RequestHeader("X-Tenant-Id") UUID tenantId) {
        return formCommandService.listForms(tenantId);
    }

    @GetMapping("/{id}")
    public FormDetailView getForm(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable("id") UUID id) {
        return formCommandService.getForm(tenantId, id);
    }

    @PostMapping
    public ResponseEntity<FormDetailView> createForm(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Dev-User-Id", required = false) String actorHeader,
            @Valid @RequestBody CreateFormRequest request) {
        UUID actorId = AdminRequestContext.resolveActorId(actorHeader);
        FormDetailView created = formCommandService.createDefinition(
                tenantId, actorId, request.code(), request.name(), request.category(), request.storageStrategy());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/versions")
    public ResponseEntity<FormVersionView> createVersion(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Dev-User-Id", required = false) String actorHeader,
            @PathVariable("id") UUID id,
            @RequestBody(required = false) CreateVersionRequest request) {
        UUID actorId = AdminRequestContext.resolveActorId(actorHeader);
        UUID cloneFrom = request == null ? null : request.cloneFromVersionId();
        FormVersionView created = formCommandService.createVersion(tenantId, actorId, id, cloneFrom);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/versions/{versionId}")
    public FormVersionView updateVersionSchema(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable("id") UUID id,
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody UpdateSchemaRequest request) {
        return formCommandService.updateDraftSchema(tenantId, id, versionId, request.schema());
    }

    @PostMapping("/{id}/versions/{versionId}/publish")
    public FormVersionView publishVersion(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable("id") UUID id,
            @PathVariable("versionId") UUID versionId) {
        return formCommandService.publish(tenantId, id, versionId);
    }

    public record CreateFormRequest(
            @NotBlank String code,
            @NotBlank String name,
            String category,
            StorageStrategy storageStrategy) {}

    public record CreateVersionRequest(UUID cloneFromVersionId) {}

    public record UpdateSchemaRequest(@NotNull JsonNode schema) {}
}
