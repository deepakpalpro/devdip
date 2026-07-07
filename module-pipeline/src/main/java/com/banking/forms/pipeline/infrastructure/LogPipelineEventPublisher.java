package com.banking.forms.pipeline.infrastructure;

import com.banking.forms.pipeline.spi.PipelineEventPublisher;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Zero-setup default publisher: logs the dequeue and relies on the in-process worker to run the
 * pipeline. Enables swapping to a Kafka/Rabbit publisher later without changing the outbox contract.
 */
@Component
public class LogPipelineEventPublisher implements PipelineEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LogPipelineEventPublisher.class);

    @Override
    public String publisherId() {
        return "log-inprocess";
    }

    @Override
    public void publish(UUID outboxId, String eventType, UUID tenantId, UUID submissionId, String payloadJson) {
        log.debug(
                "[pipeline:outbox] dequeue outboxId={} event={} submission={} payloadBytes={}",
                outboxId,
                eventType,
                submissionId,
                payloadJson == null ? 0 : payloadJson.length());
    }
}
