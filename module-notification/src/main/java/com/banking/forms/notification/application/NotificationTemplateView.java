package com.banking.forms.notification.application;

/** Admin-facing view of a message template. */
public record NotificationTemplateView(
        String eventType, String channelType, String locale, String subject, String body) {}
