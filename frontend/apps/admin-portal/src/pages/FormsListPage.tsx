import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button, ErrorState, LoadingState } from '@banking-forms/ui';
import { ApiError } from '@banking-forms/api-client';
import { useAdminForms, useCreateForm } from '../hooks/useAdminForms';
import { statusBadgeClass } from './formStatus';
import './admin-forms.css';

function CreateFormPanel({ onClose }: { onClose: () => void }) {
  const navigate = useNavigate();
  const createForm = useCreateForm();
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [category, setCategory] = useState('');
  const [storageStrategy, setStorageStrategy] = useState<'JSON_BLOB' | 'KEY_VALUE'>('JSON_BLOB');

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    createForm.mutate(
      { code: code.trim(), name: name.trim(), category: category.trim() || null, storageStrategy },
      { onSuccess: (form) => navigate(`/forms/${form.id}/builder`) },
    );
  };

  return (
    <form className="af-form" onSubmit={submit}>
      <div className="af-row">
        <div className="af-field" style={{ flex: 1 }}>
          <label htmlFor="af-code">Code</label>
          <input
            id="af-code"
            className="af-input"
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            placeholder="LOAN_APPLICATION"
            required
          />
        </div>
        <div className="af-field" style={{ flex: 2 }}>
          <label htmlFor="af-name">Name</label>
          <input
            id="af-name"
            className="af-input"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Personal Loan Application"
            required
          />
        </div>
      </div>
      <div className="af-row">
        <div className="af-field" style={{ flex: 1 }}>
          <label htmlFor="af-category">Category</label>
          <input
            id="af-category"
            className="af-input"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            placeholder="Lending"
          />
        </div>
        <div className="af-field" style={{ flex: 1 }}>
          <label htmlFor="af-storage">Storage strategy</label>
          <select
            id="af-storage"
            className="af-select"
            value={storageStrategy}
            onChange={(e) => setStorageStrategy(e.target.value as 'JSON_BLOB' | 'KEY_VALUE')}
          >
            <option value="JSON_BLOB">JSON_BLOB (flexible)</option>
            <option value="KEY_VALUE">KEY_VALUE (indexed / regulated)</option>
          </select>
        </div>
      </div>
      {createForm.isError ? (
        <ErrorState
          message={
            createForm.error instanceof ApiError
              ? createForm.error.message
              : 'Could not create the form'
          }
        />
      ) : null}
      <div className="af-row">
        <Button type="submit" disabled={createForm.isPending || !code.trim() || !name.trim()}>
          {createForm.isPending ? 'Creating…' : 'Create form'}
        </Button>
        <Button type="button" variant="secondary" onClick={onClose}>
          Cancel
        </Button>
      </div>
    </form>
  );
}

export function FormsListPage() {
  const { data, isLoading, error } = useAdminForms();
  const [creating, setCreating] = useState(false);

  if (isLoading) {
    return <LoadingState message="Loading form definitions…" />;
  }

  if (error) {
    return <ErrorState message={error instanceof Error ? error.message : 'Failed to load forms'} />;
  }

  return (
    <div>
      <div className="af-toolbar">
        <span className="af-hint">{data?.length ?? 0} form definition(s)</span>
        {!creating ? <Button onClick={() => setCreating(true)}>New form</Button> : null}
      </div>

      {creating ? <CreateFormPanel onClose={() => setCreating(false)} /> : null}

      {!data?.length ? (
        <div className="bf-empty-state">No form definitions yet. Create one to get started.</div>
      ) : (
        <div className="bf-table-wrap">
          <table className="bf-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Code</th>
                <th>Category</th>
                <th>Latest</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.map((form) => (
                <tr key={form.id}>
                  <td>{form.name}</td>
                  <td>
                    <span className="bf-badge">{form.code}</span>
                  </td>
                  <td>{form.category ?? '—'}</td>
                  <td>
                    {form.latestStatus ? (
                      <span className={statusBadgeClass(form.latestStatus)}>
                        v{form.latestVersion} · {form.latestStatus}
                      </span>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td>
                    <Link to={`/forms/${form.id}/builder`}>
                      <Button variant="secondary">Open builder</Button>
                    </Link>{' '}
                    <Link to={`/forms/${form.id}/pipeline`}>
                      <Button variant="secondary">Pipeline</Button>
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
