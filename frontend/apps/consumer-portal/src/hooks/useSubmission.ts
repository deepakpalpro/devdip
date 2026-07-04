import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { FormFieldSchema, FormSectionSchema } from '@banking-forms/form-renderer';
import { api } from '../lib/api';

const submissionKey = (id: string) => ['submission', id] as const;
const mySubmissionsKey = ['my-submissions'] as const;

export function useMySubmissions() {
  return useQuery({
    queryKey: mySubmissionsKey,
    queryFn: () => api.listMySubmissions(),
  });
}

export function useSubmission(submissionId: string | null) {
  return useQuery({
    queryKey: submissionKey(submissionId ?? ''),
    queryFn: () => api.getSubmission(submissionId!),
    enabled: Boolean(submissionId),
  });
}

/**
 * Fetches a published form's schema by code without creating a submission. Used by the wizard to
 * render sections before a draft exists (lazy-draft flow), so merely opening a form no longer
 * persists an orphan draft.
 */
export function useConsumerForm(formCode: string | undefined, enabled: boolean) {
  return useQuery({
    queryKey: ['consumer-form', formCode ?? ''] as const,
    queryFn: () => api.getConsumerForm(formCode!),
    enabled: enabled && Boolean(formCode),
  });
}

export function useCreateSubmission(formCode: string, discoverySessionId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api.createSubmission(formCode, discoverySessionId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: mySubmissionsKey }),
  });
}

export function useSaveSection() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      submissionId,
      sectionKey,
      data,
      resumeSectionKey,
    }: {
      submissionId: string;
      sectionKey: string;
      data: Record<string, unknown>;
      resumeSectionKey?: string;
    }) => api.saveSection(submissionId, sectionKey, data, resumeSectionKey),
    onSuccess: (_result, variables) =>
      queryClient.invalidateQueries({ queryKey: submissionKey(variables.submissionId) }),
  });
}

export function useSubmitApplication() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ submissionId }: { submissionId: string }) =>
      api.submitSubmission(submissionId, crypto.randomUUID()),
    onSuccess: (_result, variables) => {
      queryClient.invalidateQueries({ queryKey: submissionKey(variables.submissionId) });
      queryClient.invalidateQueries({ queryKey: mySubmissionsKey });
    },
  });
}

export function useDiscardSubmission() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (submissionId: string) => api.discardSubmission(submissionId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: mySubmissionsKey }),
  });
}

export function validateSection(section: FormSectionSchema, values: Record<string, unknown>) {
  const errors: Record<string, string> = {};
  validateFields(section.fields, values, '', errors);
  return errors;
}

function validateFields(
  fields: FormFieldSchema[],
  values: Record<string, unknown>,
  prefix: string,
  errors: Record<string, string>,
) {
  for (const field of fields) {
    const value = values?.[field.key];
    if (field.type === 'embedded_form') {
      const nested = (value as Record<string, Record<string, unknown>>) ?? {};
      if (field.embeddedForm) {
        for (const embeddedSection of field.embeddedForm.sections) {
          validateFields(
            embeddedSection.fields,
            nested[embeddedSection.key] ?? {},
            `${prefix}${field.key}.${embeddedSection.key}.`,
            errors,
          );
        }
      }
    } else if (field.required && (value === undefined || value === null || value === '')) {
      errors[`${prefix}${field.key}`] = 'Required';
    }
  }
}
