export interface EmbeddedFormSchema {
  code: string;
  name?: string;
  sections: FormSectionSchema[];
}

export interface FormFieldSchema {
  key: string;
  type: string;
  label: string;
  required?: boolean;
  options?: string[];
  formCode?: string;
  embeddedForm?: EmbeddedFormSchema;
  embeddedUnavailable?: boolean;
}

export interface FormSectionSchema {
  key: string;
  title: string;
  fields: FormFieldSchema[];
}

export interface FormSchema {
  sections: FormSectionSchema[];
}

export interface SectionRendererProps {
  section: FormSectionSchema;
  values: Record<string, unknown>;
  onChange: (fieldKey: string, value: unknown) => void;
  errors?: Record<string, string>;
}

function ScalarField({
  field,
  value,
  onChange,
  error,
}: {
  field: FormFieldSchema;
  value: unknown;
  onChange: (value: unknown) => void;
  error?: string;
}) {
  return (
    <label style={{ display: 'grid', gap: '0.375rem' }}>
      <span style={{ fontWeight: 600 }}>
        {field.label}
        {field.required ? ' *' : ''}
      </span>
      {field.type === 'select' ? (
        <select value={String(value ?? '')} onChange={(event) => onChange(event.target.value)}>
          <option value="">Select…</option>
          {field.options?.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
      ) : (
        <input
          type={field.type === 'number' ? 'number' : 'text'}
          value={String(value ?? '')}
          onChange={(event) =>
            onChange(field.type === 'number' ? Number(event.target.value) : event.target.value)
          }
        />
      )}
      {error ? <span style={{ color: 'var(--bf-danger)', fontSize: '0.875rem' }}>{error}</span> : null}
    </label>
  );
}

function EmbeddedFormField({
  field,
  value,
  onChange,
  errors,
  pathPrefix,
}: {
  field: FormFieldSchema;
  value: Record<string, Record<string, unknown>>;
  onChange: (value: Record<string, Record<string, unknown>>) => void;
  errors: Record<string, string>;
  pathPrefix: string;
}) {
  if (!field.embeddedForm) {
    return (
      <div
        style={{
          padding: '0.75rem',
          border: '1px dashed var(--bf-border, #d0d5dd)',
          borderRadius: '0.5rem',
          color: 'var(--bf-danger)',
        }}
      >
        Embedded form &quot;{field.formCode ?? 'unknown'}&quot; is unavailable.
      </div>
    );
  }

  const nested = value ?? {};
  const setNestedValue = (sectionKey: string, fieldKey: string, fieldValue: unknown) => {
    onChange({
      ...nested,
      [sectionKey]: { ...(nested[sectionKey] ?? {}), [fieldKey]: fieldValue },
    });
  };

  return (
    <fieldset
      style={{
        display: 'grid',
        gap: '1rem',
        border: '1px solid var(--bf-border, #d0d5dd)',
        borderRadius: '0.5rem',
        padding: '1rem',
      }}
    >
      <legend style={{ fontWeight: 600, padding: '0 0.375rem' }}>
        {field.label}
        {field.required ? ' *' : ''}
      </legend>
      {field.embeddedForm.sections.map((embeddedSection) => (
        <div key={embeddedSection.key} style={{ display: 'grid', gap: '0.75rem' }}>
          <h4 style={{ margin: 0, fontSize: '0.95rem', color: 'var(--bf-muted, #667085)' }}>
            {embeddedSection.title}
          </h4>
          <FieldsRenderer
            fields={embeddedSection.fields}
            values={nested[embeddedSection.key] ?? {}}
            onChange={(fieldKey, fieldValue) => setNestedValue(embeddedSection.key, fieldKey, fieldValue)}
            errors={errors}
            pathPrefix={`${pathPrefix}${embeddedSection.key}.`}
          />
        </div>
      ))}
    </fieldset>
  );
}

function FieldsRenderer({
  fields,
  values,
  onChange,
  errors,
  pathPrefix,
}: {
  fields: FormFieldSchema[];
  values: Record<string, unknown>;
  onChange: (fieldKey: string, value: unknown) => void;
  errors: Record<string, string>;
  pathPrefix: string;
}) {
  return (
    <>
      {fields.map((field) => {
        if (field.type === 'embedded_form') {
          return (
            <EmbeddedFormField
              key={field.key}
              field={field}
              value={(values[field.key] as Record<string, Record<string, unknown>>) ?? {}}
              onChange={(nestedValue) => onChange(field.key, nestedValue)}
              errors={errors}
              pathPrefix={`${pathPrefix}${field.key}.`}
            />
          );
        }
        return (
          <ScalarField
            key={field.key}
            field={field}
            value={values[field.key]}
            onChange={(value) => onChange(field.key, value)}
            error={errors[`${pathPrefix}${field.key}`] ?? errors[field.key]}
          />
        );
      })}
    </>
  );
}

export function SectionRenderer({ section, values, onChange, errors = {} }: SectionRendererProps) {
  return (
    <section style={{ display: 'grid', gap: '1rem' }}>
      <h2 style={{ margin: 0, fontSize: '1.125rem' }}>{section.title}</h2>
      <FieldsRenderer fields={section.fields} values={values} onChange={onChange} errors={errors} pathPrefix="" />
    </section>
  );
}
