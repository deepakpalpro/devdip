import { Link, useParams } from 'react-router-dom';
import { Button, ErrorState, LoadingState, PageHeader } from '@banking-forms/ui';
import { useSubmission } from '../hooks/useSubmission';
import { SubmissionReview } from '../components/SubmissionReview';
import { isDraft, statusMeta } from '../lib/submissionStatus';
import './submission-wizard.css';
import './applications.css';

export function ApplicationStatusPage() {
  const { submissionId } = useParams<{ submissionId: string }>();
  const { data: submission, isLoading, error } = useSubmission(submissionId ?? null);

  if (isLoading) {
    return <LoadingState message="Loading application…" />;
  }

  if (error) {
    return <ErrorState message={error instanceof Error ? error.message : 'Failed to load application'} />;
  }

  if (!submission) {
    return <ErrorState message="Application not found." />;
  }

  const meta = statusMeta(submission.status);

  return (
    <>
      <PageHeader title={submission.formName} description="Application status and submitted details." />

      <div className="bf-status-panel">
        <div className="bf-status-row">
          <span className={meta.className}>{meta.label}</span>
        </div>
        {meta.description ? <p className="bf-status-desc">{meta.description}</p> : null}
        <span className="bf-status-ref">Reference: {submission.id}</span>
      </div>

      {isDraft(submission.status) ? (
        <div className="submission-actions" style={{ marginBottom: '1.5rem' }}>
          <Link to={`/apply/${submission.formCode}?submission=${submission.id}`}>
            <Button>Continue application</Button>
          </Link>
        </div>
      ) : null}

      <div className="submission-panel submission-summary">
        <SubmissionReview sections={submission.schema.sections} sectionData={submission.sectionData} />
      </div>

      <div style={{ marginTop: '1.5rem' }}>
        <Link to="/applications">
          <Button variant="secondary">Back to my applications</Button>
        </Link>
      </div>
    </>
  );
}
