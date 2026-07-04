package com.banking.forms.discovery.application;

import com.banking.forms.discovery.domain.DiscoveryQuestionnaire;
import com.banking.forms.discovery.domain.DiscoverySession;
import com.banking.forms.discovery.infrastructure.DiscoveryQuestionnaireRepository;
import com.banking.forms.discovery.infrastructure.DiscoverySessionRepository;
import com.banking.forms.formdefinition.domain.FormDefinition;
import com.banking.forms.formdefinition.infrastructure.FormDefinitionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the Form Discovery Wizard: serving the questionnaire, evaluating answers into ranked
 * form recommendations (persisting the session), and building the pre-population payload that seeds
 * the chosen application so users never re-enter data they already provided.
 */
@Service
@Transactional
public class DiscoveryService {

    private final DiscoveryQuestionnaireRepository questionnaireRepository;
    private final DiscoverySessionRepository sessionRepository;
    private final FormDefinitionRepository formDefinitionRepository;
    private final RecommendationEngine recommendationEngine;
    private final ObjectMapper objectMapper;

    public DiscoveryService(
            DiscoveryQuestionnaireRepository questionnaireRepository,
            DiscoverySessionRepository sessionRepository,
            FormDefinitionRepository formDefinitionRepository,
            RecommendationEngine recommendationEngine,
            ObjectMapper objectMapper) {
        this.questionnaireRepository = questionnaireRepository;
        this.sessionRepository = sessionRepository;
        this.formDefinitionRepository = formDefinitionRepository;
        this.recommendationEngine = recommendationEngine;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public QuestionnaireView getQuestionnaire(UUID tenantId, String code) {
        DiscoveryQuestionnaire questionnaire = requireQuestionnaire(tenantId, code);
        return new QuestionnaireView(
                questionnaire.getCode(), questionnaire.getName(), parse(questionnaire.getSchemaJson()));
    }

    public DiscoveryEvaluationView evaluate(UUID tenantId, UUID userId, String code, Map<String, Object> answers) {
        DiscoveryQuestionnaire questionnaire = requireQuestionnaire(tenantId, code);
        Map<String, Object> safeAnswers = answers == null ? Map.of() : answers;

        List<TriageRule> rules = parseRules(questionnaire.getRulesJson());
        List<RankedForm> ranked = recommendationEngine.rank(rules, safeAnswers);

        List<RecommendationView> recommendations = toRecommendations(tenantId, ranked);
        String recommendedFormCode = recommendations.isEmpty() ? null : recommendations.get(0).formCode();

        DiscoverySession session = new DiscoverySession(
                UUID.randomUUID(), tenantId, userId, code, writeJson(safeAnswers), recommendedFormCode);
        sessionRepository.save(session);

        return new DiscoveryEvaluationView(session.getId(), recommendations);
    }

    /**
     * Builds the pre-population payload ({@code sectionKey -> fieldKey -> value}) for the target form
     * from a persisted discovery session. Returns empty when the session, questionnaire, or mappings
     * cannot be resolved so starting an application never fails on a stale reference.
     */
    @Transactional(readOnly = true)
    public Map<String, Map<String, Object>> buildPrefill(UUID tenantId, UUID sessionId, String targetFormCode) {
        Optional<DiscoverySession> maybeSession = sessionRepository
                .findById(sessionId)
                .filter(session -> session.getTenantId().equals(tenantId));
        if (maybeSession.isEmpty()) {
            return Map.of();
        }
        DiscoverySession session = maybeSession.get();

        Optional<DiscoveryQuestionnaire> maybeQuestionnaire =
                questionnaireRepository.findByTenantIdAndCode(tenantId, session.getQuestionnaireCode());
        if (maybeQuestionnaire.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> answers = readAnswers(session.getAnswersJson());
        List<FieldMapping> mappings =
                parseMappings(maybeQuestionnaire.get().getMappingsJson()).getOrDefault(targetFormCode, List.of());

        Map<String, Map<String, Object>> prefill = new LinkedHashMap<>();
        for (FieldMapping mapping : mappings) {
            Object value = answers.get(mapping.questionKey());
            if (value == null || String.valueOf(value).isBlank()) {
                continue;
            }
            prefill.computeIfAbsent(mapping.targetSection(), k -> new LinkedHashMap<>())
                    .put(mapping.targetField(), value);
        }
        return prefill;
    }

    private List<RecommendationView> toRecommendations(UUID tenantId, List<RankedForm> ranked) {
        List<RecommendationView> recommendations = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            RankedForm rankedForm = ranked.get(i);
            Optional<FormDefinition> definition =
                    formDefinitionRepository.findByTenantIdAndCode(tenantId, rankedForm.formCode());
            if (definition.isEmpty()) {
                continue;
            }
            recommendations.add(new RecommendationView(
                    definition.get().getCode(),
                    definition.get().getName(),
                    definition.get().getCategory(),
                    rankedForm.score(),
                    recommendations.isEmpty(),
                    rankedForm.rationales()));
        }
        return recommendations;
    }

    private DiscoveryQuestionnaire requireQuestionnaire(UUID tenantId, String code) {
        return questionnaireRepository
                .findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new DiscoveryNotFoundException("Questionnaire not found: " + code));
    }

    private List<TriageRule> parseRules(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid questionnaire rules JSON", ex);
        }
    }

    private Map<String, List<FieldMapping>> parseMappings(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid questionnaire mappings JSON", ex);
        }
    }

    private Map<String, Object> readAnswers(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid discovery answers JSON", ex);
        }
    }

    private JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid questionnaire schema JSON", ex);
        }
    }

    private String writeJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize discovery answers", ex);
        }
    }
}
