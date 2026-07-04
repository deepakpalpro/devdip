-- V3: Dynamic submission storage
-- Adds per-form storage strategy and introduces the dual-mode section storage tables.
-- JSON_BLOB forms use submission_section.section_data_json.
-- KEY_VALUE forms use normalized rows in submission_field_value (section_data_json stays NULL).

ALTER TABLE form_definition
    ADD COLUMN storage_strategy VARCHAR(16) NOT NULL DEFAULT 'JSON_BLOB';

-- Retire the original single-mode table first so its constraint names (e.g. fk_section_submission)
-- are freed before the replacement tables reuse them (dev/early-stage: no production data to migrate)
DROP TABLE IF EXISTS submission_section_data;

-- Section header table (replaces submission_section_data)
CREATE TABLE submission_section (
    id                  BINARY(16) PRIMARY KEY,
    submission_id       BINARY(16) NOT NULL,
    section_key         VARCHAR(100) NOT NULL,
    section_data_json   CLOB NULL,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_section_submission FOREIGN KEY (submission_id) REFERENCES submission(id),
    CONSTRAINT uq_submission_section UNIQUE (submission_id, section_key)
);

-- Normalized per-field storage for KEY_VALUE forms
CREATE TABLE submission_field_value (
    id              BINARY(16) PRIMARY KEY,
    section_id      BINARY(16) NOT NULL,
    field_key       VARCHAR(191) NOT NULL,
    field_value     VARCHAR(512) NULL,
    is_encrypted    BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_field_section FOREIGN KEY (section_id) REFERENCES submission_section(id),
    CONSTRAINT uq_section_field UNIQUE (section_id, field_key)
);

-- Value lookup index for KEY_VALUE query capabilities.
-- Portable form; on MySQL a prefix index (field_key, field_value(50)) is equivalent.
CREATE INDEX idx_field_lookup ON submission_field_value (field_key, field_value);

-- Demonstrate KEY_VALUE for a regulated form; leave others on the JSON_BLOB default
UPDATE form_definition
SET storage_strategy = 'KEY_VALUE'
WHERE code = 'ACCOUNT_OPENING';
