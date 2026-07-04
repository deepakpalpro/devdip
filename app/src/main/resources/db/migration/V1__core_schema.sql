-- V1__core_schema.sql
-- Core multi-tenant schema (MySQL-compatible; H2 runs in MySQL mode locally)

CREATE TABLE tenant (
    id            BINARY(16) PRIMARY KEY,
    code          VARCHAR(64) NOT NULL UNIQUE,
    name          VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE app_user (
    id              BINARY(16) PRIMARY KEY,
    tenant_id       BINARY(16) NOT NULL,
    idp_subject     VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255),
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_idp UNIQUE (tenant_id, idp_subject),
    CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE TABLE user_role (
    user_id     BINARY(16) NOT NULL,
    role        VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_role_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE form_definition (
    id              BINARY(16) PRIMARY KEY,
    tenant_id       BINARY(16) NOT NULL,
    code            VARCHAR(64) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    category        VARCHAR(64),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_form_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_form_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE TABLE form_version (
    id                  BINARY(16) PRIMARY KEY,
    form_definition_id  BINARY(16) NOT NULL,
    version_number      INT NOT NULL,
    status              VARCHAR(16) NOT NULL,
    schema_json         CLOB NOT NULL,
    published_at        TIMESTAMP NULL,
    created_by          BINARY(16) NOT NULL,
    CONSTRAINT fk_version_form FOREIGN KEY (form_definition_id) REFERENCES form_definition(id),
    CONSTRAINT uk_form_version UNIQUE (form_definition_id, version_number)
);

CREATE TABLE submission (
    id                  BINARY(16) PRIMARY KEY,
    tenant_id           BINARY(16) NOT NULL,
    form_version_id     BINARY(16) NOT NULL,
    user_id             BINARY(16) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    idempotency_key     VARCHAR(64),
    submitted_at        TIMESTAMP NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_submission_form_version FOREIGN KEY (form_version_id) REFERENCES form_version(id),
    CONSTRAINT fk_submission_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT uk_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE TABLE submission_section_data (
    id              BINARY(16) PRIMARY KEY,
    submission_id   BINARY(16) NOT NULL,
    section_key     VARCHAR(64) NOT NULL,
    data_json       CLOB NOT NULL,
    saved_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_section_submission FOREIGN KEY (submission_id) REFERENCES submission(id),
    CONSTRAINT uk_submission_section UNIQUE (submission_id, section_key)
);

CREATE TABLE submission_event (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id   BINARY(16) NOT NULL,
    event_type      VARCHAR(64) NOT NULL,
    payload_json    CLOB,
    actor_id        BINARY(16),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_submission FOREIGN KEY (submission_id) REFERENCES submission(id)
);

CREATE INDEX idx_submission_event_created ON submission_event (submission_id, created_at);
CREATE INDEX idx_submission_queue ON submission (tenant_id, status, submitted_at);

CREATE TABLE pipeline_config (
    id                  BINARY(16) PRIMARY KEY,
    form_version_id     BINARY(16) NOT NULL,
    config_json         CLOB NOT NULL,
    version             INT NOT NULL DEFAULT 1,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_pipeline_form_version FOREIGN KEY (form_version_id) REFERENCES form_version(id)
);

CREATE TABLE pipeline_execution (
    id                  BINARY(16) PRIMARY KEY,
    submission_id       BINARY(16) NOT NULL,
    pipeline_config_id  BINARY(16) NOT NULL,
    status              VARCHAR(16) NOT NULL,
    current_step        INT NOT NULL DEFAULT 0,
    started_at          TIMESTAMP NOT NULL,
    completed_at        TIMESTAMP NULL,
    error_details       CLOB,
    CONSTRAINT fk_execution_submission FOREIGN KEY (submission_id) REFERENCES submission(id)
);

CREATE TABLE outbox_event (
    id              BINARY(16) PRIMARY KEY,
    event_type      VARCHAR(64) NOT NULL,
    payload_json    CLOB NOT NULL,
    occurred_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published       BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_outbox_unpublished ON outbox_event (published, occurred_at);
