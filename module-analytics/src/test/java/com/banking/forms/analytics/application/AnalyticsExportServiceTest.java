package com.banking.forms.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.banking.forms.formdefinition.application.FormQueryService;
import com.banking.forms.formdefinition.application.PublishedFormView;
import com.banking.forms.pipeline.domain.SanitizedPayload;
import com.banking.forms.pipeline.infrastructure.SanitizedPayloadRepository;
import com.banking.forms.submission.domain.Submission;
import com.banking.forms.submission.infrastructure.SubmissionRepository;
import com.banking.forms.formdefinition.domain.StorageStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsExportServiceTest {

    private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SUBMISSION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID FORM_VERSION = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private SanitizedPayloadRepository sanitizedPayloadRepository;

    @Mock
    private FormQueryService formQueryService;

    private AnalyticsExportService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsExportService(
                submissionRepository, sanitizedPayloadRepository, formQueryService, new ObjectMapper());
    }

    @Test
    void listRecords_flattensSanitizedPayload() {
        Instant at = Instant.parse("2026-01-01T00:00:00Z");
        Submission submission = new Submission(SUBMISSION, TENANT, FORM_VERSION, UUID.randomUUID());
        submission.markSubmitted(at);
        submission.markValidating(at);
        submission.markProcessing(at);
        submission.markUnderReview(at);

        when(submissionRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(List.of(submission));
        when(sanitizedPayloadRepository.findBySubmissionId(SUBMISSION))
                .thenReturn(Optional.of(new SanitizedPayload(
                        UUID.randomUUID(),
                        SUBMISSION,
                        "{\"personal-info\":{\"firstName\":\"J***\",\"lastName\":\"D***\"}}",
                        "{}")));
        when(formQueryService.findPublishedByVersionId(FORM_VERSION))
                .thenReturn(Optional.of(new PublishedFormView(
                        UUID.randomUUID(),
                        FORM_VERSION,
                        "LOAN_APPLICATION",
                        "Loan",
                        "Lending",
                        StorageStrategy.JSON_BLOB,
                        null)));

        List<AnalyticsRecordView> records = service.listRecords(TENANT);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).formCode()).isEqualTo("LOAN_APPLICATION");
        assertThat(records.get(0).sanitizedFields()).containsEntry("personal-info.firstName", "J***");
    }

    @Test
    void exportCsv_includesHeaderAndRows() {
        Instant at = Instant.parse("2026-01-01T00:00:00Z");
        Submission submission = new Submission(SUBMISSION, TENANT, FORM_VERSION, UUID.randomUUID());
        submission.markSubmitted(at);
        submission.markValidating(at);
        submission.markProcessing(at);
        submission.markUnderReview(at);

        when(submissionRepository.findByTenantIdOrderByCreatedAtDesc(TENANT)).thenReturn(List.of(submission));
        when(sanitizedPayloadRepository.findBySubmissionId(SUBMISSION))
                .thenReturn(Optional.of(new SanitizedPayload(
                        UUID.randomUUID(),
                        SUBMISSION,
                        "{\"loan-details\":{\"amount\":25000}}",
                        "{}")));
        when(formQueryService.findPublishedByVersionId(FORM_VERSION))
                .thenReturn(Optional.of(new PublishedFormView(
                        UUID.randomUUID(),
                        FORM_VERSION,
                        "LOAN_APPLICATION",
                        "Loan",
                        "Lending",
                        StorageStrategy.JSON_BLOB,
                        null)));

        String csv = new String(service.exportCsv(TENANT));

        assertThat(csv).startsWith("submissionId,formCode,status,submittedAt,fieldPath,fieldValue");
        assertThat(csv).contains("loan-details.amount,25000");
    }
}
