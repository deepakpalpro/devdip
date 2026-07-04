-- Dev seed data for local frontend development

INSERT INTO tenant (id, code, name)
VALUES (X'11111111111111111111111111111111', 'demo-bank', 'Demo Bank');

INSERT INTO app_user (id, tenant_id, idp_subject, email, display_name, status)
VALUES (
    X'44444444444444444444444444444444',
    X'11111111111111111111111111111111',
    'dev-user',
    'dev@demo-bank.local',
    'Dev User',
    'ACTIVE'
);

INSERT INTO form_definition (id, tenant_id, code, name, category)
VALUES
    (X'22222222222222222222222222222222', X'11111111111111111111111111111111', 'LOAN_APPLICATION', 'Personal Loan Application', 'Lending'),
    (X'33333333333333333333333333333333', X'11111111111111111111111111111111', 'ACCOUNT_OPENING', 'New Account Opening', 'Deposits');

INSERT INTO form_version (id, form_definition_id, version_number, status, schema_json, created_by)
VALUES
    (
        X'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
        X'22222222222222222222222222222222',
        1,
        'PUBLISHED',
        '{"sections":[{"key":"personal-info","title":"Personal Information","fields":[{"key":"firstName","type":"text","label":"First Name","required":true},{"key":"lastName","type":"text","label":"Last Name","required":true}]},{"key":"loan-details","title":"Loan Details","fields":[{"key":"amount","type":"number","label":"Loan Amount","required":true}]}]}',
        X'44444444444444444444444444444444'
    ),
    (
        X'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
        X'33333333333333333333333333333333',
        1,
        'PUBLISHED',
        '{"sections":[{"key":"account-type","title":"Account Type","fields":[{"key":"accountType","type":"select","label":"Account Type","required":true,"options":["Checking","Savings"]}]}]}',
        X'44444444444444444444444444444444'
    );
