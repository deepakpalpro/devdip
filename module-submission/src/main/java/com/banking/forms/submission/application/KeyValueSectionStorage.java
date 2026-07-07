package com.banking.forms.submission.application;

import com.banking.forms.formdefinition.domain.StorageStrategy;
import com.banking.forms.submission.domain.SubmissionFieldValue;
import com.banking.forms.submission.domain.SubmissionSection;
import com.banking.forms.submission.infrastructure.SubmissionFieldValueRepository;
import com.banking.forms.submission.infrastructure.SubmissionSectionRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Stores each field as a normalized row in {@code submission_field_value}, keyed by section.
 * Enables per-field indexing, column-level encryption, and richer query/audit for regulated forms.
 * Values are persisted as their string representation.
 *
 * <p>Embedded/nested form values arrive as nested maps. They are flattened into dot-delimited leaf
 * keys (e.g. {@code homeAddress.address.line1}) so each leaf remains an individually indexable /
 * encryptable row, and reconstructed back into the nested structure on load. Field keys therefore
 * must not themselves contain a dot.
 */
@Component
public class KeyValueSectionStorage implements SectionStorageStrategy {

    private static final String PATH_SEPARATOR = ".";

    private final SubmissionSectionRepository sectionRepository;
    private final SubmissionFieldValueRepository fieldValueRepository;

    public KeyValueSectionStorage(
            SubmissionSectionRepository sectionRepository, SubmissionFieldValueRepository fieldValueRepository) {
        this.sectionRepository = sectionRepository;
        this.fieldValueRepository = fieldValueRepository;
    }

    @Override
    public StorageStrategy strategy() {
        return StorageStrategy.KEY_VALUE;
    }

    @Override
    public void saveSection(UUID submissionId, String sectionKey, Map<String, Object> data) {
        SubmissionSection section = sectionRepository
                .findBySubmissionIdAndSectionKey(submissionId, sectionKey)
                .orElseGet(() -> new SubmissionSection(UUID.randomUUID(), submissionId, sectionKey));
        section.touch();
        SubmissionSection savedSection = sectionRepository.save(section);

        fieldValueRepository.deleteBySectionId(savedSection.getId());
        var flattened = new HashMap<String, String>();
        flatten("", data, flattened);
        for (Map.Entry<String, String> entry : flattened.entrySet()) {
            fieldValueRepository.save(new SubmissionFieldValue(
                    UUID.randomUUID(),
                    savedSection.getId(),
                    entry.getKey(),
                    entry.getValue(),
                    shouldEncrypt(entry.getKey())));
        }
    }

    @Override
    public Map<String, Map<String, Object>> loadAllSections(UUID submissionId) {
        var result = new HashMap<String, Map<String, Object>>();
        for (SubmissionSection section : sectionRepository.findBySubmissionId(submissionId)) {
            var flat = new HashMap<String, String>();
            for (SubmissionFieldValue field : fieldValueRepository.findBySectionId(section.getId())) {
                flat.put(field.getFieldKey(), field.getFieldValue());
            }
            result.put(section.getSectionKey(), unflatten(flat));
        }
        return result;
    }

    @Override
    public void deleteSections(UUID submissionId) {
        // Delete field-value leaves first (FK -> submission_section), then the section headers.
        for (SubmissionSection section : sectionRepository.findBySubmissionId(submissionId)) {
            fieldValueRepository.deleteBySectionId(section.getId());
        }
        sectionRepository.deleteBySubmissionId(submissionId);
    }

    @SuppressWarnings("unchecked")
    private void flatten(String prefix, Map<String, Object> data, Map<String, String> out) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + PATH_SEPARATOR + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                flatten(key, (Map<String, Object>) nested, out);
            } else {
                out.put(key, value == null ? null : String.valueOf(value));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unflatten(Map<String, String> flat) {
        var root = new HashMap<String, Object>();
        for (Map.Entry<String, String> entry : flat.entrySet()) {
            String[] parts = entry.getKey().split("\\" + PATH_SEPARATOR);
            Map<String, Object> cursor = root;
            for (int i = 0; i < parts.length - 1; i++) {
                cursor = (Map<String, Object>) cursor.computeIfAbsent(parts[i], k -> new HashMap<String, Object>());
            }
            cursor.put(parts[parts.length - 1], entry.getValue());
        }
        return root;
    }

    /**
     * Placeholder for PII-profile-driven encryption. Wire to module-transformation's PII field
     * registry so sensitive fields (SSN/TIN/account numbers) are encrypted at rest.
     */
    private boolean shouldEncrypt(String fieldKey) {
        return false;
    }
}
