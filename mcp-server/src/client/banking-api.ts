import {
  BankingApiError,
  type DiscoveryRecommendation,
  FormDetail,
  FormSummary,
  SubmissionCreated,
  SubmissionDetail,
} from '../types.js';

export interface BankingClientConfig {
  baseUrl: string;
  tenantId: string;
  userId: string;
  apiKey?: string;
}

export class BankingApiClient {
  constructor(private readonly config: BankingClientConfig) {}

  async listForms(): Promise<FormSummary[]> {
    return this.request<FormSummary[]>('/api/consumer/v1/forms');
  }

  async getForm(formCode: string): Promise<FormDetail> {
    return this.request<FormDetail>(`/api/consumer/v1/forms/${encodeURIComponent(formCode)}`);
  }

  async evaluateDiscovery(
    questionnaireCode: string,
    answers: Record<string, unknown>,
  ): Promise<{ sessionId: string; recommendations: DiscoveryRecommendation[] }> {
    const result = await this.request<{
      sessionId: string;
      recommendations: Array<{ formCode: string; formName: string; score: number; rank: number }>;
    }>(`/api/consumer/v1/discovery/${encodeURIComponent(questionnaireCode)}/evaluate`, {
      method: 'POST',
      body: JSON.stringify({ answers }),
    });
    return {
      sessionId: result.sessionId,
      recommendations: result.recommendations.map((r) => ({
        formCode: r.formCode,
        formName: r.formName,
        score: r.score,
        rank: r.rank,
      })),
    };
  }

  async createDraft(formCode: string, discoverySessionId?: string): Promise<SubmissionCreated> {
    return this.request<SubmissionCreated>('/api/consumer/v1/submissions', {
      method: 'POST',
      body: JSON.stringify({ formCode, discoverySessionId: discoverySessionId ?? null }),
    });
  }

  async getSubmission(submissionId: string): Promise<SubmissionDetail> {
    return this.request<SubmissionDetail>(`/api/consumer/v1/submissions/${submissionId}`);
  }

  async saveSection(
    submissionId: string,
    sectionKey: string,
    data: Record<string, unknown>,
    resumeSectionKey?: string,
  ): Promise<void> {
    await this.request<void>(
      `/api/consumer/v1/submissions/${submissionId}/sections/${encodeURIComponent(sectionKey)}`,
      {
        method: 'PUT',
        body: JSON.stringify({ data, resumeSectionKey: resumeSectionKey ?? null }),
      },
    );
  }

  async submit(submissionId: string, idempotencyKey: string): Promise<{ submissionId: string; status: string }> {
    return this.request<{ submissionId: string; status: string }>(
      `/api/consumer/v1/submissions/${submissionId}/submit`,
      {
        method: 'POST',
        headers: { 'Idempotency-Key': idempotencyKey },
      },
    );
  }

  private async request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers: Record<string, string> = {
      'X-Tenant-Id': this.config.tenantId,
      'X-Dev-User-Id': this.config.userId,
      Accept: 'application/json',
      ...(init.headers as Record<string, string> | undefined),
    };
    if (this.config.apiKey) {
      headers.Authorization = `Bearer ${this.config.apiKey}`;
    }
    if (init.body && !headers['Content-Type']) {
      headers['Content-Type'] = 'application/json';
    }

    const response = await fetch(`${this.config.baseUrl}${path}`, { ...init, headers });

    if (!response.ok) {
      let message = `HTTP ${response.status}`;
      try {
        const body = (await response.json()) as { error?: { message?: string }; message?: string };
        message = body.error?.message ?? body.message ?? message;
      } catch {
        // ignore parse errors
      }
      throw new BankingApiError(message, response.status);
    }

    if (response.status === 204) {
      return undefined as T;
    }
    return response.json() as Promise<T>;
  }
}
