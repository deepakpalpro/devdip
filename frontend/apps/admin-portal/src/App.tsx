import { NavLink, Route, Routes } from 'react-router-dom';
import { AppShell, PageHeader } from '@banking-forms/ui';
import { FormsListPage } from './pages/FormsListPage';
import { FormBuilderPage } from './pages/FormBuilderPage';
import { SubmissionsListPage } from './pages/SubmissionsListPage';
import { SubmissionDetailPage } from './pages/SubmissionDetailPage';

function AdminNav() {
  return (
    <nav className="bf-nav">
      <NavLink to="/" end className={({ isActive }) => (isActive ? 'bf-nav-active' : '')}>
        Forms
      </NavLink>
      <NavLink to="/submissions" className={({ isActive }) => (isActive ? 'bf-nav-active' : '')}>
        Submissions
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
