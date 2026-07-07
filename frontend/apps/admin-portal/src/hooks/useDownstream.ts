import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { UpdateDownstreamProviderRequest } from '@banking-forms/api-client';
import { api } from '../lib/api';

export function useDownstreamProviders() {
  return useQuery({
    queryKey: ['downstream-providers'],
    queryFn: () => api.listDownstreamProviders(),
  });
}

export function useUpdateDownstreamProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ code, body }: { code: string; body: UpdateDownstreamProviderRequest }) =>
      api.updateDownstreamProvider(code, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['downstream-providers'] });
    },
  });
}

export function useOutboxEvents(submissionId: string | undefined) {
  return useQuery({
    queryKey: ['outbox-events', submissionId],
    queryFn: () => api.listOutboxEvents(submissionId!),
    enabled: Boolean(submissionId),
  });
}
