package com.banking.forms.downstream.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.forms.downstream.domain.OutboxEvent;
import com.banking.forms.downstream.domain.OutboxStatus;
import com.banking.forms.downstream.infrastructure.OutboxEventRepository;
import com.banking.forms.downstream.spi.ConnectorConfig;
import com.banking.forms.downstream.spi.DispatchResult;
import com.banking.forms.downstream.spi.DownstreamConnector;
import com.banking.forms.submission.application.SubmissionEventRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DownstreamDispatchServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID SUBMISSION = UUID.randomUUID();

    @Mock private DownstreamConnectorRouter connectorRouter;
    @Mock private OutboxEventRepository outboxRepository;
    @Mock private SubmissionEventRecorder eventRecorder;
    @Mock private DownstreamConnector connector;

    private DownstreamProperties properties;
    private DownstreamDispatchService service;

    @BeforeEach
    void setUp() {
        properties = new DownstreamProperties();
        service = new DownstreamDispatchService(
                connectorRouter, outboxRepository, eventRecorder, properties, new ObjectMapper());
    }

    @Test
    void enqueueFansOutToAllEnabledProviders() {
        when(connectorRouter.resolveAllEnabled())
                .thenReturn(List.of(new DownstreamConnectorRouter.Selection(
                        "log-sink", "log", connector, new ConnectorConfig(null))));

        int queued = service.enqueueForSubmission(
                TENANT, SUBMISSION, "LOAN", Map.of("loan", Map.of("amount", 5000)), "REVIEW", 0.5);

        assertThat(queued).isEqualTo(1);
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(captor.getValue().getProviderCode()).isEqualTo("log-sink");
        verify(eventRecorder).record(eq(SUBMISSION), eq("DOWNSTREAM_QUEUED"), any(), any());
    }

    @Test
    void enqueueSkipsWhenDisabled() {
        properties.setEnabled(false);

        int queued = service.enqueueForSubmission(TENANT, SUBMISSION, "LOAN", Map.of(), null, null);

        assertThat(queued).isZero();
        verify(outboxRepository, never()).save(any());
        verify(eventRecorder).record(eq(SUBMISSION), eq("DOWNSTREAM_SKIPPED"), any(), any());
    }

    @Test
    void dispatchMarksDispatchedOnSuccess() {
        UUID outboxId = UUID.randomUUID();
        OutboxEvent event = new OutboxEvent(
                outboxId,
                TENANT,
                SUBMISSION,
                DownstreamEventTypes.SUBMISSION_PROCESSED,
                "LOAN",
                "log-sink",
                "log",
                "{}",
                OutboxStatus.PENDING);
        when(outboxRepository.findById(outboxId)).thenReturn(Optional.of(event));
        when(connectorRouter.resolveProvider("log-sink"))
                .thenReturn(Optional.of(new DownstreamConnectorRouter.Selection(
                        "log-sink", "log", connector, new ConnectorConfig(null))));
        when(connector.dispatch(any(), any())).thenReturn(DispatchResult.dispatched("ref-1"));

        service.dispatch(outboxId);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DISPATCHED);
        assertThat(event.getProviderRef()).isEqualTo("ref-1");
        verify(eventRecorder).record(eq(SUBMISSION), eq("DOWNSTREAM_DISPATCHED"), any(), any());
    }
}
