package com.banking.forms.pipeline.domain;

public enum PipelineStepType {
    VALIDATE,
    PII_SCRUB,
    AI_EVALUATE,
    SERVICE_CALL,
    DOWNSTREAM,
    NOTIFY
}
