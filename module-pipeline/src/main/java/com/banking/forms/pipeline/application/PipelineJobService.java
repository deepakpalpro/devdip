package com.banking.forms.pipeline.application;

import com.banking.forms.pipeline.domain.PipelineJobDefinition;
import com.banking.forms.pipeline.domain.PipelineJobType;
import com.banking.forms.pipeline.infrastructure.PipelineJobDefinitionRepository;
import com.banking.forms.pipeline.infrastructure.PipelineJobRunRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PipelineJobService {

    private final PipelineJobDefinitionRepository jobRepository;
    private final PipelineJobRunRepository runRepository;
    private final PipelineJobRunner jobRunner;
    private final ObjectMapper objectMapper;

    public PipelineJobService(
            PipelineJobDefinitionRepository jobRepository,
            PipelineJobRunRepository runRepository,
            PipelineJobRunner jobRunner,
            ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.runRepository = runRepository;
        this.jobRunner = jobRunner;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PipelineJobView> listJobs(UUID tenantId) {
        return jobRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PipelineJobView> listJobsForForm(UUID tenantId, UUID formVersionId) {
        return jobRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .filter(job -> formVersionId.equals(job.getFormVersionId()))
                .map(this::toView)
                .toList();
    }

    public PipelineJobView createJob(UUID tenantId, CreateJobRequest request) {
        if (jobRepository.findByTenantIdAndCode(tenantId, request.code()).isPresent()) {
            throw new PipelineConfigurationException("Job code already exists: " + request.code());
        }
        PipelineJobDefinition job = jobRepository.save(new PipelineJobDefinition(
                UUID.randomUUID(),
                tenantId,
                request.formVersionId(),
                request.code(),
                request.name(),
                request.jobType(),
                request.pipelineId(),
                request.triggerEvent(),
                writeJson(request.queryConfig()),
                request.scheduleCron()));
        if (!request.enabled()) {
            job.update(
                    job.getName(),
                    job.getFormVersionId(),
                    job.getPipelineDefinitionId(),
                    job.getTriggerEvent(),
                    job.getQueryConfigJson(),
                    job.getScheduleCron(),
                    false);
            jobRepository.save(job);
        }
        return toView(job);
    }

    public PipelineJobView updateJob(UUID tenantId, String code, UpdateJobRequest request) {
        PipelineJobDefinition job = loadOwned(tenantId, code);
        job.update(
                request.name(),
                request.formVersionId(),
                request.pipelineId(),
                request.triggerEvent(),
                writeJson(request.queryConfig()),
                request.scheduleCron(),
                request.enabled());
        return toView(jobRepository.save(job));
    }

    public PipelineJobRunner.PipelineJobRunView triggerJob(UUID tenantId, String code, UUID submissionId) {
        PipelineJobDefinition job = loadOwned(tenantId, code);
        return jobRunner.run(tenantId, job.getId(), submissionId);
    }

    @Transactional(readOnly = true)
    public List<PipelineJobRunner.PipelineJobRunView> listRuns(UUID tenantId, String code) {
        PipelineJobDefinition job = loadOwned(tenantId, code);
        return runRepository.findByJobDefinitionIdOrderByStartedAtDesc(job.getId()).stream()
                .map(PipelineJobRunner::toView)
                .toList();
    }

    private PipelineJobDefinition loadOwned(UUID tenantId, String code) {
        return jobRepository
                .findByTenantIdAndCode(tenantId, code)
                .filter(job -> job.getTenantId().equals(tenantId))
                .orElseThrow(() -> new PipelineConfigurationException("Unknown pipeline job: " + code));
    }

    private PipelineJobView toView(PipelineJobDefinition job) {
        return new PipelineJobView(
                job.getId(),
                job.getCode(),
                job.getName(),
                job.getJobType(),
                job.getFormVersionId(),
                job.getPipelineDefinitionId(),
                job.getTriggerEvent(),
                readJson(job.getQueryConfigJson()),
                job.getScheduleCron(),
                job.isEnabled(),
                job.getLastRunAt());
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
            throw new PipelineConfigurationException("Invalid query config JSON");
        }
    }

    public record PipelineJobView(
            UUID id,
            String code,
            String name,
            PipelineJobType jobType,
            UUID formVersionId,
            UUID pipelineId,
            String triggerEvent,
            JsonNode queryConfig,
            String scheduleCron,
            boolean enabled,
            java.time.Instant lastRunAt) {}

    public record CreateJobRequest(
            String code,
            String name,
            PipelineJobType jobType,
            UUID formVersionId,
            UUID pipelineId,
            String triggerEvent,
            JsonNode queryConfig,
            String scheduleCron,
            boolean enabled) {}

    public record UpdateJobRequest(
            String name,
            UUID formVersionId,
            UUID pipelineId,
            String triggerEvent,
            JsonNode queryConfig,
            String scheduleCron,
            boolean enabled) {}
}
