import { createApiClient, DEV_TENANT_ID } from '@banking-forms/api-client';
import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
    },
  },
});

export const api = createApiClient({
  tenantId: import.meta.env.VITE_TENANT_ID ?? DEV_TENANT_ID,
  getRequestId: () => crypto.randomUUID(),
});
