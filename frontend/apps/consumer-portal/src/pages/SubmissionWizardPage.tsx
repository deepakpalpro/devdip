import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { SectionRenderer, type FormSectionSchema } from '@banking-forms/form-renderer';
import { Button, ErrorState, LoadingState, PageHeader } from '@banking-forms/ui';
import {
  useConsumerForm,
  useCreateSubmission,
  useSaveSection,
  useSubmission,
  useSubmitApplication,
  validateSection,
} from '../hooks/useSubmission';
import { SubmissionReview } from '../components/SubmissionReview';
import './submission-wizard.css';

function storageKey(formCode: string) {
  return `bf:submission:${formCode}`;
}

export function SubmissionWizardPage() {
  const { formCode } = useParams<{ formCode: string }>();
  const [searchParams] = useSearchParams();
  const discoverySessionId = searchParams.get('discovery') ?? undefined;
  // Server-backed resume: an explicit submission id (e.g. from "My applications") takes priority
  // over the per-device localStorage pointer.
  const resumeId = searchParams.get('submission');
  const [submissionId, setSubmissionId] = useState<string | null>(() => {
    if (resumeId) return resumeId;
    // When arriving from the discovery wizard, always start a fresh (pre-filled) draft.
    return formCode && !discoverySessionId ? localStorage.getItem(storageKey(formCode)) : null;
  });
  const [currentIndex, setCurrentIndex] = useState(0);
  const [sectionValues, setSectionValues] = useState<Record<string, Record<string, unknown>>>({});
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [mode, setMode] = useState<'sections' | 'review' | 'done'>('sections');

  const createSubmission = useCreateSubmission(formCode ?? '', discoverySessionId);
  const { data: submission, isLoading, error, refetch } = useSubmission(submissionId);
  // Lazy-draft: when there is no submission yet, render sections straight from the published form
  // schema. A draft is only created on the first save/submit, so merely opening a form (and leaving)
  // never persists an orphan draft.
  const {
    data: form,
    isLoading: formLoading,
    error: formError,
  } = useConsumerForm(formCode, !submissionId);
  const saveSection = useSaveSection();
  const submitApplication = useSubmitApplication();

  // A discovery hand-off intentionally starts a pre-filled draft up-front so the applicant sees their
  // questionnaire answers; that draft is a deliberate action, not an accidental orphan.
  useEffect(() => {
    if (!formCode || submissionId || !discoverySessionId) {
      return;
    }
    if (createSubmission.isPending || createSubmission.isSuccess) {
      return;
    }
    createSubmission.mutate(undefined, {
      onSuccess: (created) => {
        localStorage.setItem(storageKey(formCode), created.submissionId);
        setSubmissionId(created.submissionId);
      },
    });
  }, [formCode, submissionId, discoverySessionId, createSubmission]);

  useEffect(() => {
    if (!submission) return;
    setSectionValues((prev) => ({ ...submission.sectionData, ...prev }));
  }, [submission]);

  // Ensures a draft exists, creating one lazily on first persistence. Returns the submission id.
  const ensureSubmissionId = async (): Promise<string> => {
    if (submissionId) return submissionId;
    const created = await createSubmission.mutateAsync();
    if (formCode) localStorage.setItem(storageKey(formCode), created.submissionId);
    setSubmissionId(created.submissionId);
    return created.submissionId;
  };

  // On resume, land on the section the applicant last worked on — but only once, so that the
  // refetches triggered by each auto-save don't keep yanking the user's position around.
  const initializedPosition = useRef(false);
  useEffect(() => {
    if (!submission || initializedPosition.current) return;
    initializedPosition.current = true;
    const resumeKey = submission.currentSectionKey;
    if (!resumeKey) return;
    const index = submission.schema.sections.findIndex((section) => section.key === resumeKey);
    if (index >= 0) setCurrentIndex(index);
  }, [submission]);

  // Schema comes from the loaded submission when resuming, otherwise from the published form.
  const schema = submission?.schema ?? form?.schema;
  const formName = submission?.formName ?? form?.name ?? formCode ?? '';
  const serverSectionData = submission?.sectionData ?? {};
  const sections = useMemo(() => schema?.sections ?? [], [schema]);
  const currentSection: FormSectionSchema | undefined = sections[currentIndex];

  if (!formCode) {
    return <ErrorState message="Missing form code." />;
  }

  if (createSubmission.isError || error || formError) {
    return (
      <ErrorState
        message={
          (createSubmission.error instanceof Error && createSubmission.error.message) ||
          (error instanceof Error && error.message) ||
          (formError instanceof Error && formError.message) ||
          'Unable to start application'
        }
      />
    );
  }

  if (isLoading || formLoading || !schema) {
    return <LoadingState message="Loading form…" />;
  }

  if (!currentSection) {
    return <ErrorState message="Form schema unavailable." />;
  }

  const values = sectionValues[currentSection.key] ?? {};

  const handleFieldChange = (fieldKey: string, value: unknown) => {
    setSectionValues((prev) => ({
      ...prev,
      [currentSection.key]: { ...prev[currentSection.key], [fieldKey]: value },
    }));
    setFieldErrors((prev) => {
      const next = { ...prev };
      delete next[fieldKey];
      return next;
    });
  };

  // Draft-friendly partial save: persist whatever the applicant has entered so far (no required-field
  // gate) and record the section they should resume on next time. Completeness is enforced on submit.
  const persistSection = async (resumeSectionKey: string) => {
    const id = await ensureSubmissionId();
    await saveSection.mutateAsync({ submissionId: id, sectionKey: currentSection.key, data: values, resumeSectionKey });
    if (submissionId) await refetch();
  };

  const goNext = async () => {
    const isLast = currentIndex >= sections.length - 1;
    const resumeKey = isLast ? currentSection.key : sections[currentIndex + 1].key;
    await persistSection(resumeKey);
    if (!isLast) {
      setCurrentIndex((index) => index + 1);
      setFieldErrors({});
      return;
    }
    setMode('review');
  };

  const goBack = async () => {
    if (currentIndex === 0) return;
    const resumeKey = sections[currentIndex - 1].key;
    await persistSection(resumeKey);
    setCurrentIndex((index) => index - 1);
    setFieldErrors({});
  };

  const handleSubmit = async () => {
    // Enforce completeness here: jump to the first section with missing required fields rather than
    // failing the submit opaquely on the server.
    for (let index = 0; index < sections.length; index += 1) {
      const section = sections[index];
      const errors = validateSection(section, sectionValues[section.key] ?? {});
      if (Object.keys(errors).length > 0) {
        setCurrentIndex(index);
        setFieldErrors(errors);
        setMode('sections');
        return;
      }
    }
    const id = await ensureSubmissionId();
    await submitApplication.mutateAsync({ submissionId: id });
    if (formCode) {
      localStorage.removeItem(storageKey(formCode));
    }
    setMode('done');
  };

  if (mode === 'done') {
    return (
      <>
        <PageHeader title="Application submitted" description="Your application has been received." />
        <div className="submission-success">
          <p style={{ margin: 0 }}>
            Reference: <strong>{submissionId}</strong>
          </p>
        </div>
        <div className="submission-actions" style={{ marginTop: '1rem' }}>
          <Link to="/">
            <Button variant="secondary">Back to catalog</Button>
          </Link>
          <Link to={`/applications/${submissionId}`}>
            <Button>View application status</Button>
          </Link>
        </div>
      </>
    );
  }

  return (
    <div className="submission-wizard">
      <PageHeader title={formName} description="Complete each section — progress is saved as you go." />

      {discoverySessionId ? (
        <div className="submission-prefill-note">
          We pre-filled some answers from your questionnaire — please review them as you go.
        </div>
      ) : null}

      <div className="submission-stepper">
        {sections.map((section, index) => (
          <span
            key={section.key}
            className={[
              'submission-step',
              index === currentIndex && mode === 'sections' ? 'submission-step-active' : '',
              index < currentIndex || serverSectionData[section.key] ? 'submission-step-done' : '',
            ]
              .filter(Boolean)
              .join(' ')}
          >
            {index + 1}. {section.title}
          </span>
        ))}
        <span className={`submission-step ${mode === 'review' ? 'submission-step-active' : ''}`}>Review</span>
      </div>

      {mode === 'review' ? (
        <div className="submission-panel submission-summary">
          <SubmissionReview sections={sections} sectionData={sectionValues} />
          <div className="submission-actions">
            <Button variant="secondary" onClick={() => setMode('sections')}>
              Back
            </Button>
            <Button onClick={handleSubmit} disabled={submitApplication.isPending || createSubmission.isPending}>
              {submitApplication.isPending || createSubmission.isPending ? 'Submitting…' : 'Submit application'}
            </Button>
          </div>
          {submitApplication.isError ? (
            <ErrorState
              message={
                submitApplication.error instanceof Error
                  ? submitApplication.error.message
                  : 'Submit failed'
              }
            />
          ) : null}
        </div>
      ) : (
        <div className="submission-panel">
          <SectionRenderer
            section={currentSection}
            values={values}
            onChange={handleFieldChange}
            errors={fieldErrors}
          />
          <div className="submission-actions" style={{ marginTop: '1.5rem' }}>
            <div>
              {currentIndex > 0 ? (
                <Button variant="secondary" onClick={goBack} disabled={saveSection.isPending || createSubmission.isPending}>
                  Back
                </Button>
              ) : (
                <Link to="/">
                  <Button variant="secondary">Cancel</Button>
                </Link>
              )}
            </div>
            <Button onClick={goNext} disabled={saveSection.isPending || createSubmission.isPending}>
              {saveSection.isPending || createSubmission.isPending
                ? 'Saving…'
                : currentIndex < sections.length - 1
                  ? 'Save & continue'
                  : 'Review'}
            </Button>
          </div>
          {saveSection.isError ? (
            <ErrorState
              message={saveSection.error instanceof Error ? saveSection.error.message : 'Save failed'}
            />
          ) : null}
        </div>
      )}
    </div>
  );
}
