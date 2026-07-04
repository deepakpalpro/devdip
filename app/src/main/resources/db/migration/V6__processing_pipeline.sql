-- V6: Automated processing pipeline
-- Stores the PII-sanitized copy of a submission produced by the on-submit pipeline
-- (validate -> PII scrub -> downstream dispatch). One row per submission (latest scrub).
-- Pipeline execution status/audit reuses the existing pipeline_execution and submission_event tables.

CREATE TABLE submission_sanitized_payload (
    id              BINARY(16) PRIMARY KEY,
    submission_id   BINARY(16) NOT NULL,
    payload_json    CLOB NOT NULL,
    transformed_json CLOB NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sanitized_submission FOREIGN KEY (submission_id) REFERENCES submission(id),
    CONSTRAINT uq_sanitized_submission UNIQUE (submission_id)
);
