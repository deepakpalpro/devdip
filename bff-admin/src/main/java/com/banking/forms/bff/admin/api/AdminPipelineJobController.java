package com.banking.forms.bff.admin.api;

import com.banking.forms.pipeline.application.PipelineJobRunner;
import com.banking.forms.pipeline.application.PipelineJobService;
import com.banking.forms.pipeline.application.PipelineJobService.CreateJobRequest;
import com.banking.forms.pipeline.application.PipelineJobService.PipelineJobView;
import com.banking.forms.pipeline.application.PipelineJobService.UpdateJobRequest;
import com.banking.forms.pipeline.domain.PipelineJobType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/v1")
public class AdminPipelineJobController {

    private final PipelineJobService jobService;

    public AdminPipelineJobController(PipelineJobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/pipeline-jobs")
    public List<PipelineJobView> listJobs(@RequestHeader("X-Tenant-Id") UUID tenantId) {
        return jobService.listJobs(tenantId);
    }

    @GetMapping("/forms/{formId}/versions/{versionId}/pipeline-jobs")
    public List<PipelineJobView> listJobsForForm(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable("versionId") UUID versionId) {
        return jobService.listJobsForForm(tenantId, versionId);
    }

    @PostMapping("/pipeline-jobs")
    public ResponseEntity<PipelineJobView> createJob(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @Valid @RequestBody CreateJobApiRequest request) {
        PipelineJobView created = jobService.createJob(
                tenantId,
                new CreateJobRequest(
                        request.code(),
                        request.name(),
                        request.jobType(),
                        request.formVersionId(),
                        request.pipelineId(),
                        request.triggerEvent(),
                        request.queryConfig(),
                        request.scheduleCron(),
                        request.enabled()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/pipeline-jobs/{code}")
    public PipelineJobView updateJob(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable("code") String code,
            @Valid @RequestBody UpdateJobApiRequest request) {
        return jobService.updateJob(
                tenantId,
                code,
                new UpdateJobRequest(
                        request.name(),
                        request.formVersionId(),
                        request.pipelineId(),
                        request.triggerEvent(),
                        request.queryConfig(),
                        request.scheduleCron(),
                        request.enabled()));
    }

    @PostMapping("/pipeline-jobs/{code}/trigger")
    public PipelineJobRunner.PipelineJobRunView triggerJob(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable("code") String code,
            @RequestParam(value = "submissionId", required = false) UUID submissionId) {
        return jobService.triggerJob(tenantId, code, submissionId);
    }

    @GetMapping("/pipeline-jobs/{code}/runs")
    public List<PipelineJobRunner.PipelineJobRunView> listRuns(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable("code") String code) {
        return jobService.listRuns(tenantId, code);
    }

    public record CreateJobApiRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotNull PipelineJobType jobType,
            UUID formVersionId,
            @NotNull UUID pipelineId,
            String triggerEvent,
            JsonNode queryConfig,
            String scheduleCron,
            boolean enabled) {}

    public record UpdateJobApiRequest(
            @NotBlank String name,
            UUID formVersionId,
            @NotNull UUID pipelineId,
            String triggerEvent,
            JsonNode queryConfig,
            String scheduleCron,
            boolean enabled) {}
}
