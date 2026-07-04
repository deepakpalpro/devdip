package com.banking.forms.formdefinition.application;

import com.banking.forms.formdefinition.domain.FormVersionStatus;
import com.banking.forms.formdefinition.infrastructure.FormDefinitionRepository;
import com.banking.forms.formdefinition.infrastructure.FormVersionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FormQueryService {

    private final FormDefinitionRepository formDefinitionRepository;
    private final FormVersionRepository formVersionRepository;
    private final FormSchemaComposer formSchemaComposer;
    private final ObjectMapper objectMapper;

    public FormQueryService(
            FormDefinitionRepository formDefinitionRepository,
            FormVersionRepository formVersionRepository,
            FormSchemaComposer formSchemaComposer,
            ObjectMapper objectMapper) {
        this.formDefinitionRepository = formDefinitionRepository;
        this.formVersionRepository = formVersionRepository;
        this.formSchemaComposer = formSchemaComposer;
        this.objectMapper = objectMapper;
    }

    public Optional<PublishedFormView> findPublishedByCode(UUID tenantId, String code) {
        return formDefinitionRepository
                .findByTenantIdAndCode(tenantId, code)
                .flatMap(form -> formVersionRepository
                        .findFirstByFormDefinitionIdAndStatusOrderByVersionNumberDesc(
                                form.getId(), FormVersionStatus.PUBLISHED)
                        .map(version -> new PublishedFormView(
                                form.getId(),
                                version.getId(),
                                form.getCode(),
                                form.getName(),
                                form.getCategory(),
                                form.getStorageStrategy(),
                                composeSchema(form.getTenantId(), version.getSchemaJson()))));
    }

    public Optional<PublishedFormView> findPublishedByVersionId(UUID formVersionId) {
        return formVersionRepository
                .findById(formVersionId)
                .filter(version -> version.getStatus() == FormVersionStatus.PUBLISHED)
                .flatMap(version -> formDefinitionRepository
                        .findById(version.getFormDefinitionId())
                        .map(form -> new PublishedFormView(
                                form.getId(),
                                version.getId(),
                                form.getCode(),
                                form.getName(),
                                form.getCategory(),
                                form.getStorageStrategy(),
                                composeSchema(form.getTenantId(), version.getSchemaJson()))));
    }

    private JsonNode composeSchema(UUID tenantId, String schemaJson) {
        return formSchemaComposer.compose(tenantId, parseSchema(schemaJson));
    }

    private JsonNode parseSchema(String schemaJson) {
        try {
            return objectMapper.readTree(schemaJson);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid form schema JSON", ex);
        }
    }
}
