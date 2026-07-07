-- V18: Scoped service instances + bindings (Phase 6.2, client remark 10).

CREATE TABLE service_instance (
    id                  BINARY(16) PRIMARY KEY,
    tenant_id           BINARY(16) NOT NULL,
    service_provider_id BINARY(16) NOT NULL,
    code                VARCHAR(64) NOT NULL,
    name                VARCHAR(128) NOT NULL,
    config_json         CLOB,
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_service_instance_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_service_instance_provider FOREIGN KEY (service_provider_id) REFERENCES service_provider(id)
);

CREATE INDEX idx_service_instance_tenant ON service_instance (tenant_id, enabled);

CREATE TABLE service_binding (
    id                      BINARY(16) PRIMARY KEY,
    tenant_id               BINARY(16) NOT NULL,
    service_instance_id     BINARY(16) NOT NULL,
    scope                   VARCHAR(16) NOT NULL,
    form_version_id         BINARY(16),
    pipeline_definition_id  BINARY(16),
    pipeline_step_id        BINARY(16),
    enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_binding_instance FOREIGN KEY (service_instance_id) REFERENCES service_instance(id),
    CONSTRAINT fk_service_binding_form_version FOREIGN KEY (form_version_id) REFERENCES form_version(id),
    CONSTRAINT fk_service_binding_pipeline FOREIGN KEY (pipeline_definition_id) REFERENCES pipeline_definition(id),
    CONSTRAINT fk_service_binding_step FOREIGN KEY (pipeline_step_id) REFERENCES pipeline_step(id)
);

CREATE INDEX idx_service_binding_form ON service_binding (tenant_id, form_version_id, enabled);
CREATE INDEX idx_service_binding_pipeline ON service_binding (tenant_id, pipeline_definition_id, enabled);
CREATE INDEX idx_service_binding_step ON service_binding (tenant_id, pipeline_step_id, enabled);

-- Demo tenant: form-scoped log-service instance for loan pipeline testing.
INSERT INTO service_instance (id, tenant_id, service_provider_id, code, name, config_json, enabled) VALUES
    (X'e4000000000000000000000000000001', X'11111111111111111111111111111111',
     X'e0000000000000000000000000000001', 'loan-log-sink', 'Loan form log sink',
     '{"note":"form-scoped instance"}', TRUE);
