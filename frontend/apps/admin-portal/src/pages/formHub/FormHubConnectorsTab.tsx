import { useEffect, useState } from 'react';
import { Button, ErrorState } from '@banking-forms/ui';
import type { DownstreamProvider } from '@banking-forms/api-client';
import { useDownstreamProviders, useUpdateDownstreamProvider } from '../../hooks/useDownstream';

function ConnectorCard({ provider }: { provider: DownstreamProvider }) {
  const update = useUpdateDownstreamProvider();
  const [priority, setPriority] = useState(provider.priority);
  const [configText, setConfigText] = useState(
    provider.config ? JSON.stringify(provider.config, null, 2) : '',
  );
  const [configError, setConfigError] = useState<string | null>(null);

  useEffect(() => {
    setPriority(provider.priority);
    setConfigText(provider.config ? JSON.stringify(provider.config, null, 2) : '');
  }, [provider.priority, provider.config]);

  const persist = (enabled: boolean) => {
    let config: Record<string, unknown> | null = null;
    if (configText.trim()) {
      try {
        config = JSON.parse(configText) as Record<string, unknown>;
      } catch {
        setConfigError('Config must be valid JSON');
        return;
      }
    }
    setConfigError(null);
    update.mutate({ code: provider.code, body: { enabled, priority, config } });
  };

  return (
    <div className="provider-card">
      <div className="provider-head">
        <div>
          <strong>{provider.name}</strong>
          <div className="af-hint">
            <span className="bf-badge">{provider.connectorType}</span> <code>{provider.code}</code>
          </div>
        </div>
        <span className={provider.enabled ? 'import-conf import-conf-high' : 'import-conf import-conf-med'}>
          {provider.enabled ? 'Enabled (dual-write)' : 'Disabled'}
        </span>
      </div>
      <div className="af-row">
        <div className="af-field" style={{ width: 120 }}>
          <label htmlFor={`cprio-${provider.code}`}>Priority</label>
          <input
            id={`cprio-${provider.code}`}
            className="af-input"
            type="number"
            value={priority}
            onChange={(e) => setPriority(Number(e.target.value))}
          />
        </div>
        <div className="af-field" style={{ flex: 1 }}>
          <label htmlFor={`ccfg-${provider.code}`}>Config</label>
          <textarea
            id={`ccfg-${provider.code}`}
            className="af-textarea provider-config"
            value={configText}
            onChange={(e) => setConfigText(e.target.value)}
          />
        </div>
      </div>
      {configError ? <ErrorState message={configError} /> : null}
      <div className="af-row">
        <Button variant="primary" onClick={() => persist(true)} disabled={update.isPending}>
          Enable
        </Button>
        <Button variant="secondary" onClick={() => persist(false)} disabled={update.isPending}>
          Disable
        </Button>
      </div>
    </div>
  );
}

export function FormHubConnectorsTab() {
  const { data, isLoading, error } = useDownstreamProviders();

  if (isLoading) return <p className="af-hint">Loading downstream connectors…</p>;
  if (error) return <ErrorState message={(error as Error).message} />;

  return (
    <div>
      <h2 className="submission-section-title">Downstream connectors</h2>
      <p className="af-hint">
        Enable multiple connectors to dual-write sanitized payloads on pipeline completion. Tenant-wide
        settings apply to all forms; pipeline <code>connector-push</code> steps can filter targets.
      </p>
      {data?.map((provider) => (
        <ConnectorCard key={provider.code} provider={provider} />
      ))}
      {!data?.length ? <div className="bf-empty-state">No downstream providers registered.</div> : null}
    </div>
  );
}
