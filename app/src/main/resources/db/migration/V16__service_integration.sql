-- V16: Service-integration adapter registry + call log (US-8.4).
-- Data-driven external API adapters (credit bureau, identity verification, REST hooks, …) managed
-- from the admin Settings UI. `service_call_log` records every invocation for audit. Secrets are
-- never stored here (config holds only a `secretRef`).

CREATE TABLE service_provider (
    id           BINARY(16) PRIMARY KEY,
    code         VARCHAR(64) NOT NULL UNIQUE,
    name         VARCHAR(128) NOT NULL,
    adapter_type VARCHAR(32) NOT NULL,
    enabled      BOOLEAN NOT NULL DEFAULT FALSE,
    priority     INT NOT NULL DEFAULT 100,
    config_json  CLOB,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_service_provider_type ON service_provider (adapter_type, enabled, priority);

CREATE TABLE service_call_log (
    id             BINARY(16) PRIMARY KEY,
    tenant_id      BINARY(16) NOT NULL,
    submission_id  BINARY(16),
    provider_code  VARCHAR(64) NOT NULL,
    adapter_type   VARCHAR(32) NOT NULL,
    operation      VARCHAR(64) NOT NULL,
    form_code      VARCHAR(64),
    status         VARCHAR(16) NOT NULL,
    provider_ref   VARCHAR(256),
    response_json  CLOB,
    error          CLOB,
    duration_ms    BIGINT,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_service_call_submission ON service_call_log (submission_id, created_at);
CREATE INDEX idx_service_call_provider ON service_call_log (provider_code, created_at);

-- Providers. `log-service` is enabled by default for demoability; `rest-api` ships with a real
-- JDK-HttpClient implementation but is disabled until an admin sets an endpoint. `credit-bureau`
-- and `identity-verify` are configured-but-unavailable seams (no implementation bean yet).
INSERT INTO service_provider (id, code, name, adapter_type, enabled, priority, config_json) VALUES
    (X'e0000000000000000000000000000001', 'log-service',      'Log sink (no external call)',           'log',      TRUE,  10, NULL),
    (X'e0000000000000000000000000000002', 'rest-api',         'REST API (HTTP POST/GET)',              'rest',     FALSE, 20,
     '{"endpoint":"","method":"POST","secretRef":"SERVICE_API_TOKEN"}'),
    (X'e0000000000000000000000000000003', 'credit-bureau',    'Credit bureau check (external seam)',   'credit',   FALSE, 30,
     '{"endpoint":"","productCode":"STANDARD"}'),
    (X'e0000000000000000000000000000004', 'identity-verify',  'Identity verification (external seam)', 'identity', FALSE, 40,
     '{"endpoint":"","secretRef":"IDENTITY_API_KEY"}');
