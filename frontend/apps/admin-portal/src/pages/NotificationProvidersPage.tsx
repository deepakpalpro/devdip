import { useEffect, useState } from 'react';
import { Button, ErrorState, LoadingState } from '@banking-forms/ui';
import { ApiError, type NotificationProvider } from '@banking-forms/api-client';
import {
  useNotificationProviders,
  useNotificationTemplates,
  useUpdateNotificationProvider,
} from '../hooks/useNotifications';
import './admin-forms.css';

function ProviderRow({ provider }: { provider: NotificationProvider }) {
  const update = useUpdateNotificationProvider();
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
            <span className="bf-badge">{provider.channelType}</span> <code>{provider.code}</code>
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
          <label htmlFor={`nprio-${provider.code}`}>Priority</label>
          <input
            id={`nprio-${provider.code}`}
            className="af-input"
            type="number"
            value={priority}
            onChange={(event) => setPriority(Number(event.target.value))}
          />
        </div>
        <div className="af-field" style={{ flex: 1 }}>
          <label htmlFor={`ncfg-${provider.code}`}>Config (JSON — from / endpoint / phoneNumberId / secretRef)</label>
          <textarea
            id={`ncfg-${provider.code}`}
            className="af-textarea provider-config"
            value={configText}
            onChange={(event) => setConfigText(event.target.value)}
            placeholder='{"endpoint":"https://…","phoneNumberId":"…","secretRef":"ENV_VAR_NAME"}'
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

function TemplatesTable() {
  const { data, isLoading, error } = useNotificationTemplates();
  if (isLoading) {
    return <LoadingState message="Loading templates…" />;
  }
  if (error) {
    return <ErrorState message={error instanceof Error ? error.message : 'Failed to load templates'} />;
  }
  const templates = data ?? [];
  if (templates.length === 0) {
    return null;
  }
  return (
    <div className="provider-card">
      <strong>Message templates</strong>
      <div className="af-hint">
        Templates are keyed by event, channel, and locale. {'{{placeholders}}'} (formName, reference,
        status) are substituted at send time.
      </div>
      <table className="bf-table" style={{ marginTop: 12 }}>
        <thead>
          <tr>
            <th>Event</th>
            <th>Channel</th>
            <th>Locale</th>
            <th>Subject / template</th>
          </tr>
        </thead>
        <tbody>
          {templates.map((t) => (
            <tr key={`${t.eventType}-${t.channelType}-${t.locale}`}>
              <td><code>{t.eventType}</code></td>
              <td>{t.channelType}</td>
              <td>{t.locale}</td>
              <td>{t.subject ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function NotificationProvidersPage() {
  const { data, isLoading, error } = useNotificationProviders();

  if (isLoading) {
    return <LoadingState message="Loading notification providers…" />;
  }
  if (error) {
    return <ErrorState message={error instanceof Error ? error.message : 'Failed to load providers'} />;
  }

  return (
    <div className="provider-list">
      <span className="af-hint">
        Configure which adapter delivers each channel. The <code>log-email</code> sink runs locally with
        no setup; <code>smtp-email</code> needs an SMTP host and <code>whatsapp-cloud</code> needs an
        endpoint + <code>secretRef</code> before they can be enabled.
      </span>
      {(data ?? []).map((provider) => (
        <ProviderRow key={provider.code} provider={provider} />
      ))}
      <TemplatesTable />
    </div>
  );
}
