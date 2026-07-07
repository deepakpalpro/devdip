package com.banking.forms.notification.application;

import com.banking.forms.formdefinition.application.FormQueryService;
import com.banking.forms.formdefinition.application.PublishedFormView;
import com.banking.forms.notification.domain.NotificationMessage;
import com.banking.forms.notification.domain.NotificationStatus;
import com.banking.forms.notification.infrastructure.NotificationMessageRepository;
import com.banking.forms.notification.spi.DeliveryResult;
import com.banking.forms.notification.spi.NotificationChannels;
import com.banking.forms.notification.spi.OutboundNotification;
import com.banking.forms.submission.application.SubmissionEventRecorder;
import com.banking.forms.submission.application.event.SubmissionLifecycleEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates customer notifications. On a submission lifecycle transition it resolves the recipient,
 * fans out one {@link NotificationMessage} per eligible channel (enqueued PENDING in the durable
 * outbox), and records a timeline event. The async {@code NotificationDispatcher} later delivers each
 * message via its channel. Every path is fail-safe — notifications never break the submission flow.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);

    private final FormQueryService formQueryService;
    private final RecipientResolver recipientResolver;
    private final TemplateRenderer templateRenderer;
    private final NotificationChannelRouter channelRouter;
    private final NotificationMessageRepository messageRepository;
    private final SubmissionEventRecorder eventRecorder;
    private final NotificationProperties properties;

    public NotificationService(
            FormQueryService formQueryService,
            RecipientResolver recipientResolver,
            TemplateRenderer templateRenderer,
            NotificationChannelRouter channelRouter,
            NotificationMessageRepository messageRepository,
            SubmissionEventRecorder eventRecorder,
            NotificationProperties properties) {
        this.formQueryService = formQueryService;
        this.recipientResolver = recipientResolver;
        this.templateRenderer = templateRenderer;
        this.channelRouter = channelRouter;
        this.messageRepository = messageRepository;
        this.eventRecorder = eventRecorder;
        this.properties = properties;
    }

    /**
     * Enqueue notifications for a lifecycle transition. Invoked from an {@code AFTER_COMMIT} listener,
     * where the source transaction has committed but its resources are still bound; we therefore force
     * a brand-new transaction ({@code REQUIRES_NEW}) so the outbox rows and timeline events durably commit.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(SubmissionLifecycleEvent event) {
        try {
            enqueue(event);
        } catch (Exception ex) {
            // Advisory feature — never propagate. Log and move on.
            log.warn("Notification enqueue failed for submission {}: {}", event.submissionId(), ex.toString());
        }
    }

    private void enqueue(SubmissionLifecycleEvent event) {
        if (!properties.isEnabled()) {
            return;
        }
        String eventType = NotificationEventTypes.forStatus(event.toStatus());
        if (eventType == null) {
            return; // not a customer-facing transition
        }

        Optional<PublishedFormView> form = formQueryService.findPublishedByVersionId(event.formVersionId());
        String formName = form.map(PublishedFormView::name).orElse("your");
        String formCode = form.map(PublishedFormView::code).orElse("");
        Recipient recipient = form.map(f -> recipientResolver.resolve(f, event.submissionId())).orElse(Recipient.empty());

        if (!recipient.hasAnyContact()) {
            recordTimeline(event.submissionId(), "NOTIFICATION_SKIPPED", Map.of("event", eventType, "reason", "no-recipient"));
            return;
        }
        if (properties.isRequireConsent() && !recipient.consentGranted()) {
            recordTimeline(event.submissionId(), "NOTIFICATION_SKIPPED", Map.of("event", eventType, "reason", "no-consent"));
            return;
        }

        String locale = recipient.locale() != null ? recipient.locale() : properties.getDefaultLocale();
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("formName", formName == null || formName.isBlank() ? "your" : formName);
        variables.put("formCode", formCode == null ? "" : formCode);
        variables.put("reference", reference(event.submissionId()));
        variables.put("status", event.toStatus().name());

        List<String> channels = new ArrayList<>();
        if (recipient.hasEmail()) {
            channels.add(NotificationChannels.EMAIL);
        }
        if (recipient.hasPhone()) {
            channels.add(NotificationChannels.WHATSAPP);
        }

        boolean anyQueued = false;
        for (String channelType : channels) {
            Optional<NotificationChannelRouter.Selection> selection = channelRouter.resolve(channelType);
            if (selection.isEmpty()) {
                continue; // no enabled provider for this channel
            }
            String address = NotificationChannels.EMAIL.equals(channelType) ? recipient.email() : recipient.phone();
            RenderedMessage rendered = templateRenderer.render(eventType, channelType, locale, variables);
            NotificationMessage message = new NotificationMessage(
                    UUID.randomUUID(),
                    event.tenantId(),
                    event.submissionId(),
                    eventType,
                    channelType,
                    selection.get().providerCode(),
                    address,
                    rendered.subject(),
                    rendered.body(),
                    rendered.templateName(),
                    NotificationStatus.PENDING);
            messageRepository.save(message);
            anyQueued = true;
            recordTimeline(
                    event.submissionId(),
                    "NOTIFICATION_QUEUED",
                    Map.of(
                            "event", eventType,
                            "channel", channelType,
                            "provider", selection.get().providerCode(),
                            "recipient", PiiMask.recipient(address)));
        }
        if (!anyQueued) {
            recordTimeline(event.submissionId(), "NOTIFICATION_SKIPPED", Map.of("event", eventType, "reason", "no-provider"));
        }
    }

    /** Deliver a single queued message. Fail-safe with retry/dead-letter semantics. */
    @Transactional
    public void dispatch(UUID messageId) {
        NotificationMessage message = messageRepository.findById(messageId).orElse(null);
        if (message == null || message.getStatus() != NotificationStatus.PENDING) {
            return;
        }
        Optional<NotificationChannelRouter.Selection> selection = channelRouter.resolveProvider(message.getProviderCode());
        if (selection.isEmpty()) {
            message.markFailed("No implementation for provider " + message.getProviderCode());
            messageRepository.save(message);
            recordDelivery(message, false, "no-implementation");
            return;
        }

        OutboundNotification outbound = new OutboundNotification(
                message.getTenantId(),
                message.getSubmissionId(),
                message.getEventType(),
                message.getChannelType(),
                message.getRecipient(),
                message.getSubject(),
                message.getBody(),
                message.getTemplateName(),
                properties.getDefaultLocale(),
                Map.of());

        DeliveryResult result;
        try {
            result = selection.get().channel().send(outbound, selection.get().config());
        } catch (Exception ex) {
            result = DeliveryResult.failed("channel threw: " + ex.getMessage());
        }

        if (result.isSent()) {
            message.markSent(result.providerMessageId());
            messageRepository.save(message);
            recordDelivery(message, true, result.providerMessageId());
        } else if (message.getAttempts() + 1 >= properties.getMaxAttempts()) {
            message.markFailed(result.detail());
            messageRepository.save(message);
            recordDelivery(message, false, result.detail());
        } else {
            message.markRetryable(result.detail()); // stays PENDING, retried on the next tick
            messageRepository.save(message);
        }
    }

    /** Apply a provider delivery-status callback (webhook) to a previously-sent message. */
    @Transactional
    public boolean applyDeliveryStatus(String providerCode, String providerMessageId, boolean delivered) {
        return messageRepository
                .findByProviderCodeAndProviderMessageId(providerCode, providerMessageId)
                .map(message -> {
                    if (delivered) {
                        message.markDelivered();
                        messageRepository.save(message);
                        recordTimeline(
                                message.getSubmissionId(),
                                "NOTIFICATION_DELIVERED",
                                Map.of("channel", message.getChannelType(), "provider", providerCode));
                    } else {
                        message.markFailed("provider reported failed delivery");
                        messageRepository.save(message);
                        recordDelivery(message, false, "provider-callback");
                    }
                    return true;
                })
                .orElse(false);
    }

    private void recordDelivery(NotificationMessage message, boolean sent, String detail) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", message.getEventType());
        payload.put("channel", message.getChannelType());
        payload.put("provider", message.getProviderCode());
        payload.put("recipient", PiiMask.recipient(message.getRecipient()));
        if (detail != null) {
            payload.put("detail", detail);
        }
        recordTimeline(message.getSubmissionId(), sent ? "NOTIFICATION_SENT" : "NOTIFICATION_FAILED", payload);
    }

    private void recordTimeline(UUID submissionId, String type, Map<String, Object> payload) {
        if (submissionId == null) {
            return;
        }
        try {
            eventRecorder.record(submissionId, type, payload, SYSTEM_ACTOR);
        } catch (Exception ex) {
            log.debug("Timeline event {} could not be recorded: {}", type, ex.getMessage());
        }
    }

    private static String reference(UUID submissionId) {
        return submissionId.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
