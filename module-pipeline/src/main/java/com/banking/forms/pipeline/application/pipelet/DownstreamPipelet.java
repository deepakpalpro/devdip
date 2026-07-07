package com.banking.forms.pipeline.application.pipelet;

import com.banking.forms.downstream.application.DownstreamDispatchService;
import com.banking.forms.pipeline.application.PipelineTimelineRecorder;
import com.banking.forms.pipeline.spi.Pipelet;
import com.banking.forms.pipeline.spi.PipeletConfig;
import com.banking.forms.pipeline.spi.PipeletContext;
import com.banking.forms.pipeline.spi.PipeletResult;
import org.springframework.stereotype.Component;

@Component
public class DownstreamPipelet implements Pipelet {

    private final DownstreamDispatchService downstreamDispatchService;
    private final PipelineTimelineRecorder timeline;

    public DownstreamPipelet(
            DownstreamDispatchService downstreamDispatchService, PipelineTimelineRecorder timeline) {
        this.downstreamDispatchService = downstreamDispatchService;
        this.timeline = timeline;
    }

    @Override
    public String code() {
        return "downstream";
    }

    @Override
    public PipeletResult execute(PipeletContext context, PipeletConfig config) {
        if (context.scrubResult() == null) {
            return PipeletResult.failed("PII scrub must run before downstream dispatch");
        }
        int queued = downstreamDispatchService.enqueueForSubmission(
                context.tenantId(),
                context.submissionId(),
                context.form().code(),
                context.scrubResult().sanitized(),
                context.riskRecommendation(),
                context.riskScore());
        timeline.record(context.submissionId(), "DOWNSTREAM_ENQUEUED", "queued", queued);
        return PipeletResult.success(String.valueOf(queued));
    }
}
