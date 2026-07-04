package com.banking.forms.processing.application;

import com.banking.forms.submission.domain.SubmissionStatus;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The manual review state machine. Defines which {@link ReviewAction}s are legal from a submission's
 * current {@link SubmissionStatus} and the resulting target status.
 *
 * <pre>
 *   SUBMITTED ─start─▶ PENDING_REVIEW ─approve──▶ APPROVED (terminal)
 *   NEEDS_INFO ─start─▶ PENDING_REVIEW ─reject───▶ REJECTED (terminal)
 *                       PENDING_REVIEW ─request──▶ NEEDS_INFO
 * </pre>
 */
@Component
public class ReviewWorkflow {

    public SubmissionStatus resolveTarget(ReviewAction action, SubmissionStatus current) {
        return switch (action) {
            case START_REVIEW -> transition(
                    action, current, EnumSet.of(SubmissionStatus.SUBMITTED, SubmissionStatus.NEEDS_INFO),
                    SubmissionStatus.PENDING_REVIEW);
            case APPROVE -> transition(
                    action, current, EnumSet.of(SubmissionStatus.PENDING_REVIEW), SubmissionStatus.APPROVED);
            case REJECT -> transition(
                    action, current, EnumSet.of(SubmissionStatus.PENDING_REVIEW), SubmissionStatus.REJECTED);
            case REQUEST_INFO -> transition(
                    action, current, EnumSet.of(SubmissionStatus.PENDING_REVIEW), SubmissionStatus.NEEDS_INFO);
        };
    }

    private SubmissionStatus transition(
            ReviewAction action, SubmissionStatus current, Set<SubmissionStatus> allowedFrom, SubmissionStatus target) {
        if (!allowedFrom.contains(current)) {
            throw new ReviewException(
                    "Action " + action + " is not allowed from status " + current + " (expected one of " + allowedFrom
                            + ")");
        }
        return target;
    }
}
