package com.banking.forms.bff.admin.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.forms.app.config.GlobalExceptionHandler;
import com.banking.forms.formimport.application.AcceptedFormView;
import com.banking.forms.formimport.application.FormImportJobView;
import com.banking.forms.formimport.application.FormImportNotFoundException;
import com.banking.forms.formimport.application.FormImportService;
import com.banking.forms.formimport.application.SourceTypeDetector;
import com.banking.forms.formimport.spi.FormImportSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminFormImportControllerTest {

    private static final String TENANT = "11111111-1111-1111-1111-111111111111";

    @Mock private FormImportService formImportService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminFormImportController(formImportService, new SourceTypeDetector()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private FormImportJobView view(String status, String sourceType, String providerCode) {
        return new FormImportJobView(
                UUID.randomUUID(),
                status,
                sourceType,
                providerCode,
                "loan.pdf",
                "ACROFORM",
                "Loan Application",
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode(),
                null,
                null,
                Instant.now());
    }

    @Test
    void upload_pdf_detectsTypeAndReturns201() throws Exception {
        when(formImportService.createImport(any(), any(), any(FormImportSource.class)))
                .thenReturn(view("NEEDS_REVIEW", "PDF", "pdfbox"));

        MockMultipartFile file =
                new MockMultipartFile("file", "loan.pdf", "application/pdf", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/admin/v1/form-imports").file(file).header("X-Tenant-Id", TENANT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.sourceType").value("PDF"))
                .andExpect(jsonPath("$.providerCode").value("pdfbox"));
    }

    @Test
    void upload_unsupportedType_returns400AndSkipsService() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "notes.rtf", "application/rtf", new byte[] {1, 2});

        mockMvc.perform(multipart("/api/admin/v1/form-imports").file(file).header("X-Tenant-Id", TENANT))
                .andExpect(status().isBadRequest());

        verify(formImportService, never()).createImport(any(), any(), any(FormImportSource.class));
    }

    @Test
    void fromUrl_createsHtmlImport() throws Exception {
        when(formImportService.createImport(any(), any(), any(FormImportSource.class)))
                .thenReturn(view("NEEDS_REVIEW", "HTML", "jsoup-html"));

        mockMvc.perform(post("/api/admin/v1/form-imports/from-url")
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://bank.example/apply\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceType").value("HTML"));
    }

    @Test
    void accept_returnsCreatedFormReference() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID formId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        when(formImportService.accept(any(), any(), eq(jobId), eq("LOAN"), eq("Loan"), any(), any(), any()))
                .thenReturn(new AcceptedFormView(jobId, formId, versionId));

        mockMvc.perform(post("/api/admin/v1/form-imports/" + jobId + "/accept")
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"LOAN\",\"name\":\"Loan\",\"storageStrategy\":\"JSON_BLOB\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formId").value(formId.toString()))
                .andExpect(jsonPath("$.versionId").value(versionId.toString()));
    }

    @Test
    void get_missingJob_returns404() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(formImportService.getImport(any(), eq(jobId))).thenThrow(new FormImportNotFoundException(jobId));

        mockMvc.perform(get("/api/admin/v1/form-imports/" + jobId).header("X-Tenant-Id", TENANT))
                .andExpect(status().isNotFound());
    }
}
