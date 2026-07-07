package com.banking.forms.pipeline.application;

import com.banking.forms.formdefinition.application.FormQueryService;
import com.banking.forms.formdefinition.application.PublishedFormView;
import com.banking.forms.pipeline.domain.PipelineStep;
import com.banking.forms.pipeline.domain.PipelineTrigger;
import com.banking.forms.pipeline.spi.Pipelet;
import com.banking.forms.pipeline.spi.PipeletConfig;
import com.banking.forms.pipeline.spi.PipeletContext;
import com.banking.forms.pipeline.spi.PipeletResult;
import com.banking.forms.submission.application.SectionStorageRouter;
import com.banking.forms.submission.application.SubmissionNotFoundException;
import com.banking.forms.submission.domain.Submission;
import com.banking.forms.submission.domain.SubmissionStatus;
import com.banking.forms.submission.infrastructure.SubmissionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.banking.forms.pipeline.domain.PipelineExecution;
import com.banking.forms.pipeline.infrastructure.PipelineExecutionRepository;

@Service
public class PipelineOrchestrator {

    private final SubmissionRepository submissionRepository;
    private final SectionStorageRouter sectionStorageRouter;
    private final FormQueryService formQueryService;
    private final PipeletRegistry pipeletRegistry;
    private final PipelineExecutionRepository executionRepository;
    private final PipelineTimelineRecorder timeline;
    private final ObjectMapper objectMapper;

    public PipelineOrchestrator(
            SubmissionRepository submissionRepository,
            SectionStorageRouter sectionStorageRouter,
            FormQueryService formQueryService,
            PipeletRegistry pipeletRegistry,
            PipelineExecutionRepository executionRepository,
            PipelineTimelineRecorder timeline,
            ObjectMapper objectMapper) {
        this.submissionRepository = submissionRepository;
        this.sectionStorageRouter = sectionStorageRouter;
        this.formQueryService = formQueryService;
        this.pipeletRegistry = pipeletRegistry;
        this.executionRepository = executionRepository;
        this.timeline = timeline;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PipelineResult execute(
            UUID tenantId, UUID submissionId, ResolvedPipeline pipeline, PipelineTrigger trigger) {
        Submission submission = loadOwned(tenantId, submissionId);
        if (trigger == PipelineTrigger.ON_SUBMIT && submission.getStatus() != SubmissionStatus.SUBMITTED) {
            return PipelineResult.skipped(submission.getStatus().name());
        }

        PublishedFormView form = formQueryService
                .findPublishedByVersionId(submission.getFormVersionId())
                .orElseThrow(() -> new PipelineConfigurationException("Published form version not available"));

        Map<String, Map<String, Object>> sectionData =
                sectionStorageRouter.resolve(form.storageStrategy()).loadAllSections(submissionId);

        PipelineExecution execution =
                executionRepository.save(new PipelineExecution(UUID.randomUUID(), submissionId, pipeline.definition().getId()));

        timeline.record(
                submissionId,
                "PIPELINE_STARTED",
                "pipelineCode",
                pipeline.definition().getCode(),
                "trigger",
                trigger.name(),
                "steps",
                stepSummary(pipeline.steps()));

        PipeletContext context = new PipeletContext(tenantId, submissionId, trigger, submission, form, sectionData);
        context.setPipelineDefinitionId(pipeline.definition().getId());

        try {
            int stepIndex = 0;
            for (PipelineStep step : pipeline.steps()) {
                stepIndex++;
                context.setPipelineStepId(step.getId());
                Pipelet pipelet = pipeletRegistry.require(step.getPipeletCode());
                PipeletConfig config = parseConfig(step.getPropertiesJson());
                PipeletResult result = pipelet.execute(context, config);
                if (!result.isSuccess()) {
                    throw new RuntimeException(result.detail() == null ? "Pipelet failed: " + step.getPipeletCode() : result.detail());
                }
                execution.advanceTo(stepIndex);
            }

            if (trigger == PipelineTrigger.ON_SUBMIT) {
                submission.markUnderReview(Instant.now());
                submissionRepository.save(submission);
            }

            execution.complete(pipeline.steps().size());
            executionRepository.save(execution);
            timeline.record(
                    submissionId,
                    "PIPELINE_COMPLETED",
                    "pipelineCode",
                    pipeline.definition().getCode(),
                    "to",
                    trigger == PipelineTrigger.ON_SUBMIT ? SubmissionStatus.PENDING_REVIEW.name() : submission.getStatus().name());
            int transformed = context.scrubResult() == null ? 0 : context.scrubResult().transformedCount();
            return PipelineResult.completed(transformed);
        } catch (RuntimeException ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            if (trigger == PipelineTrigger.ON_SUBMIT) {
                submission.revertToSubmitted(Instant.now());
                submissionRepository.save(submission);
            }
            execution.fail(execution.getCurrentStep(), message);
            executionRepository.save(execution);
            timeline.record(submissionId, "PIPELINE_FAILED", "error", message, "atStep", execution.getCurrentStep());
            return PipelineResult.failed(message);
        }
    }

    private Submission loadOwned(UUID tenantId, UUID submissionId) {
        return submissionRepository
                .findById(submissionId)
                .filter(candidate -> candidate.getTenantId().equals(tenantId))
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));
    }

    private PipeletConfig parseConfig(String json) {
        if (json == null || json.isBlank()) {
            return new PipeletConfig(null);
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            return new PipeletConfig(node);
        } catch (Exception ex) {
            return new PipeletConfig(null);
        }
    }

    private static String stepSummary(List<PipelineStep> steps) {
        return steps.stream().map(PipelineStep::getPipeletCode).collect(Collectors.joining(","));
    }
}
