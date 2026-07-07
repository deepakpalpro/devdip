package com.banking.forms.submission.application;

import com.banking.forms.formdefinition.domain.StorageStrategy;
import com.banking.forms.submission.domain.SubmissionSection;
import com.banking.forms.submission.infrastructure.SubmissionSectionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Stores each section as a single JSON document in {@code submission_section.section_data_json}. */
@Component
public class JsonBlobSectionStorage implements SectionStorageStrategy {

    private final SubmissionSectionRepository sectionRepository;
    private final ObjectMapper objectMapper;

    public JsonBlobSectionStorage(SubmissionSectionRepository sectionRepository, ObjectMapper objectMapper) {
        this.sectionRepository = sectionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public StorageStrategy strategy() {
        return StorageStrategy.JSON_BLOB;
    }

    @Override
    public void saveSection(UUID submissionId, String sectionKey, Map<String, Object> data) {
        String json = writeJson(data);
        SubmissionSection section = sectionRepository
                .findBySubmissionIdAndSectionKey(submissionId, sectionKey)
                .orElseGet(() -> new SubmissionSection(UUID.randomUUID(), submissionId, sectionKey));
        section.setSectionDataJson(json);
        sectionRepository.save(section);
    }

    @Override
    public Map<String, Map<String, Object>> loadAllSections(UUID submissionId) {
        var result = new HashMap<String, Map<String, Object>>();
        for (SubmissionSection section : sectionRepository.findBySubmissionId(submissionId)) {
            if (section.getSectionDataJson() != null) {
                result.put(section.getSectionKey(), readJson(section.getSectionDataJson()));
            }
        }
        return result;
    }

    @Override
    public void deleteSections(UUID submissionId) {
        sectionRepository.deleteBySubmissionId(submissionId);
    }

    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid section JSON", ex);
        }
    }

    private String writeJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize section data", ex);
        }
    }
}
