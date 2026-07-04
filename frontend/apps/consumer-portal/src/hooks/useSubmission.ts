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

export function useCreateSubmission(formCode: string, discoverySessionId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api.createSubmission(formCode, discoverySessionId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: mySubmissionsKey }),
  });
}

export function useSaveSection(submissionId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ sectionKey, data }: { sectionKey: string; data: Record<string, unknown> }) =>
      api.saveSection(submissionId, sectionKey, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: submissionKey(submissionId) }),
  });
}

export function useSubmitApplication(submissionId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api.submitSubmission(submissionId, crypto.randomUUID()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: submissionKey(submissionId) });
      queryClient.invalidateQueries({ queryKey: mySubmissionsKey });
    },
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
