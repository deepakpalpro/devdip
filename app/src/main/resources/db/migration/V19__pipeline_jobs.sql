-- V19: Batch + real-time pipeline jobs (Phase 6.2, client remark 11).

CREATE TABLE pipeline_job_definition (
    id                      BINARY(16) PRIMARY KEY,
    tenant_id               BINARY(16) NOT NULL,
    form_version_id         BINARY(16),
    code                    VARCHAR(64) NOT NULL,
    name                    VARCHAR(128) NOT NULL,
    job_type                VARCHAR(16) NOT NULL,
    pipeline_definition_id  BINARY(16) NOT NULL,
    trigger_event           VARCHAR(32),
    query_config_json       CLOB,
    schedule_cron           VARCHAR(64),
    enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    last_run_at             TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_pipeline_job_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_job_pipeline FOREIGN KEY (pipeline_definition_id) REFERENCES pipeline_definition(id),
    CONSTRAINT fk_job_form_version FOREIGN KEY (form_version_id) REFERENCES form_version(id)
);

CREATE INDEX idx_pipeline_job_type ON pipeline_job_definition (tenant_id, job_type, enabled);

CREATE TABLE pipeline_job_run (
    id                  BINARY(16) PRIMARY KEY,
    job_definition_id   BINARY(16) NOT NULL,
    status              VARCHAR(16) NOT NULL,
    records_processed   INT NOT NULL DEFAULT 0,
    error_message       CLOB,
    started_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at         TIMESTAMP,
    CONSTRAINT fk_job_run_definition FOREIGN KEY (job_definition_id) REFERENCES pipeline_job_definition(id)
);

CREATE INDEX idx_pipeline_job_run_job ON pipeline_job_run (job_definition_id, started_at);

-- Job-oriented pipelets.
INSERT INTO pipelet_definition (id, code, name, description, config_schema_json, enabled) VALUES
    (X'e1000000000000000000000000000007', 'query-submissions', 'Query submissions',
     'Load submission IDs matching filter into job context', '{"type":"object","properties":{"status":{"type":"string"}}}', TRUE),
    (X'e1000000000000000000000000000008', 'connector-push', 'Connector push',
     'Push sanitized payload to downstream connectors (optional connector filter)', '{"type":"object","properties":{"connectors":{"type":"array"}}}', TRUE);

-- Demo: real-time export pipeline on approval (pii-scrub + connector-push).
INSERT INTO pipeline_definition (id, tenant_id, code, name, description, version, status, system_default) VALUES
    (X'e2000000000000000000000000000002', X'11111111111111111111111111111111', 'realtime-export-on-approve',
     'Real-time export on approval', 'Scrub + push to downstream on approval', 1, 'ACTIVE', FALSE);

INSERT INTO pipeline_step (id, pipeline_definition_id, step_order, step_key, pipelet_code, properties_json) VALUES
    (X'e3000000000000000000000000000006', X'e2000000000000000000000000000002', 1, 'pii-scrub', 'pii-scrub', NULL),
    (X'e3000000000000000000000000000007', X'e2000000000000000000000000000002', 2, 'connector-push', 'connector-push', NULL);
