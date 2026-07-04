package com.banking.forms.formdefinition.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.forms.formdefinition.domain.FormDefinition;
import com.banking.forms.formdefinition.domain.FormVersion;
import com.banking.forms.formdefinition.domain.FormVersionStatus;
import com.banking.forms.formdefinition.domain.StorageStrategy;
import com.banking.forms.formdefinition.infrastructure.FormDefinitionRepository;
import com.banking.forms.formdefinition.infrastructure.FormVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FormCommandServiceTest {

    private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ACTOR = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String VALID_SCHEMA =
            "{\"sections\":[{\"key\":\"personal\",\"title\":\"Personal\","
                    + "\"fields\":[{\"key\":\"firstName\",\"type\":\"text\",\"label\":\"First Name\"}]}]}";

    @Mock private FormDefinitionRepository formDefinitionRepository;
    @Mock private FormVersionRepository formVersionRepository;

    private FormCommandService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new FormCommandService(formDefinitionRepository, formVersionRepository, objectMapper);
    }

    @Test
    void createDefinition_persistsDefinitionAndEmptyDraftV1() {
        when(formDefinitionRepository.findByTenantIdAndCode(TENANT, "LOAN")).thenReturn(Optional.empty());
        when(formVersionRepository.findByFormDefinitionIdOrderByVersionNumberDesc(any())).thenReturn(List.of());

        FormDetailView detail =
                service.createDefinition(TENANT, ACTOR, "LOAN", "Loan Application", "Lending", StorageStrategy.KEY_VALUE);

        assertThat(detail.code()).isEqualTo("LOAN");
        assertThat(detail.name()).isEqualTo("Loan Application");
        assertThat(detail.storageStrategy()).isEqualTo("KEY_VALUE");

        ArgumentCaptor<FormVersion> versionCaptor = ArgumentCaptor.forClass(FormVersion.class);
        verify(formDefinitionRepository).save(any(FormDefinition.class));
        verify(formVersionRepository).save(versionCaptor.capture());
        FormVersion firstVersion = versionCaptor.getValue();
        assertThat(firstVersion.getVersionNumber()).isEqualTo(1);
        assertThat(firstVersion.getStatus()).isEqualTo(FormVersionStatus.DRAFT);
        assertThat(firstVersion.getCreatedBy()).isEqualTo(ACTOR);
    }

    @Test
    void createDefinition_duplicateCode_throwsConflict() {
        when(formDefinitionRepository.findByTenantIdAndCode(TENANT, "LOAN"))
                .thenReturn(Optional.of(definition(UUID.randomUUID(), "LOAN")));

        assertThatThrownBy(() ->
                        service.createDefinition(TENANT, ACTOR, "LOAN", "Loan", "Lending", StorageStrategy.JSON_BLOB))
                .isInstanceOf(FormConflictException.class);

        verify(formDefinitionRepository, never()).save(any());
        verify(formVersionRepository, never()).save(any());
    }

    @Test
    void createVersion_incrementsNumberAndClonesLatestSchema() {
        UUID formId = UUID.randomUUID();
        FormVersion latest = version(formId, 2, FormVersionStatus.PUBLISHED, VALID_SCHEMA);
        when(formDefinitionRepository.findByIdAndTenantId(formId, TENANT))
                .thenReturn(Optional.of(definition(formId, "LOAN")));
        when(formVersionRepository.findByFormDefinitionIdOrderByVersionNumberDesc(formId))
                .thenReturn(List.of(latest));

        FormVersionView created = service.createVersion(TENANT, ACTOR, formId, null);

        assertThat(created.versionNumber()).isEqualTo(3);
        assertThat(created.status()).isEqualTo("DRAFT");

        ArgumentCaptor<FormVersion> captor = ArgumentCaptor.forClass(FormVersion.class);
        verify(formVersionRepository).save(captor.capture());
        assertThat(captor.getValue().getSchemaJson()).isEqualTo(VALID_SCHEMA);
    }

    @Test
    void updateDraftSchema_valid_updatesAndPersists() {
        UUID formId = UUID.randomUUID();
        FormVersion draft = version(formId, 1, FormVersionStatus.DRAFT, "{\"sections\":[]}");
        when(formDefinitionRepository.findByIdAndTenantId(formId, TENANT))
                .thenReturn(Optional.of(definition(formId, "LOAN")));
        when(formVersionRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

        service.updateDraftSchema(TENANT, formId, draft.getId(), readTree(VALID_SCHEMA));

        assertThat(draft.getSchemaJson()).contains("firstName");
        verify(formVersionRepository).save(draft);
    }

    @Test
    void updateDraftSchema_whenPublished_throwsConflict() {
        UUID formId = UUID.randomUUID();
        FormVersion published = version(formId, 1, FormVersionStatus.PUBLISHED, VALID_SCHEMA);
        when(formDefinitionRepository.findByIdAndTenantId(formId, TENANT))
                .thenReturn(Optional.of(definition(formId, "LOAN")));
        when(formVersionRepository.findById(published.getId())).thenReturn(Optional.of(published));

        assertThatThrownBy(() ->
                        service.updateDraftSchema(TENANT, formId, published.getId(), readTree(VALID_SCHEMA)))
                .isInstanceOf(FormConflictException.class);
    }

    @Test
    void updateDraftSchema_missingSections_throwsSchemaException() {
        UUID formId = UUID.randomUUID();
        FormVersion draft = version(formId, 1, FormVersionStatus.DRAFT, "{\"sections\":[]}");
        when(formDefinitionRepository.findByIdAndTenantId(formId, TENANT))
                .thenReturn(Optional.of(definition(formId, "LOAN")));
        when(formVersionRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() ->
                        service.updateDraftSchema(TENANT, formId, draft.getId(), readTree("{\"foo\":true}")))
                .isInstanceOf(FormSchemaException.class);
    }

    @Test
    void updateDraftSchema_duplicateFieldKey_throwsSchemaException() {
        UUID formId = UUID.randomUUID();
        FormVersion draft = version(formId, 1, FormVersionStatus.DRAFT, "{\"sections\":[]}");
        when(formDefinitionRepository.findByIdAndTenantId(formId, TENANT))
                .thenReturn(Optional.of(definition(formId, "LOAN")));
        when(formVersionRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

        String dupField = "{\"sections\":[{\"key\":\"s\",\"title\":\"S\",\"fields\":["
                + "{\"key\":\"f\",\"type\":\"text\",\"label\":\"A\"},"
                + "{\"key\":\"f\",\"type\":\"text\",\"label\":\"B\"}]}]}";

        assertThatThrownBy(() -> service.updateDraftSchema(TENANT, formId, draft.getId(), readTree(dupField)))
                .isInstanceOf(FormSchemaException.class);
    }

    @Test
    void updateDraftSchema_dotInKey_throwsSchemaException() {
        UUID formId = UUID.randomUUID();
        FormVersion draft = version(formId, 1, FormVersionStatus.DRAFT, "{\"sections\":[]}");
        when(formDefinitionRepository.findByIdAndTenantId(formId, TENANT))
                .thenReturn(Optional.of(definition(formId, "LOAN")));
        when(formVersionRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

        String dotted = "{\"sections\":[{\"key\":\"a.b\",\"title\":\"S\",\"fields\":[]}]}";

        assertThatThrownBy(() -> service.updateDraftSchema(TENANT, formId, draft.getId(), readTree(dotted)))
                .isInstanceOf(FormSchemaException.class);
    }

    @Test
    void publish_draftTransitionsAndDeprecatesPreviousPublished() {
        UUID formId = UUID.randomUUID();
        FormVersion draft = version(formId, 2, FormVersionStatus.DRAFT, VALID_SCHEMA);
        FormVersion previouslyPublished = version(formId, 1, FormVersionStatus.PUBLISHED, VALID_SCHEMA);
        when(formDefinitionRepository.findByIdAndTenantId(formId, TENANT))
                .thenReturn(Optional.of(definition(formId, "LOAN")));
        when(formVersionRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(formVersionRepository.findFirstByFormDefinitionIdAndStatusOrderByVersionNumberDesc(
                        formId, FormVersionStatus.PUBLISHED))
                .thenReturn(Optional.of(previouslyPublished));

        FormVersionView result = service.publish(TENANT, formId, draft.getId());

        assertThat(result.status()).isEqualTo("PUBLISHED");
        assertThat(result.publishedAt()).isNotNull();
        assertThat(draft.getStatus()).isEqualTo(FormVersionStatus.PUBLISHED);
        assertThat(previouslyPublished.getStatus()).isEqualTo(FormVersionStatus.DEPRECATED);
        verify(formVersionRepository).save(previouslyPublished);
        verify(formVersionRepository).save(draft);
    }

    @Test
    void publish_whenAlreadyPublished_throwsConflict() {
        UUID formId = UUID.randomUUID();
        FormVersion published = version(formId, 1, FormVersionStatus.PUBLISHED, VALID_SCHEMA);
        when(formDefinitionRepository.findByIdAndTenantId(formId, TENANT))
                .thenReturn(Optional.of(definition(formId, "LOAN")));
        when(formVersionRepository.findById(published.getId())).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> service.publish(TENANT, formId, published.getId()))
                .isInstanceOf(FormConflictException.class);
    }

    @Test
    void getForm_unknownId_throwsNotFound() {
        UUID formId = UUID.randomUUID();
        when(formDefinitionRepository.findByIdAndTenantId(formId, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForm(TENANT, formId)).isInstanceOf(FormNotFoundException.class);
    }

    @Test
    void requireVersion_versionOfDifferentForm_throwsNotFound() {
        UUID formId = UUID.randomUUID();
        FormVersion foreignVersion = version(UUID.randomUUID(), 1, FormVersionStatus.DRAFT, VALID_SCHEMA);
        when(formDefinitionRepository.findByIdAndTenantId(formId, TENANT))
                .thenReturn(Optional.of(definition(formId, "LOAN")));
        when(formVersionRepository.findById(foreignVersion.getId())).thenReturn(Optional.of(foreignVersion));

        assertThatThrownBy(() ->
                        service.updateDraftSchema(TENANT, formId, foreignVersion.getId(), readTree(VALID_SCHEMA)))
                .isInstanceOf(FormNotFoundException.class);
    }

    private FormDefinition definition(UUID id, String code) {
        return new FormDefinition(id, TENANT, code, "Name", "Category", StorageStrategy.JSON_BLOB);
    }

    private FormVersion version(UUID formId, int number, FormVersionStatus status, String schema) {
        return new FormVersion(UUID.randomUUID(), formId, number, status, schema, ACTOR);
    }

    private com.fasterxml.jackson.databind.JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
