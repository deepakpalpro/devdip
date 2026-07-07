import { useState } from 'react';
import { useParams, useSearchParams, Navigate } from 'react-router-dom';
import { ErrorState, LoadingState } from '@banking-forms/ui';
import { useAdminForm } from '../hooks/useAdminForms';
import { FormHubLayout, type FormHubTab } from './formHub/FormHubLayout';
import { FormHubDataTab } from './formHub/FormHubDataTab';
import { FormHubPipelineTab } from './formHub/FormHubPipelineTab';
import { FormHubServicesTab } from './formHub/FormHubServicesTab';
import { FormHubJobsTab } from './formHub/FormHubJobsTab';
import { FormHubConnectorsTab } from './formHub/FormHubConnectorsTab';

const VALID_TABS: FormHubTab[] = ['data', 'pipeline', 'services', 'jobs', 'connectors'];

function parseTab(value: string | null): FormHubTab {
  if (value && VALID_TABS.includes(value as FormHubTab)) {
    return value as FormHubTab;
  }
  return 'data';
}

export function FormHubPage() {
  const { formId = '' } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const formQuery = useAdminForm(formId);
  const [versionId, setVersionId] = useState('');

  const activeTab = parseTab(searchParams.get('tab'));
  const versions = formQuery.data?.versions ?? [];
  const selectedVersionId = versionId || versions[0]?.id || '';

  if (formQuery.isLoading) return <LoadingState message="Loading form…" />;
  if (formQuery.isError) return <ErrorState message={(formQuery.error as Error).message} />;
  if (!formQuery.data) return <ErrorState message="Form not found" />;

  const form = formQuery.data;

  const setTab = (tab: FormHubTab) => {
    setSearchParams(tab === 'data' ? {} : { tab });
  };

  return (
    <FormHubLayout
      form={form}
      versions={versions}
      selectedVersionId={selectedVersionId}
      onVersionChange={setVersionId}
      activeTab={activeTab}
      onTabChange={setTab}
    >
      {activeTab === 'data' ? <FormHubDataTab form={form} /> : null}
      {activeTab === 'pipeline' && selectedVersionId ? (
        <FormHubPipelineTab form={form} versionId={selectedVersionId} />
      ) : null}
      {activeTab === 'services' && selectedVersionId ? (
        <FormHubServicesTab form={form} versionId={selectedVersionId} />
      ) : null}
      {activeTab === 'jobs' && selectedVersionId ? (
        <FormHubJobsTab form={form} versionId={selectedVersionId} />
      ) : null}
      {activeTab === 'connectors' ? <FormHubConnectorsTab /> : null}
      {activeTab !== 'data' && activeTab !== 'connectors' && !selectedVersionId ? (
        <p className="af-hint">Select a form version in the sidebar.</p>
      ) : null}
    </FormHubLayout>
  );
}

/** Legacy route — redirect to hub pipeline tab. */
export function FormPipelineRedirect() {
  const { formId = '' } = useParams();
  return <Navigate to={`/forms/${formId}?tab=pipeline`} replace />;
}
