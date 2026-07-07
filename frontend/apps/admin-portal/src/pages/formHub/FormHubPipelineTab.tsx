import { useEffect, useMemo, useState } from 'react';
import { Button } from '@banking-forms/ui';
import type { AdminFormDetail, PipelineStepRequest } from '@banking-forms/api-client';
import {
  PIPELINE_TRIGGERS,
  useFormPipelineBindings,
  usePipelets,
  usePipelines,
  usePipeline,
  useUpdatePipelineSteps,
  useUpsertFormPipelineBinding,
} from '../../hooks/usePipelines';

interface FormHubPipelineTabProps {
  form: AdminFormDetail;
  versionId: string;
}

export function FormHubPipelineTab({ form, versionId }: FormHubPipelineTabProps) {
  const pipeletsQuery = usePipelets();
  const pipelinesQuery = usePipelines();
  const bindingsQuery = useFormPipelineBindings(form.id, versionId);
  const upsertBinding = useUpsertFormPipelineBinding(form.id, versionId);

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
  const [trigger, setTrigger] = useState<(typeof PIPELINE_TRIGGERS)[number]>('ON_SUBMIT');

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

  const saveBinding = () => {
    if (!activePipelineId) return;
    upsertBinding.mutate({ pipelineId: activePipelineId, trigger, enabled: true });
  };

  return (
    <div>
      <h2 className="submission-section-title">Pipeline binding</h2>
      <p className="af-hint">Associate a configurable pipelet pipeline with trigger events for this version.</p>

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
        <select
          id="trigger-select"
          className="af-input"
          value={trigger}
          onChange={(e) => setTrigger(e.target.value as (typeof PIPELINE_TRIGGERS)[number])}
        >
          {PIPELINE_TRIGGERS.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>
      </div>

      <Button variant="primary" onClick={saveBinding} disabled={!activePipelineId || upsertBinding.isPending}>
        Save binding
      </Button>

      {bindingsQuery.data && bindingsQuery.data.length > 0 ? (
        <ul className="af-hint" style={{ marginTop: '1rem' }}>
          {bindingsQuery.data.map((b) => (
            <li key={b.id}>
              {b.trigger} → {b.pipelineDefinitionId.slice(0, 8)}… ({b.enabled ? 'enabled' : 'disabled'})
            </li>
          ))}
        </ul>
      ) : null}

      <h2 className="submission-section-title" style={{ marginTop: '2rem' }}>
        Pipelet steps {selectedPipeline ? `— ${selectedPipeline.name}` : ''}
      </h2>

      <div className="af-row" style={{ gap: '0.5rem', marginBottom: '1rem' }}>
        <Button
          variant="secondary"
          onClick={() => {
            const code = pipeletsQuery.data?.[0]?.code;
            if (!code) return;
            setDraftSteps((steps) => [
              ...steps,
              { stepKey: `step-${steps.length + 1}`, pipeletCode: code, properties: null },
            ]);
          }}
        >
          Add pipelet
        </Button>
        <Button
          variant="primary"
          onClick={() => updateSteps.mutate(draftSteps)}
          disabled={!activePipelineId || selectedPipeline?.systemDefault}
        >
          Save steps
        </Button>
      </div>

      {selectedPipeline?.systemDefault ? (
        <p className="af-hint">System default pipeline steps are read-only. Create a custom pipeline to edit.</p>
      ) : null}

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
                  {p.name}
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
                // ignore while typing
              }
            }}
          />
        </div>
      ))}
    </div>
  );
}
