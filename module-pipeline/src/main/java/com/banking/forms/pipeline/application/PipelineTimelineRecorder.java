package com.banking.forms.pipeline.application;

import com.banking.forms.submission.application.SubmissionEventRecorder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PipelineTimelineRecorder {

    private static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);

    private final SubmissionEventRecorder eventRecorder;

    public PipelineTimelineRecorder(SubmissionEventRecorder eventRecorder) {
        this.eventRecorder = eventRecorder;
    }

    public void record(UUID submissionId, String eventType, Map<String, Object> payload) {
        eventRecorder.record(submissionId, eventType, payload, SYSTEM_ACTOR);
    }

    public void record(UUID submissionId, String eventType, Object... keyValues) {
        record(submissionId, eventType, payload(keyValues));
    }

    static Map<String, Object> payload(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }
}
