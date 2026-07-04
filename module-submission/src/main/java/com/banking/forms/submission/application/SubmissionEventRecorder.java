package com.banking.forms.submission.application;

import com.banking.forms.submission.domain.SubmissionEvent;
import com.banking.forms.submission.infrastructure.SubmissionEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Writes append-only submission audit events. Shared by the submission lifecycle (e.g. SUBMITTED)
 * and the processing module's review actions so all transitions land in one timeline.
 */
@Component
public class SubmissionEventRecorder {

    private final SubmissionEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public SubmissionEventRecorder(SubmissionEventRepository eventRepository, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    public void record(UUID submissionId, String eventType, Map<String, Object> payload, UUID actorId) {
        String payloadJson = (payload == null || payload.isEmpty()) ? null : writeJson(payload);
        eventRepository.save(new SubmissionEvent(submissionId, eventType, payloadJson, actorId));
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize submission event payload", ex);
        }
    }
}
