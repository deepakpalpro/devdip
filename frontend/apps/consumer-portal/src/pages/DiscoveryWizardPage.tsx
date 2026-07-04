import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { DEFAULT_QUESTIONNAIRE_CODE, type DiscoveryEvaluation } from '@banking-forms/api-client';
import { SectionRenderer } from '@banking-forms/form-renderer';
import { Button, ErrorState, LoadingState, PageHeader } from '@banking-forms/ui';
import { validateSection } from '../hooks/useSubmission';
import { useEvaluateDiscovery, useQuestionnaire } from '../hooks/useDiscovery';
import './discovery-wizard.css';

export function DiscoveryWizardPage() {
  const { code } = useParams<{ code: string }>();
  const questionnaireCode = code ?? DEFAULT_QUESTIONNAIRE_CODE;
  const navigate = useNavigate();

  const { data: questionnaire, isLoading, error } = useQuestionnaire(questionnaireCode);
  const evaluate = useEvaluateDiscovery(questionnaireCode);

  const [answers, setAnswers] = useState<Record<string, unknown>>({});
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [result, setResult] = useState<DiscoveryEvaluation | null>(null);

  if (isLoading) {
    return <LoadingState message="Loading questionnaire…" />;
  }
  if (error || !questionnaire) {
    return <ErrorState message={error instanceof Error ? error.message : 'Questionnaire unavailable'} />;
  }

  const sections = questionnaire.schema.sections ?? [];

  const handleChange = (fieldKey: string, value: unknown) => {
    setAnswers((prev) => ({ ...prev, [fieldKey]: value }));
    setFieldErrors((prev) => {
      const next = { ...prev };
      delete next[fieldKey];
      return next;
    });
  };

  const handleEvaluate = async () => {
    const errors = sections.reduce<Record<string, string>>(
      (acc, section) => ({ ...acc, ...validateSection(section, answers) }),
      {},
    );
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }
    const evaluation = await evaluate.mutateAsync(answers);
    setResult(evaluation);
  };

  if (result) {
    return (
      <div className="discovery-wizard">
        <PageHeader
          title="Here is what we recommend"
          description="Based on your answers, these applications fit best. Your details carry over automatically."
        />
        {result.recommendations.length === 0 ? (
          <div className="submission-panel">
            <p style={{ margin: 0 }}>
              We could not find a strong match. You can still{' '}
              <Link to="/">browse all applications</Link>.
            </p>
          </div>
        ) : (
          <div className="discovery-recommendations">
            {result.recommendations.map((recommendation) => (
              <article
                key={recommendation.formCode}
                className={`discovery-rec${recommendation.recommended ? ' discovery-rec-top' : ''}`}
              >
                <div className="discovery-rec-head">
                  <div>
                    <h3 className="discovery-rec-title">{recommendation.formName}</h3>
                    <span className="discovery-rec-meta">{recommendation.category ?? 'General'}</span>
                  </div>
                  {recommendation.recommended ? (
                    <span className="discovery-rec-badge">Recommended</span>
                  ) : null}
                </div>
                {recommendation.reasons.length > 0 ? (
                  <ul className="discovery-rec-reasons">
                    {recommendation.reasons.map((reason) => (
                      <li key={reason}>{reason}</li>
                    ))}
                  </ul>
                ) : null}
                <div className="discovery-rec-action">
                  <Link to={`/apply/${recommendation.formCode}?discovery=${result.sessionId}`}>
                    <Button variant={recommendation.recommended ? 'primary' : 'secondary'}>
                      Start application
                    </Button>
                  </Link>
                </div>
              </article>
            ))}
          </div>
        )}
        <div className="submission-actions" style={{ justifyContent: 'flex-start' }}>
          <Button variant="secondary" onClick={() => setResult(null)}>
            Change my answers
          </Button>
          <Button variant="secondary" onClick={() => navigate('/')}>
            Back to catalog
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="discovery-wizard">
      <PageHeader
        title={questionnaire.name}
        description="Answer a few quick questions and we will point you to the right application."
      />
      <div className="submission-panel" style={{ display: 'grid', gap: '1.5rem' }}>
        {sections.map((section) => (
          <SectionRenderer
            key={section.key}
            section={section}
            values={answers}
            onChange={handleChange}
            errors={fieldErrors}
          />
        ))}
        <div className="submission-actions">
          <Link to="/">
            <Button variant="secondary">Cancel</Button>
          </Link>
          <Button onClick={handleEvaluate} disabled={evaluate.isPending}>
            {evaluate.isPending ? 'Evaluating…' : 'See recommendations'}
          </Button>
        </div>
        {evaluate.isError ? (
          <ErrorState
            message={evaluate.error instanceof Error ? evaluate.error.message : 'Evaluation failed'}
          />
        ) : null}
      </div>
    </div>
  );
}
