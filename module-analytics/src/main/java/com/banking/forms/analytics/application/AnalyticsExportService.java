package com.banking.forms.analytics.application;

import com.banking.forms.formdefinition.application.FormQueryService;
import com.banking.forms.pipeline.domain.SanitizedPayload;
import com.banking.forms.pipeline.infrastructure.SanitizedPayloadRepository;
import com.banking.forms.submission.domain.Submission;
import com.banking.forms.submission.infrastructure.SubmissionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Exports PII-scrubbed submission payloads for analytics (US-9.4). Never reads raw section data. */
@Service
public class AnalyticsExportService {

    private static final TypeReference<Map<String, Map<String, Object>>> PAYLOAD_TYPE = new TypeReference<>() {};

    private final SubmissionRepository submissionRepository;
    private final SanitizedPayloadRepository sanitizedPayloadRepository;
    private final FormQueryService formQueryService;
    private final ObjectMapper objectMapper;

    public AnalyticsExportService(
            SubmissionRepository submissionRepository,
            SanitizedPayloadRepository sanitizedPayloadRepository,
            FormQueryService formQueryService,
            ObjectMapper objectMapper) {
        this.submissionRepository = submissionRepository;
        this.sanitizedPayloadRepository = sanitizedPayloadRepository;
        this.formQueryService = formQueryService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AnalyticsRecordView> listRecords(UUID tenantId) {
        List<AnalyticsRecordView> records = new ArrayList<>();
        for (Submission submission : submissionRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)) {
            sanitizedPayloadRepository.findBySubmissionId(submission.getId()).ifPresent(payload -> {
                String formCode = formQueryService
                        .findPublishedByVersionId(submission.getFormVersionId())
                        .map(form -> form.code())
                        .orElse("UNKNOWN");
                records.add(new AnalyticsRecordView(
                        submission.getId(),
                        formCode,
                        submission.getStatus().name(),
                        submission.getSubmittedAt(),
                        flattenPayload(payload)));
            });
        }
        return records;
    }

    @Transactional(readOnly = true)
    public byte[] exportJson(UUID tenantId) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(listRecords(tenantId));
        } catch (Exception ex) {
            throw new AnalyticsExportException("Failed to serialize analytics export", ex);
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(UUID tenantId) {
        List<AnalyticsRecordView> records = listRecords(tenantId);
        StringBuilder csv = new StringBuilder("submissionId,formCode,status,submittedAt,fieldPath,fieldValue\n");
        for (AnalyticsRecordView record : records) {
            for (Map.Entry<String, Object> field : record.sanitizedFields().entrySet()) {
                csv.append(escapeCsv(record.submissionId().toString()))
                        .append(',')
                        .append(escapeCsv(record.formCode()))
                        .append(',')
                        .append(escapeCsv(record.status()))
                        .append(',')
                        .append(escapeCsv(record.submittedAt() == null ? "" : record.submittedAt().toString()))
                        .append(',')
                        .append(escapeCsv(field.getKey()))
                        .append(',')
                        .append(escapeCsv(String.valueOf(field.getValue())))
                        .append('\n');
            }
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Map<String, Object> flattenPayload(SanitizedPayload payload) {
        try {
            Map<String, Map<String, Object>> sections =
                    objectMapper.readValue(payload.getPayloadJson(), PAYLOAD_TYPE);
            Map<String, Object> flat = new LinkedHashMap<>();
            flattenMap("", sections, flat);
            return flat;
        } catch (Exception ex) {
            throw new AnalyticsExportException("Failed to parse sanitized payload", ex);
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

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
