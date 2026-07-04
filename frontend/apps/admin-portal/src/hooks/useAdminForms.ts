import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { CreateFormRequest, FormSchema } from '@banking-forms/api-client';
import { api } from '../lib/api';

export function useAdminForms() {
  return useQuery({
    queryKey: ['admin-forms'],
    queryFn: () => api.listAdminForms(),
  });
}

export function useAdminForm(formId: string | undefined) {
  return useQuery({
    queryKey: ['admin-form', formId],
    queryFn: () => api.getAdminForm(formId!),
    enabled: Boolean(formId),
  });
}

export function useCreateForm() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateFormRequest) => api.createAdminForm(body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-forms'] });
    },
  });
}

export function useCreateVersion(formId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (cloneFromVersionId?: string) => api.createAdminFormVersion(formId!, cloneFromVersionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-form', formId] });
      queryClient.invalidateQueries({ queryKey: ['admin-forms'] });
    },
  });
}

export function useUpdateVersionSchema(formId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ versionId, schema }: { versionId: string; schema: FormSchema }) =>
      api.updateAdminFormVersion(formId!, versionId, schema),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-form', formId] });
    },
  });
}

export function usePublishVersion(formId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (versionId: string) => api.publishAdminFormVersion(formId!, versionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-form', formId] });
      queryClient.invalidateQueries({ queryKey: ['admin-forms'] });
    },
  });
}
