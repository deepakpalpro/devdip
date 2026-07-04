package com.banking.forms.bff.admin.api;

import com.banking.forms.formdefinition.domain.StorageStrategy;
import com.banking.forms.formimport.application.AcceptedFormView;
import com.banking.forms.formimport.application.FormImportException;
import com.banking.forms.formimport.application.FormImportJobView;
import com.banking.forms.formimport.application.FormImportService;
import com.banking.forms.formimport.application.SourceTypeDetector;
import com.banking.forms.formimport.spi.FormImportSource;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin import-to-form: upload a file (PDF/CSV/XLS(X)/image) or point at a form URL, review the
 * extracted proposal, then accept it to create a DRAFT form. The source type is detected and routed
 * to the configured provider; publishing still goes through the normal form-authoring flow.
 */
@RestController
@RequestMapping("/api/admin/v1/form-imports")
public class AdminFormImportController {

    private final FormImportService formImportService;
    private final SourceTypeDetector sourceTypeDetector;

    public AdminFormImportController(FormImportService formImportService, SourceTypeDetector sourceTypeDetector) {
        this.formImportService = formImportService;
        this.sourceTypeDetector = sourceTypeDetector;
    }

    @PostMapping
    public ResponseEntity<FormImportJobView> upload(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Dev-User-Id", required = false) String actorHeader,
            @RequestParam("file") MultipartFile file) {
        UUID actorId = AdminRequestContext.resolveActorId(actorHeader);
        if (file == null || file.isEmpty()) {
            throw new FormImportException("No file uploaded");
        }
        String fileName = file.getOriginalFilename();
        String sourceType = sourceTypeDetector
                .detectFile(fileName, file.getContentType())
                .orElseThrow(() -> new FormImportException(
                        "Unsupported file type. Supported: PDF, CSV, XLS/XLSX, images, HTML."));
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new FormImportException("Could not read uploaded file", ex);
        }
        FormImportSource source =
                FormImportSource.ofFile(sourceType, bytes, fileName, file.getContentType());
        FormImportJobView job = formImportService.createImport(tenantId, actorId, source);
        return ResponseEntity.status(HttpStatus.CREATED).body(job);
    }

    @PostMapping("/from-url")
    public ResponseEntity<FormImportJobView> fromUrl(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Dev-User-Id", required = false) String actorHeader,
            @Valid @RequestBody ImportUrlRequest request) {
        UUID actorId = AdminRequestContext.resolveActorId(actorHeader);
        String sourceType = sourceTypeDetector.detectUrl(request.url());
        FormImportSource source = FormImportSource.ofUrl(sourceType, request.url().trim());
        FormImportJobView job = formImportService.createImport(tenantId, actorId, source);
        return ResponseEntity.status(HttpStatus.CREATED).body(job);
    }

    @GetMapping
    public List<FormImportJobView> list(@RequestHeader("X-Tenant-Id") UUID tenantId) {
        return formImportService.listImports(tenantId);
    }

    @GetMapping("/{id}")
    public FormImportJobView get(@RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable("id") UUID id) {
        return formImportService.getImport(tenantId, id);
    }

    @PostMapping("/{id}/accept")
    public AcceptedFormView accept(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Dev-User-Id", required = false) String actorHeader,
            @PathVariable("id") UUID id,
            @Valid @RequestBody AcceptImportRequest request) {
        UUID actorId = AdminRequestContext.resolveActorId(actorHeader);
        return formImportService.accept(
                tenantId,
                actorId,
                id,
                request.code(),
                request.name(),
                request.category(),
                request.storageStrategy(),
                request.schema());
    }

    public record ImportUrlRequest(@NotBlank String url) {}

    public record AcceptImportRequest(
            @NotBlank String code,
            @NotBlank String name,
            String category,
            StorageStrategy storageStrategy,
            JsonNode schema) {}
}
