-- V12: AI evaluation of a submission (advisory risk score + recommendation).
-- Produced by the pipeline AI_EVALUATE step from the PII-sanitized payload. Advisory only — surfaced
-- to the human reviewer, never auto-deciding. One row per submission (latest evaluation).

CREATE TABLE submission_ai_evaluation (
    id                 BINARY(16) PRIMARY KEY,
    submission_id      BINARY(16) NOT NULL,
    evaluator_id       VARCHAR(64) NOT NULL,
    model              VARCHAR(128) NULL,
    risk_score         DOUBLE NOT NULL,
    recommendation     VARCHAR(16) NOT NULL,
    rationale          CLOB NULL,
    signals_json       CLOB NULL,
    processing_time_ms BIGINT NOT NULL DEFAULT 0,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_eval_submission FOREIGN KEY (submission_id) REFERENCES submission(id),
    CONSTRAINT uq_ai_eval_submission UNIQUE (submission_id)
);
