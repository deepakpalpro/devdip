import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ReviewActionKey } from '@banking-forms/api-client';
import { api } from '../lib/api';

export function useAdminSubmissions() {
  return useQuery({
    queryKey: ['admin-submissions'],
    queryFn: () => api.listAdminSubmissions(),
  });
}

export function useAdminSubmission(submissionId: string | undefined) {
  return useQuery({
    queryKey: ['admin-submission', submissionId],
    queryFn: () => api.getAdminSubmission(submissionId!),
    enabled: Boolean(submissionId),
  });
}

export function useReviewSubmission(submissionId: string | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ action, note }: { action: ReviewActionKey; note?: string }) =>
      api.reviewSubmission(submissionId!, action, note),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-submission', submissionId] });
      queryClient.invalidateQueries({ queryKey: ['admin-submissions'] });
    },
  });
}

export function useSubmissionPipeline(submissionId: string | undefined) {
  return useQuery({
    queryKey: ['admin-submission-pipeline', submissionId],
    queryFn: () => api.getSubmissionPipeline(submissionId!),
    enabled: Boolean(submissionId),
  });
}
