package com.banking.forms.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.forms.formdefinition.application.FormQueryService;
import com.banking.forms.formdefinition.application.PublishedFormView;
import com.banking.forms.formdefinition.domain.StorageStrategy;
import com.banking.forms.notification.domain.NotificationMessage;
import com.banking.forms.notification.domain.NotificationStatus;
import com.banking.forms.notification.infrastructure.NotificationMessageRepository;
import com.banking.forms.notification.spi.ChannelConfig;
import com.banking.forms.notification.spi.DeliveryResult;
import com.banking.forms.notification.spi.NotificationChannel;
import com.banking.forms.notification.spi.OutboundNotification;
import com.banking.forms.submission.application.SubmissionEventRecorder;
import com.banking.forms.submission.application.event.SubmissionLifecycleEvent;
import com.banking.forms.submission.domain.SubmissionStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

    private final FormQueryService formQueryService = mock(FormQueryService.class);
    private final RecipientResolver recipientResolver = mock(RecipientResolver.class);
    private final TemplateRenderer templateRenderer = mock(TemplateRenderer.class);
    private final NotificationChannelRouter channelRouter = mock(NotificationChannelRouter.class);
    private final NotificationMessageRepository messageRepository = mock(NotificationMessageRepository.class);
    private final SubmissionEventRecorder eventRecorder = mock(SubmissionEventRecorder.class);
    private final NotificationProperties properties = new NotificationProperties();

    private NotificationService service;

    private final UUID tenant = UUID.randomUUID();
    private final UUID submission = UUID.randomUUID();
    private final UUID user = UUID.randomUUID();
    private final UUID version = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new NotificationService(
                formQueryService, recipientResolver, templateRenderer, channelRouter, messageRepository, eventRecorder, properties);
        when(formQueryService.findPublishedByVersionId(version)).thenReturn(Optional.of(form()));
        when(templateRenderer.render(any(), any(), any(), any()))
                .thenReturn(new RenderedMessage("subject", "body", null));
    }

    private PublishedFormView form() {
        return new PublishedFormView(
                UUID.randomUUID(), version, "LOAN", "Loan Application", "LENDING", StorageStrategy.JSON_BLOB, null);
    }

    private SubmissionLifecycleEvent event(SubmissionStatus to) {
        return new SubmissionLifecycleEvent(tenant, submission, user, version, SubmissionStatus.PENDING_REVIEW, to);
    }

    private NotificationChannelRouter.Selection selection(String code) {
        NotificationChannel channel = mock(NotificationChannel.class);
        return new NotificationChannelRouter.Selection(code, channel, new ChannelConfig(null));
    }

    @Test
    void enqueuesEmailAndWhatsAppForApprovedWithBothContacts() {
        when(recipientResolver.resolve(any(), eq(submission)))
                .thenReturn(new Recipient("a@b.com", "+61400000000", true, "en"));
        when(channelRouter.resolve("email")).thenReturn(Optional.of(selection("log-email")));
        when(channelRouter.resolve("whatsapp")).thenReturn(Optional.of(selection("whatsapp-cloud")));

        service.handle(event(SubmissionStatus.APPROVED));

        verify(messageRepository, times(2)).save(any(NotificationMessage.class));
        verify(eventRecorder, times(2)).record(eq(submission), eq("NOTIFICATION_QUEUED"), anyMap(), any());
    }

    @Test
    void ignoresNonCustomerFacingTransition() {
        service.handle(event(SubmissionStatus.PROCESSING));

        verify(messageRepository, never()).save(any());
        verify(recipientResolver, never()).resolve(any(), any());
    }

    @Test
    void skipsWhenNoRecipient() {
        when(recipientResolver.resolve(any(), any())).thenReturn(Recipient.empty());

        service.handle(event(SubmissionStatus.SUBMITTED));

        verify(messageRepository, never()).save(any());
        verify(eventRecorder).record(eq(submission), eq("NOTIFICATION_SKIPPED"), anyMap(), any());
    }

    @Test
    void skipsWhenNoEnabledProvider() {
        when(recipientResolver.resolve(any(), any())).thenReturn(new Recipient("a@b.com", null, true, "en"));
        when(channelRouter.resolve("email")).thenReturn(Optional.empty());

        service.handle(event(SubmissionStatus.SUBMITTED));

        verify(messageRepository, never()).save(any());
        verify(eventRecorder).record(eq(submission), eq("NOTIFICATION_SKIPPED"), anyMap(), any());
    }

    @Test
    void skipsWhenConsentRequiredButNotGranted() {
        properties.setRequireConsent(true);
        when(recipientResolver.resolve(any(), any())).thenReturn(new Recipient("a@b.com", null, null, "en"));

        service.handle(event(SubmissionStatus.APPROVED));

        verify(messageRepository, never()).save(any());
        verify(eventRecorder).record(eq(submission), eq("NOTIFICATION_SKIPPED"), anyMap(), any());
    }

    @Test
    void dispatchMarksSentOnSuccessfulDelivery() {
        NotificationMessage message = pendingMessage();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.send(any(OutboundNotification.class), any())).thenReturn(DeliveryResult.sent("wamid.1"));
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(channelRouter.resolveProvider("log-email"))
                .thenReturn(Optional.of(new NotificationChannelRouter.Selection("log-email", channel, new ChannelConfig(null))));

        service.dispatch(message.getId());

        assertThat(message.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(message.getProviderMessageId()).isEqualTo("wamid.1");
        verify(eventRecorder).record(eq(submission), eq("NOTIFICATION_SENT"), anyMap(), any());
    }

    @Test
    void dispatchKeepsPendingForRetryWhenAttemptsRemain() {
        properties.setMaxAttempts(3);
        NotificationMessage message = pendingMessage();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.send(any(OutboundNotification.class), any())).thenReturn(DeliveryResult.failed("smtp down"));
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(channelRouter.resolveProvider("log-email"))
                .thenReturn(Optional.of(new NotificationChannelRouter.Selection("log-email", channel, new ChannelConfig(null))));

        service.dispatch(message.getId());

        assertThat(message.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(message.getAttempts()).isEqualTo(1);
        verify(eventRecorder, never()).record(eq(submission), eq("NOTIFICATION_SENT"), anyMap(), any());
    }

    @Test
    void dispatchDeadLettersWhenMaxAttemptsReached() {
        properties.setMaxAttempts(1);
        NotificationMessage message = pendingMessage();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.send(any(OutboundNotification.class), any())).thenReturn(DeliveryResult.failed("smtp down"));
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(channelRouter.resolveProvider("log-email"))
                .thenReturn(Optional.of(new NotificationChannelRouter.Selection("log-email", channel, new ChannelConfig(null))));

        service.dispatch(message.getId());

        assertThat(message.getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(eventRecorder).record(eq(submission), eq("NOTIFICATION_FAILED"), anyMap(), any());
    }

    @Test
    void applyDeliveryStatusMarksDelivered() {
        NotificationMessage message = pendingMessage();
        message.markSent("wamid.9");
        when(messageRepository.findByProviderCodeAndProviderMessageId("whatsapp-cloud", "wamid.9"))
                .thenReturn(Optional.of(message));

        boolean matched = service.applyDeliveryStatus("whatsapp-cloud", "wamid.9", true);

        assertThat(matched).isTrue();
        assertThat(message.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
    }

    private NotificationMessage pendingMessage() {
        return new NotificationMessage(
                UUID.randomUUID(),
                tenant,
                submission,
                "APPLICATION_APPROVED",
                "email",
                "log-email",
                "a@b.com",
                "subject",
                "body",
                null,
                NotificationStatus.PENDING);
    }
}
