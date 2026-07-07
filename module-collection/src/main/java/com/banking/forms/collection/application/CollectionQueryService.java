package com.banking.forms.collection.application;

import com.banking.forms.formdefinition.application.FormQueryService;
import com.banking.forms.formdefinition.domain.FormVersion;
import com.banking.forms.formdefinition.infrastructure.FormDefinitionRepository;
import com.banking.forms.formdefinition.infrastructure.FormVersionRepository;
import com.banking.forms.pipeline.domain.SanitizedPayload;
import com.banking.forms.pipeline.infrastructure.SanitizedPayloadRepository;
import com.banking.forms.submission.domain.Submission;
import com.banking.forms.submission.domain.SubmissionStatus;
import com.banking.forms.submission.infrastructure.SubmissionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * External collection query over PII-scrubbed submission payloads only. Never reads raw section
 * storage.
 */
@Service
public class CollectionQueryService {

    private static final TypeReference<Map<String, Map<String, Object>>> PAYLOAD_TYPE = new TypeReference<>() {};

    private final SubmissionRepository submissionRepository;
    private final SanitizedPayloadRepository sanitizedPayloadRepository;
    private final FormQueryService formQueryService;
    private final FormDefinitionRepository formDefinitionRepository;
    private final FormVersionRepository formVersionRepository;
    private final ObjectMapper objectMapper;

    public CollectionQueryService(
            SubmissionRepository submissionRepository,
            SanitizedPayloadRepository sanitizedPayloadRepository,
            FormQueryService formQueryService,
            FormDefinitionRepository formDefinitionRepository,
            FormVersionRepository formVersionRepository,
            ObjectMapper objectMapper) {
        this.submissionRepository = submissionRepository;
        this.sanitizedPayloadRepository = sanitizedPayloadRepository;
        this.formQueryService = formQueryService;
        this.formDefinitionRepository = formDefinitionRepository;
        this.formVersionRepository = formVersionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public CollectionQueryResult query(UUID tenantId, CollectionQueryRequest request) {
        PageRequest pageable =
                PageRequest.of(request.safePage(), request.safeSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Submission> page = loadPage(tenantId, request, pageable);
        List<CollectionRecordView> items = new ArrayList<>();
        for (Submission submission : page.getContent()) {
            if (!matchesSubmittedWindow(submission, request.submittedAfter(), request.submittedBefore())) {
                continue;
            }
            toRecord(submission, request.fieldProjection()).ifPresent(items::add);
        }
        return new CollectionQueryResult(page.getNumber(), page.getSize(), page.getTotalElements(), items);
    }

    private Page<Submission> loadPage(UUID tenantId, CollectionQueryRequest request, PageRequest pageable) {
        List<UUID> formVersionIds = resolveFormVersionIds(tenantId, request.formCode());
        if (formVersionIds != null && formVersionIds.isEmpty()) {
            return Page.empty(pageable);
        }
        if (request.status() != null && !request.status().isBlank()) {
            SubmissionStatus status = SubmissionStatus.valueOf(request.status());
            if (formVersionIds != null) {
                return submissionRepository.findByTenantIdAndFormVersionIdInAndStatus(
                        tenantId, formVersionIds, status, pageable);
            }
            return submissionRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        }
        if (formVersionIds != null) {
            return submissionRepository.findByTenantIdAndFormVersionIdIn(tenantId, formVersionIds, pageable);
        }
        return submissionRepository.findByTenantId(tenantId, pageable);
    }

    private List<UUID> resolveFormVersionIds(UUID tenantId, String formCode) {
        if (formCode == null || formCode.isBlank()) {
            return null;
        }
        return formDefinitionRepository
                .findByTenantIdAndCode(tenantId, formCode)
                .map(form -> formVersionRepository.findByFormDefinitionIdOrderByVersionNumberDesc(form.getId()).stream()
                        .map(FormVersion::getId)
                        .toList())
                .orElse(List.of());
    }

    private java.util.Optional<CollectionRecordView> toRecord(Submission submission, Set<String> projection) {
        return sanitizedPayloadRepository.findBySubmissionId(submission.getId()).flatMap(payload -> {
            String formCode = formQueryService
                    .findPublishedByVersionId(submission.getFormVersionId())
                    .map(form -> form.code())
                    .orElse("UNKNOWN");
            Map<String, Object> fields = projectFields(flattenPayload(payload), projection);
            return java.util.Optional.of(new CollectionRecordView(
                    submission.getId(),
                    formCode,
                    submission.getStatus().name(),
                    submission.getSubmittedAt(),
                    fields));
        });
    }

    private Map<String, Object> projectFields(Map<String, Object> allFields, Set<String> projection) {
        if (projection == null || projection.isEmpty()) {
            return allFields;
        }
        Map<String, Object> projected = new LinkedHashMap<>();
        for (String field : projection) {
            if (allFields.containsKey(field)) {
                projected.put(field, allFields.get(field));
            }
        }
        return projected;
    }

    private Map<String, Object> flattenPayload(SanitizedPayload payload) {
        try {
            Map<String, Map<String, Object>> sections =
                    objectMapper.readValue(payload.getPayloadJson(), PAYLOAD_TYPE);
            Map<String, Object> flat = new LinkedHashMap<>();
            flattenMap("", sections, flat);
            return flat;
        } catch (Exception ex) {
            throw new CollectionException("Failed to parse sanitized payload");
        }
    }

    @SuppressWarnings("unchecked")
    private void flattenMap(String prefix, Map<String, ?> source, Map<String, Object> target) {
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                flattenMap(key, (Map<String, ?>) nested, target);
            } else {
                target.put(key, value);
            }
        }
    }

    private static boolean matchesSubmittedWindow(Submission submission, Instant after, Instant before) {
        Instant submittedAt = submission.getSubmittedAt();
        if (submittedAt == null) {
            return after == null && before == null;
        }
        if (after != null && submittedAt.isBefore(after)) {
            return false;
        }
        if (before != null && submittedAt.isAfter(before)) {
            return false;
        }
        return true;
    }
}
