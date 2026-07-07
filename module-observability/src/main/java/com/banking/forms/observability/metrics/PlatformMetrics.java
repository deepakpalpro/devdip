package com.banking.forms.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/** Custom business metrics for pipeline runs and API traffic (US-9.2). */
@Component
public class PlatformMetrics {

    private final Counter pipelineCompleted;
    private final Counter pipelineFailed;
    private final Counter pipelineSkipped;
    private final Timer pipelineDuration;
    private final Counter httpRequests;

    public PlatformMetrics(MeterRegistry registry) {
        this.pipelineCompleted = Counter.builder("banking.pipeline.runs")
                .tag("outcome", "completed")
                .description("Pipeline runs that completed successfully")
                .register(registry);
        this.pipelineFailed = Counter.builder("banking.pipeline.runs")
                .tag("outcome", "failed")
                .description("Pipeline runs that failed")
                .register(registry);
        this.pipelineSkipped = Counter.builder("banking.pipeline.runs")
                .tag("outcome", "skipped")
                .description("Pipeline runs skipped due to submission status")
                .register(registry);
        this.pipelineDuration = Timer.builder("banking.pipeline.duration")
                .description("Pipeline processing duration")
                .register(registry);
        this.httpRequests = Counter.builder("banking.http.requests")
                .description("HTTP requests handled by the platform")
                .register(registry);
    }

    public Timer.Sample startPipelineTimer() {
        return Timer.start();
    }

    public void recordPipelineRun(String outcome, Timer.Sample sample) {
        sample.stop(pipelineDuration);
        switch (outcome) {
            case "COMPLETED" -> pipelineCompleted.increment();
            case "FAILED" -> pipelineFailed.increment();
            default -> pipelineSkipped.increment();
        }
    }

    public void recordHttpRequest() {
        httpRequests.increment();
    }
}
