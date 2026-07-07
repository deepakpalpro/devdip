package com.banking.forms.pipeline.application.pipelet;

import com.banking.forms.pipeline.application.PipelineTimelineRecorder;
import com.banking.forms.pipeline.domain.PipelineTrigger;
import com.banking.forms.pipeline.spi.Pipelet;
import com.banking.forms.pipeline.spi.PipeletConfig;
import com.banking.forms.pipeline.spi.PipeletContext;
import com.banking.forms.pipeline.spi.PipeletResult;
import com.banking.forms.submission.application.SectionValidator;
import com.banking.forms.submission.application.SubmissionValidationException;
import com.banking.forms.submission.infrastructure.SubmissionRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ValidatePipelet implements Pipelet {

    private final SectionValidator sectionValidator;
    private final SubmissionRepository submissionRepository;
    private final PipelineTimelineRecorder timeline;

    public ValidatePipelet(
            SectionValidator sectionValidator,
            SubmissionRepository submissionRepository,
            PipelineTimelineRecorder timeline) {
        this.sectionValidator = sectionValidator;
        this.submissionRepository = submissionRepository;
        this.timeline = timeline;
    }

    @Override
    public String code() {
        return "validate";
    }

    @Override
    public PipeletResult execute(PipeletContext context, PipeletConfig config) {
        if (context.trigger() == PipelineTrigger.ON_SUBMIT) {
            context.submission().markValidating(Instant.now());
            submissionRepository.save(context.submission());
        }
        try {
            sectionValidator.validateAllSections(context.form().schema(), context.sectionData());
        } catch (SubmissionValidationException ex) {
            return PipeletResult.failed(ex.getMessage());
        }
        timeline.record(
                context.submissionId(),
                "VALIDATED",
                "sections",
                context.sectionData().size(),
                "trigger",
                context.trigger().name());
        return PipeletResult.success();
    }
}
