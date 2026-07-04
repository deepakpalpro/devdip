import { NavLink, Route, Routes } from 'react-router-dom';
import { AppShell, PageHeader } from '@banking-forms/ui';
import { FormsListPage } from './pages/FormsListPage';
import { FormBuilderPage } from './pages/FormBuilderPage';
import { FormImportPage } from './pages/FormImportPage';
import { ImportProvidersPage } from './pages/ImportProvidersPage';
import { NotificationProvidersPage } from './pages/NotificationProvidersPage';
import { DownstreamProvidersPage } from './pages/DownstreamProvidersPage';
import { ServiceProvidersPage } from './pages/ServiceProvidersPage';
import { SubmissionsListPage } from './pages/SubmissionsListPage';
import { SubmissionDetailPage } from './pages/SubmissionDetailPage';

function AdminNav() {
  return (
    <nav className="bf-nav">
      <NavLink to="/" end className={({ isActive }) => (isActive ? 'bf-nav-active' : '')}>
        Forms
      </NavLink>
      <NavLink to="/import" className={({ isActive }) => (isActive ? 'bf-nav-active' : '')}>
        Import
      </NavLink>
      <NavLink to="/submissions" className={({ isActive }) => (isActive ? 'bf-nav-active' : '')}>
        Submissions
      </NavLink>
      <NavLink to="/settings/import-providers" className={({ isActive }) => (isActive ? 'bf-nav-active' : '')}>
        Import providers
      </NavLink>
      <NavLink to="/settings/notification-providers" className={({ isActive }) => (isActive ? 'bf-nav-active' : '')}>
        Notifications
      </NavLink>
      <NavLink to="/settings/downstream-providers" className={({ isActive }) => (isActive ? 'bf-nav-active' : '')}>
        Downstream
      </NavLink>
      <NavLink to="/settings/service-providers" className={({ isActive }) => (isActive ? 'bf-nav-active' : '')}>
        Services
      </NavLink>
    </nav>
  );
}

export function App() {
  return (
    <AppShell title="Banking Forms Admin" subtitle="Operations & form design" theme="admin" nav={<AdminNav />}>
      <Routes>
        <Route
          path="/"
          element={
            <>
              <PageHeader
                title="Form definitions"
                description="Manage application templates, versions, and processing configuration."
              />
              <FormsListPage />
            </>
          }
        />
        <Route path="/forms/:formId/builder" element={<FormBuilderPage />} />
        <Route
          path="/import"
          element={
            <>
              <PageHeader
                title="Import a form"
                description="Upload a PDF, CSV, spreadsheet, or image — or point at a form URL. The platform extracts a draft schema for you to review, edit, and publish."
              />
              <FormImportPage />
            </>
          }
        />
        <Route
          path="/settings/import-providers"
          element={
            <>
              <PageHeader
                title="Import providers"
                description="Choose and configure which engine extracts each source type. External providers plug in via module-service-integration."
              />
              <ImportProvidersPage />
            </>
          }
        />
        <Route
          path="/settings/notification-providers"
          element={
            <>
              <PageHeader
                title="Notification providers"
                description="Choose and configure which adapter delivers each channel (email, WhatsApp). External providers plug in via module-service-integration."
              />
              <NotificationProvidersPage />
            </>
          }
        />
        <Route
          path="/settings/downstream-providers"
          element={
            <>
              <PageHeader
                title="Downstream providers"
                description="Configure where sanitized submission payloads are delivered (log, REST webhook, Kafka, S3). External connectors plug in via module-service-integration."
              />
              <DownstreamProvidersPage />
            </>
          }
        />
        <Route
          path="/settings/service-providers"
          element={
            <>
              <PageHeader
                title="Service providers"
                description="Configure external API adapters (credit bureau, identity verification, REST hooks) invoked during pipeline processing."
              />
              <ServiceProvidersPage />
            </>
          }
        />
        <Route
          path="/submissions"
          element={
            <>
              <PageHeader
                title="Submitted applications"
                description="Review consumer applications and the data captured in each section."
              />
              <SubmissionsListPage />
            </>
          }
        />
        <Route path="/submissions/:id" element={<SubmissionDetailPage />} />
      </Routes>
    </AppShell>
  );
}
