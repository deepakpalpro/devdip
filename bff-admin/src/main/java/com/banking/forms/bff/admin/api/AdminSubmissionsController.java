package com.banking.forms.bff.admin.api;

import com.banking.forms.formdefinition.application.FormQueryService;
import com.banking.forms.submission.application.SubmissionDetailView;
import com.banking.forms.submission.application.SubmissionEventView;
import com.banking.forms.submission.application.SubmissionNotFoundException;
import com.banking.forms.submission.application.SubmissionService;
import com.banking.forms.submission.application.SubmissionSummaryView;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Admin-facing read access to consumer submissions (list + full detail for review). */
@RestController
@RequestMapping("/api/admin/v1/submissions")
public class AdminSubmissionsController {

    private final SubmissionService submissionService;
    private final FormQueryService formQueryService;

    public AdminSubmissionsController(SubmissionService submissionService, FormQueryService formQueryService) {
        this.submissionService = submissionService;
        this.formQueryService = formQueryService;
    }

    @GetMapping
    public List<SubmissionSummaryView> listSubmissions(@RequestHeader("X-Tenant-Id") UUID tenantId) {
        return submissionService.listSubmissions(tenantId);
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
            List<SubmissionEventView> timeline = submissionService.getTimeline(tenantId, id);
            return new SubmissionDetailResponse(
                    detail.id(),
                    detail.status(),
                    detail.formCode(),
                    detail.formName(),
                    schema,
                    detail.sectionData(),
                    timeline);
        } catch (SubmissionNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    public record SubmissionDetailResponse(
            UUID id,
            String status,
            String formCode,
            String formName,
            JsonNode schema,
            Map<String, Map<String, Object>> sectionData,
            List<SubmissionEventView> timeline) {}
}
