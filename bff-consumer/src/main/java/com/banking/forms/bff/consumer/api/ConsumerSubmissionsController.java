package com.banking.forms.bff.consumer.api;

import com.banking.forms.discovery.application.DiscoveryService;
import com.banking.forms.formdefinition.application.FormQueryService;
import com.banking.forms.pipeline.application.PipelineSubmitCoordinator;
import com.banking.forms.submission.application.SubmissionDetailView;
import com.banking.forms.submission.application.SubmissionNotFoundException;
import com.banking.forms.submission.application.SubmissionService;
import com.banking.forms.submission.application.SubmissionSummaryView;
import com.banking.forms.submission.application.SubmissionValidationException;
import com.banking.forms.submission.domain.Submission;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/consumer/v1/submissions")
public class ConsumerSubmissionsController {

    private final SubmissionService submissionService;
    private final FormQueryService formQueryService;
    private final DiscoveryService discoveryService;
    private final PipelineSubmitCoordinator pipelineSubmitCoordinator;

    public ConsumerSubmissionsController(
            SubmissionService submissionService,
            FormQueryService formQueryService,
            DiscoveryService discoveryService,
            PipelineSubmitCoordinator pipelineSubmitCoordinator) {
        this.submissionService = submissionService;
        this.formQueryService = formQueryService;
        this.discoveryService = discoveryService;
        this.pipelineSubmitCoordinator = pipelineSubmitCoordinator;
    }

    @PostMapping
    public SubmissionCreatedResponse createDraft(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Dev-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody CreateSubmissionRequest request) {
        UUID userId = DevRequestContext.resolveUserId(userIdHeader);

        Map<String, Map<String, Object>> prefill = request.discoverySessionId() == null
                ? Map.of()
                : discoveryService.buildPrefill(tenantId, request.discoverySessionId(), request.formCode());

        Submission submission = submissionService.createDraft(tenantId, userId, request.formCode(), prefill);
        var form = formQueryService
                .findPublishedByVersionId(submission.getFormVersionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form not found"));
        return new SubmissionCreatedResponse(
                submission.getId(), submission.getStatus().name(), form.code(), form.schema());
    }

    @GetMapping
    public List<SubmissionSummaryView> listMySubmissions(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Dev-User-Id", required = false) String userIdHeader) {
        UUID userId = DevRequestContext.resolveUserId(userIdHeader);
        return submissionService.listSubmissions(tenantId, userId);
    }

    @GetMapping("/{id}")
    public SubmissionDetailResponse getSubmission(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable("id") UUID id) {
        try {
            SubmissionDetailView detail = submissionService.getSubmission(tenantId, id);
            JsonNode schema = formQueryService
                    .findPublishedByVersionId(detail.formVersionId())
                    .map(form -> form.schema())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form not found"));
            return new SubmissionDetailResponse(
                    detail.id(),
                    detail.status(),
                    detail.formCode(),
                    detail.formName(),
                    schema,
                    detail.currentSectionKey(),
                    detail.sectionData());
        } catch (SubmissionNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PutMapping("/{id}/sections/{sectionKey}")
    public ResponseEntity<Void> saveSection(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable("id") UUID id,
            @PathVariable("sectionKey") String sectionKey,
            @Valid @RequestBody SaveSectionRequest request) {
        try {
            submissionService.saveSection(tenantId, id, sectionKey, request.data(), request.resumeSectionKey());
            return ResponseEntity.noContent().build();
        } catch (SubmissionNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (SubmissionValidationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> discardDraft(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Dev-User-Id", required = false) String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID userId = DevRequestContext.resolveUserId(userIdHeader);
        try {
            submissionService.discardDraft(tenantId, id, userId);
            return ResponseEntity.noContent().build();
        } catch (SubmissionNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (SubmissionValidationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<SubmitResponse> submit(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable("id") UUID id) {
        try {
            submissionService.submit(tenantId, id, idempotencyKey);
            // Async mode: pipeline runs off the request path via outbox + worker (returns SUBMITTED).
            // Sync mode (pipeline.process-mode=sync): runs inline and returns post-pipeline status.
            pipelineSubmitCoordinator.onSubmitted(tenantId, id);
            SubmissionDetailView detail = submissionService.getSubmission(tenantId, id);
            return ResponseEntity.accepted().body(new SubmitResponse(id, detail.status()));
        } catch (SubmissionNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (SubmissionValidationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    public record CreateSubmissionRequest(@NotBlank String formCode, UUID discoverySessionId) {}

    public record SaveSectionRequest(@NotNull Map<String, Object> data, String resumeSectionKey) {}

    public record SubmissionCreatedResponse(UUID submissionId, String status, String formCode, JsonNode schema) {}

    public record SubmissionDetailResponse(
            UUID id,
            String status,
            String formCode,
            String formName,
            JsonNode schema,
            String currentSectionKey,
            Map<String, Map<String, Object>> sectionData) {}

    public record SubmitResponse(UUID submissionId, String status) {}
}
