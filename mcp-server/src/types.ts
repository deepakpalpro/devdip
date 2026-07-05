export interface FormSummary {
  code: string;
  name: string;
  category: string | null;
}

export interface FormFieldSchema {
  key: string;
  type: string;
  label: string;
  required?: boolean;
  options?: string[];
  formCode?: string;
  embeddedForm?: FormSchema;
}

export interface FormSectionSchema {
  key: string;
  title: string;
  fields: FormFieldSchema[];
}

export interface FormSchema {
  sections: FormSectionSchema[];
}

export interface FormDetail {
  code: string;
  name: string;
  category: string | null;
  formVersionId: string;
  schema: FormSchema;
}

export interface AgentFormDefinition {
  code: string;
  name: string;
  category: string | null;
  formVersionId: string;
  sections: FormSectionSchema[];
  fields: FlatField[];
}

export interface FlatField {
  path: string;
  sectionKey: string;
  fieldKey: string;
  label: string;
  type: string;
  required: boolean;
  options?: string[];
}

export interface FormSuggestion {
  formCode: string;
  formName: string;
  category: string | null;
  score: number;
  reason: string;
}

export interface ExtractedEntity {
  name: string;
  value: string | number | boolean;
  confidence: number;
}

export interface PrefillPreview {
  formCode: string;
  sectionData: Record<string, Record<string, unknown>>;
  mappedFields: string[];
  missingRequired: string[];
  ambiguous: string[];
}

export interface SubmissionCreated {
  submissionId: string;
  status: string;
  formCode: string;
  schema: FormSchema;
}

export interface SubmissionDetail {
  id: string;
  status: string;
  formCode: string;
  formName: string;
  schema: FormSchema;
  currentSectionKey: string | null;
  sectionData: Record<string, Record<string, unknown>>;
}

export interface DiscoveryRecommendation {
  formCode: string;
  formName: string;
  score: number;
  rank: number;
}

export class BankingApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code?: string,
  ) {
    super(message);
    this.name = 'BankingApiError';
  }
}