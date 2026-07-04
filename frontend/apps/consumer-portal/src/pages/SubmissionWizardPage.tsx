import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { SectionRenderer, type FormSectionSchema } from '@banking-forms/form-renderer';
import { Button, ErrorState, LoadingState, PageHeader } from '@banking-forms/ui';
import {
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
  const saveSection = useSaveSection(submissionId ?? '');
  const submitApplication = useSubmitApplication(submissionId ?? '');

  useEffect(() => {
    if (!formCode || submissionId || createSubmission.isPending || createSubmission.isSuccess) {
      return;
    }
    createSubmission.mutate(undefined, {
      onSuccess: (created) => {
        localStorage.setItem(storageKey(formCode), created.submissionId);
        setSubmissionId(created.submissionId);
      },
    });
  }, [formCode, submissionId, createSubmission]);

  useEffect(() => {
    if (!submission) return;
    setSectionValues((prev) => ({ ...submission.sectionData, ...prev }));
  }, [submission]);

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

  const sections = useMemo(() => submission?.schema.sections ?? [], [submission]);
  const currentSection: FormSectionSchema | undefined = sections[currentIndex];

  if (!formCode) {
    return <ErrorState message="Missing form code." />;
  }

  if (createSubmission.isPending || isLoading || !submissionId) {
    return <LoadingState message="Preparing your application…" />;
  }

  if (createSubmission.isError || error) {
    return (
      <ErrorState
        message={
          (createSubmission.error instanceof Error && createSubmission.error.message) ||
          (error instanceof Error && error.message) ||
          'Unable to start application'
        }
      />
    );
  }

  if (!submission || !currentSection) {
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
    await saveSection.mutateAsync({ sectionKey: currentSection.key, data: values, resumeSectionKey });
    await refetch();
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
    await submitApplication.mutateAsync();
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
      <PageHeader title={submission.formName} description="Complete each section — progress is saved as you go." />

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
              index < currentIndex || submission.sectionData[section.key] ? 'submission-step-done' : '',
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
            <Button onClick={handleSubmit} disabled={submitApplication.isPending}>
              {submitApplication.isPending ? 'Submitting…' : 'Submit application'}
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
                <Button variant="secondary" onClick={goBack} disabled={saveSection.isPending}>
                  Back
                </Button>
              ) : (
                <Link to="/">
                  <Button variant="secondary">Cancel</Button>
                </Link>
              )}
            </div>
            <Button onClick={goNext} disabled={saveSection.isPending}>
              {saveSection.isPending ? 'Saving…' : currentIndex < sections.length - 1 ? 'Save & continue' : 'Review'}
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
