import { useEffect, useState } from 'react';
import { Button, ErrorState, LoadingState } from '@banking-forms/ui';
import { ApiError, type ImportProvider } from '@banking-forms/api-client';
import { useImportProviders, useUpdateImportProvider } from '../hooks/useFormImport';
import './admin-forms.css';

function ProviderRow({ provider }: { provider: ImportProvider }) {
  const update = useUpdateImportProvider();
  const [priority, setPriority] = useState(provider.priority);
  const [configText, setConfigText] = useState(
    provider.config ? JSON.stringify(provider.config, null, 2) : '',
  );
  const [configError, setConfigError] = useState<string | null>(null);

  // Keep local edits in sync if the server data changes underneath us.
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
            <span className="bf-badge">{provider.sourceType}</span> <code>{provider.code}</code>
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
          <label htmlFor={`prio-${provider.code}`}>Priority</label>
          <input
            id={`prio-${provider.code}`}
            className="af-input"
            type="number"
            value={priority}
            onChange={(event) => setPriority(Number(event.target.value))}
          />
        </div>
        <div className="af-field" style={{ flex: 1 }}>
          <label htmlFor={`cfg-${provider.code}`}>Config (JSON — endpoint / model / secretRef)</label>
          <textarea
            id={`cfg-${provider.code}`}
            className="af-textarea provider-config"
            value={configText}
            onChange={(event) => setConfigText(event.target.value)}
            placeholder='{"endpoint":"https://…","model":"…","secretRef":"ENV_VAR_NAME"}'
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

export function ImportProvidersPage() {
  const { data, isLoading, error } = useImportProviders();

  if (isLoading) {
    return <LoadingState message="Loading import providers…" />;
  }
  if (error) {
    return <ErrorState message={error instanceof Error ? error.message : 'Failed to load providers'} />;
  }

  return (
    <div className="provider-list">
      <span className="af-hint">
        Configure which engine extracts each source type. In-JVM providers run locally; external
        providers (e.g. LLM vision) require an endpoint and a <code>secretRef</code> before they can be
        enabled.
      </span>
      {(data ?? []).map((provider) => (
        <ProviderRow key={provider.code} provider={provider} />
      ))}
    </div>
  );
}
