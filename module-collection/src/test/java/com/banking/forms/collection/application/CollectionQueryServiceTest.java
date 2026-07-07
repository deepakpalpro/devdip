package com.banking.forms.collection.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.banking.forms.formdefinition.application.FormQueryService;
import com.banking.forms.formdefinition.application.PublishedFormView;
import com.banking.forms.formdefinition.domain.StorageStrategy;
import com.banking.forms.formdefinition.infrastructure.FormDefinitionRepository;
import com.banking.forms.formdefinition.infrastructure.FormVersionRepository;
import com.banking.forms.pipeline.domain.SanitizedPayload;
import com.banking.forms.pipeline.infrastructure.SanitizedPayloadRepository;
import com.banking.forms.submission.domain.Submission;
import com.banking.forms.submission.infrastructure.SubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CollectionQueryServiceTest {

    private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SUBMISSION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID FORM_VERSION = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private SanitizedPayloadRepository sanitizedPayloadRepository;

    @Mock
    private FormQueryService formQueryService;

    @Mock
    private FormDefinitionRepository formDefinitionRepository;

    @Mock
    private FormVersionRepository formVersionRepository;

    private CollectionQueryService service;

    @BeforeEach
    void setUp() {
        service = new CollectionQueryService(
                submissionRepository,
                sanitizedPayloadRepository,
                formQueryService,
                formDefinitionRepository,
                formVersionRepository,
                new ObjectMapper());
    }

    @Test
    void query_projectsRequestedFields() {
        Instant at = Instant.parse("2026-01-01T00:00:00Z");
        Submission submission = new Submission(SUBMISSION, TENANT, FORM_VERSION, UUID.randomUUID());
        submission.markSubmitted(at);
        submission.markValidating(at);
        submission.markProcessing(at);
        submission.markUnderReview(at);

        when(submissionRepository.findByTenantId(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(submission)));
        when(sanitizedPayloadRepository.findBySubmissionId(SUBMISSION))
                .thenReturn(Optional.of(new SanitizedPayload(
                        UUID.randomUUID(),
                        SUBMISSION,
                        "{\"loan-details\":{\"amount\":25000,\"term\":12}}",
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

        CollectionQueryResult result = service.query(
                TENANT,
                new CollectionQueryRequest(null, null, null, null, 0, 20, Set.of("loan-details.amount")));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).fields()).containsOnlyKeys("loan-details.amount");
        assertThat(result.items().get(0).fields()).containsEntry("loan-details.amount", 25000);
    }
}
