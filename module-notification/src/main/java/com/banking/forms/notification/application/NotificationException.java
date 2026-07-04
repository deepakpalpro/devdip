package com.banking.forms.notification.application;

/** Thrown for invalid notification admin operations (unknown provider, bad config JSON). */
public class NotificationException extends RuntimeException {

    public NotificationException(String message) {
        super(message);
    }
}
