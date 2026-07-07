package com.banking.forms.pipeline.application.pipelet;

import com.banking.forms.pipeline.application.AiEvaluatorRouter;
import com.banking.forms.pipeline.application.PipelineTimelineRecorder;
import com.banking.forms.pipeline.domain.AiEvaluation;
import com.banking.forms.pipeline.infrastructure.AiEvaluationRepository;
import com.banking.forms.pipeline.spi.AiEvaluationContext;
import com.banking.forms.pipeline.spi.AiEvaluationResult;
import com.banking.forms.pipeline.spi.Pipelet;
import com.banking.forms.pipeline.spi.PipeletConfig;
import com.banking.forms.pipeline.spi.PipeletContext;
import com.banking.forms.pipeline.spi.PipeletResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AiEvaluatePipelet implements Pipelet {

    private final AiEvaluatorRouter aiEvaluatorRouter;
    private final AiEvaluationRepository aiEvaluationRepository;
    private final PipelineTimelineRecorder timeline;
    private final ObjectMapper objectMapper;

    public AiEvaluatePipelet(
            AiEvaluatorRouter aiEvaluatorRouter,
            AiEvaluationRepository aiEvaluationRepository,
            PipelineTimelineRecorder timeline,
            ObjectMapper objectMapper) {
        this.aiEvaluatorRouter = aiEvaluatorRouter;
        this.aiEvaluationRepository = aiEvaluationRepository;
        this.timeline = timeline;
        this.objectMapper = objectMapper;
    }

    @Override
    public String code() {
        return "ai-evaluate";
    }

    @Override
    public PipeletResult execute(PipeletContext context, PipeletConfig config) {
        if (!config.bool("enabled", aiEvaluatorRouter.isEnabled())) {
            timeline.record(context.submissionId(), "AI_EVALUATION_SKIPPED", "reason", "disabled");
            return PipeletResult.skipped("disabled");
        }
        if (context.scrubResult() == null) {
            return PipeletResult.failed("PII scrub must run before AI evaluate");
        }
        AiEvaluationContext aiContext = new AiEvaluationContext(
                context.submissionId(),
                context.form().code(),
                context.scrubResult().sanitized(),
                Map.of("formCode", context.form().code()));
        AiEvaluationResult aiResult = aiEvaluatorRouter.evaluate(aiContext);
        persistAiEvaluation(context.submissionId(), aiResult);
        context.setRiskEvaluation(aiResult.riskScore(), aiResult.recommendation().name());
        timeline.record(
                context.submissionId(),
                "AI_EVALUATED",
                "recommendation",
                aiResult.recommendation().name(),
                "riskScore",
                aiResult.riskScore(),
                "evaluator",
                aiResult.evaluatorId());
        return PipeletResult.success();
    }

    private void persistAiEvaluation(UUID submissionId, AiEvaluationResult result) {
        String signalsJson = writeJson(result.signals());
        aiEvaluationRepository
                .findBySubmissionId(submissionId)
                .ifPresentOrElse(
                        existing -> existing.update(
                                result.evaluatorId(),
                                result.model(),
                                result.riskScore(),
                                result.recommendation().name(),
                                result.rationale(),
                                signalsJson,
                                result.processingTimeMs()),
                        () -> aiEvaluationRepository.save(new AiEvaluation(
                                UUID.randomUUID(),
                                submissionId,
                                result.evaluatorId(),
                                result.model(),
                                result.riskScore(),
                                result.recommendation().name(),
                                result.rationale(),
                                signalsJson,
                                result.processingTimeMs())));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize AI signals", ex);
        }
    }
}
