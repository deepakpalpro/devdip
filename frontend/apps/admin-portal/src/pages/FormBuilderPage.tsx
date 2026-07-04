import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, ErrorState, LoadingState, PageHeader } from '@banking-forms/ui';
import { ApiError, type AdminFormVersion, type FormSchema } from '@banking-forms/api-client';
import {
  useAdminForm,
  useCreateVersion,
  usePublishVersion,
  useUpdateVersionSchema,
} from '../hooks/useAdminForms';
import { statusBadgeClass } from './formStatus';
import './admin-forms.css';

const SCHEMA_HINT =
  'Schema shape: { "sections": [ { "key", "title", "fields": [ { "key", "type", "label", "required?", "options?" } ] } ] }';

function mutationError(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return fallback;
}

export function FormBuilderPage() {
  const { formId } = useParams<{ formId: string }>();
  const { data: form, isLoading, error } = useAdminForm(formId);

  const [selectedVersionId, setSelectedVersionId] = useState<string | null>(null);
  const [schemaText, setSchemaText] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);

  const updateSchema = useUpdateVersionSchema(formId);
  const publish = usePublishVersion(formId);
  const createVersion = useCreateVersion(formId);

  const versions = useMemo(() => form?.versions ?? [], [form]);
  const selected: AdminFormVersion | undefined =
    versions.find((version) => version.id === selectedVersionId) ?? versions[0];

  useEffect(() => {
    if (selected) {
      setSchemaText(JSON.stringify(selected.schema, null, 2));
      setLocalError(null);
    }
    // Re-sync the editor when switching versions; deliberately keyed on id only so
    // an in-progress edit isn't clobbered by background refetches of the same version.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selected?.id]);

  if (isLoading) {
    return <LoadingState message="Loading form…" />;
  }

  if (error || !form) {
    return <ErrorState message={error instanceof Error ? error.message : 'Form not found'} />;
  }

  const isDraft = selected?.status === 'DRAFT';

  const handleSave = () => {
    if (!selected) return;
    let parsed: FormSchema;
    try {
      parsed = JSON.parse(schemaText) as FormSchema;
    } catch {
      setLocalError('Schema is not valid JSON.');
      return;
    }
    setLocalError(null);
    updateSchema.mutate({ versionId: selected.id, schema: parsed });
  };

  const handlePublish = () => {
    if (selected) publish.mutate(selected.id);
  };

  const handleNewVersion = () => {
    createVersion.mutate(selected?.id, {
      onSuccess: (created) => setSelectedVersionId(created.id),
    });
  };

  return (
    <>
      <PageHeader
        title={`${form.name} · ${form.code}`}
        description={`Storage strategy: ${form.storageStrategy}. Edit a draft's schema, then publish it.`}
      />

      <div className="builder-layout">
        <aside className="builder-versions">
          <div className="af-toolbar" style={{ marginBottom: '0.25rem' }}>
            <span className="af-hint">Versions</span>
          </div>
          {versions.map((version) => (
            <button
              key={version.id}
              type="button"
              className={`builder-version${version.id === selected?.id ? ' builder-version-active' : ''}`}
              onClick={() => setSelectedVersionId(version.id)}
            >
              <strong>Version {version.versionNumber}</strong>
              <span className={statusBadgeClass(version.status)}>{version.status}</span>
            </button>
          ))}
          <Button
            variant="secondary"
            onClick={handleNewVersion}
            disabled={createVersion.isPending}
          >
            {createVersion.isPending ? 'Creating…' : 'New draft version'}
          </Button>
          {createVersion.isError ? (
            <ErrorState message={mutationError(createVersion.error, 'Could not create version')} />
          ) : null}
        </aside>

        <section className="builder-editor">
          {!isDraft ? (
            <div className="af-hint">
              This version is <strong>{selected?.status}</strong> and read-only. Create a new draft
              version to make changes.
            </div>
          ) : null}

          <div className="af-field">
            <label htmlFor="builder-schema">Schema (JSON)</label>
            <textarea
              id="builder-schema"
              className="af-textarea builder-schema"
              value={schemaText}
              spellCheck={false}
              readOnly={!isDraft}
              onChange={(event) => setSchemaText(event.target.value)}
            />
            <span className="af-hint">{SCHEMA_HINT}</span>
          </div>

          {localError ? <ErrorState message={localError} /> : null}
          {updateSchema.isError ? (
            <ErrorState message={mutationError(updateSchema.error, 'Save failed')} />
          ) : null}
          {publish.isError ? (
            <ErrorState message={mutationError(publish.error, 'Publish failed')} />
          ) : null}
          {updateSchema.isSuccess && !updateSchema.isPending ? (
            <div className="af-success">Draft saved.</div>
          ) : null}

          {isDraft ? (
            <div className="builder-actions">
              <Button onClick={handleSave} disabled={updateSchema.isPending}>
                {updateSchema.isPending ? 'Saving…' : 'Save draft'}
              </Button>
              <Button variant="secondary" onClick={handlePublish} disabled={publish.isPending}>
                {publish.isPending ? 'Publishing…' : 'Publish'}
              </Button>
            </div>
          ) : null}
        </section>
      </div>

      <div style={{ marginTop: '1.25rem' }}>
        <Link to="/">
          <Button variant="secondary">Back to forms</Button>
        </Link>
      </div>
    </>
  );
}
