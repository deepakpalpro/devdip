package com.banking.forms.notification.application;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the scheduled outbox poller ({@link NotificationDispatcher}). */
@Configuration
@EnableScheduling
public class NotificationConfig {}
