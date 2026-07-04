import type { FormFieldSchema, FormSectionSchema } from '@banking-forms/form-renderer';

/** Read-only recursive rendering of a single field's value (handles embedded forms). */
export function ReviewField({ field, value }: { field: FormFieldSchema; value: unknown }) {
  if (field.type === 'embedded_form' && field.embeddedForm) {
    const nested = (value as Record<string, Record<string, unknown>>) ?? {};
    return (
      <div>
        <strong>{field.label}</strong>
        <div style={{ paddingLeft: '1rem' }}>
          {field.embeddedForm.sections.flatMap((section) =>
            section.fields.map((embeddedField) => (
              <ReviewField
                key={`${section.key}.${embeddedField.key}`}
                field={embeddedField}
                value={nested[section.key]?.[embeddedField.key]}
              />
            )),
          )}
        </div>
      </div>
    );
  }
  return (
    <div>
      {field.label}: {String(value ?? '—')}
    </div>
  );
}

/** Read-only summary of every section, used in the wizard review step and the status page. */
export function SubmissionReview({
  sections,
  sectionData,
}: {
  sections: FormSectionSchema[];
  sectionData: Record<string, Record<string, unknown>>;
}) {
  return (
    <>
      {sections.map((section) => (
        <dl key={section.key}>
          <dt>{section.title}</dt>
          <dd>
            {section.fields.map((field) => (
              <ReviewField key={field.key} field={field} value={sectionData[section.key]?.[field.key]} />
            ))}
          </dd>
        </dl>
      ))}
    </>
  );
}
