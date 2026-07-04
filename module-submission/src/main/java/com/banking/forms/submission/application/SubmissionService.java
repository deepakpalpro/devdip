package com.banking.forms.submission.application;

import com.banking.forms.formdefinition.application.FormQueryService;
import com.banking.forms.formdefinition.application.PublishedFormView;
import com.banking.forms.submission.domain.Submission;
import com.banking.forms.submission.domain.SubmissionEvent;
import com.banking.forms.submission.domain.SubmissionStatus;
import com.banking.forms.submission.infrastructure.SubmissionEventRepository;
import com.banking.forms.submission.infrastructure.SubmissionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final FormQueryService formQueryService;
    private final SectionValidator sectionValidator;
    private final SectionStorageRouter sectionStorageRouter;
    private final SubmissionEventRecorder eventRecorder;
    private final SubmissionEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public SubmissionService(
            SubmissionRepository submissionRepository,
            FormQueryService formQueryService,
            SectionValidator sectionValidator,
            SectionStorageRouter sectionStorageRouter,
            SubmissionEventRecorder eventRecorder,
            SubmissionEventRepository eventRepository,
            ObjectMapper objectMapper) {
        this.submissionRepository = submissionRepository;
        this.formQueryService = formQueryService;
        this.sectionValidator = sectionValidator;
        this.sectionStorageRouter = sectionStorageRouter;
        this.eventRecorder = eventRecorder;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    public Submission createDraft(UUID tenantId, UUID userId, String formCode) {
        return createDraft(tenantId, userId, formCode, null);
    }

    /**
     * Creates a draft and optionally seeds it with pre-population data
     * ({@code sectionKey -> fieldKey -> value}). Pre-filled sections are persisted through the form's
     * storage strategy without required-field validation, since a seeded draft is intentionally
     * partial; validation is enforced on final submit.
     */
    public Submission createDraft(
            UUID tenantId, UUID userId, String formCode, Map<String, Map<String, Object>> prefill) {
        var form = formQueryService
                .findPublishedByCode(tenantId, formCode)
                .orElseThrow(() -> new SubmissionValidationException("Form not found: " + formCode));

        var submission = new Submission(UUID.randomUUID(), tenantId, form.formVersionId(), userId);
        submissionRepository.save(submission);

        if (prefill != null && !prefill.isEmpty()) {
            var storage = sectionStorageRouter.resolve(form.storageStrategy());
            for (var entry : prefill.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                storage.saveSection(submission.getId(), entry.getKey(), sectionValidator.toMap(entry.getValue()));
            }
        }
        return submission;
    }

    @Transactional(readOnly = true)
    public List<SubmissionSummaryView> listSubmissions(UUID tenantId) {
        return submissionRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toSummaryView)
                .toList();
    }

    /** Consumer-facing list scoped to a single applicant ("my applications"). */
    @Transactional(readOnly = true)
    public List<SubmissionSummaryView> listSubmissions(UUID tenantId, UUID userId) {
        return submissionRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId).stream()
                .map(this::toSummaryView)
                .toList();
    }

    private SubmissionSummaryView toSummaryView(Submission submission) {
        var form = formQueryService.findPublishedByVersionId(submission.getFormVersionId());
        return new SubmissionSummaryView(
                submission.getId(),
                form.map(PublishedFormView::code).orElse("UNKNOWN"),
                form.map(PublishedFormView::name).orElse("Unknown form"),
                submission.getStatus().name(),
                submission.getCreatedAt(),
                submission.getSubmittedAt());
    }

    @Transactional(readOnly = true)
    public SubmissionDetailView getSubmission(UUID tenantId, UUID submissionId) {
        var submission = requireSubmission(tenantId, submissionId);
        var form = requireForm(submission.getFormVersionId());

        return new SubmissionDetailView(
                submission.getId(),
                submission.getFormVersionId(),
                form.code(),
                form.name(),
                submission.getStatus().name(),
                submission.getCurrentSectionKey(),
                loadSectionData(submissionId, form));
    }

    public void saveSection(UUID tenantId, UUID submissionId, String sectionKey, Map<String, Object> data) {
        saveSection(tenantId, submissionId, sectionKey, data, null);
    }

    /**
     * Persists a single section of a draft. This is a <em>partial</em> (auto-)save: required-field
     * validation is intentionally NOT enforced here so multi-section forms can be filled and left
     * incomplete across sessions — completeness is enforced only on {@link #submit}. The
     * {@code resumeSectionKey} (defaulting to the saved section) records where the applicant should
     * resume next time.
     */
    public void saveSection(
            UUID tenantId, UUID submissionId, String sectionKey, Map<String, Object> data, String resumeSectionKey) {
        var submission = requireDraftSubmission(tenantId, submissionId);
        var form = requireForm(submission.getFormVersionId());

        if (!sectionValidator.sectionExists(form.schema(), sectionKey)) {
            throw new SubmissionValidationException("Unknown section: " + sectionKey);
        }

        var normalized = sectionValidator.toMap(data);
        sectionStorageRouter.resolve(form.storageStrategy()).saveSection(submissionId, sectionKey, normalized);

        String resume = (resumeSectionKey != null && !resumeSectionKey.isBlank()) ? resumeSectionKey : sectionKey;
        submission.updateCurrentSection(resume, Instant.now());
        submissionRepository.save(submission);
    }

    public Submission submit(UUID tenantId, UUID submissionId, String idempotencyKey) {
        var submission = requireDraftSubmission(tenantId, submissionId);
        var form = requireForm(submission.getFormVersionId());

        sectionValidator.validateAllSections(form.schema(), loadSectionData(submissionId, form));
        submission.assignIdempotencyKey(idempotencyKey);
        submission.markSubmitted(Instant.now());
        Submission saved = submissionRepository.save(submission);
        eventRecorder.record(
                saved.getId(),
                "SUBMITTED",
                Map.of("from", SubmissionStatus.DRAFT.name(), "to", SubmissionStatus.SUBMITTED.name()),
                saved.getUserId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SubmissionEventView> getTimeline(UUID tenantId, UUID submissionId) {
        requireSubmission(tenantId, submissionId);
        return eventRepository.findBySubmissionIdOrderByIdAsc(submissionId).stream()
                .map(this::toEventView)
                .toList();
    }

    private SubmissionEventView toEventView(SubmissionEvent event) {
        Map<String, Object> payload = readPayload(event.getPayloadJson());
        return new SubmissionEventView(
                event.getEventType(),
                asString(payload.get("note")),
                asString(payload.get("from")),
                asString(payload.get("to")),
                event.getActorId(),
                event.getCreatedAt());
    }

    private Map<String, Object> readPayload(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Submission requireSubmission(UUID tenantId, UUID submissionId) {
        return submissionRepository
                .findById(submissionId)
                .filter(submission -> submission.getTenantId().equals(tenantId))
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));
    }

    private Submission requireDraftSubmission(UUID tenantId, UUID submissionId) {
        var submission = requireSubmission(tenantId, submissionId);
        if (submission.getStatus() != SubmissionStatus.DRAFT) {
            throw new SubmissionValidationException("Submission is not editable");
        }
        return submission;
    }

    private PublishedFormView requireForm(UUID formVersionId) {
        return formQueryService
                .findPublishedByVersionId(formVersionId)
                .orElseThrow(() -> new SubmissionValidationException("Form version not available"));
    }

    private Map<String, Map<String, Object>> loadSectionData(UUID submissionId, PublishedFormView form) {
        return sectionStorageRouter.resolve(form.storageStrategy()).loadAllSections(submissionId);
    }
}
