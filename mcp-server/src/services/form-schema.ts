import type { AgentFormDefinition, FlatField, FormDetail, FormFieldSchema, FormSchema } from '../types.js';

export function flattenFormSchema(form: FormDetail): AgentFormDefinition {
  const fields: FlatField[] = [];
  for (const section of form.schema.sections) {
    collectFields(section.key, section.fields, '', fields);
  }
  return {
    code: form.code,
    name: form.name,
    category: form.category,
    formVersionId: form.formVersionId,
    sections: form.schema.sections,
    fields,
  };
}

function collectFields(
  sectionKey: string,
  fieldList: FormFieldSchema[],
  prefix: string,
  out: FlatField[],
): void {
  for (const field of fieldList) {
    const path = prefix ? `${prefix}.${field.key}` : field.key;
    if (field.type === 'embedded_form' && field.embeddedForm) {
      for (const embeddedSection of field.embeddedForm.sections) {
        collectFields(
          sectionKey,
          embeddedSection.fields,
          `${path}.${embeddedSection.key}`,
          out,
        );
      }
      continue;
    }
    out.push({
      path,
      sectionKey,
      fieldKey: path,
      label: field.label,
      type: field.type,
      required: field.required ?? false,
      options: field.options,
    });
  }
}

export function buildSectionDataFromFlat(
  schema: FormSchema,
  flatValues: Record<string, unknown>,
): Record<string, Record<string, unknown>> {
  const sectionData: Record<string, Record<string, unknown>> = {};
  for (const section of schema.sections) {
    const data = nestSectionFields(section.fields, '', flatValues);
    if (Object.keys(data).length > 0) {
      sectionData[section.key] = data;
    }
  }
  return sectionData;
}

function nestSectionFields(
  fields: FormFieldSchema[],
  prefix: string,
  flatValues: Record<string, unknown>,
): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  for (const field of fields) {
    const path = prefix ? `${prefix}.${field.key}` : field.key;
    if (field.type === 'embedded_form' && field.embeddedForm) {
      for (const embeddedSection of field.embeddedForm.sections) {
        const nested = nestSectionFields(
          embeddedSection.fields,
          `${path}.${embeddedSection.key}`,
          flatValues,
        );
        if (Object.keys(nested).length > 0) {
          result[field.key] = { ...(result[field.key] as Record<string, unknown> | undefined), [embeddedSection.key]: nested };
        }
      }
      const direct = flatValues[path];
      if (direct !== undefined) {
        result[field.key] = direct;
      }
      continue;
    }
    if (flatValues[path] !== undefined) {
      setNested(result, field.key, flatValues[path]);
    }
  }
  return result;
}

function setNested(target: Record<string, unknown>, key: string, value: unknown): void {
  target[key] = value;
}

export function missingRequiredFields(
  fields: FlatField[],
  flatValues: Record<string, unknown>,
): string[] {
  return fields
    .filter((f) => f.required && flatValues[f.path] === undefined)
    .map((f) => f.path);
}
