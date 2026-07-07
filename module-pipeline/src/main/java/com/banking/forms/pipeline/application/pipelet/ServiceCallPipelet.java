package com.banking.forms.pipeline.application.pipelet;

import com.banking.forms.pipeline.application.PipelineTimelineRecorder;
import com.banking.forms.pipeline.spi.Pipelet;
import com.banking.forms.pipeline.spi.PipeletConfig;
import com.banking.forms.pipeline.spi.PipeletContext;
import com.banking.forms.pipeline.spi.PipeletResult;
import com.banking.forms.pipeline.spi.ServiceCallContext;
import com.banking.forms.pipeline.spi.ServiceCallExecutor;
import org.springframework.stereotype.Component;

@Component
public class ServiceCallPipelet implements Pipelet {

    private final ServiceCallExecutor serviceCallExecutor;
    private final PipelineTimelineRecorder timeline;

    public ServiceCallPipelet(ServiceCallExecutor serviceCallExecutor, PipelineTimelineRecorder timeline) {
        this.serviceCallExecutor = serviceCallExecutor;
        this.timeline = timeline;
    }

    @Override
    public String code() {
        return "service-call";
    }

    @Override
    public PipeletResult execute(PipeletContext context, PipeletConfig config) {
        if (!config.bool("enabled", serviceCallExecutor.isEnabled())) {
            timeline.record(context.submissionId(), "SERVICE_CALL_SKIPPED", "reason", "disabled");
            return PipeletResult.skipped("disabled");
        }
        if (context.scrubResult() == null) {
            return PipeletResult.failed("PII scrub must run before service call");
        }
        int invoked = serviceCallExecutor.invoke(new ServiceCallContext(
                context.tenantId(),
                context.submissionId(),
                context.form().code(),
                context.scrubResult().sanitized(),
                context.riskRecommendation(),
                context.riskScore(),
                context.formVersionId(),
                context.pipelineDefinitionId(),
                context.pipelineStepId()));
        timeline.record(context.submissionId(), "SERVICE_CALL_INVOKED", "invoked", invoked);
        return PipeletResult.success(String.valueOf(invoked));
    }
}
