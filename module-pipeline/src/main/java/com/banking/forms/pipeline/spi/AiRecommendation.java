package com.banking.forms.pipeline.spi;

/**
 * Advisory outcome of an {@link AiEvaluator}. The recommendation is surfaced to the human reviewer —
 * it never auto-decides a submission (human-in-the-loop for regulated decisions).
 */
public enum AiRecommendation {
    /** Low risk — evaluator suggests the submission looks approvable. */
    APPROVE,
    /** Uncertain / needs a human look. This is also the safe fallback on any evaluator failure. */
    REVIEW,
    /** High risk — evaluator suggests declining, subject to human confirmation. */
    REJECT
}
