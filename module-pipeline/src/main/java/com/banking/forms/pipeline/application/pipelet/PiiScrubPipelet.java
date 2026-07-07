package com.banking.forms.pipeline.application.pipelet;

import com.banking.forms.pipeline.application.PipelineTimelineRecorder;
import com.banking.forms.pipeline.domain.PipelineTrigger;
import com.banking.forms.pipeline.domain.SanitizedPayload;
import com.banking.forms.pipeline.infrastructure.SanitizedPayloadRepository;
import com.banking.forms.pipeline.spi.Pipelet;
import com.banking.forms.pipeline.spi.PipeletConfig;
import com.banking.forms.pipeline.spi.PipeletContext;
import com.banking.forms.pipeline.spi.PipeletResult;
import com.banking.forms.submission.infrastructure.SubmissionRepository;
import com.banking.forms.transformation.application.PiiScrubber;
import com.banking.forms.transformation.application.ScrubResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PiiScrubPipelet implements Pipelet {

    private final PiiScrubber piiScrubber;
    private final SanitizedPayloadRepository sanitizedPayloadRepository;
    private final SubmissionRepository submissionRepository;
    private final PipelineTimelineRecorder timeline;
    private final ObjectMapper objectMapper;

    public PiiScrubPipelet(
            PiiScrubber piiScrubber,
            SanitizedPayloadRepository sanitizedPayloadRepository,
            SubmissionRepository submissionRepository,
            PipelineTimelineRecorder timeline,
            ObjectMapper objectMapper) {
        this.piiScrubber = piiScrubber;
        this.sanitizedPayloadRepository = sanitizedPayloadRepository;
        this.submissionRepository = submissionRepository;
        this.timeline = timeline;
        this.objectMapper = objectMapper;
    }

    @Override
    public String code() {
        return "pii-scrub";
    }

    @Override
    public PipeletResult execute(PipeletContext context, PipeletConfig config) {
        if (context.trigger() == PipelineTrigger.ON_SUBMIT) {
            context.submission().markProcessing(Instant.now());
            submissionRepository.save(context.submission());
        }
        ScrubResult scrub = piiScrubber.scrub(context.form().code(), context.sectionData());
        persistSanitized(context.submissionId(), scrub);
        context.setScrubResult(scrub);
        timeline.record(
                context.submissionId(),
                "PII_SCRUBBED",
                "transformedFields",
                scrub.transformedCount(),
                "profile",
                config.text("profile", "default"));
        return PipeletResult.success();
    }

    private void persistSanitized(UUID submissionId, ScrubResult scrub) {
        String payloadJson = writeJson(scrub.sanitized());
        String transformedJson = writeJson(scrub.transformed());
        sanitizedPayloadRepository
                .findBySubmissionId(submissionId)
                .ifPresentOrElse(
                        existing -> existing.update(payloadJson, transformedJson),
                        () -> sanitizedPayloadRepository.save(
                                new SanitizedPayload(UUID.randomUUID(), submissionId, payloadJson, transformedJson)));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize pipeline payload", ex);
        }
    }
}
