import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { UpdateNotificationProviderRequest } from '@banking-forms/api-client';
import { api } from '../lib/api';

export function useNotificationProviders() {
  return useQuery({
    queryKey: ['notification-providers'],
    queryFn: () => api.listNotificationProviders(),
  });
}

export function useUpdateNotificationProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ code, body }: { code: string; body: UpdateNotificationProviderRequest }) =>
      api.updateNotificationProvider(code, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notification-providers'] });
    },
  });
}

export function useNotificationTemplates() {
  return useQuery({
    queryKey: ['notification-templates'],
    queryFn: () => api.listNotificationTemplates(),
  });
}
