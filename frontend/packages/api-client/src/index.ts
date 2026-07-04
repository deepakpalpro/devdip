export const DEV_TENANT_ID = '11111111-1111-1111-1111-111111111111';
export const DEFAULT_QUESTIONNAIRE_CODE = 'BANKING_NEEDS';

export interface EmbeddedFormSchema {
  code: string;
  name?: string;
  sections: FormSectionSchema[];
}

export interface FormFieldSchema {
  key: string;
  type: string;
  label: string;
  required?: boolean;
  options?: string[];
  formCode?: string;
  embeddedForm?: EmbeddedFormSchema;
  embeddedUnavailable?: boolean;
}

export interface FormSectionSchema {
  key: string;
  title: string;
  fields: FormFieldSchema[];
}

export interface FormSchema {
  sections: FormSectionSchema[];
}

export interface FormSummary {
  code: string;
  name: string;
  category: string | null;
}

export interface AdminFormSummary extends FormSummary {
  id: string;
  storageStrategy: string;
  latestVersion: number | null;
  latestStatus: string | null;
}

export interface AdminFormVersion {
  id: string;
  versionNumber: number;
  status: string;
  publishedAt: string | null;
  schema: FormSchema;
}

export interface AdminFormDetail {
  id: string;
  code: string;
  name: string;
  category: string | null;
  storageStrategy: string;
  versions: AdminFormVersion[];
}

export interface CreateFormRequest {
  code: string;
  name: string;
  category?: string | null;
  storageStrategy?: 'JSON_BLOB' | 'KEY_VALUE';
}

export interface FormDetail extends FormSummary {
  formVersionId: string;
  schema: FormSchema;
}

export interface SubmissionCreated {
  submissionId: string;
  status: string;
  formCode: string;
  schema: FormSchema;
}

export interface QuestionnaireDetail {
  code: string;
  name: string;
  schema: FormSchema;
}

export interface Recommendation {
  formCode: string;
  formName: string;
  category: string | null;
  score: number;
  recommended: boolean;
  reasons: string[];
}

export interface DiscoveryEvaluation {
  sessionId: string;
  recommendations: Recommendation[];
}

export interface SubmissionDetail {
  id: string;
  status: string;
  formCode: string;
  formName: string;
  schema: FormSchema;
  sectionData: Record<string, Record<string, unknown>>;
}

export interface SubmissionSummary {
  id: string;
  formCode: string;
  formName: string;
  status: string;
  createdAt: string;
  submittedAt: string | null;
}

export type AdminSubmissionSummary = SubmissionSummary;

export interface TimelineEvent {
  eventType: string;
  note: string | null;
  fromStatus: string | null;
  toStatus: string | null;
  actorId: string | null;
  createdAt: string;
}

export interface AdminSubmissionDetail extends SubmissionDetail {
  timeline: TimelineEvent[];
}

export type ReviewActionKey = 'start' | 'approve' | 'reject' | 'request-info';

export interface ReviewResult {
  id: string;
  status: string;
}

export interface PipelineExecution {
  status: string;
  currentStep: number;
  totalSteps: number;
  startedAt: string;
  completedAt: string | null;
  errorDetails: string | null;
}

export interface TransformedField {
  fieldPath: string;
  strategy: string;
}

export interface PipelineReport {
  execution: PipelineExecution | null;
  sanitizedPayload: Record<string, Record<string, unknown>> | null;
  transformedFields: TransformedField[];
}

