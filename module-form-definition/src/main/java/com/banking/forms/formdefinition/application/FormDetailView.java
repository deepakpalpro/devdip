package com.banking.forms.formdefinition.application;

import java.util.List;
import java.util.UUID;

/** Admin-facing detail of a form definition with its full version history (newest first). */
public record FormDetailView(
        UUID id,
        String code,
        String name,
        String category,
        String storageStrategy,
        List<FormVersionView> versions) {}
