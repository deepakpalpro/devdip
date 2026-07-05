import type { ExtractedEntity, FlatField, PrefillPreview } from '../types.js';
import { buildSectionDataFromFlat, missingRequiredFields } from './form-schema.js';
import type { FormSchema } from '../types.js';

const ENTITY_TO_FIELD: Record<string, string[]> = {
  firstName: ['firstName', 'first_name', 'givenName'],
  lastName: ['lastName', 'last_name', 'surname', 'familyName'],
  amount: ['amount', 'loanAmount', 'loan_amount'],
  email: ['email', 'emailAddress'],
  phone: ['phone', 'phoneNumber', 'mobile'],
  accountType: ['accountType', 'account_type'],
  city: ['city'],
  postcode: ['postcode', 'zip', 'postalCode'],
  line1: ['line1', 'addressLine1', 'street'],
};

export function mapEntitiesToFields(
  formCode: string,
  schema: FormSchema,
  fields: FlatField[],
  entities: ExtractedEntity[],
): PrefillPreview {
  const flatValues: Record<string, unknown> = {};
  const mappedFields: string[] = [];
  const ambiguous: string[] = [];

  for (const entity of entities) {
    const candidates = ENTITY_TO_FIELD[entity.name] ?? [entity.name];
    const matches = fields.filter((f) =>
      candidates.some(
        (c) =>
          f.fieldKey.toLowerCase().endsWith(c.toLowerCase()) ||
          f.label.toLowerCase().includes(c.toLowerCase()),
      ),
    );

    if (matches.length === 1) {
      flatValues[matches[0].path] = entity.value;
      mappedFields.push(matches[0].path);
    } else if (matches.length > 1) {
      ambiguous.push(`${entity.name} → ${matches.map((m) => m.path).join('|')}`);
    }
  }

  const sectionData = buildSectionDataFromFlat(schema, flatValues);
  const missingRequired = missingRequiredFields(fields, flatValues);

  return {
    formCode,
    sectionData,
    mappedFields,
    missingRequired,
    ambiguous,
  };
}
