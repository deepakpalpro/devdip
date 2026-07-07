import { Link } from 'react-router-dom';
import type { AdminFormDetail, AdminFormVersion } from '@banking-forms/api-client';
import { statusBadgeClass } from '../formStatus';

export type FormHubTab = 'data' | 'pipeline' | 'services' | 'jobs' | 'connectors';

const TABS: { id: FormHubTab; label: string }[] = [
  { id: 'data', label: 'Submissions' },
  { id: 'pipeline', label: 'Pipeline' },
  { id: 'services', label: 'Services' },
  { id: 'jobs', label: 'Jobs' },
  { id: 'connectors', label: 'Connectors' },
];

interface FormHubLayoutProps {
  form: AdminFormDetail;
  versions: AdminFormVersion[];
  selectedVersionId: string;
  onVersionChange: (versionId: string) => void;
  activeTab: FormHubTab;
  onTabChange: (tab: FormHubTab) => void;
  children: React.ReactNode;
}

export function FormHubLayout({
  form,
  versions,
  selectedVersionId,
  onVersionChange,
  activeTab,
  onTabChange,
  children,
}: FormHubLayoutProps) {
  return (
    <div className="builder-layout">
      <aside className="builder-sidebar">
        <p className="af-hint">
          <Link to="/">← All forms</Link>
        </p>
        <h2 className="builder-form-title">{form.name}</h2>
        <p className="af-hint">
          <span className="bf-badge">{form.code}</span>
        </p>
        <p className="af-hint">
          <Link to={`/forms/${form.id}/builder`}>Open schema builder →</Link>
        </p>
        <h3 className="submission-section-title" style={{ fontSize: '0.95rem', marginTop: '1rem' }}>
          Form version
        </h3>
        <ul className="builder-version-list">
          {versions.map((v) => (
            <li key={v.id}>
              <button
                type="button"
                className={`builder-version-btn ${selectedVersionId === v.id ? 'builder-version-active' : ''}`}
                onClick={() => onVersionChange(v.id)}
              >
                <span className={statusBadgeClass(v.status)}>v{v.versionNumber}</span>
                <span>{v.status}</span>
              </button>
            </li>
          ))}
        </ul>
      </aside>

      <section className="builder-main">
        <nav className="form-hub-tabs" aria-label="Form hub sections">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              type="button"
              className={`form-hub-tab ${activeTab === tab.id ? 'form-hub-tab-active' : ''}`}
              onClick={() => onTabChange(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </nav>
        {children}
      </section>
    </div>
  );
}
