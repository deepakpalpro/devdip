package com.banking.forms.bff.admin.api;

import com.banking.forms.notification.application.NotificationService;
import java.util.Locale;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives provider delivery-status callbacks (e.g. SendGrid/Twilio/WhatsApp) and updates the matching
 * notification message to DELIVERED/FAILED. Deliberately generic ({@code messageId} + {@code status});
 * a production deployment would add per-provider payload mapping and signature verification (the
 * gateway leaves this path unauthenticated — see SecurityConfig). Always returns 200 so providers do
 * not needlessly retry.
 */
@RestController
@RequestMapping("/api/webhooks/notifications")
public class NotificationWebhookController {

    private final NotificationService notificationService;

    public NotificationWebhookController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/{provider}")
    public Map<String, Object> receive(@PathVariable("provider") String provider, @RequestBody DeliveryStatus body) {
        boolean delivered = isDelivered(body.status());
        boolean matched = body.messageId() != null
                && notificationService.applyDeliveryStatus(provider, body.messageId(), delivered);
        return Map.of("matched", matched);
    }

    private static boolean isDelivered(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("delivered") || normalized.equals("sent") || normalized.equals("read");
    }

    public record DeliveryStatus(String messageId, String status) {}
}
