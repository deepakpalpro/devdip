package com.banking.forms.pipeline.domain;

/** Lifecycle hook that selects which pipeline binding runs for a form version. */
public enum PipelineTrigger {
    ON_SUBMIT,
    ON_APPROVED,
    ON_REJECTED,
    ON_STATUS_CHANGE;

    public static PipelineTrigger fromReviewTarget(String toStatus) {
        return switch (toStatus) {
            case "APPROVED" -> ON_APPROVED;
            case "REJECTED" -> ON_REJECTED;
            default -> ON_STATUS_CHANGE;
        };
    }
}
