package com.banking.forms.processing.application;

import com.banking.forms.submission.application.SubmissionEventRecorder;
import com.banking.forms.submission.application.SubmissionNotFoundException;
import com.banking.forms.submission.domain.Submission;
import com.banking.forms.submission.domain.SubmissionStatus;
import com.banking.forms.submission.infrastructure.SubmissionRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Drives the manual review workflow: applies a {@link ReviewAction} to a submission through the
 * {@link ReviewWorkflow} state machine, persists the new status, and records an audit event.
 */
@Service
@Transactional
public class ReviewService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionEventRecorder eventRecorder;
    private final ReviewWorkflow workflow;

    public ReviewService(
            SubmissionRepository submissionRepository,
            SubmissionEventRecorder eventRecorder,
            ReviewWorkflow workflow) {
        this.submissionRepository = submissionRepository;
        this.eventRecorder = eventRecorder;
        this.workflow = workflow;
    }

    public SubmissionStatus decide(
            UUID tenantId, UUID submissionId, UUID actorId, ReviewAction action, String note) {
        Submission submission = submissionRepository
                .findById(submissionId)
                .filter(candidate -> candidate.getTenantId().equals(tenantId))
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));

        SubmissionStatus from = submission.getStatus();
        SubmissionStatus to = workflow.resolveTarget(action, from);
        Instant now = Instant.now();

        switch (to) {
            case PENDING_REVIEW -> submission.markUnderReview(now);
            case APPROVED -> submission.markApproved(now);
            case REJECTED -> submission.markRejected(now);
            case NEEDS_INFO -> submission.markNeedsInfo(now);
            default -> throw new ReviewException("Unsupported target status " + to);
        }
        submissionRepository.save(submission);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", from.name());
        payload.put("to", to.name());
        if (note != null && !note.isBlank()) {
            payload.put("note", note.trim());
        }
        eventRecorder.record(submissionId, eventType(action), payload, actorId);

        return to;
    }

    private String eventType(ReviewAction action) {
        return switch (action) {
            case START_REVIEW -> "REVIEW_STARTED";
            case APPROVE -> "APPROVED";
            case REJECT -> "REJECTED";
            case REQUEST_INFO -> "INFO_REQUESTED";
        };
    }
}
