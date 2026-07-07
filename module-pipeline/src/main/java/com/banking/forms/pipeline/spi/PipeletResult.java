package com.banking.forms.pipeline.spi;

/** Outcome of a single pipelet execution. */
public record PipeletResult(PipeletOutcome outcome, String detail) {

    public enum PipeletOutcome {
        SUCCESS,
        SKIPPED,
        FAILED
    }

    public static PipeletResult success() {
        return new PipeletResult(PipeletOutcome.SUCCESS, null);
    }

    public static PipeletResult success(String detail) {
        return new PipeletResult(PipeletOutcome.SUCCESS, detail);
    }

    public static PipeletResult skipped(String reason) {
        return new PipeletResult(PipeletOutcome.SKIPPED, reason);
    }

    public static PipeletResult failed(String detail) {
        return new PipeletResult(PipeletOutcome.FAILED, detail);
    }

    public boolean isSuccess() {
        return outcome == PipeletOutcome.SUCCESS || outcome == PipeletOutcome.SKIPPED;
    }
}
