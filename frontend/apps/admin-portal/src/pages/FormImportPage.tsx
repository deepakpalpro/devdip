import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, ErrorState } from '@banking-forms/ui';
import { ApiError, type FormImportJob } from '@banking-forms/api-client';
import { useAcceptFormImport, useImportFromUrl, useUploadFormImport } from '../hooks/useFormImport';
import './admin-forms.css';

function toCode(name: string): string {
  return name
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .slice(0, 40);
}

function confidence(value: number): { text: string; className: string } {
  if (value >= 0.8) return { text: 'High', className: 'import-conf import-conf-high' };
  if (value >= 0.5) return { text: 'Medium', className: 'import-conf import-conf-med' };
  return { text: 'Low', className: 'import-conf import-conf-low' };
}

export function FormImportPage() {
  const navigate = useNavigate();
  const upload = useUploadFormImport();
  const importFromUrl = useImportFromUrl();
  const [job, setJob] = useState<FormImportJob | null>(null);
  const accept = useAcceptFormImport(job?.id);

  const [mode, setMode] = useState<'file' | 'url'>('file');
  const [file, setFile] = useState<File | null>(null);
  const [url, setUrl] = useState('');
  const [name, setName] = useState('');
  const [code, setCode] = useState('');
  const [codeTouched, setCodeTouched] = useState(false);
  const [category, setCategory] = useState('');
  const [storageStrategy, setStorageStrategy] = useState<'JSON_BLOB' | 'KEY_VALUE'>('JSON_BLOB');

  const reset = () => {
    setJob(null);
    setFile(null);
    setUrl('');
    setName('');
    setCode('');
    setCodeTouched(false);
    setCategory('');
    upload.reset();
    importFromUrl.reset();
    accept.reset();
  };

  const onExtracted = (result: FormImportJob) => {
    setJob(result);
    if (result.status === 'NEEDS_REVIEW' && result.suggestedName) {
      setName(result.suggestedName);
      setCode(toCode(result.suggestedName));
    }
  };

  const handleSubmitSource = (event: React.FormEvent) => {
    event.preventDefault();
    if (mode === 'file') {
      if (!file) return;
      upload.mutate(file, { onSuccess: onExtracted });
    } else {
      if (!url.trim()) return;
      importFromUrl.mutate(url.trim(), { onSuccess: onExtracted });
    }
  };

  const handleNameChange = (value: string) => {
    setName(value);
    if (!codeTouched) setCode(toCode(value));
  };

  const handleAccept = (event: React.FormEvent) => {
    event.preventDefault();
    if (!job) return;
    accept.mutate(
      {
        code: code.trim(),
        name: name.trim(),
        category: category.trim() || null,
        storageStrategy,
        schema: job.proposedSchema ?? undefined,
      },
      { onSuccess: (result) => navigate(`/forms/${result.formId}/builder`) },
    );
  };

  const busy = upload.isPending || importFromUrl.isPending;
  const sourceError = upload.error ?? importFromUrl.error;
  const sections = job?.proposedSchema?.sections ?? [];
  const fieldCount = sections.reduce((total, section) => total + section.fields.length, 0);

  return (
    <div>
      {!job ? (
        <form className="af-form" onSubmit={handleSubmitSource}>
          <div className="import-modes">
            <button
              type="button"
              className={mode === 'file' ? 'import-mode import-mode-active' : 'import-mode'}
              onClick={() => setMode('file')}
            >
              Upload a file
            </button>
            <button
              type="button"
              className={mode === 'url' ? 'import-mode import-mode-active' : 'import-mode'}
              onClick={() => setMode('url')}
            >
              From a URL
            </button>
          </div>

          {mode === 'file' ? (
            <div className="af-field">
              <label htmlFor="import-file">Form file</label>
              <input
                id="import-file"
                className="af-input"
                type="file"
                accept="application/pdf,.pdf,.csv,.xls,.xlsx,.htm,.html,image/*"
                onChange={(event) => setFile(event.target.files?.[0] ?? null)}
              />
              <span className="af-hint">Supported: PDF, CSV, XLS/XLSX, HTML, images (via a configured OCR provider).</span>
            </div>
          ) : (
            <div className="af-field">
              <label htmlFor="import-url">Form page URL</label>
              <input
                id="import-url"
                className="af-input"
                type="url"
                value={url}
                onChange={(event) => setUrl(event.target.value)}
                placeholder="https://bank.example/apply"
              />
              <span className="af-hint">The platform fetches the page and reads its HTML form controls.</span>
            </div>
          )}

          <span className="af-hint">
            A proposal is generated for you to review — nothing is published automatically.
          </span>

          {sourceError ? (
            <ErrorState message={sourceError instanceof ApiError ? sourceError.message : 'Import failed'} />
          ) : null}
          <div className="af-row">
            <Button type="submit" disabled={busy || (mode === 'file' ? !file : !url.trim())}>
              {busy ? 'Analyzing…' : 'Analyze source'}
            </Button>
          </div>
        </form>
      ) : null}

      {job && job.status === 'FAILED' ? (
        <div className="af-form">
          <ErrorState message={job.error ?? 'Could not read the source.'} />
          <div className="af-row">
            <Button variant="secondary" onClick={reset}>
              Try another source
            </Button>
          </div>
        </div>
      ) : null}

      {job && job.status === 'NEEDS_REVIEW' ? (
        <form className="af-form" onSubmit={handleAccept}>
          <div className="import-summary">
            <span className="af-hint">
              {job.sourceType} · provider {job.providerCode} · {job.source} · {sections.length} section(s),{' '}
              {fieldCount} field(s)
            </span>
            {job.confidence ? (
              <span className={confidence(job.confidence.overall).className}>
                Overall confidence: {confidence(job.confidence.overall).text} (
                {Math.round(job.confidence.overall * 100)}%)
              </span>
            ) : null}
          </div>

          {fieldCount === 0 ? (
            <span className="af-hint">
              No fields were detected automatically. You can still create an empty draft and build it
              manually in the form builder.
            </span>
          ) : null}

          <div className="af-row">
            <div className="af-field" style={{ flex: 1 }}>
              <label htmlFor="import-code">Code</label>
              <input
                id="import-code"
                className="af-input"
                value={code}
                onChange={(event) => {
                  setCodeTouched(true);
                  setCode(event.target.value.toUpperCase());
                }}
                placeholder="LOAN_APPLICATION"
                required
              />
            </div>
            <div className="af-field" style={{ flex: 2 }}>
              <label htmlFor="import-name">Name</label>
              <input
                id="import-name"
                className="af-input"
                value={name}
                onChange={(event) => handleNameChange(event.target.value)}
                placeholder="Personal Loan Application"
                required
              />
            </div>
          </div>
          <div className="af-row">
            <div className="af-field" style={{ flex: 1 }}>
              <label htmlFor="import-category">Category</label>
              <input
                id="import-category"
                className="af-input"
                value={category}
                onChange={(event) => setCategory(event.target.value)}
                placeholder="Lending"
              />
            </div>
            <div className="af-field" style={{ flex: 1 }}>
              <label htmlFor="import-storage">Storage strategy</label>
              <select
                id="import-storage"
                className="af-select"
                value={storageStrategy}
                onChange={(event) => setStorageStrategy(event.target.value as 'JSON_BLOB' | 'KEY_VALUE')}
              >
                <option value="JSON_BLOB">JSON_BLOB (flexible)</option>
                <option value="KEY_VALUE">KEY_VALUE (indexed / regulated)</option>
              </select>
            </div>
          </div>

          {sections.length ? (
            <div className="import-preview">
              {sections.map((section) => (
                <div key={section.key} className="import-section">
                  <h4>
                    {section.title} <span className="bf-badge">{section.key}</span>
                  </h4>
                  <ul className="import-fields">
                    {section.fields.map((field) => {
                      const value = job.confidence?.fields?.[section.key]?.[field.key];
                      return (
                        <li key={field.key}>
                          <span className="import-field-label">{field.label}</span>
                          <span className="bf-badge">{field.type}</span>
                          {field.required ? <span className="import-req">required</span> : null}
                          {typeof value === 'number' ? (
                            <span className={confidence(value).className}>{Math.round(value * 100)}%</span>
                          ) : null}
                        </li>
                      );
                    })}
                  </ul>
                </div>
              ))}
            </div>
          ) : null}

          {accept.isError ? (
            <ErrorState
              message={accept.error instanceof ApiError ? accept.error.message : 'Could not create the draft'}
            />
          ) : null}

          <div className="af-row">
            <Button type="submit" disabled={accept.isPending || !code.trim() || !name.trim()}>
              {accept.isPending ? 'Creating draft…' : 'Create draft form'}
            </Button>
            <Button type="button" variant="secondary" onClick={reset}>
              Start over
            </Button>
          </div>
          <span className="af-hint">
            Accepting creates a DRAFT form and opens the builder, where you review and refine every
            field before publishing.
          </span>
        </form>
      ) : null}
    </div>
  );
}
