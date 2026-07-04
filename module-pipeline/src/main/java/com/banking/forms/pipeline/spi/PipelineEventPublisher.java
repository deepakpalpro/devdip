package com.banking.forms.pipeline.spi;

import java.util.UUID;

/**
 * Optional broker seam for pipeline outbox events. The default {@code log-inprocess} publisher is a
 * no-op pass-through — the {@code PipelineOutboxDispatcher} runs the worker in-process after dequeue.
 * A future {@code kafka} publisher would emit to a topic so separate worker instances can consume.
 */
public interface PipelineEventPublisher {

    String publisherId();

    void publish(UUID outboxId, String eventType, UUID tenantId, UUID submissionId, String payloadJson);
}
