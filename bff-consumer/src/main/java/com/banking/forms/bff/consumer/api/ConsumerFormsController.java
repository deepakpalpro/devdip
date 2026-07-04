package com.banking.forms.bff.consumer.api;

import com.banking.forms.formdefinition.domain.FormDefinition;
import com.banking.forms.formdefinition.infrastructure.FormDefinitionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consumer/v1/forms")
public class ConsumerFormsController {

    private final FormDefinitionRepository formDefinitionRepository;

    public ConsumerFormsController(FormDefinitionRepository formDefinitionRepository) {
        this.formDefinitionRepository = formDefinitionRepository;
    }

    @GetMapping
    public List<FormSummary> listForms(@RequestHeader("X-Tenant-Id") UUID tenantId) {
        return formDefinitionRepository.findByTenantId(tenantId).stream()
                .map(form -> new FormSummary(form.getCode(), form.getName(), form.getCategory()))
                .toList();
    }

    public record FormSummary(String code, String name, String category) {}
}
