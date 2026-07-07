-- V14: Downstream connectors + transactional outbox (US-8.1).
-- Replaces the stubbed DOWNSTREAM pipeline step with real, reliable delivery of the PII-scrubbed
-- submission payload to configurable destinations. Mirrors the notification/form-import provider
-- pattern: which adapter serves a connector type — and whether an external sink is used at all — is
-- data-driven from `downstream_provider`, managed in the admin Settings UI.
--
-- `downstream_outbox` is the durable transactional outbox for downstream delivery (distinct from the
-- generic `outbox_event` placeholder in V1 reserved for future broker-based eventing). The pipeline
-- writes PENDING rows in the SAME transaction that advances the submission, so delivery intent
-- commits atomically with the state change. The async dispatcher then picks them up, delivers via the
-- connector, and moves each to DISPATCHED / FAILED (dead-letter after retries).

CREATE TABLE downstream_provider (
    id             BINARY(16) PRIMARY KEY,
    code           VARCHAR(64) NOT NULL UNIQUE,
    name           VARCHAR(128) NOT NULL,
    connector_type VARCHAR(32) NOT NULL,
    enabled        BOOLEAN NOT NULL DEFAULT FALSE,
    priority       INT NOT NULL DEFAULT 100,
    config_json    CLOB,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_downstream_provider_type ON downstream_provider (connector_type, enabled, priority);

-- Durable outbox + delivery log. One row per (submission, provider) fan-out. `payload_json` is the
-- sanitized, ready-to-deliver body; it never contains raw PII (produced by the PII_SCRUB step).
CREATE TABLE downstream_outbox (
    id             BINARY(16) PRIMARY KEY,
    tenant_id      BINARY(16) NOT NULL,
    submission_id  BINARY(16),
    event_type     VARCHAR(64) NOT NULL,
    form_code      VARCHAR(64),
    provider_code  VARCHAR(64) NOT NULL,
    connector_type VARCHAR(32) NOT NULL,
    payload_json   CLOB,
    status         VARCHAR(16) NOT NULL,
    attempts       INT NOT NULL DEFAULT 0,
    provider_ref   VARCHAR(256),
    error          CLOB,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_downstream_outbox_status ON downstream_outbox (status, created_at);
CREATE INDEX idx_downstream_outbox_submission ON downstream_outbox (submission_id);

-- Providers. The zero-setup `log-sink` is enabled by default so the downstream step is demoable and
-- testable out of the box; `rest-webhook` ships with a real JDK-HttpClient implementation but is
-- disabled until an admin sets an endpoint. `kafka-stream` and `s3-archive` are configured-but-
-- unavailable seams (no implementation bean yet) — enabling them without an adapter dead-letters with
-- a clear "no-implementation" reason.
INSERT INTO downstream_provider (id, code, name, connector_type, enabled, priority, config_json) VALUES
    (X'd0000000000000000000000000000001', 'log-sink',     'Log sink (no external delivery)',     'log',   TRUE,  10, NULL),
    (X'd0000000000000000000000000000002', 'rest-webhook', 'REST webhook (HTTP POST)',            'rest',  FALSE, 20,
     '{"endpoint":"","method":"POST","secretRef":"DOWNSTREAM_WEBHOOK_TOKEN"}'),
    (X'd0000000000000000000000000000003', 'kafka-stream', 'Kafka topic (core-banking stream)',   'kafka', FALSE, 30,
     '{"bootstrapServers":"","topic":"submissions.processed"}'),
    (X'd0000000000000000000000000000004', 's3-archive',   'S3 archive (object store)',           's3',    FALSE, 40,
     '{"bucket":"","region":"us-east-1","prefix":"submissions/","secretRef":"AWS_ACCESS_KEY"}');
