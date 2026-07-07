import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { PipelineStepRequest, PipelineTrigger, UpsertPipelineBindingRequest } from '@banking-forms/api-client';
import { api } from '../lib/api';

export function usePipelets() {
  return useQuery({ queryKey: ['pipelets'], queryFn: () => api.listPipelets() });
}

export function usePipelines() {
  return useQuery({ queryKey: ['pipelines'], queryFn: () => api.listPipelines() });
}

export function usePipeline(pipelineId: string | undefined) {
  return useQuery({
    queryKey: ['pipelines', pipelineId],
    queryFn: () => api.getPipeline(pipelineId!),
    enabled: !!pipelineId,
  });
}

export function useFormPipelineBindings(formId: string | undefined, versionId: string | undefined) {
  return useQuery({
    queryKey: ['form-pipeline-bindings', formId, versionId],
    queryFn: () => api.listFormPipelineBindings(formId!, versionId!),
    enabled: !!formId && !!versionId,
  });
}

export function useUpdatePipelineSteps(pipelineId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (steps: PipelineStepRequest[]) => api.updatePipelineSteps(pipelineId, steps),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['pipelines'] });
      qc.invalidateQueries({ queryKey: ['pipelines', pipelineId] });
    },
  });
}

export function useUpsertFormPipelineBinding(formId: string, versionId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: UpsertPipelineBindingRequest) =>
      api.upsertFormPipelineBinding(formId, versionId, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['form-pipeline-bindings', formId, versionId] });
    },
  });
}

export const PIPELINE_TRIGGERS: PipelineTrigger[] = [
  'ON_SUBMIT',
  'ON_APPROVED',
  'ON_REJECTED',
  'ON_STATUS_CHANGE',
];
