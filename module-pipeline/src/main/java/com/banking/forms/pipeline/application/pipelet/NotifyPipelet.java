package com.banking.forms.pipeline.application.pipelet;

import com.banking.forms.pipeline.application.PipelineTimelineRecorder;
import com.banking.forms.pipeline.spi.Pipelet;
import com.banking.forms.pipeline.spi.PipeletConfig;
import com.banking.forms.pipeline.spi.PipeletContext;
import com.banking.forms.pipeline.spi.PipeletResult;
import org.springframework.stereotype.Component;

/** Placeholder pipelet — notifications remain lifecycle-driven until 6.2 wiring. */
@Component
public class NotifyPipelet implements Pipelet {

    private final PipelineTimelineRecorder timeline;

    public NotifyPipelet(PipelineTimelineRecorder timeline) {
        this.timeline = timeline;
    }

    @Override
    public String code() {
        return "notify";
    }

    @Override
    public PipeletResult execute(PipeletContext context, PipeletConfig config) {
        timeline.record(
                context.submissionId(),
                "NOTIFICATION_SKIPPED",
                "reason",
                "notify pipelet deferred to lifecycle module",
                "event",
                config.text("event", context.trigger().name()));
        return PipeletResult.skipped("deferred");
    }
}
