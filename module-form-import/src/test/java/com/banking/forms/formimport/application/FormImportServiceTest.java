package com.banking.forms.formimport.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.forms.formdefinition.application.FormCommandService;
import com.banking.forms.formdefinition.application.FormDetailView;
import com.banking.forms.formdefinition.application.FormVersionView;
import com.banking.forms.formdefinition.domain.StorageStrategy;
import com.banking.forms.formimport.domain.FormImportJob;
import com.banking.forms.formimport.infrastructure.FormImportJobRepository;
import com.banking.forms.formimport.spi.FormExtractor;
import com.banking.forms.formimport.spi.FormImportSource;
import com.banking.forms.formimport.spi.ProviderConfig;
import com.banking.forms.formimport.spi.SourceTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FormImportServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FormImportJobRepository jobRepository;
    private FormExtractorRouter router;
    private FormExtractor extractor;
    private FormCommandService formCommandService;
    private FormImportService service;

    @BeforeEach
    void setUp() {
        jobRepository = mock(FormImportJobRepository.class);
        router = mock(FormExtractorRouter.class);
        extractor = mock(FormExtractor.class);
        formCommandService = mock(FormCommandService.class);
        when(jobRepository.save(any(FormImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new FormImportService(
                jobRepository, router, new SchemaMapper(objectMapper), formCommandService, objectMapper);
    }

    private FormImportSource pdfSource() {
        return FormImportSource.ofFile(SourceTypes.PDF, new byte[] {1, 2, 3}, "loan.pdf", "application/pdf");
    }

    @Test
    void createImportRoutesToProviderAndAwaitsReview() {
        when(router.resolve(SourceTypes.PDF))
                .thenReturn(new FormExtractorRouter.ResolvedProvider("pdfbox", extractor, ProviderConfig.empty()));
        when(extractor.extract(any(), any()))
                .thenReturn(new ExtractedForm(
                        "Loan Application",
                        "ACROFORM",
                        List.of(new ExtractedField("First Name", FieldKind.TEXT, List.of(), true, 0.9, null))));

        FormImportJobView view = service.createImport(TENANT, ACTOR, pdfSource());

        assertThat(view.status()).isEqualTo("NEEDS_REVIEW");
        assertThat(view.sourceType()).isEqualTo("PDF");
        assertThat(view.providerCode()).isEqualTo("pdfbox");
        assertThat(view.source()).isEqualTo("ACROFORM");
        assertThat(view.suggestedName()).isEqualTo("Loan Application");
        assertThat(view.proposedSchema().get("sections")).hasSize(1);
    }

    @Test
    void createImportRecordsFailureWhenExtractionThrows() {
        when(router.resolve(SourceTypes.PDF))
                .thenReturn(new FormExtractorRouter.ResolvedProvider("pdfbox", extractor, ProviderConfig.empty()));
        when(extractor.extract(any(), any())).thenThrow(new FormImportException("corrupt pdf"));

        FormImportJobView view = service.createImport(TENANT, ACTOR, pdfSource());

        assertThat(view.status()).isEqualTo("FAILED");
        assertThat(view.error()).contains("corrupt pdf");
    }

    @Test
    void createImportRecordsFailureWhenNoProviderConfigured() {
        when(router.resolve(SourceTypes.PDF)).thenThrow(new FormImportException("No enabled import provider"));

        FormImportJobView view = service.createImport(TENANT, ACTOR, pdfSource());

        assertThat(view.status()).isEqualTo("FAILED");
        assertThat(view.error()).contains("No enabled import provider");
    }

    @Test
    void createImportRejectsEmptySource() {
        FormImportSource empty = FormImportSource.ofFile(SourceTypes.PDF, new byte[0], "x.pdf", "application/pdf");
        assertThatThrownBy(() -> service.createImport(TENANT, ACTOR, empty)).isInstanceOf(FormImportException.class);
    }

    @Test
    void acceptCreatesDraftAndWritesSchema() {
        UUID jobId = UUID.randomUUID();
        UUID formId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        FormImportJob job = reviewableJob(jobId);
        when(jobRepository.findByIdAndTenantId(jobId, TENANT)).thenReturn(Optional.of(job));
        when(formCommandService.createDefinition(TENANT, ACTOR, "LOAN", "Loan", "lending", StorageStrategy.JSON_BLOB))
                .thenReturn(new FormDetailView(
                        formId,
                        "LOAN",
                        "Loan",
                        "lending",
                        "JSON_BLOB",
                        List.of(new FormVersionView(versionId, 1, "DRAFT", null, objectMapper.createObjectNode()))));

        AcceptedFormView result =
                service.accept(TENANT, ACTOR, jobId, "LOAN", "Loan", "lending", StorageStrategy.JSON_BLOB, null);

        assertThat(result.formId()).isEqualTo(formId);
        assertThat(result.versionId()).isEqualTo(versionId);
        assertThat(job.getStatus().name()).isEqualTo("ACCEPTED");
        verify(formCommandService).updateDraftSchema(eq(TENANT), eq(formId), eq(versionId), any());
    }

    @Test
    void acceptRejectsJobNotAwaitingReview() {
        UUID jobId = UUID.randomUUID();
        FormImportJob pending = new FormImportJob(jobId, TENANT, ACTOR, "PDF", "f.pdf", "hash", 10);
        when(jobRepository.findByIdAndTenantId(jobId, TENANT)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() ->
                        service.accept(TENANT, ACTOR, jobId, "LOAN", "Loan", "lending", StorageStrategy.JSON_BLOB, null))
                .isInstanceOf(FormImportException.class);
    }

    @Test
    void getImportThrowsWhenMissing() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findByIdAndTenantId(jobId, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getImport(TENANT, jobId)).isInstanceOf(FormImportNotFoundException.class);
    }

    private FormImportJob reviewableJob(UUID jobId) {
        FormImportJob job = new FormImportJob(jobId, TENANT, ACTOR, "PDF", "loan.pdf", "hash", 100);
        job.completeReview(
                "ACROFORM",
                "Loan",
                "{\"sections\":[{\"key\":\"s1\",\"title\":\"S1\",\"fields\":[{\"key\":\"f1\",\"type\":\"text\",\"label\":\"F1\"}]}]}",
                "{\"overall\":0.9}");
        return job;
    }
}
