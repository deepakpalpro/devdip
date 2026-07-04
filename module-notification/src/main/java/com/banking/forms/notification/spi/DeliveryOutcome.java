package com.banking.forms.notification.spi;

/** Result of a single delivery attempt by a {@link NotificationChannel}. */
public enum DeliveryOutcome {
    SENT,
    FAILED
}
