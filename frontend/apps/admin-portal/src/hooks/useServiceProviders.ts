import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { UpdateServiceProviderRequest } from '@banking-forms/api-client';
import { api } from '../lib/api';

export function useServiceProviders() {
  return useQuery({
    queryKey: ['service-providers'],
    queryFn: () => api.listServiceProviders(),
  });
}

export function useUpdateServiceProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ code, body }: { code: string; body: UpdateServiceProviderRequest }) =>
      api.updateServiceProvider(code, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['service-providers'] });
    },
  });
}

export function useServiceCalls(submissionId: string | undefined) {
  return useQuery({
    queryKey: ['service-calls', submissionId],
    queryFn: () => api.listServiceCalls(submissionId!),
    enabled: Boolean(submissionId),
  });
}
