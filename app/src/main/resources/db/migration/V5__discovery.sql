-- V5: Form Discovery Wizard (preliminary questionnaire + triage + pre-population)
-- discovery_questionnaire holds three config documents: questions (schema_json), triage rules
-- (rules_json), and pre-population mappings (mappings_json). discovery_session persists a user's
-- answers + top recommendation so the chosen application can be pre-filled.

CREATE TABLE discovery_questionnaire (
    id              BINARY(16) PRIMARY KEY,
    tenant_id       BINARY(16) NOT NULL,
    code            VARCHAR(64) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    schema_json     CLOB NOT NULL,
    rules_json      CLOB NOT NULL,
    mappings_json   CLOB NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_questionnaire_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_questionnaire_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE TABLE discovery_session (
    id                      BINARY(16) PRIMARY KEY,
    tenant_id               BINARY(16) NOT NULL,
    user_id                 BINARY(16) NOT NULL,
    questionnaire_code      VARCHAR(64) NOT NULL,
    answers_json            CLOB NOT NULL,
    recommended_form_code   VARCHAR(64),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE INDEX idx_discovery_session_user ON discovery_session (tenant_id, user_id, created_at);

-- Seed a "What are you here for?" questionnaire that triages between the loan and account forms
-- and pre-populates the applicant's name / loan amount / account type.
INSERT INTO discovery_questionnaire (id, tenant_id, code, name, schema_json, rules_json, mappings_json)
VALUES (
    X'66666666666666666666666666666666',
    X'11111111111111111111111111111111',
    'BANKING_NEEDS',
    'Find the right application',
    '{"sections":[{"key":"needs","title":"Tell us what you need","fields":[{"key":"goal","type":"select","label":"What brings you in today?","required":true,"options":["Borrow money","Open a bank account"]},{"key":"firstName","type":"text","label":"First name","required":true},{"key":"lastName","type":"text","label":"Last name","required":true},{"key":"amount","type":"number","label":"How much would you like to borrow? (optional)"},{"key":"accountType","type":"select","label":"Preferred account type (optional)","options":["Checking","Savings"]}]}]}',
    '[{"targetFormCode":"LOAN_APPLICATION","weight":10,"rationale":"You told us you want to borrow money.","conditions":[{"questionKey":"goal","operator":"EQUALS","value":"Borrow money"}]},{"targetFormCode":"ACCOUNT_OPENING","weight":10,"rationale":"You told us you want to open a bank account.","conditions":[{"questionKey":"goal","operator":"EQUALS","value":"Open a bank account"}]},{"targetFormCode":"LOAN_APPLICATION","weight":3,"rationale":"You entered a loan amount.","conditions":[{"questionKey":"amount","operator":"GT","value":0}]},{"targetFormCode":"ACCOUNT_OPENING","weight":3,"rationale":"You selected a preferred account type.","conditions":[{"questionKey":"accountType","operator":"EXISTS"}]}]',
    '{"LOAN_APPLICATION":[{"questionKey":"firstName","targetSection":"personal-info","targetField":"firstName"},{"questionKey":"lastName","targetSection":"personal-info","targetField":"lastName"},{"questionKey":"amount","targetSection":"loan-details","targetField":"amount"}],"ACCOUNT_OPENING":[{"questionKey":"accountType","targetSection":"account-type","targetField":"accountType"}]}'
);
