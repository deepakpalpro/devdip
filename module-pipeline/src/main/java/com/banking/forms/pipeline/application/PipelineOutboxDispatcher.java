package com.banking.forms.pipeline.application;

import com.banking.forms.pipeline.domain.PipelineOutboxEvent;
import com.banking.forms.pipeline.infrastructure.PipelineOutboxRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls unpublished {@code outbox_event} rows and hands each to {@link PipelineOutboxService#process}
 * (a fresh transaction per row). Decouples submit from pipeline execution without a broker; swapping
 * the poller for a Kafka consumer later is a localized change.
 */
@Component
public class PipelineOutboxDispatcher {

    private final PipelineOutboxRepository outboxRepository;
    private final PipelineOutboxService outboxService;
    private final PipelineProperties properties;

    public PipelineOutboxDispatcher(
            PipelineOutboxRepository outboxRepository,
            PipelineOutboxService outboxService,
            PipelineProperties properties) {
        this.outboxRepository = outboxRepository;
        this.outboxService = outboxService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${pipeline.dispatch-interval-ms:3000}")
    public void dispatchPending() {
        if (!properties.isAsync()) {
            return;
        }
        Instant now = Instant.now();
        for (PipelineOutboxEvent event : pending()) {
            if (isEligible(event, now)) {
                outboxService.process(event.getId());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<PipelineOutboxEvent> pending() {
        return outboxRepository.findByPublishedFalseOrderByOccurredAtAsc(
                PageRequest.of(0, properties.getDispatchBatchSize()));
    }

    private boolean isEligible(PipelineOutboxEvent event, Instant now) {
        if (event.getAttempts() == 0) {
            return true;
        }
        long waitMs = properties.getRetryBackoffMs() * event.getAttempts();
        return event.getUpdatedAt().plusMillis(waitMs).isBefore(now);
    }
}
