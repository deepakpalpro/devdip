package com.banking.forms.formdefinition.application;

import java.util.UUID;

/** Admin list-view summary of a form definition plus its latest version status. */
public record FormAdminSummaryView(
        UUID id,
        String code,
        String name,
        String category,
        String storageStrategy,
        Integer latestVersion,
        String latestStatus) {}
