import { useState } from 'react';
import { Button, ErrorState } from '@banking-forms/ui';
import type { AdminFormDetail, PipelineJobType } from '@banking-forms/api-client';
import {
  useCreatePipelineJob,
  useFormPipelineJobs,
  usePipelinesForJobs,
  useTriggerPipelineJob,
} from '../../hooks/usePipelineJobs';

interface FormHubJobsTabProps {
  form: AdminFormDetail;
  versionId: string;
}

export function FormHubJobsTab({ form, versionId }: FormHubJobsTabProps) {
  const jobsQuery = useFormPipelineJobs(form.id, versionId);
  const pipelinesQuery = usePipelinesForJobs();
  const createJob = useCreatePipelineJob();
  const triggerJob = useTriggerPipelineJob();

  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [jobType, setJobType] = useState<PipelineJobType>('BATCH');
  const [pipelineId, setPipelineId] = useState('');
  const [triggerEvent, setTriggerEvent] = useState('ON_APPROVED');
  const [scheduleCron, setScheduleCron] = useState('0 0 2 * * *');

  const submit = () => {
    if (!code.trim() || !name.trim() || !pipelineId) return;
    createJob.mutate(
      {
        code: code.trim(),
        name: name.trim(),
        jobType,
        formVersionId: versionId,
        pipelineId,
        triggerEvent: jobType === 'REALTIME' ? triggerEvent : null,
        scheduleCron: jobType === 'BATCH' ? scheduleCron : null,
        queryConfig: jobType === 'BATCH' ? { status: 'APPROVED' } : null,
        enabled: true,
      },
      {
        onSuccess: () => {
          setCode('');
          setName('');
        },
      },
    );
  };

  return (
    <div>
      <h2 className="submission-section-title">Batch &amp; real-time jobs</h2>
      <p className="af-hint">
        Scheduled batch exports or event-driven pipelines that query sanitized submission data and
        push via connectors.
      </p>

      <div className="af-form">
        <h3 style={{ margin: 0 }}>New job</h3>
        <div className="af-row">
          <div className="af-field" style={{ flex: 1 }}>
            <label htmlFor="job-code">Code</label>
            <input id="job-code" className="af-input" value={code} onChange={(e) => setCode(e.target.value)} />
          </div>
          <div className="af-field" style={{ flex: 2 }}>
            <label htmlFor="job-name">Name</label>
            <input id="job-name" className="af-input" value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <div className="af-field" style={{ flex: 1 }}>
            <label htmlFor="job-type">Type</label>
            <select
              id="job-type"
              className="af-input"
              value={jobType}
              onChange={(e) => setJobType(e.target.value as PipelineJobType)}
            >
              <option value="BATCH">BATCH</option>
              <option value="REALTIME">REALTIME</option>
            </select>
          </div>
        </div>
        <div className="af-field">
          <label htmlFor="job-pipeline">Pipeline</label>
          <select
            id="job-pipeline"
            className="af-input"
            value={pipelineId}
            onChange={(e) => setPipelineId(e.target.value)}
          >
            <option value="">Select pipeline…</option>
            {pipelinesQuery.data?.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
        </div>
        {jobType === 'REALTIME' ? (
          <div className="af-field">
            <label htmlFor="job-trigger">Trigger event</label>
            <input
              id="job-trigger"
              className="af-input"
              value={triggerEvent}
              onChange={(e) => setTriggerEvent(e.target.value)}
              placeholder="ON_APPROVED"
            />
          </div>
        ) : (
          <div className="af-field">
            <label htmlFor="job-cron">Schedule (cron)</label>
            <input
              id="job-cron"
              className="af-input"
              value={scheduleCron}
              onChange={(e) => setScheduleCron(e.target.value)}
            />
          </div>
        )}
        <Button variant="primary" onClick={submit} disabled={createJob.isPending}>
          Create job
        </Button>
      </div>

      <h2 className="submission-section-title" style={{ marginTop: '2rem' }}>
        Jobs for this version
      </h2>
      {!jobsQuery.data?.length ? (
        <p className="af-hint">No jobs configured for this form version.</p>
      ) : (
        jobsQuery.data.map((job) => (
          <div key={job.id} className="provider-card">
            <div className="provider-head">
              <div>
                <strong>{job.name}</strong>
                <div className="af-hint">
                  <code>{job.code}</code> · {job.jobType}{' '}
                  {job.enabled ? '(enabled)' : '(disabled)'}
                </div>
              </div>
              {job.jobType === 'BATCH' ? (
                <Button variant="secondary" onClick={() => triggerJob.mutate({ code: job.code })}>
                  Run now
                </Button>
              ) : null}
            </div>
            {job.lastRunAt ? (
              <p className="af-hint">Last run: {new Date(job.lastRunAt).toLocaleString()}</p>
            ) : null}
          </div>
        ))
      )}

      {createJob.isError || triggerJob.isError ? (
        <ErrorState
          message={((createJob.error ?? triggerJob.error) as Error)?.message ?? 'Job operation failed'}
        />
      ) : null}
    </div>
  );
}
