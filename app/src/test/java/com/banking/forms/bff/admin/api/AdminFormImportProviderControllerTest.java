package com.banking.forms.bff.admin.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.forms.app.config.GlobalExceptionHandler;
import com.banking.forms.formimport.application.ProviderSettingsService;
import com.banking.forms.formimport.application.ProviderView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminFormImportProviderControllerTest {

    @Mock private ProviderSettingsService providerSettingsService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminFormImportProviderController(providerSettingsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void list_returnsProviders() throws Exception {
        when(providerSettingsService.list())
                .thenReturn(List.of(new ProviderView("pdfbox", "PDFBox", "PDF", true, 10, true, null)));

        mockMvc.perform(get("/api/admin/v1/form-import-providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("pdfbox"))
                .andExpect(jsonPath("$[0].available").value(true));
    }

    @Test
    void update_appliesChanges() throws Exception {
        when(providerSettingsService.update(eq("llm-vision"), eq(true), eq(5), any()))
                .thenReturn(new ProviderView("llm-vision", "LLM Vision", "IMAGE", true, 5, true, null));

        mockMvc.perform(put("/api/admin/v1/form-import-providers/llm-vision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"priority\":5,\"config\":{\"endpoint\":\"https://x\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.priority").value(5));
    }
}
