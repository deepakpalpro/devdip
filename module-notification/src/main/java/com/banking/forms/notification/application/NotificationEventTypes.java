package com.banking.forms.notification.application;

import com.banking.forms.submission.domain.SubmissionStatus;

/**
 * Maps a submission lifecycle transition to the customer-facing notification event type used to select
 * a template. Only transitions worth notifying the customer about are mapped; others return
 * {@code null} and are ignored.
 */
public final class NotificationEventTypes {

    public static final String APPLICATION_SUBMITTED = "APPLICATION_SUBMITTED";
    public static final String APPLICATION_APPROVED = "APPLICATION_APPROVED";
    public static final String APPLICATION_REJECTED = "APPLICATION_REJECTED";
    public static final String APPLICATION_NEEDS_INFO = "APPLICATION_NEEDS_INFO";

    private NotificationEventTypes() {}

    public static String forStatus(SubmissionStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case SUBMITTED -> APPLICATION_SUBMITTED;
            case APPROVED -> APPLICATION_APPROVED;
            case REJECTED -> APPLICATION_REJECTED;
            case NEEDS_INFO -> APPLICATION_NEEDS_INFO;
            default -> null;
        };
    }
}
