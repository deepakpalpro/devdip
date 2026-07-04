package com.banking.forms.notification.infrastructure;

import com.banking.forms.notification.application.PiiMask;
import com.banking.forms.notification.spi.ChannelConfig;
import com.banking.forms.notification.spi.DeliveryResult;
import com.banking.forms.notification.spi.NotificationChannel;
import com.banking.forms.notification.spi.NotificationChannels;
import com.banking.forms.notification.spi.OutboundNotification;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Zero-setup default email channel: renders to the application log instead of delivering to a real
 * mailbox, so the notification pipeline is demoable and testable out of the box (mirrors the in-JVM
 * default providers used elsewhere). Enabled by default; the {@code smtp-email} provider supersedes it
 * once configured. The recipient is masked in the log to avoid leaking PII.
 */
@Component
public class LogEmailChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(LogEmailChannel.class);

    @Override
    public String channelId() {
        return "log-email";
    }

    @Override
    public String channelType() {
        return NotificationChannels.EMAIL;
    }

    @Override
    public DeliveryResult send(OutboundNotification notification, ChannelConfig config) {
        log.info(
                "[notification:log-email] event={} to={} subject=\"{}\" body=\"{}\"",
                notification.eventType(),
                PiiMask.recipient(notification.recipient()),
                notification.subject(),
                notification.body());
        return DeliveryResult.sent("log-" + UUID.randomUUID());
    }
}
