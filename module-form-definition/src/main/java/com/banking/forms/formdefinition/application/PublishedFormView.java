package com.banking.forms.formdefinition.application;

import com.banking.forms.formdefinition.domain.StorageStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record PublishedFormView(
        UUID formDefinitionId,
        UUID formVersionId,
        String code,
        String name,
        String category,
        StorageStrategy storageStrategy,
        JsonNode schema) {}
