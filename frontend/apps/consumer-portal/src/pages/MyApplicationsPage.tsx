import { Link } from 'react-router-dom';
import { Button, EmptyState, ErrorState, LoadingState, PageHeader } from '@banking-forms/ui';
import { useMySubmissions } from '../hooks/useSubmission';
import { isDraft, statusMeta } from '../lib/submissionStatus';
import './applications.css';

function formatDate(value: string | null): string {
  if (!value) return '—';
  return new Date(value).toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
}

export function MyApplicationsPage() {
  const { data, isLoading, error } = useMySubmissions();

  if (isLoading) {
    return <LoadingState message="Loading your applications…" />;
  }

  if (error) {
    return <ErrorState message={error instanceof Error ? error.message : 'Failed to load applications'} />;
  }

  return (
    <>
      <PageHeader
        title="My applications"
        description="Track the status of your applications and continue any drafts."
      />
      {!data?.length ? (
        <EmptyState message="You have not started any applications yet." />
      ) : (
        <table className="bf-applications-table">
          <thead>
            <tr>
              <th>Application</th>
              <th>Status</th>
              <th>Started</th>
              <th>Submitted</th>
              <th aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {data.map((submission) => {
              const meta = statusMeta(submission.status);
              return (
                <tr key={submission.id}>
                  <td>{submission.formName}</td>
                  <td>
                    <span className={meta.className}>{meta.label}</span>
                  </td>
                  <td className="bf-app-time">{formatDate(submission.createdAt)}</td>
                  <td className="bf-app-time">{formatDate(submission.submittedAt)}</td>
                  <td className="bf-app-actions">
                    {isDraft(submission.status) ? (
                      <Link to={`/apply/${submission.formCode}?submission=${submission.id}`}>
                        <Button>Continue</Button>
                      </Link>
                    ) : (
                      <Link to={`/applications/${submission.id}`}>
                        <Button variant="secondary">View status</Button>
                      </Link>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </>
  );
}
