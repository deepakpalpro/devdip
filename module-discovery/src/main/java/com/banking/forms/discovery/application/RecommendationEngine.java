package com.banking.forms.discovery.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Pure, stateless triage engine. Evaluates a set of {@link TriageRule}s against a user's answers and
 * returns candidate forms ranked by accumulated score, each with the rationales that contributed.
 */
@Component
public class RecommendationEngine {

    public List<RankedForm> rank(List<TriageRule> rules, Map<String, Object> answers) {
        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, List<String>> rationales = new LinkedHashMap<>();

        for (TriageRule rule : rules) {
            if (!allConditionsMatch(rule, answers)) {
                continue;
            }
            scores.merge(rule.targetFormCode(), rule.weight(), Double::sum);
            if (rule.rationale() != null && !rule.rationale().isBlank()) {
                rationales.computeIfAbsent(rule.targetFormCode(), k -> new ArrayList<>()).add(rule.rationale());
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> new RankedForm(
                        entry.getKey(),
                        entry.getValue(),
                        rationales.getOrDefault(entry.getKey(), List.of())))
                .toList();
    }

    private boolean allConditionsMatch(TriageRule rule, Map<String, Object> answers) {
        if (rule.conditions() == null || rule.conditions().isEmpty()) {
            return false;
        }
        for (RuleCondition condition : rule.conditions()) {
            if (!matches(condition, answers.get(condition.questionKey()))) {
                return false;
            }
        }
        return true;
    }

    private boolean matches(RuleCondition condition, Object answer) {
        return switch (condition.operator()) {
            case EXISTS -> !isBlank(answer);
            case NOT_EXISTS -> isBlank(answer);
            case EQUALS -> equalsValue(answer, condition.value());
            case NOT_EQUALS -> !equalsValue(answer, condition.value());
            case IN -> inCollection(answer, condition.value());
            case GT -> compareNumeric(answer, condition.value()) > 0;
            case GTE -> compareNumeric(answer, condition.value()) >= 0;
            case LT -> compareNumeric(answer, condition.value()) < 0;
            case LTE -> compareNumeric(answer, condition.value()) <= 0;
        };
    }

    private boolean equalsValue(Object answer, Object value) {
        if (isBlank(answer) || value == null) {
            return false;
        }
        return String.valueOf(answer).equals(String.valueOf(value));
    }

    private boolean inCollection(Object answer, Object value) {
        if (isBlank(answer) || !(value instanceof Collection<?> collection)) {
            return false;
        }
        String answerText = String.valueOf(answer);
        return collection.stream().anyMatch(candidate -> String.valueOf(candidate).equals(answerText));
    }

    /** Returns {@link Integer#MIN_VALUE} when either side is not numeric so comparisons fail closed. */
    private int compareNumeric(Object answer, Object value) {
        Double a = toDouble(answer);
        Double b = toDouble(value);
        if (a == null || b == null) {
            return Integer.MIN_VALUE;
        }
        return Double.compare(a, b);
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isBlank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }
}
