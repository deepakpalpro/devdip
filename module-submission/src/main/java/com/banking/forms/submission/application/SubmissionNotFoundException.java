package com.banking.forms.submission.application;

import java.util.UUID;

public class SubmissionNotFoundException extends RuntimeException {

    public SubmissionNotFoundException(UUID submissionId) {
        super("Submission not found: " + submissionId);
    }
}
