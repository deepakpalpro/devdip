import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import type { FormFieldSchema, PipelineReport, ReviewActionKey, TimelineEvent } from '@banking-forms/api-client';
import { Button, ErrorState, LoadingState, PageHeader } from '@banking-forms/ui';
import { useAdminSubmission, useReviewSubmission, useSubmissionPipeline } from '../hooks/useAdminSubmissions';
import './admin-submissions.css';

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Draft',
  SUBMITTED: 'Submitted',
  VALIDATING: 'Validating',
  PROCESSING: 'Processing',
  PENDING_REVIEW: 'Under review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  NEEDS_INFO: 'More info requested',
};

const EVENT_LABELS: Record<string, string> = {
  SUBMITTED: 'Application submitted',
  PIPELINE_STARTED: 'Automated pipeline started',
  VALIDATED: 'Validation passed',
  PII_SCRUBBED: 'PII scrubbed',
  AI_EVALUATED: 'AI risk evaluation',
  AI_EVALUATION_SKIPPED: 'AI evaluation skipped',
  DOWNSTREAM_DISPATCHED: 'Dispatched downstream',
  PIPELINE_COMPLETED: 'Automated pipeline completed',
  PIPELINE_FAILED: 'Automated pipeline failed',
  REVIEW_STARTED: 'Review started',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  INFO_REQUESTED: 'More information requested',
  NOTIFICATION_QUEUED: 'Notification queued',
  NOTIFICATION_SENT: 'Notification sent',
  NOTIFICATION_DELIVERED: 'Notification delivered',
  NOTIFICATION_FAILED: 'Notification failed',
  NOTIFICATION_SKIPPED: 'Notification skipped',
};

interface ReviewButton {
  action: ReviewActionKey;
  label: string;
  variant: 'primary' | 'secondary';
}

function actionsForStatus(status: string): ReviewButton[] {
  switch (status) {
    case 'SUBMITTED':
      return [{ action: 'start', label: 'Start review', variant: 'primary' }];
    case 'NEEDS_INFO':
      return [{ action: 'start', label: 'Resume review', variant: 'primary' }];
    case 'PENDING_REVIEW':
      return [
        { action: 'approve', label: 'Approve', variant: 'primary' },
        { action: 'reject', label: 'Reject', variant: 'secondary' },
        { action: 'request-info', label: 'Request info', variant: 'secondary' },
      ];
    default:
      return [];
  }
}

