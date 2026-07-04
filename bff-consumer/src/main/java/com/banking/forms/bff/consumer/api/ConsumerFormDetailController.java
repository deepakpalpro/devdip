package com.banking.forms.bff.consumer.api;

import com.banking.forms.formdefinition.application.FormQueryService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/consumer/v1/forms")
public class ConsumerFormDetailController {

    private final FormQueryService formQueryService;

    public ConsumerFormDetailController(FormQueryService formQueryService) {
        this.formQueryService = formQueryService;
    }

    @GetMapping("/{formCode}")
    public FormDetailResponse getForm(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable("formCode") String formCode) {
        return formQueryService
                .findPublishedByCode(tenantId, formCode)
                .map(form -> new FormDetailResponse(
                        form.code(),
                        form.name(),
                        form.category(),
                        form.formVersionId(),
                        form.schema()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form not found"));
    }

    public record FormDetailResponse(
            String code, String name, String category, UUID formVersionId, JsonNode schema) {}
}
