import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';

export function useConsumerForms() {
  return useQuery({
    queryKey: ['consumer-forms'],
    queryFn: () => api.listConsumerForms(),
  });
}
