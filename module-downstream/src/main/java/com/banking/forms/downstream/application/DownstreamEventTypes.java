package com.banking.forms.downstream.application;

/** Event types written to the outbox and surfaced in the submission timeline. */
public final class DownstreamEventTypes {

    public static final String SUBMISSION_PROCESSED = "SUBMISSION_PROCESSED";

    private DownstreamEventTypes() {}
}
