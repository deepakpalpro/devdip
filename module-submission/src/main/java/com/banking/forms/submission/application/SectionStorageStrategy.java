package com.banking.forms.submission.application;

import com.banking.forms.formdefinition.domain.StorageStrategy;
import java.util.Map;
import java.util.UUID;

/**
 * Persistence strategy for submission section data. One implementation exists per
 * {@link StorageStrategy}; {@link SectionStorageRouter} selects the right one based on the
 * owning form's configuration.
 */
public interface SectionStorageStrategy {

    StorageStrategy strategy();

    void saveSection(UUID submissionId, String sectionKey, Map<String, Object> data);

    Map<String, Map<String, Object>> loadAllSections(UUID submissionId);

    /** Removes all persisted section data for a submission (used when a draft is discarded). */
    void deleteSections(UUID submissionId);
}
