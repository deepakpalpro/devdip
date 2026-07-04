package com.banking.forms.bff.admin.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.forms.app.config.GlobalExceptionHandler;
import com.banking.forms.formdefinition.application.FormCommandService;
import com.banking.forms.formdefinition.application.FormConflictException;
import com.banking.forms.formdefinition.application.FormDetailView;
import com.banking.forms.formdefinition.application.FormNotFoundException;
import com.banking.forms.formdefinition.application.FormSchemaException;
import com.banking.forms.formdefinition.application.FormVersionView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminFormsControllerTest {

    private static final String TENANT = "11111111-1111-1111-1111-111111111111";

    @Mock private FormCommandService formCommandService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminFormsController(formCommandService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listForms_returnsOk() throws Exception {
        when(formCommandService.listForms(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/v1/forms").header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk());
    }

    @Test
    void createForm_returns201WithDetail() throws Exception {
        FormVersionView v1 = new FormVersionView(UUID.randomUUID(), 1, "DRAFT", null, objectMapper.createObjectNode());
        FormDetailView detail =
                new FormDetailView(UUID.randomUUID(), "LOAN", "Loan Application", "Lending", "JSON_BLOB", List.of(v1));
        when(formCommandService.createDefinition(any(), any(), eq("LOAN"), any(), any(), any()))
                .thenReturn(detail);

        mockMvc.perform(post("/api/admin/v1/forms")
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"LOAN\",\"name\":\"Loan Application\","
                                + "\"category\":\"Lending\",\"storageStrategy\":\"JSON_BLOB\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("LOAN"))
                .andExpect(jsonPath("$.versions[0].status").value("DRAFT"));
    }

    @Test
    void createForm_missingCode_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/v1/forms")
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Loan Application\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createForm_duplicateCode_returns409() throws Exception {
        when(formCommandService.createDefinition(any(), any(), any(), any(), any(), any()))
                .thenThrow(new FormConflictException("Form code already exists: LOAN"));

        mockMvc.perform(post("/api/admin/v1/forms")
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"LOAN\",\"name\":\"Loan Application\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Form code already exists: LOAN"));
    }

    @Test
    void getForm_notFound_returns404() throws Exception {
        UUID formId = UUID.randomUUID();
        when(formCommandService.getForm(any(), eq(formId)))
                .thenThrow(new FormNotFoundException("Form not found: " + formId));

        mockMvc.perform(get("/api/admin/v1/forms/" + formId).header("X-Tenant-Id", TENANT))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateVersionSchema_schemaException_returns400() throws Exception {
        UUID formId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        when(formCommandService.updateDraftSchema(any(), eq(formId), eq(versionId), any()))
                .thenThrow(new FormSchemaException("Schema must contain a 'sections' array"));

        mockMvc.perform(put("/api/admin/v1/forms/" + formId + "/versions/" + versionId)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schema\":{\"foo\":true}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publishVersion_returnsOk() throws Exception {
        UUID formId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        FormVersionView published =
                new FormVersionView(versionId, 2, "PUBLISHED", java.time.Instant.now(), objectMapper.createObjectNode());
        when(formCommandService.publish(any(), eq(formId), eq(versionId))).thenReturn(published);

        mockMvc.perform(post("/api/admin/v1/forms/" + formId + "/versions/" + versionId + "/publish")
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }
}
