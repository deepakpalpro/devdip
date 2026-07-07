import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { CreateServiceInstanceRequest, UpsertServiceBindingRequest } from '@banking-forms/api-client';
import { api } from '../lib/api';

export function useServiceInstances() {
  return useQuery({ queryKey: ['service-instances'], queryFn: () => api.listServiceInstances() });
}

export function useFormServiceBindings(formId: string | undefined, versionId: string | undefined) {
  return useQuery({
    queryKey: ['form-service-bindings', formId, versionId],
    queryFn: () => api.listFormServiceBindings(formId!, versionId!),
    enabled: !!formId && !!versionId,
  });
}

export function useCreateServiceInstance() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateServiceInstanceRequest) => api.createServiceInstance(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['service-instances'] }),
  });
}

export function useUpsertFormServiceBinding(formId: string, versionId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: UpsertServiceBindingRequest) =>
      api.upsertFormServiceBinding(formId, versionId, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['form-service-bindings', formId, versionId] });
    },
  });
}
