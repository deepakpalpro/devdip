package com.banking.forms.formimport.application;

import com.banking.forms.formdefinition.application.FormCommandService;
import com.banking.forms.formdefinition.application.FormDetailView;
import com.banking.forms.formdefinition.domain.StorageStrategy;
import com.banking.forms.formimport.domain.FormImportJob;
import com.banking.forms.formimport.domain.FormImportStatus;
import com.banking.forms.formimport.infrastructure.FormImportJobRepository;
import com.banking.forms.formimport.spi.FormImportSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates import-to-form for <em>any</em> source (PDF, CSV, spreadsheet, HTML/URL, image):
 * resolve the configured provider for the source type, extract a proposal, hold it for human review,
 * and — on accept — materialize it as a DRAFT form via {@link FormCommandService}. Never publishes;
 * a human always reviews (and may edit) the generated schema.
 *
 * <p>Uploaded bytes are never persisted (only a hash + metadata). Extraction runs synchronously for
 * in-JVM providers; the job lifecycle is preserved so slower external providers can move off-thread.
 */
@Service
@Transactional
public class FormImportService {

    private final FormImportJobRepository jobRepository;
    private final FormExtractorRouter extractorRouter;
    private final SchemaMapper schemaMapper;
    private final FormCommandService formCommandService;
    private final ObjectMapper objectMapper;

    public FormImportService(
            FormImportJobRepository jobRepository,
            FormExtractorRouter extractorRouter,
            SchemaMapper schemaMapper,
            FormCommandService formCommandService,
            ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.extractorRouter = extractorRouter;
        this.schemaMapper = schemaMapper;
        this.formCommandService = formCommandService;
        this.objectMapper = objectMapper;
    }

    /** Ingests a source, routes to the configured provider, and records a job awaiting review. */
    public FormImportJobView createImport(UUID tenantId, UUID actorId, FormImportSource source) {
        if (!source.hasContent() && (source.url() == null || source.url().isBlank())) {
            throw new FormImportException("No file content or URL provided");
        }

        String reference = source.fileName() != null ? safeFileName(source.fileName()) : source.url();
        FormImportJob job = new FormImportJob(
                UUID.randomUUID(),
                tenantId,
                actorId,
                source.sourceType(),
                reference,
                source.hasContent() ? sha256(source.content()) : null,
                source.hasContent() ? source.content().length : 0L);
        jobRepository.save(job);

        try {
            FormExtractorRouter.ResolvedProvider resolved = extractorRouter.resolve(source.sourceType());
            job.markExtracting(resolved.code());
            jobRepository.save(job);

            ExtractedForm extracted = resolved.extractor().extract(source, resolved.config());
            MappedSchema mapped = schemaMapper.map(extracted);
            job.completeReview(
                    extracted.source(),
                    deriveName(extracted.suggestedName(), reference),
                    objectMapper.writeValueAsString(mapped.schema()),
                    objectMapper.writeValueAsString(mapped.confidence()));
        } catch (Exception ex) {
            job.fail(ex.getMessage());
        }
        jobRepository.save(job);
        return toView(job);
    }

    @Transactional(readOnly = true)
    public FormImportJobView getImport(UUID tenantId, UUID jobId) {
        return toView(requireJob(tenantId, jobId));
    }

    @Transactional(readOnly = true)
    public List<FormImportJobView> listImports(UUID tenantId) {
        return jobRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toView)
                .toList();
    }

    /**
     * Accepts a reviewed proposal: creates a DRAFT form definition and writes the (optionally
     * human-edited) schema onto its first version. Returns the form/version to open in the builder.
     */
    public AcceptedFormView accept(
            UUID tenantId,
            UUID actorId,
            UUID jobId,
            String code,
            String name,
            String category,
            StorageStrategy storageStrategy,
            JsonNode schemaOverride) {
        FormImportJob job = requireJob(tenantId, jobId);
        if (job.getStatus() != FormImportStatus.NEEDS_REVIEW) {
            throw new FormImportException("Import job is not awaiting review (status " + job.getStatus() + ")");
        }

        JsonNode schema = schemaOverride != null ? schemaOverride : readTree(job.getProposedSchema());
        if (schema == null) {
            throw new FormImportException("No schema available to accept");
        }

        FormDetailView created =
                formCommandService.createDefinition(tenantId, actorId, code, name, category, storageStrategy);
        UUID formId = created.id();
        UUID versionId = created.versions().get(0).id();
        formCommandService.updateDraftSchema(tenantId, formId, versionId, schema);

        job.accept(formId);
        jobRepository.save(job);
        return new AcceptedFormView(jobId, formId, versionId);
    }

    private FormImportJob requireJob(UUID tenantId, UUID jobId) {
        return jobRepository
                .findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new FormImportNotFoundException(jobId));
    }

    private FormImportJobView toView(FormImportJob job) {
        return new FormImportJobView(
                job.getId(),
                job.getStatus().name(),
                job.getSourceType(),
                job.getProviderCode(),
                job.getFileName(),
                job.getSource(),
                job.getSuggestedName(),
                readTree(job.getProposedSchema()),
                readTree(job.getConfidenceJson()),
                job.getErrorDetails(),
                job.getFormId(),
                job.getCreatedAt());
    }

    private JsonNode readTree(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return null;
        }
    }

    private String deriveName(String extractedName, String reference) {
        if (extractedName != null && !extractedName.isBlank()) {
            return extractedName.trim();
        }
        if (reference == null || reference.isBlank()) {
            return "Imported form";
        }
        int dot = reference.lastIndexOf('.');
        String withoutExt = dot > 0 ? reference.substring(0, dot) : reference;
        String cleaned = withoutExt.replaceAll("[._\\-]+", " ").trim();
        return cleaned.isBlank() ? "Imported form" : cleaned;
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String trimmed = fileName.trim();
        return trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception ex) {
            return null;
        }
    }
}
