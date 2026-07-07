-- V17: Configurable pipelines — pipelet catalog, pipeline definitions, form bindings (Phase 6.1).

CREATE TABLE pipelet_definition (
    id                  BINARY(16) PRIMARY KEY,
    code                VARCHAR(64) NOT NULL UNIQUE,
    name                VARCHAR(128) NOT NULL,
    description         VARCHAR(512),
    config_schema_json  CLOB,
    enabled             BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE pipeline_definition (
    id              BINARY(16) PRIMARY KEY,
    tenant_id       BINARY(16) NOT NULL,
    code            VARCHAR(64) NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     VARCHAR(512),
    version         INT NOT NULL DEFAULT 1,
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    system_default  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_pipeline_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE pipeline_step (
    id                      BINARY(16) PRIMARY KEY,
    pipeline_definition_id  BINARY(16) NOT NULL,
    step_order              INT NOT NULL,
    step_key                VARCHAR(64) NOT NULL,
    pipelet_code            VARCHAR(64) NOT NULL,
    properties_json         CLOB,
    CONSTRAINT fk_step_pipeline FOREIGN KEY (pipeline_definition_id) REFERENCES pipeline_definition(id),
    CONSTRAINT uk_pipeline_step_order UNIQUE (pipeline_definition_id, step_order),
    CONSTRAINT uk_pipeline_step_key UNIQUE (pipeline_definition_id, step_key)
);

CREATE TABLE form_pipeline_binding (
    id                      BINARY(16) PRIMARY KEY,
    tenant_id               BINARY(16) NOT NULL,
    form_version_id         BINARY(16) NOT NULL,
    pipeline_definition_id  BINARY(16) NOT NULL,
    trigger_event           VARCHAR(32) NOT NULL,
    enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_binding_form_version FOREIGN KEY (form_version_id) REFERENCES form_version(id),
    CONSTRAINT fk_binding_pipeline FOREIGN KEY (pipeline_definition_id) REFERENCES pipeline_definition(id),
    CONSTRAINT uk_form_trigger UNIQUE (form_version_id, trigger_event)
);

CREATE INDEX idx_form_pipeline_binding_tenant ON form_pipeline_binding (tenant_id, enabled);

INSERT INTO pipelet_definition (id, code, name, description, config_schema_json, enabled) VALUES
    (X'e1000000000000000000000000000001', 'validate', 'Validate submission', 'Re-run section validation on stored data', NULL, TRUE),
    (X'e1000000000000000000000000000002', 'pii-scrub', 'PII scrub', 'Produce sanitized payload copy', '{"type":"object","properties":{"profile":{"type":"string"}}}', TRUE),
    (X'e1000000000000000000000000000003', 'ai-evaluate', 'AI evaluate', 'Advisory risk scoring on sanitized payload', '{"type":"object","properties":{"evaluatorId":{"type":"string"},"enabled":{"type":"boolean"}}}', TRUE),
    (X'e1000000000000000000000000000004', 'service-call', 'Service call', 'Invoke enabled external service adapters', '{"type":"object","properties":{"enabled":{"type":"boolean"}}}', TRUE),
    (X'e1000000000000000000000000000005', 'downstream', 'Downstream dispatch', 'Enqueue sanitized payload to downstream connectors', NULL, TRUE),
    (X'e1000000000000000000000000000006', 'notify', 'Notify customer', 'Send lifecycle notification messages', '{"type":"object","properties":{"event":{"type":"string"}}}', TRUE);

INSERT INTO pipeline_definition (id, tenant_id, code, name, description, version, status, system_default) VALUES
    (X'e2000000000000000000000000000001', X'11111111111111111111111111111111', 'system-default-submit',
     'System default submit pipeline', 'Built-in pipeline matching legacy hardcoded flow', 1, 'ACTIVE', TRUE);

INSERT INTO pipeline_step (id, pipeline_definition_id, step_order, step_key, pipelet_code, properties_json) VALUES
    (X'e3000000000000000000000000000001', X'e2000000000000000000000000000001', 1, 'validate', 'validate', NULL),
    (X'e3000000000000000000000000000002', X'e2000000000000000000000000000001', 2, 'pii-scrub', 'pii-scrub', NULL),
    (X'e3000000000000000000000000000003', X'e2000000000000000000000000000001', 3, 'ai-evaluate', 'ai-evaluate', NULL),
    (X'e3000000000000000000000000000004', X'e2000000000000000000000000000001', 4, 'service-call', 'service-call', NULL),
    (X'e3000000000000000000000000000005', X'e2000000000000000000000000000001', 5, 'downstream', 'downstream', NULL);
