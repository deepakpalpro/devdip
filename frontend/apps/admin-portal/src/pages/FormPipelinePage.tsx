import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, ErrorState, LoadingState } from '@banking-forms/ui';
import type { PipelineStepRequest } from '@banking-forms/api-client';
import { useAdminForm } from '../hooks/useAdminForms';
import {
  PIPELINE_TRIGGERS,
  useFormPipelineBindings,
  usePipelets,
  usePipelines,
  usePipeline,
  useUpdatePipelineSteps,
  useUpsertFormPipelineBinding,
} from '../hooks/usePipelines';
import { statusBadgeClass } from './formStatus';
import './admin-forms.css';

export function FormPipelinePage() {
  const { formId = '' } = useParams();
  const formQuery = useAdminForm(formId);
  const pipeletsQuery = usePipelets();
  const pipelinesQuery = usePipelines();

  const versions = formQuery.data?.versions ?? [];
  const [versionId, setVersionId] = useState<string>('');
  const selectedVersionId = versionId || versions[0]?.id || '';

  const bindingsQuery = useFormPipelineBindings(formId, selectedVersionId || undefined);
  const upsertBinding = useUpsertFormPipelineBinding(formId, selectedVersionId);

  const submitBinding = bindingsQuery.data?.find((b) => b.trigger === 'ON_SUBMIT');
  const [pipelineId, setPipelineId] = useState('');
  const activePipelineId = pipelineId || submitBinding?.pipelineDefinitionId || '';

  const selectedPipeline = useMemo(
    () => pipelinesQuery.data?.find((p) => p.id === activePipelineId),
    [pipelinesQuery.data, activePipelineId],
  );

  const pipelineDetailQuery = usePipeline(activePipelineId || undefined);
  const updateSteps = useUpdatePipelineSteps(activePipelineId);

  const [draftSteps, setDraftSteps] = useState<PipelineStepRequest[]>([]);

  useEffect(() => {
    if (pipelineDetailQuery.data?.steps) {
      setDraftSteps(
        pipelineDetailQuery.data.steps.map((s) => ({
          stepKey: s.stepKey,
          pipeletCode: s.pipeletCode,
          properties: s.properties,
        })),
      );
    }
  }, [pipelineDetailQuery.data]);

  const addStep = () => {
    const code = pipeletsQuery.data?.[0]?.code;
    if (!code) return;
    setDraftSteps((steps) => [
      ...steps,
      { stepKey: `step-${steps.length + 1}`, pipeletCode: code, properties: null },
    ]);
  };

  const saveBinding = () => {
    if (!activePipelineId) return;
    upsertBinding.mutate({ pipelineId: activePipelineId, trigger: 'ON_SUBMIT', enabled: true });
  };

  const saveSteps = () => {
    updateSteps.mutate(draftSteps);
  };

  if (formQuery.isLoading) return <LoadingState message="Loading form…" />;
  if (formQuery.isError) return <ErrorState message={(formQuery.error as Error).message} />;
  if (!formQuery.data) return <ErrorState message="Form not found" />;

  const form = formQuery.data;

  return (
    <div className="builder-layout">
      <aside className="builder-sidebar">
        <p className="af-hint">
          <Link to={`/forms/${form.id}/builder`}>← Back to builder</Link>
        </p>
        <h2 className="builder-form-title">{form.name}</h2>
        <p className="af-hint">{form.code}</p>
        <ul className="builder-version-list">
          {versions.map((v) => (
            <li key={v.id}>
              <button
                type="button"
                className={`builder-version-btn ${selectedVersionId === v.id ? 'builder-version-active' : ''}`}
                onClick={() => setVersionId(v.id)}
              >
                <span className={statusBadgeClass(v.status)}>v{v.versionNumber}</span>
                <span>{v.status}</span>
              </button>
            </li>
          ))}
        </ul>
      </aside>

      <section className="builder-main">
        <h2 className="submission-section-title">Pipeline binding</h2>
        <p className="af-hint">Associate a pipeline with this form version and trigger event.</p>

        <div className="af-field">
          <label htmlFor="pipeline-select">Pipeline</label>
          <select
            id="pipeline-select"
            className="af-input"
            value={activePipelineId}
            onChange={(e) => setPipelineId(e.target.value)}
          >
            <option value="">Select pipeline…</option>
            {pipelinesQuery.data?.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} ({p.code}){p.systemDefault ? ' — system default' : ''}
              </option>
            ))}
          </select>
        </div>

        <div className="af-field">
          <label htmlFor="trigger-select">Trigger</label>
          <select id="trigger-select" className="af-input" defaultValue="ON_SUBMIT" disabled>
            {PIPELINE_TRIGGERS.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </div>

        <Button variant="primary" onClick={saveBinding} disabled={!activePipelineId || upsertBinding.isPending}>
          Save ON_SUBMIT binding
        </Button>

        {bindingsQuery.data && bindingsQuery.data.length > 0 && (
          <ul className="af-hint" style={{ marginTop: '1rem' }}>
            {bindingsQuery.data.map((b) => (
              <li key={b.id}>
                {b.trigger} → pipeline {b.pipelineDefinitionId.slice(0, 8)}… ({b.enabled ? 'enabled' : 'disabled'})
              </li>
            ))}
          </ul>
        )}

        <h2 className="submission-section-title" style={{ marginTop: '2rem' }}>
          Pipelet steps {selectedPipeline ? `— ${selectedPipeline.name}` : ''}
        </h2>
        <p className="af-hint">
          Load steps from the selected pipeline, reorder, and edit per-step properties JSON.
        </p>

        <div className="af-row" style={{ gap: '0.5rem', marginBottom: '1rem' }}>
          <Button variant="secondary" onClick={addStep}>
            Add pipelet
          </Button>
          <Button variant="primary" onClick={saveSteps} disabled={!activePipelineId || selectedPipeline?.systemDefault}>
            Save steps
          </Button>
        </div>

        {selectedPipeline?.systemDefault && (
          <p className="af-hint">System default pipeline steps are read-only. Clone to a custom pipeline to edit.</p>
        )}

        {draftSteps.map((step, index) => (
          <div key={`${step.stepKey}-${index}`} className="provider-card">
            <div className="provider-head">
              <strong>{step.stepKey}</strong>
              <select
                className="af-input"
                value={step.pipeletCode}
                onChange={(e) =>
                  setDraftSteps((steps) =>
                    steps.map((s, i) => (i === index ? { ...s, pipeletCode: e.target.value } : s)),
                  )
                }
              >
                {pipeletsQuery.data?.map((p) => (
                  <option key={p.code} value={p.code}>
                    {p.name} ({p.code})
                  </option>
                ))}
              </select>
            </div>
            <textarea
              className="provider-config"
              rows={3}
              value={JSON.stringify(step.properties ?? {}, null, 2)}
              onChange={(e) => {
                try {
                  const properties = JSON.parse(e.target.value) as Record<string, unknown>;
                  setDraftSteps((steps) =>
                    steps.map((s, i) => (i === index ? { ...s, properties } : s)),
                  );
                } catch {
                  // ignore invalid JSON while typing
                }
              }}
            />
          </div>
        ))}

        <details style={{ marginTop: '1rem' }}>
          <summary>Available pipelets</summary>
          <ul>
            {pipeletsQuery.data?.map((p) => (
              <li key={p.code}>
                <code>{p.code}</code> — {p.name} {p.available ? '✓' : '(no impl)'}
              </li>
            ))}
          </ul>
        </details>
      </section>
    </div>
  );
}
