-- PDF-to-form import: an admin uploads a form PDF, the platform extracts a proposed form schema,
-- and (after human review) materializes it as a DRAFT form definition.
-- The uploaded bytes are NOT stored (PII-safe) — only a hash + metadata and the generated proposal.
-- (V10 leaves V9 to the submission-progress feature developed on a parallel branch.)
CREATE TABLE form_import_job (
    id                  BINARY(16) PRIMARY KEY,
    tenant_id           BINARY(16) NOT NULL,
    actor_id            BINARY(16) NOT NULL,
    file_name           VARCHAR(255),
    file_hash           VARCHAR(64),
    file_size           BIGINT,
    source_type         VARCHAR(32),
    provider_code       VARCHAR(64),
    source              VARCHAR(32),
    status              VARCHAR(32) NOT NULL,
    suggested_name      VARCHAR(255),
    proposed_schema     CLOB,
    confidence_json     CLOB,
    error_details       VARCHAR(1024),
    form_id             BINARY(16),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_form_import_tenant ON form_import_job (tenant_id, created_at);
