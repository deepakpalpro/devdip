package com.banking.forms.pipeline.application;

import com.banking.forms.pipeline.domain.PipelineStepType;
import org.springframework.stereotype.Service;

@Service
public class PipelineOrchestrator {

    public void executeStep(PipelineStepType stepType, String submissionId) {
        // Phase 3: wire step executors from config
    }
}
