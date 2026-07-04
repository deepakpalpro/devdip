package com.banking.forms.submission.application;

import java.util.Map;
import java.util.UUID;

public record SubmissionDetailView(
        UUID id,
        UUID formVersionId,
        String formCode,
        String formName,
        String status,
        String currentSectionKey,
        Map<String, Map<String, Object>> sectionData) {}
