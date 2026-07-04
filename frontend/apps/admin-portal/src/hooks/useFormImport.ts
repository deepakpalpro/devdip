import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { AcceptImportRequest, UpdateImportProviderRequest } from '@banking-forms/api-client';
import { api } from '../lib/api';

export function useUploadFormImport() {
  return useMutation({
    mutationFn: (file: File) => api.uploadFormImport(file),
  });
}

export function useImportFromUrl() {
  return useMutation({
    mutationFn: (url: string) => api.importFormFromUrl(url),
  });
}

export function useAcceptFormImport(jobId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: AcceptImportRequest) => api.acceptFormImport(jobId!, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-forms'] });
    },
  });
}

export function useImportProviders() {
  return useQuery({
    queryKey: ['import-providers'],
    queryFn: () => api.listImportProviders(),
  });
}

export function useUpdateImportProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ code, body }: { code: string; body: UpdateImportProviderRequest }) =>
      api.updateImportProvider(code, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['import-providers'] });
    },
  });
}
