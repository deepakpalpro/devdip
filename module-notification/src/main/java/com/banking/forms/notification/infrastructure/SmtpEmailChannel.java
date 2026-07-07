package com.banking.forms.notification.infrastructure;

import com.banking.forms.notification.spi.ChannelConfig;
import com.banking.forms.notification.spi.DeliveryResult;
import com.banking.forms.notification.spi.NotificationChannel;
import com.banking.forms.notification.spi.NotificationChannels;
import com.banking.forms.notification.spi.OutboundNotification;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Real email delivery via Spring's {@link JavaMailSender}. The mail sender is auto-configured only
 * when {@code spring.mail.*} is set, so this channel is injected lazily via {@link ObjectProvider} and
 * fails safe (returns {@code FAILED}) if mail is not configured — it never breaks application startup.
 * Enable the {@code smtp-email} provider once an SMTP host (e.g. a local Mailpit) is configured.
 */
@Component
public class SmtpEmailChannel implements NotificationChannel {

    private final ObjectProvider<JavaMailSender> mailSender;

    public SmtpEmailChannel(ObjectProvider<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String channelId() {
        return "smtp-email";
    }

    @Override
    public String channelType() {
        return NotificationChannels.EMAIL;
    }

    @Override
    public DeliveryResult send(OutboundNotification notification, ChannelConfig config) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            return DeliveryResult.failed("JavaMailSender not configured (set spring.mail.host to enable SMTP)");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(notification.recipient());
            String from = config.text("from", null);
            if (from != null && !from.isBlank()) {
                message.setFrom(from);
            }
            message.setSubject(notification.subject() != null ? notification.subject() : "Notification");
            message.setText(notification.body() != null ? notification.body() : "");
            sender.send(message);
            return DeliveryResult.sent("smtp-" + UUID.randomUUID());
        } catch (Exception ex) {
            return DeliveryResult.failed("SMTP send failed: " + ex.getMessage());
        }
    }
}
