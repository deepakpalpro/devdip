package com.banking.forms.bff.admin.api;

import com.banking.forms.pipeline.application.PipelineOutboxService;
import com.banking.forms.pipeline.application.PipelineOutboxView;
import com.banking.forms.pipeline.application.PipelineReportView;
import com.banking.forms.pipeline.application.SubmissionPipelineService;
import com.banking.forms.submission.application.SubmissionNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Admin view of the automated processing pipeline: run status + sanitized (PII-scrubbed) payload. */
@RestController
@RequestMapping("/api/admin/v1/submissions/{id}/pipeline")
public class AdminPipelineController {

    private final SubmissionPipelineService pipelineService;
    private final PipelineOutboxService outboxService;

    public AdminPipelineController(
            SubmissionPipelineService pipelineService, PipelineOutboxService outboxService) {
        this.pipelineService = pipelineService;
        this.outboxService = outboxService;
    }

    @GetMapping
    public PipelineReportView getPipeline(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable("id") UUID id) {
        try {
            return pipelineService.getReport(tenantId, id);
        } catch (SubmissionNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/outbox")
    public List<PipelineOutboxView> getOutbox(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable("id") UUID id) {
        try {
            pipelineService.getReport(tenantId, id);
            return outboxService.listForSubmission(id);
        } catch (SubmissionNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }
}
