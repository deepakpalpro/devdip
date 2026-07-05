import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { BankingApiClient } from './client/banking-api.js';
import { extractEntities } from './services/entity-extractor.js';
import { mapEntitiesToFields } from './services/field-mapper.js';
import { flattenFormSchema } from './services/form-schema.js';
import { suggestForms } from './services/intent-matcher.js';
import { z } from 'zod';

function jsonText(data: unknown) {
  return { content: [{ type: 'text' as const, text: JSON.stringify(data, null, 2) }] };
}

function toolError(message: string, status?: number) {
  return {
    content: [{ type: 'text' as const, text: JSON.stringify({ error: message, status: status ?? 500 }) }],
    isError: true as const,
  };
}

export function createMcpServer(client: BankingApiClient): McpServer {
  const server = new McpServer({
    name: 'banking-forms-platform',
    version: '0.1.0',
  });

  server.tool(
    'list_forms',
    'List published banking forms available for the tenant',
    {},
    async () => {
      try {
        const forms = await client.listForms();
        return jsonText({ forms });
      } catch (e) {
        return toolError(e instanceof Error ? e.message : 'Failed to list forms', (e as { status?: number }).status);
      }
    },
  );

  server.tool(
    'get_form_definition',
    'Retrieve the full JSON schema for a form by code (agent-friendly flattened fields included)',
    { formCode: z.string().describe('Published form code, e.g. LOAN_APPLICATION') },
    async ({ formCode }) => {
      try {
        const form = await client.getForm(formCode);
        const agentView = flattenFormSchema(form);
        return jsonText({ definition: agentView });
      } catch (e) {
        return toolError(e instanceof Error ? e.message : 'Form not found', (e as { status?: number }).status);
      }
    },
  );

  server.tool(
    'suggest_forms',
    'Interpret natural-language user intent and suggest matching banking forms',
    {
      userIntent: z.string().describe('What the user wants to do, e.g. "I want to open a savings account"'),
    },
    async ({ userIntent }) => {
      try {
        const catalog = await client.listForms();
        const suggestions = suggestForms(userIntent, catalog);
        return jsonText({
          userIntent,
          suggestions,
          recommended: suggestions[0] ?? null,
          message:
            suggestions.length === 0
              ? 'No matching forms found. Ask the user to clarify their banking need.'
              : `Top suggestion: ${suggestions[0].formName} (${suggestions[0].formCode})`,
        });
      } catch (e) {
        return toolError(e instanceof Error ? e.message : 'Suggestion failed', (e as { status?: number }).status);
      }
    },
  );

  server.tool(
    'evaluate_discovery',
    'Run the BANKING_NEEDS discovery questionnaire with structured answers for ranked recommendations',
    {
      answers: z.record(z.unknown()).describe('Questionnaire answers keyed by question id'),
      questionnaireCode: z.string().optional().describe('Defaults to BANKING_NEEDS'),
    },
    async ({ answers, questionnaireCode }) => {
      try {
        const code = questionnaireCode ?? 'BANKING_NEEDS';
        const result = await client.evaluateDiscovery(code, answers);
        return jsonText(result);
      } catch (e) {
        return toolError(e instanceof Error ? e.message : 'Discovery evaluation failed', (e as { status?: number }).status);
      }
    },
  );

  server.tool(
    'preview_prefill',
    'Extract entities from conversational text and preview how they map to form fields (no submission created)',
    {
      formCode: z.string(),
      userMessage: z.string().describe('Conversational input from the user'),
    },
    async ({ formCode, userMessage }) => {
      try {
        const form = await client.getForm(formCode);
        const agentView = flattenFormSchema(form);
        const entities = extractEntities(userMessage);
        const preview = mapEntitiesToFields(formCode, form.schema, agentView.fields, entities);
        return jsonText({ entities, preview, requiresUserConfirmation: true });
      } catch (e) {
        return toolError(e instanceof Error ? e.message : 'Prefill preview failed', (e as { status?: number }).status);
      }
    },
  );

  server.tool(
    'create_draft_submission',
    'Create a DRAFT submission for a form (optionally linked to a discovery session)',
    {
      formCode: z.string(),
      discoverySessionId: z.string().optional(),
    },
    async ({ formCode, discoverySessionId }) => {
      try {
        const draft = await client.createDraft(formCode, discoverySessionId);
        return jsonText(draft);
      } catch (e) {
        return toolError(e instanceof Error ? e.message : 'Failed to create draft', (e as { status?: number }).status);
      }
    },
  );

  server.tool(
    'save_section',
    'Save section data on a draft submission',
    {
      submissionId: z.string(),
      sectionKey: z.string(),
      data: z.record(z.unknown()),
      resumeSectionKey: z.string().optional(),
    },
    async ({ submissionId, sectionKey, data, resumeSectionKey }) => {
      try {
        await client.saveSection(submissionId, sectionKey, data, resumeSectionKey);
        return jsonText({ saved: true, submissionId, sectionKey });
      } catch (e) {
        return toolError(e instanceof Error ? e.message : 'Failed to save section', (e as { status?: number }).status);
      }
    },
  );

  server.tool(
    'fill_from_conversation',
    'Extract entities from user text, map to form fields, create a draft, and save all mapped sections. Returns preview for user confirmation before submit.',
    {
      formCode: z.string(),
      userMessage: z.string(),
      discoverySessionId: z.string().optional(),
    },
    async ({ formCode, userMessage, discoverySessionId }) => {
      try {
        const form = await client.getForm(formCode);
        const agentView = flattenFormSchema(form);
        const entities = extractEntities(userMessage);
        const preview = mapEntitiesToFields(formCode, form.schema, agentView.fields, entities);

        const draft = await client.createDraft(formCode, discoverySessionId);
        for (const [sectionKey, data] of Object.entries(preview.sectionData)) {
          await client.saveSection(draft.submissionId, sectionKey, data);
        }

        const updated = await client.getSubmission(draft.submissionId);
        return jsonText({
          submissionId: draft.submissionId,
          status: updated.status,
          entities,
          preview,
          sectionData: updated.sectionData,
          missingRequired: preview.missingRequired,
          requiresUserConfirmation: true,
          nextStep: 'Call submit_submission with confirmed=true after user approves the pre-filled data.',
        });
      } catch (e) {
        return toolError(e instanceof Error ? e.message : 'Fill from conversation failed', (e as { status?: number }).status);
      }
    },
  );

  server.tool(
    'get_submission',
    'Get submission status and section data for review',
    { submissionId: z.string() },
    async ({ submissionId }) => {
      try {
        const detail = await client.getSubmission(submissionId);
        return jsonText(detail);
      } catch (e) {
        return toolError(e instanceof Error ? e.message : 'Submission not found', (e as { status?: number }).status);
      }
    },
  );

  server.tool(
    'submit_submission',
    'Submit a draft after user confirmation. Requires confirmed=true to prevent accidental submission.',
    {
      submissionId: z.string(),
      confirmed: z.boolean().describe('Must be true — user has reviewed pre-filled data'),
      idempotencyKey: z.string().optional(),
    },
    async ({ submissionId, confirmed, idempotencyKey }) => {
      if (!confirmed) {
        return toolError('Submission blocked: user confirmation required (set confirmed=true)', 400);
      }
      try {
        const key = idempotencyKey ?? `mcp-${submissionId}-${Date.now()}`;
        const result = await client.submit(submissionId, key);
        return jsonText({ ...result, message: 'Submission accepted. Pipeline runs asynchronously.' });
      } catch (e) {
        return toolError(e instanceof Error ? e.message : 'Submit failed', (e as { status?: number }).status);
      }
    },
  );

  return server;
}