export interface ApiClientConfig {
  baseUrl?: string;
  tenantId?: string;
  getRequestId?: () => string;
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code?: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

async function request<T>(url: string, config: ApiClientConfig, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  headers.set('Accept', 'application/json');
  if (config.tenantId) {
    headers.set('X-Tenant-Id', config.tenantId);
  }
  if (config.getRequestId) {
    headers.set('X-Request-Id', config.getRequestId());
  }

  const response = await fetch(`${config.baseUrl ?? ''}${url}`, {
    ...init,
    headers,
  });

  if (!response.ok) {
    let message = response.statusText;
    try {
      const body = (await response.json()) as { error?: { message?: string; code?: string }; message?: string };
      message = body.error?.message ?? body.message ?? message;
      throw new ApiError(message, response.status, body.error?.code);
    } catch (error) {
      if (error instanceof ApiError) throw error;
      throw new ApiError(message, response.status);
    }
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export function createApiClient(config: ApiClientConfig = {}) {
  return {
    listConsumerForms: () =>
      request<FormSummary[]>('/api/consumer/v1/forms', config),
    getConsumerForm: (formCode: string) =>
      request<FormDetail>(`/api/consumer/v1/forms/${encodeURIComponent(formCode)}`, config),
    listAdminForms: () =>
      request<AdminFormSummary[]>('/api/admin/v1/forms', config),
    getAdminForm: (formId: string) =>
      request<AdminFormDetail>(`/api/admin/v1/forms/${formId}`, config),
    createAdminForm: (body: CreateFormRequest) =>
      request<AdminFormDetail>('/api/admin/v1/forms', config, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      }),
    createAdminFormVersion: (formId: string, cloneFromVersionId?: string) =>
      request<AdminFormVersion>(`/api/admin/v1/forms/${formId}/versions`, config, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cloneFromVersionId: cloneFromVersionId ?? null }),
      }),
    updateAdminFormVersion: (formId: string, versionId: string, schema: FormSchema) =>
      request<AdminFormVersion>(`/api/admin/v1/forms/${formId}/versions/${versionId}`, config, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ schema }),
      }),
    publishAdminFormVersion: (formId: string, versionId: string) =>
      request<AdminFormVersion>(`/api/admin/v1/forms/${formId}/versions/${versionId}/publish`, config, {
        method: 'POST',
      }),
    listAdminSubmissions: () =>
      request<AdminSubmissionSummary[]>('/api/admin/v1/submissions', config),
    getAdminSubmission: (submissionId: string) =>
      request<AdminSubmissionDetail>(`/api/admin/v1/submissions/${submissionId}`, config),
    reviewSubmission: (submissionId: string, action: ReviewActionKey, note?: string) =>
      request<ReviewResult>(`/api/admin/v1/submissions/${submissionId}/review/${action}`, config, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ note: note ?? null }),
      }),
    getSubmissionPipeline: (submissionId: string) =>
      request<PipelineReport>(`/api/admin/v1/submissions/${submissionId}/pipeline`, config),
    getQuestionnaire: (code: string) =>
      request<QuestionnaireDetail>(`/api/consumer/v1/discovery/${encodeURIComponent(code)}`, config),
    evaluateDiscovery: (code: string, answers: Record<string, unknown>) =>
      request<DiscoveryEvaluation>(`/api/consumer/v1/discovery/${encodeURIComponent(code)}/evaluate`, config, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ answers }),
      }),
    listMySubmissions: () =>
      request<SubmissionSummary[]>('/api/consumer/v1/submissions', config),
    createSubmission: (formCode: string, discoverySessionId?: string) =>
      request<SubmissionCreated>('/api/consumer/v1/submissions', config, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ formCode, discoverySessionId: discoverySessionId ?? null }),
      }),
    getSubmission: (submissionId: string) =>
      request<SubmissionDetail>(`/api/consumer/v1/submissions/${submissionId}`, config),
    saveSection: (submissionId: string, sectionKey: string, data: Record<string, unknown>) =>
      request<void>(`/api/consumer/v1/submissions/${submissionId}/sections/${encodeURIComponent(sectionKey)}`, config, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ data }),
      }),
    submitSubmission: (submissionId: string, idempotencyKey: string) =>
      request<{ submissionId: string; status: string }>(
        `/api/consumer/v1/submissions/${submissionId}/submit`,
        config,
        {
          method: 'POST',
          headers: { 'Idempotency-Key': idempotencyKey },
        },
      ),
  };
}

export type ApiClient = ReturnType<typeof createApiClient>;
