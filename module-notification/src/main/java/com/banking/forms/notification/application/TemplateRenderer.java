package com.banking.forms.notification.application;

import com.banking.forms.notification.domain.NotificationTemplate;
import com.banking.forms.notification.infrastructure.NotificationTemplateRepository;
import com.banking.forms.notification.spi.NotificationChannels;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renders a {@link NotificationTemplate} for an (event, channel, locale) into a concrete message,
 * substituting {@code {{placeholder}}} tokens from the supplied variables. Falls back to the {@code en}
 * template and then to a built-in default so a message can always be produced.
 */
@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    private final NotificationTemplateRepository templateRepository;

    public TemplateRenderer(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Transactional(readOnly = true)
    public RenderedMessage render(String eventType, String channelType, String locale, Map<String, String> variables) {
        Optional<NotificationTemplate> template = templateRepository
                .findByEventTypeAndChannelTypeAndLocale(eventType, channelType, locale)
                .or(() -> templateRepository.findByEventTypeAndChannelTypeAndLocale(eventType, channelType, "en"));

        String rawSubject = template.map(NotificationTemplate::getSubject).orElseGet(() -> defaultSubject(eventType));
        String rawBody = template.map(NotificationTemplate::getBody).orElseGet(() -> defaultBody(eventType));

        if (NotificationChannels.WHATSAPP.equals(channelType)) {
            // For WhatsApp the subject holds the provider-approved template name (an identifier, not
            // substituted); the body is the plain-text fallback.
            return new RenderedMessage(null, substitute(rawBody, variables), rawSubject);
        }
        return new RenderedMessage(substitute(rawSubject, variables), substitute(rawBody, variables), null);
    }

    private static String substitute(String template, Map<String, String> variables) {
        if (template == null) {
            return null;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.getOrDefault(key, "");
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String defaultSubject(String eventType) {
        return switch (eventType) {
            case NotificationEventTypes.APPLICATION_SUBMITTED -> "We received your {{formName}} application";
            case NotificationEventTypes.APPLICATION_APPROVED -> "Your {{formName}} application is approved";
            case NotificationEventTypes.APPLICATION_REJECTED -> "Update on your {{formName}} application";
            case NotificationEventTypes.APPLICATION_NEEDS_INFO -> "We need more information for your {{formName}} application";
            default -> "Update on your application";
        };
    }

    private static String defaultBody(String eventType) {
        return switch (eventType) {
            case NotificationEventTypes.APPLICATION_SUBMITTED ->
                "Thank you — we have received your {{formName}} application (reference {{reference}}).";
            case NotificationEventTypes.APPLICATION_APPROVED ->
                "Good news — your {{formName}} application (reference {{reference}}) has been approved.";
            case NotificationEventTypes.APPLICATION_REJECTED ->
                "After review, your {{formName}} application (reference {{reference}}) was not approved at this time.";
            case NotificationEventTypes.APPLICATION_NEEDS_INFO ->
                "We need more information for your {{formName}} application (reference {{reference}}).";
            default -> "There is an update on your application (reference {{reference}}).";
        };
    }
}