function statusBadgeClass(status: string) {
  if (status === 'APPROVED') return 'bf-badge bf-badge-success';
  if (status === 'REJECTED') return 'bf-badge bf-badge-danger';
  if (status === 'PENDING_REVIEW' || status === 'NEEDS_INFO') return 'bf-badge bf-badge-warning';
  return 'bf-badge';
}

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function ReadOnlyField({ field, value }: { field: FormFieldSchema; value: unknown }) {
  if (field.type === 'embedded_form' && field.embeddedForm) {
    const nested = (value as Record<string, Record<string, unknown>>) ?? {};
    return (
      <div className="submission-field-group">
        <div className="submission-field-label">{field.label}</div>
        <div className="submission-embedded">
          {field.embeddedForm.sections.flatMap((section) =>
            section.fields.map((embeddedField) => (
              <ReadOnlyField
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

  const display = value === undefined || value === null || value === '' ? '—' : String(value);
  return (
    <div className="submission-field">
      <span className="submission-field-label">{field.label}</span>
      <span className="submission-field-value">{display}</span>
    </div>
  );
}

function Timeline({ events }: { events: TimelineEvent[] }) {
  if (events.length === 0) {
    return <p style={{ margin: 0, color: 'var(--bf-text-muted)' }}>No activity yet.</p>;
  }
  return (
    <ol className="submission-timeline">
      {events.map((event, index) => (
        <li key={index} className="submission-timeline-item">
          <div className="submission-timeline-dot" />
          <div className="submission-timeline-body">
            <div className="submission-timeline-title">
              {EVENT_LABELS[event.eventType] ?? event.eventType}
            </div>
            {event.note ? <div className="submission-timeline-note">“{event.note}”</div> : null}
            <div className="submission-timeline-time">{formatDate(event.createdAt)}</div>
          </div>
        </li>
      ))}
    </ol>
  );
}

function pipelineBadgeClass(status: string) {
  if (status === 'COMPLETED') return 'bf-badge bf-badge-success';
  if (status === 'FAILED') return 'bf-badge bf-badge-danger';
  return 'bf-badge bf-badge-warning';
}

function recommendationBadgeClass(recommendation: string) {
  if (recommendation === 'APPROVE') return 'bf-badge bf-badge-success';
  if (recommendation === 'REJECT') return 'bf-badge bf-badge-danger';
  return 'bf-badge bf-badge-warning';
}

function AiEvaluationBlock({ ai }: { ai: NonNullable<PipelineReport['aiEvaluation']> }) {
  const riskPct = Math.round(ai.riskScore * 100);
  const signalEntries = Object.entries(ai.signals ?? {});
  return (
    <div className="submission-ai-eval">
      <div className="submission-field-label">AI risk evaluation (advisory)</div>
      <div className="submission-ai-eval-head">
        <span className={recommendationBadgeClass(ai.recommendation)}>{ai.recommendation}</span>
        <span className="submission-ai-eval-score">Risk {riskPct}%</span>
        <span className="submission-ai-eval-meta">
          {ai.evaluatorId}
          {ai.model ? ` · ${ai.model}` : ''}
        </span>
      </div>
      {ai.rationale ? <p className="submission-ai-eval-rationale">{ai.rationale}</p> : null}
      {signalEntries.length > 0 ? (
        <details className="submission-ai-eval-signals">
          <summary>Signals</summary>
          <ul className="submission-pipeline-list">
            {signalEntries.map(([key, value]) => (
              <li key={key}>
                <code>{key}</code>
                <span className="bf-badge">{String(value)}</span>
              </li>
            ))}
          </ul>
        </details>
      ) : null}
    </div>
  );
}

function PipelineCard({ report }: { report: PipelineReport }) {
  const { execution, sanitizedPayload, transformedFields, aiEvaluation } = report;
  if (!execution) {
    return null;
  }
  return (
    <section className="submission-section-card">
      <h2 className="submission-section-title">Automated processing</h2>
      <div className="submission-pipeline-head">
        <span className={pipelineBadgeClass(execution.status)}>{execution.status}</span>
        <span className="submission-pipeline-steps">
          Step {execution.currentStep} of {execution.totalSteps}
        </span>
      </div>
      {execution.errorDetails ? (
        <p className="submission-pipeline-error">{execution.errorDetails}</p>
      ) : null}

      {aiEvaluation ? <AiEvaluationBlock ai={aiEvaluation} /> : null}

      {transformedFields.length > 0 ? (
        <div className="submission-pipeline-transformed">
          <div className="submission-field-label">PII transformed ({transformedFields.length})</div>
          <ul className="submission-pipeline-list">
            {transformedFields.map((field) => (
              <li key={field.fieldPath}>
                <code>{field.fieldPath}</code>
                <span className="bf-badge">{field.strategy}</span>
              </li>
            ))}
          </ul>
        </div>
      ) : null}

      {sanitizedPayload ? (
        <details className="submission-pipeline-payload">
          <summary>Sanitized payload (downstream/AI-safe)</summary>
          <pre>{JSON.stringify(sanitizedPayload, null, 2)}</pre>
        </details>
      ) : null}
    </section>
  );
}

export function SubmissionDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data, isLoading, error } = useAdminSubmission(id);
  const pipeline = useSubmissionPipeline(id);
  const review = useReviewSubmission(id);
  const [note, setNote] = useState('');

  if (isLoading) {
    return <LoadingState message="Loading submission…" />;
  }
  if (error || !data) {
    return <ErrorState message={error instanceof Error ? error.message : 'Submission not found'} />;
  }

  const actions = actionsForStatus(data.status);

  const runAction = async (action: ReviewActionKey) => {
    await review.mutateAsync({ action, note: note.trim() || undefined });
    setNote('');
  };

  return (
    <div className="submission-detail">
      <PageHeader title={data.formName} description={data.formCode} />

      <div className="submission-status-row">
        <span className={statusBadgeClass(data.status)}>{STATUS_LABELS[data.status] ?? data.status}</span>
        <Link to="/submissions">
          <Button variant="secondary">← Back to submissions</Button>
        </Link>
      </div>

      {actions.length > 0 ? (
        <div className="submission-review-panel">
          <h2 className="submission-section-title">Review actions</h2>
          <textarea
            className="submission-note"
            placeholder="Optional note (shown in the timeline)…"
            value={note}
            onChange={(event) => setNote(event.target.value)}
            rows={2}
          />
          <div className="submission-review-actions">
            {actions.map((button) => (
              <Button
                key={button.action}
                variant={button.variant}
                disabled={review.isPending}
                onClick={() => runAction(button.action)}
              >
                {button.label}
              </Button>
            ))}
          </div>
          {review.isError ? (
            <ErrorState message={review.error instanceof Error ? review.error.message : 'Action failed'} />
          ) : null}
        </div>
      ) : null}

      {pipeline.data ? <PipelineCard report={pipeline.data} /> : null}

      {data.schema.sections.map((section) => (
        <section key={section.key} className="submission-section-card">
          <h2 className="submission-section-title">{section.title}</h2>
          <div className="submission-fields">
            {section.fields.map((field) => (
              <ReadOnlyField
                key={field.key}
                field={field}
                value={data.sectionData[section.key]?.[field.key]}
              />
            ))}
          </div>
        </section>
      ))}

      <section className="submission-section-card">
        <h2 className="submission-section-title">Activity timeline</h2>
        <Timeline events={data.timeline} />
      </section>
    </div>
  );
}
