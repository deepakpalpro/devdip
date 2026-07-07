import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@banking-forms/ui';
import type { AdminFormDetail } from '@banking-forms/api-client';
import { useAdminSubmissions } from '../../hooks/useAdminSubmissions';
import { statusBadgeClass } from '../formStatus';

const PAGE_SIZE = 10;

const STATUSES = [
  'DRAFT',
  'SUBMITTED',
  'VALIDATING',
  'PROCESSING',
  'PENDING_REVIEW',
  'APPROVED',
  'REJECTED',
  'NEEDS_INFO',
] as const;

interface FormHubDataTabProps {
  form: AdminFormDetail;
}

export function FormHubDataTab({ form }: FormHubDataTabProps) {
  const { data, isLoading } = useAdminSubmissions();
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);

  const filtered = useMemo(() => {
    const rows = (data ?? []).filter((s) => s.formCode === form.code);
    if (!statusFilter) return rows;
    return rows.filter((s) => s.status === statusFilter);
  }, [data, form.code, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const pageRows = filtered.slice(safePage * PAGE_SIZE, safePage * PAGE_SIZE + PAGE_SIZE);

  return (
    <div>
      <h2 className="submission-section-title">Submission data</h2>
      <p className="af-hint">Applications for this form — filter by status and paginate.</p>

      <div className="af-row" style={{ marginBottom: '1rem' }}>
        <div className="af-field">
          <label htmlFor="status-filter">Status</label>
          <select
            id="status-filter"
            className="af-input"
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All statuses</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
        <span className="af-hint">{filtered.length} submission(s)</span>
      </div>

      {isLoading ? (
        <p className="af-hint">Loading submissions…</p>
      ) : !pageRows.length ? (
        <div className="bf-empty-state">No submissions match this filter.</div>
      ) : (
        <div className="bf-table-wrap">
          <table className="bf-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Status</th>
                <th>Submitted</th>
                <th>Created</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {pageRows.map((row) => (
                <tr key={row.id}>
                  <td>
                    <code>{row.id.slice(0, 8)}…</code>
                  </td>
                  <td>
                    <span className={statusBadgeClass(row.status)}>{row.status}</span>
                  </td>
                  <td>{row.submittedAt ? new Date(row.submittedAt).toLocaleString() : '—'}</td>
                  <td>{new Date(row.createdAt).toLocaleString()}</td>
                  <td>
                    <Link to={`/submissions/${row.id}`}>
                      <Button variant="secondary">Review</Button>
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {filtered.length > PAGE_SIZE ? (
        <div className="af-row" style={{ marginTop: '1rem' }}>
          <Button variant="secondary" disabled={safePage <= 0} onClick={() => setPage((p) => p - 1)}>
            Previous
          </Button>
          <span className="af-hint">
            Page {safePage + 1} of {totalPages}
          </span>
          <Button
            variant="secondary"
            disabled={safePage >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </Button>
        </div>
      ) : null}
    </div>
  );
}
