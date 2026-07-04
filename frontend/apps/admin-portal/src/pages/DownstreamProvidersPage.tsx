import { useEffect, useState } from 'react';
import { Button, ErrorState, LoadingState } from '@banking-forms/ui';
import { ApiError, type DownstreamProvider } from '@banking-forms/api-client';
import { useDownstreamProviders, useUpdateDownstreamProvider } from '../hooks/useDownstream';
import './admin-forms.css';

function ProviderRow({ provider }: { provider: DownstreamProvider }) {
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
        <div className="provider-flags">
          {provider.available ? (
            <span className="import-conf import-conf-high">Available</span>
          ) : (
            <span className="import-conf import-conf-low">No implementation</span>
          )}
          <span className={provider.enabled ? 'import-conf import-conf-high' : 'import-conf import-conf-med'}>
            {provider.enabled ? 'Enabled' : 'Disabled'}
          </span>
        </div>
      </div>

      <div className="af-row">
        <div className="af-field" style={{ width: 120 }}>
          <label htmlFor={`dprio-${provider.code}`}>Priority</label>
          <input
            id={`dprio-${provider.code}`}
            className="af-input"
            type="number"
            value={priority}
            onChange={(event) => setPriority(Number(event.target.value))}
          />
        </div>
        <div className="af-field" style={{ flex: 1 }}>
          <label htmlFor={`dcfg-${provider.code}`}>Config (JSON — endpoint / topic / bucket / secretRef)</label>
          <textarea
            id={`dcfg-${provider.code}`}
            className="af-textarea provider-config"
            value={configText}
            onChange={(event) => setConfigText(event.target.value)}
            placeholder='{"endpoint":"https://…","method":"POST","secretRef":"ENV_VAR_NAME"}'
          />
        </div>
      </div>

      {configError ? <ErrorState message={configError} /> : null}
      {update.isError ? (
        <ErrorState message={update.error instanceof ApiError ? update.error.message : 'Update failed'} />
      ) : null}

      <div className="af-row">
        <Button
          variant={provider.enabled ? 'secondary' : 'primary'}
          onClick={() => persist(!provider.enabled)}
          disabled={update.isPending}
        >
          {provider.enabled ? 'Disable' : 'Enable'}
        </Button>
        <Button variant="secondary" onClick={() => persist(provider.enabled)} disabled={update.isPending}>
          {update.isPending ? 'Saving…' : 'Save changes'}
        </Button>
      </div>
    </div>
  );
}

export function DownstreamProvidersPage() {
  const { data, isLoading, error } = useDownstreamProviders();

  if (isLoading) {
    return <LoadingState message="Loading downstream providers…" />;
  }
  if (error) {
    return <ErrorState message={error instanceof Error ? error.message : 'Failed to load providers'} />;
  }

  return (
    <div className="provider-list">
      <span className="af-hint">
        Configure where sanitized submission payloads are delivered after pipeline processing. The{' '}
        <code>log-sink</code> runs locally with no setup; <code>rest-webhook</code> needs an endpoint and optional{' '}
        <code>secretRef</code>. <code>kafka-stream</code> and <code>s3-archive</code> are configured-but-unavailable
        until adapters are added to module-service-integration.
      </span>
      {(data ?? []).map((provider) => (
        <ProviderRow key={provider.code} provider={provider} />
      ))}
    </div>
  );
}
