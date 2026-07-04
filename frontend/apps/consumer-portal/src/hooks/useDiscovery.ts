import { useMutation, useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';

export function useQuestionnaire(code: string) {
  return useQuery({
    queryKey: ['questionnaire', code],
    queryFn: () => api.getQuestionnaire(code),
    enabled: Boolean(code),
  });
}

export function useEvaluateDiscovery(code: string) {
  return useMutation({
    mutationFn: (answers: Record<string, unknown>) => api.evaluateDiscovery(code, answers),
  });
}
