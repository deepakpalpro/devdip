import { Link, NavLink, Route, Routes } from 'react-router-dom';
import { AppShell, Button, PageHeader } from '@banking-forms/ui';
import { FormCatalog } from './pages/FormCatalog';
import { DiscoveryWizardPage } from './pages/DiscoveryWizardPage';
import { SubmissionWizardPage } from './pages/SubmissionWizardPage';
import { MyApplicationsPage } from './pages/MyApplicationsPage';
import { ApplicationStatusPage } from './pages/ApplicationStatusPage';

const navLinkClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'bf-nav-active' : undefined);

export function App() {
  return (
    <AppShell
      title="Banking Forms"
      subtitle="Consumer portal"
      nav={
        <nav className="bf-nav">
          <NavLink to="/" end className={navLinkClass}>
            Catalog
          </NavLink>
          <NavLink to="/applications" className={navLinkClass}>
            My applications
          </NavLink>
        </nav>
      }
    >
      <Routes>
        <Route
          path="/"
          element={
            <>
              <PageHeader
                title="Available applications"
                description="Choose a form to begin your application. Progress is saved section by section."
              />
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: '1rem',
                  flexWrap: 'wrap',
                  padding: '1.25rem 1.5rem',
                  marginBottom: '1.5rem',
                  borderRadius: 'var(--bf-radius-lg)',
                  background: 'var(--bf-primary-soft)',
                  border: '1px solid color-mix(in srgb, var(--bf-primary) 25%, var(--bf-border))',
                }}
              >
                <div>
                  <strong>Not sure which application you need?</strong>
                  <p style={{ margin: '0.25rem 0 0', color: 'var(--bf-text-muted)' }}>
                    Answer a few quick questions and we will recommend the right one — and carry your answers over.
                  </p>
                </div>
                <Link to="/discover">
                  <Button>Help me choose</Button>
                </Link>
              </div>
              <FormCatalog />
            </>
          }
        />
        <Route path="/discover" element={<DiscoveryWizardPage />} />
        <Route path="/discover/:code" element={<DiscoveryWizardPage />} />
        <Route path="/apply/:formCode" element={<SubmissionWizardPage />} />
        <Route path="/applications" element={<MyApplicationsPage />} />
        <Route path="/applications/:submissionId" element={<ApplicationStatusPage />} />
      </Routes>
    </AppShell>
  );
}
