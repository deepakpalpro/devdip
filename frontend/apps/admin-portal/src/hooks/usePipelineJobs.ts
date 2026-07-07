import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { CreatePipelineJobRequest } from '@banking-forms/api-client';
import { api } from '../lib/api';

export function useFormPipelineJobs(formId: string | undefined, versionId: string | undefined) {
  return useQuery({
    queryKey: ['form-pipeline-jobs', formId, versionId],
    queryFn: () => api.listFormPipelineJobs(formId!, versionId!),
    enabled: !!formId && !!versionId,
  });
}

export function usePipelinesForJobs() {
  return useQuery({ queryKey: ['pipelines'], queryFn: () => api.listPipelines() });
}

export function useCreatePipelineJob() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreatePipelineJobRequest) => api.createPipelineJob(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['form-pipeline-jobs'] });
      qc.invalidateQueries({ queryKey: ['pipeline-jobs'] });
    },
  });
}

export function useTriggerPipelineJob() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ code, submissionId }: { code: string; submissionId?: string }) =>
      api.triggerPipelineJob(code, submissionId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['form-pipeline-jobs'] }),
  });
}
