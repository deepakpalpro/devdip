package com.banking.forms.bff.admin.api;

import com.banking.forms.processing.application.ReviewAction;
import com.banking.forms.processing.application.ReviewException;
import com.banking.forms.processing.application.ReviewService;
import com.banking.forms.submission.application.SubmissionNotFoundException;
import com.banking.forms.submission.domain.SubmissionStatus;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Admin review actions that drive a submission through the processing state machine. */
@RestController
@RequestMapping("/api/admin/v1/submissions/{id}/review")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/start")
    public ReviewResponse startReview(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Dev-User-Id", required = false) String actorHeader,
            @PathVariable("id") UUID id) {
        return apply(tenantId, actorHeader, id, ReviewAction.START_REVIEW, null);
    }

    @PostMapping("/approve")
    public ReviewResponse approve(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Dev-User-Id", required = false) String actorHeader,
            @PathVariable("id") UUID id,
            @RequestBody(required = false) NoteRequest request) {
        return apply(tenantId, actorHeader, id, ReviewAction.APPROVE, note(request));
    }

    @PostMapping("/reject")
    public ReviewResponse reject(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Dev-User-Id", required = false) String actorHeader,
            @PathVariable("id") UUID id,
            @RequestBody(required = false) NoteRequest request) {
        return apply(tenantId, actorHeader, id, ReviewAction.REJECT, note(request));
    }

    @PostMapping("/request-info")
    public ReviewResponse requestInfo(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Dev-User-Id", required = false) String actorHeader,
            @PathVariable("id") UUID id,
            @RequestBody(required = false) NoteRequest request) {
        return apply(tenantId, actorHeader, id, ReviewAction.REQUEST_INFO, note(request));
    }

    private ReviewResponse apply(
            UUID tenantId, String actorHeader, UUID id, ReviewAction action, String note) {
        UUID actorId = AdminRequestContext.resolveActorId(actorHeader);
        try {
            SubmissionStatus status = reviewService.decide(tenantId, id, actorId, action, note);
            return new ReviewResponse(id, status.name());
        } catch (SubmissionNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (ReviewException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    private String note(NoteRequest request) {
        return request == null ? null : request.note();
    }

    public record NoteRequest(String note) {}

    public record ReviewResponse(UUID id, String status) {}
}
