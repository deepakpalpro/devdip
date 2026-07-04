-- V4: Embedded/nested form demo data
-- Adds a reusable ADDRESS_DETAILS building-block form and embeds it into two host forms:
--   * LOAN_APPLICATION (JSON_BLOB) -> nested data stored natively in section_data_json
--   * ACCOUNT_OPENING  (KEY_VALUE) -> nested data flattened into submission_field_value rows

INSERT INTO form_definition (id, tenant_id, code, name, category, storage_strategy)
VALUES (
    X'55555555555555555555555555555555',
    X'11111111111111111111111111111111',
    'ADDRESS_DETAILS',
    'Address Details',
    'Shared',
    'JSON_BLOB'
);

INSERT INTO form_version (id, form_definition_id, version_number, status, schema_json, created_by)
VALUES (
    X'cccccccccccccccccccccccccccccccc',
    X'55555555555555555555555555555555',
    1,
    'PUBLISHED',
    '{"sections":[{"key":"address","title":"Address","fields":[{"key":"line1","type":"text","label":"Address Line 1","required":true},{"key":"city","type":"text","label":"City","required":true},{"key":"postcode","type":"text","label":"Postcode","required":true}]}]}',
    X'44444444444444444444444444444444'
);

-- Embed ADDRESS_DETAILS into LOAN_APPLICATION via a new "residence" section
UPDATE form_version
SET schema_json = '{"sections":[{"key":"personal-info","title":"Personal Information","fields":[{"key":"firstName","type":"text","label":"First Name","required":true},{"key":"lastName","type":"text","label":"Last Name","required":true}]},{"key":"loan-details","title":"Loan Details","fields":[{"key":"amount","type":"number","label":"Loan Amount","required":true}]},{"key":"residence","title":"Residence","fields":[{"key":"homeAddress","type":"embedded_form","label":"Home Address","formCode":"ADDRESS_DETAILS","required":true}]}]}'
WHERE id = X'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa';

-- Embed ADDRESS_DETAILS into ACCOUNT_OPENING (KEY_VALUE) to exercise nested flatten/unflatten
UPDATE form_version
SET schema_json = '{"sections":[{"key":"account-type","title":"Account Type","fields":[{"key":"accountType","type":"select","label":"Account Type","required":true,"options":["Checking","Savings"]}]},{"key":"mailing","title":"Mailing Address","fields":[{"key":"mailingAddress","type":"embedded_form","label":"Mailing Address","formCode":"ADDRESS_DETAILS","required":true}]}]}'
WHERE id = X'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb';
