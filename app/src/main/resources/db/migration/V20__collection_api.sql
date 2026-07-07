-- V20: Collection API keys for external sanitized-data queries (Phase 6.3).

CREATE TABLE collection_api_key (
    id          BINARY(16) PRIMARY KEY,
    tenant_id   BINARY(16) NOT NULL,
    name        VARCHAR(128) NOT NULL,
    key_hash    VARCHAR(64) NOT NULL,
    key_prefix  VARCHAR(12) NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    CONSTRAINT uk_collection_api_key_hash UNIQUE (key_hash)
);

CREATE INDEX idx_collection_api_key_tenant ON collection_api_key (tenant_id, enabled);

-- Dev key: plain text `bfp_dev_collection_key` (SHA-256 hash stored; never log the plain key in prod).
INSERT INTO collection_api_key (id, tenant_id, name, key_hash, key_prefix, enabled) VALUES
    (X'f0000000000000000000000000000001', X'11111111111111111111111111111111', 'Dev collection reader',
     '663bf722d40b5a2553a24929e2f7139d70d23f077c61602ff5a0ac0e57461839', 'bfp_dev_coll', TRUE);
