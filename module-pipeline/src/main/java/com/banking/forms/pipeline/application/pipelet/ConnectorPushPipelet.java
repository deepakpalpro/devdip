package com.banking.forms.pipeline.application.pipelet;

import com.banking.forms.downstream.application.DownstreamDispatchService;
import com.banking.forms.pipeline.application.PipelineTimelineRecorder;
import com.banking.forms.pipeline.spi.Pipelet;
import com.banking.forms.pipeline.spi.PipeletConfig;
import com.banking.forms.pipeline.spi.PipeletContext;
import com.banking.forms.pipeline.spi.PipeletResult;
import java.util.List;
import org.springframework.stereotype.Component;

/** Push sanitized payload to downstream connectors; optional connector filter for dual-write control. */
@Component
public class ConnectorPushPipelet implements Pipelet {

    private final DownstreamDispatchService downstreamDispatchService;
    private final PipelineTimelineRecorder timeline;

    public ConnectorPushPipelet(
            DownstreamDispatchService downstreamDispatchService, PipelineTimelineRecorder timeline) {
        this.downstreamDispatchService = downstreamDispatchService;
        this.timeline = timeline;
    }

    @Override
    public String code() {
        return "connector-push";
    }

    @Override
    public PipeletResult execute(PipeletContext context, PipeletConfig config) {
        if (context.scrubResult() == null) {
            return PipeletResult.failed("PII scrub must run before connector push");
        }
        List<String> connectors = config.stringList("connectors");
        List<String> filter = connectors.isEmpty() ? null : connectors;
        int queued = downstreamDispatchService.enqueueForSubmission(
                context.tenantId(),
                context.submissionId(),
                context.form().code(),
                context.scrubResult().sanitized(),
                context.riskRecommendation(),
                context.riskScore(),
                filter);
        timeline.record(context.submissionId(), "CONNECTOR_PUSH_ENQUEUED", "queued", queued);
        return PipeletResult.success(String.valueOf(queued));
    }
}
