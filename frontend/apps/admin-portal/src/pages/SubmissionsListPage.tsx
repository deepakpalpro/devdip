import { Link } from 'react-router-dom';
import { Button, EmptyState, ErrorState, LoadingState } from '@banking-forms/ui';
import { useAdminSubmissions } from '../hooks/useAdminSubmissions';

function formatDate(value: string | null) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function statusBadgeClass(status: string) {
  return status === 'SUBMITTED' ? 'bf-badge bf-badge-success' : 'bf-badge';
}

export function SubmissionsListPage() {
  const { data, isLoading, error } = useAdminSubmissions();

  if (isLoading) {
    return <LoadingState message="Loading submissions…" />;
  }

  if (error) {
    return <ErrorState message={error instanceof Error ? error.message : 'Failed to load submissions'} />;
  }

  if (!data?.length) {
    return <EmptyState message="No applications have been started yet." />;
  }

  return (
    <div className="bf-table-wrap">
      <table className="bf-table">
        <thead>
          <tr>
            <th>Application</th>
            <th>Status</th>
            <th>Started</th>
            <th>Submitted</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {data.map((submission) => (
            <tr key={submission.id}>
              <td>
                <div style={{ fontWeight: 600 }}>{submission.formName}</div>
                <span className="bf-badge">{submission.formCode}</span>
              </td>
              <td>
                <span className={statusBadgeClass(submission.status)}>{submission.status}</span>
              </td>
              <td>{formatDate(submission.createdAt)}</td>
              <td>{formatDate(submission.submittedAt)}</td>
              <td>
                <Link to={`/submissions/${submission.id}`}>
                  <Button variant="secondary">View</Button>
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
