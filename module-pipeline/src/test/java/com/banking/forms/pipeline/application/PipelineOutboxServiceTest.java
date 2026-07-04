package com.banking.forms.pipeline.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.forms.pipeline.domain.PipelineOutboxEvent;
import com.banking.forms.pipeline.infrastructure.PipelineOutboxRepository;
import com.banking.forms.pipeline.spi.PipelineEventPublisher;
import com.banking.forms.submission.application.SubmissionEventRecorder;
import com.banking.forms.submission.application.event.SubmissionLifecycleEvent;
import com.banking.forms.submission.domain.SubmissionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PipelineOutboxServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID SUBMISSION = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final UUID VERSION = UUID.randomUUID();

    @Mock private PipelineOutboxRepository outboxRepository;
    @Mock private SubmissionPipelineService pipelineService;
    @Mock private SubmissionEventRecorder eventRecorder;
    @Mock private PipelineEventPublisher eventPublisher;

    private PipelineProperties properties;
    private PipelineOutboxService service;

    @BeforeEach
    void setUp() {
        properties = new PipelineProperties();
        service = new PipelineOutboxService(
                outboxRepository,
                pipelineService,
                eventRecorder,
                eventPublisher,
                properties,
                new ObjectMapper());
    }

    @Test
    void enqueueWritesUnpublishedOutboxRowInAsyncMode() {
        when(outboxRepository.findFirstBySubmissionIdAndEventTypeAndPublishedFalse(
                        SUBMISSION, PipelineEventTypes.PIPELINE_REQUESTED))
                .thenReturn(Optional.empty());

        service.enqueue(new SubmissionLifecycleEvent(
                TENANT, SUBMISSION, USER, VERSION, SubmissionStatus.DRAFT, SubmissionStatus.SUBMITTED));

        ArgumentCaptor<PipelineOutboxEvent> captor = ArgumentCaptor.forClass(PipelineOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(PipelineEventTypes.PIPELINE_REQUESTED);
        assertThat(captor.getValue().isPublished()).isFalse();
        verify(eventRecorder).record(eq(SUBMISSION), eq("PIPELINE_QUEUED"), any(), any());
    }

    @Test
    void enqueueSkippedInSyncMode() {
        properties.setProcessMode("sync");

        service.enqueue(new SubmissionLifecycleEvent(
                TENANT, SUBMISSION, USER, VERSION, SubmissionStatus.DRAFT, SubmissionStatus.SUBMITTED));

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void processRunsPipelineAndMarksPublished() {
        UUID outboxId = UUID.randomUUID();
        PipelineOutboxEvent outbox = new PipelineOutboxEvent(
                outboxId, PipelineEventTypes.PIPELINE_REQUESTED, "{}", TENANT, SUBMISSION);
        when(outboxRepository.findById(outboxId)).thenReturn(Optional.of(outbox));

        service.process(outboxId);

        verify(pipelineService).process(TENANT, SUBMISSION);
        assertThat(outbox.isPublished()).isTrue();
    }
}
