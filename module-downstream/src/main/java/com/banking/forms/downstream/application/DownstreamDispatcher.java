package com.banking.forms.downstream.application;

import com.banking.forms.downstream.domain.OutboxEvent;
import com.banking.forms.downstream.domain.OutboxStatus;
import com.banking.forms.downstream.infrastructure.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls the outbox for PENDING rows and hands each to {@link DownstreamDispatchService#dispatch} (a
 * fresh transaction per row so a single failure cannot roll back the batch). Decoupling enqueue from
 * delivery gives async processing, retries, and dead-lettering without a broker.
 */
@Component
public class DownstreamDispatcher {

    private final OutboxEventRepository outboxRepository;
    private final DownstreamDispatchService dispatchService;
    private final DownstreamProperties properties;

    public DownstreamDispatcher(
            OutboxEventRepository outboxRepository,
            DownstreamDispatchService dispatchService,
            DownstreamProperties properties) {
        this.outboxRepository = outboxRepository;
        this.dispatchService = dispatchService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${downstream.dispatch-interval-ms:5000}")
    public void dispatchPending() {
        if (!properties.isEnabled()) {
            return;
        }
        Instant now = Instant.now();
        for (OutboxEvent event : pending()) {
            if (isEligible(event, now)) {
                dispatchService.dispatch(event.getId());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> pending() {
        return outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING.name(), PageRequest.of(0, properties.getDispatchBatchSize()));
    }

    /** First attempt is immediate; retries wait a backoff that grows with the attempt count. */
    private boolean isEligible(OutboxEvent event, Instant now) {
        if (event.getAttempts() == 0) {
            return true;
        }
        long waitMs = properties.getRetryBackoffMs() * event.getAttempts();
        return event.getUpdatedAt().plusMillis(waitMs).isBefore(now);
    }
}
