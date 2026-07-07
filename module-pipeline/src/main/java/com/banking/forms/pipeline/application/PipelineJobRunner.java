package com.banking.forms.pipeline.application;

import com.banking.forms.pipeline.domain.PipelineJobDefinition;
import com.banking.forms.pipeline.domain.PipelineJobRun;
import com.banking.forms.pipeline.domain.PipelineJobRunStatus;
import com.banking.forms.pipeline.domain.PipelineJobType;
import com.banking.forms.pipeline.domain.PipelineTrigger;
import com.banking.forms.pipeline.infrastructure.PipelineJobDefinitionRepository;
import com.banking.forms.pipeline.infrastructure.PipelineJobRunRepository;
import com.banking.forms.submission.domain.Submission;
import com.banking.forms.submission.domain.SubmissionStatus;
import com.banking.forms.submission.infrastructure.SubmissionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PipelineJobRunner {

    private final PipelineJobDefinitionRepository jobRepository;
    private final PipelineJobRunRepository runRepository;
    private final FormPipelineResolver pipelineResolver;
    private final PipelineOrchestrator orchestrator;
    private final SubmissionRepository submissionRepository;
    private final ObjectMapper objectMapper;

    public PipelineJobRunner(
            PipelineJobDefinitionRepository jobRepository,
            PipelineJobRunRepository runRepository,
            FormPipelineResolver pipelineResolver,
            PipelineOrchestrator orchestrator,
            SubmissionRepository submissionRepository,
            ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.runRepository = runRepository;
        this.pipelineResolver = pipelineResolver;
        this.orchestrator = orchestrator;
        this.submissionRepository = submissionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PipelineJobRunView run(UUID tenantId, UUID jobId, UUID singleSubmissionId) {
        PipelineJobDefinition job = jobRepository
                .findById(jobId)
                .filter(candidate -> candidate.getTenantId().equals(tenantId))
                .orElseThrow(() -> new PipelineConfigurationException("Unknown pipeline job: " + jobId));

        ResolvedPipeline pipeline = pipelineResolver
                .loadById(tenantId, job.getPipelineDefinitionId())
                .orElseThrow(() -> new PipelineConfigurationException("Pipeline not found for job"));

        PipelineJobRun run = runRepository.save(new PipelineJobRun(UUID.randomUUID(), job.getId()));
        job.markRunStarted(Instant.now());
        jobRepository.save(job);

        PipelineTrigger trigger = parseTrigger(job.getTriggerEvent());
        List<UUID> submissionIds = resolveSubmissionIds(job, tenantId, singleSubmissionId);

        int processed = 0;
        try {
            for (UUID submissionId : submissionIds) {
                PipelineResult result = orchestrator.execute(tenantId, submissionId, pipeline, trigger);
                if (!"SKIPPED".equals(result.status())) {
                    processed++;
                }
            }
            run.complete(processed);
        } catch (RuntimeException ex) {
            run.fail(ex.getMessage());
        }
        runRepository.save(run);
        return toView(run);
    }

    private List<UUID> resolveSubmissionIds(
            PipelineJobDefinition job, UUID tenantId, UUID singleSubmissionId) {
        if (singleSubmissionId != null) {
            return List.of(singleSubmissionId);
        }
        if (job.getJobType() == PipelineJobType.REALTIME) {
            throw new PipelineConfigurationException("Real-time job requires a submission id");
        }
        if (job.getFormVersionId() == null) {
            throw new PipelineConfigurationException("Batch job requires formVersionId");
        }
        String statusFilter = readQueryField(job.getQueryConfigJson(), "status");
        List<Submission> submissions;
        if (statusFilter != null && !statusFilter.isBlank()) {
            submissions = submissionRepository.findByTenantIdAndFormVersionIdAndStatus(
                    tenantId, job.getFormVersionId(), SubmissionStatus.valueOf(statusFilter));
        } else {
            submissions = submissionRepository.findByTenantIdAndFormVersionId(tenantId, job.getFormVersionId());
        }
        List<UUID> ids = new ArrayList<>();
        for (Submission submission : submissions) {
            ids.add(submission.getId());
        }
        return ids;
    }

    private PipelineTrigger parseTrigger(String triggerEvent) {
        if (triggerEvent == null || triggerEvent.isBlank()) {
            return PipelineTrigger.ON_STATUS_CHANGE;
        }
        return PipelineTrigger.valueOf(triggerEvent);
    }

    private String readQueryField(String json, String field) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode value = node.get(field);
            return value == null || value.isNull() ? null : value.asText();
        } catch (Exception ex) {
            return null;
        }
    }

    static PipelineJobRunView toView(PipelineJobRun run) {
        return new PipelineJobRunView(
                run.getId(),
                run.getJobDefinitionId(),
                run.getStatus(),
                run.getRecordsProcessed(),
                run.getErrorMessage(),
                run.getStartedAt(),
                run.getFinishedAt());
    }

    public record PipelineJobRunView(
            UUID id,
            UUID jobDefinitionId,
            PipelineJobRunStatus status,
            int recordsProcessed,
            String errorMessage,
            Instant startedAt,
            Instant finishedAt) {}
}
