import { useState } from 'react';
import { Button, ErrorState } from '@banking-forms/ui';
import type { AdminFormDetail } from '@banking-forms/api-client';
import { useServiceProviders } from '../../hooks/useServiceProviders';
import {
  useCreateServiceInstance,
  useFormServiceBindings,
  useServiceInstances,
  useUpsertFormServiceBinding,
} from '../../hooks/useServiceInstances';

interface FormHubServicesTabProps {
  form: AdminFormDetail;
  versionId: string;
}

export function FormHubServicesTab({ form, versionId }: FormHubServicesTabProps) {
  const instancesQuery = useServiceInstances();
  const providersQuery = useServiceProviders();
  const bindingsQuery = useFormServiceBindings(form.id, versionId);
  const createInstance = useCreateServiceInstance();
  const upsertBinding = useUpsertFormServiceBinding(form.id, versionId);

  const [instanceCode, setInstanceCode] = useState('');
  const [instanceName, setInstanceName] = useState('');
  const [providerCode, setProviderCode] = useState('');

  const create = () => {
    if (!instanceCode.trim() || !instanceName.trim() || !providerCode) return;
    createInstance.mutate(
      { code: instanceCode.trim(), name: instanceName.trim(), providerCode },
      {
        onSuccess: () => {
          setInstanceCode('');
          setInstanceName('');
        },
      },
    );
  };

  const bindForm = (code: string) => {
    upsertBinding.mutate({
      instanceCode: code,
      scope: 'FORM',
      enabled: true,
    });
  };

  return (
    <div>
      <h2 className="submission-section-title">Service instances</h2>
      <p className="af-hint">
        Scoped service adapters for this form version. Bindings override global provider config at
        FORM → PIPELINE → PIPELET precedence.
      </p>

      <div className="af-form">
        <h3 style={{ margin: 0 }}>New instance</h3>
        <div className="af-row">
          <div className="af-field" style={{ flex: 1 }}>
            <label htmlFor="si-code">Code</label>
            <input
              id="si-code"
              className="af-input"
              value={instanceCode}
              onChange={(e) => setInstanceCode(e.target.value)}
              placeholder="loan-bureau-primary"
            />
          </div>
          <div className="af-field" style={{ flex: 2 }}>
            <label htmlFor="si-name">Name</label>
            <input
              id="si-name"
              className="af-input"
              value={instanceName}
              onChange={(e) => setInstanceName(e.target.value)}
              placeholder="Loan bureau check"
            />
          </div>
          <div className="af-field" style={{ flex: 1 }}>
            <label htmlFor="si-provider">Provider</label>
            <select
              id="si-provider"
              className="af-input"
              value={providerCode}
              onChange={(e) => setProviderCode(e.target.value)}
            >
              <option value="">Select…</option>
              {providersQuery.data?.map((p) => (
                <option key={p.code} value={p.code}>
                  {p.name}
                </option>
              ))}
            </select>
          </div>
        </div>
        <Button variant="primary" onClick={create} disabled={createInstance.isPending}>
          Create instance
        </Button>
      </div>

      {instancesQuery.data?.map((instance) => (
        <div key={instance.id} className="provider-card">
          <div className="provider-head">
            <div>
              <strong>{instance.name}</strong>
              <div className="af-hint">
                <code>{instance.code}</code> · adapter {instance.providerCode}
              </div>
            </div>
            <Button variant="secondary" onClick={() => bindForm(instance.code)}>
              Bind to this form version
            </Button>
          </div>
        </div>
      ))}

      <h2 className="submission-section-title" style={{ marginTop: '2rem' }}>
        Active bindings
      </h2>
      {!bindingsQuery.data?.length ? (
        <p className="af-hint">No service bindings for this form version yet.</p>
      ) : (
        <ul>
          {bindingsQuery.data.map((b) => (
            <li key={b.id}>
              <code>{b.instanceCode}</code> — {b.scope} ({b.enabled ? 'enabled' : 'disabled'})
            </li>
          ))}
        </ul>
      )}

      {upsertBinding.isError ? (
        <ErrorState message={(upsertBinding.error as Error).message} />
      ) : null}
    </div>
  );
}
