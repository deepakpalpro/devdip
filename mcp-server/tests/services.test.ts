import { describe, expect, it } from 'vitest';
import { extractEntities } from '../src/services/entity-extractor.js';
import { mapEntitiesToFields } from '../src/services/field-mapper.js';
import { flattenFormSchema, missingRequiredFields } from '../src/services/form-schema.js';
import { suggestForms } from '../src/services/intent-matcher.js';
import type { FormDetail, FormSummary } from '../src/types.js';

const CATALOG: FormSummary[] = [
  { code: 'LOAN_APPLICATION', name: 'Personal Loan Application', category: 'Lending' },
  { code: 'ACCOUNT_OPENING', name: 'New Account Opening', category: 'Deposits' },
];

describe('intent-matcher', () => {
  it('suggests loan form for loan intent', () => {
    const results = suggestForms('I need a personal loan for $25000', CATALOG);
    expect(results[0].formCode).toBe('LOAN_APPLICATION');
  });

  it('suggests account form for savings intent', () => {
    const results = suggestForms('I want to open a savings account', CATALOG);
    expect(results[0].formCode).toBe('ACCOUNT_OPENING');
  });
});

describe('entity-extractor', () => {
  it('extracts name and amount', () => {
    const entities = extractEntities('My name is Jane Doe and I need $25000');
    expect(entities.some((e) => e.name === 'firstName' && e.value === 'Jane')).toBe(true);
    expect(entities.some((e) => e.name === 'amount' && e.value === 25000)).toBe(true);
  });
});

describe('field-mapper', () => {
  const form: FormDetail = {
    code: 'LOAN_APPLICATION',
    name: 'Loan',
    category: 'Lending',
    formVersionId: 'v1',
    schema: {
      sections: [
        {
          key: 'personal-info',
          title: 'Personal',
          fields: [
            { key: 'firstName', type: 'text', label: 'First Name', required: true },
            { key: 'lastName', type: 'text', label: 'Last Name', required: true },
          ],
        },
        {
          key: 'loan-details',
          title: 'Loan',
          fields: [{ key: 'amount', type: 'number', label: 'Loan Amount', required: true }],
        },
      ],
    },
  };

  it('maps entities to form fields', () => {
    const flat = flattenFormSchema(form);
    const preview = mapEntitiesToFields(
      form.code,
      form.schema,
      flat.fields,
      extractEntities('My name is Jane Doe and I need $25000'),
    );
    expect(preview.sectionData['personal-info']?.firstName).toBe('Jane');
    expect(preview.sectionData['loan-details']?.amount).toBe(25000);
    expect(missingRequiredFields(flat.fields, { 'lastName': 'Doe' }).length).toBeGreaterThan(0);
  });
});
