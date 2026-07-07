package com.banking.forms.pipeline.infrastructure;

import com.banking.forms.pipeline.spi.AiEvaluationContext;
import com.banking.forms.pipeline.spi.AiEvaluationResult;
import com.banking.forms.pipeline.spi.AiEvaluator;
import com.banking.forms.pipeline.spi.AiRecommendation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Deterministic, zero-dependency default {@link AiEvaluator} (id {@code heuristic}). Produces a risk
 * score from generic, form-agnostic signals so the {@code AI_EVALUATE} step is demoable and testable
 * with no external model. LLM-backed evaluators (e.g. {@code ollama}) can replace it via
 * {@code pipeline.ai.evaluator} without touching the pipeline.
 *
 * <p>Signals: <em>incompleteness</em> (unfilled leaf fields), a <em>high-value amount</em> above a
 * threshold, a <em>missing contact</em> channel, and the presence of <em>risk keywords</em>.
 */
@Component
public class HeuristicAiEvaluator implements AiEvaluator {

    private static final String MODEL = "rules-v1";
    private static final double HIGH_AMOUNT = 100_000d;
    private static final Set<String> AMOUNT_KEYS =
            Set.of("amount", "income", "loan", "salary", "balance", "limit", "value", "deposit");
    private static final Set<String> CONTACT_KEYS = Set.of("email", "phone", "mobile", "contact");
    private static final Set<String> RISK_WORDS =
            Set.of("bankrupt", "default", "unemployed", "overdue", "fraud", "delinquent");

    @Override
    public String evaluatorId() {
        return "heuristic";
    }

    @Override
    public AiEvaluationResult evaluate(AiEvaluationContext context) {
        long start = System.currentTimeMillis();
        Map<String, Object> flat = new LinkedHashMap<>();
        if (context.sanitizedData() != null) {
            context.sanitizedData().forEach((section, fields) -> flatten(section, fields, flat));
        }

        int totalFields = flat.size();
        long filledFields = flat.values().stream().filter(HeuristicAiEvaluator::notBlank).count();
        double completeness = totalFields == 0 ? 1.0 : (double) filledFields / totalFields;

        boolean highValueAmount = flat.entrySet().stream()
                .anyMatch(e -> isKeyOf(e.getKey(), AMOUNT_KEYS) && asAmount(e.getValue()) >= HIGH_AMOUNT);
        boolean hasContact = flat.entrySet().stream()
                .anyMatch(e -> isKeyOf(e.getKey(), CONTACT_KEYS) && notBlank(e.getValue()));
        boolean riskKeywords = flat.values().stream().anyMatch(HeuristicAiEvaluator::containsRiskWord);

        double risk = (1.0 - completeness) * 0.40
                + (highValueAmount ? 0.25 : 0.0)
                + (hasContact ? 0.0 : 0.15)
                + (riskKeywords ? 0.30 : 0.0);
        risk = clamp(risk);

        AiRecommendation recommendation =
                risk < 0.30 ? AiRecommendation.APPROVE : risk < 0.70 ? AiRecommendation.REVIEW : AiRecommendation.REJECT;

        Map<String, Object> signals = new LinkedHashMap<>();
        signals.put("completeness", round(completeness));
        signals.put("filledFields", filledFields);
        signals.put("totalFields", totalFields);
        signals.put("highValueAmount", highValueAmount);
        signals.put("hasContact", hasContact);
        signals.put("riskKeywords", riskKeywords);

        return new AiEvaluationResult(
                evaluatorId(),
                MODEL,
                round(risk),
                recommendation,
                buildRationale(completeness, highValueAmount, hasContact, riskKeywords),
                signals,
                System.currentTimeMillis() - start);
    }

    private static String buildRationale(
            double completeness, boolean highValueAmount, boolean hasContact, boolean riskKeywords) {
        List<String> reasons = new ArrayList<>();
        if (completeness < 1.0) {
            reasons.add(Math.round(completeness * 100) + "% of fields completed");
        }
        if (highValueAmount) {
            reasons.add("a high-value amount was declared");
        }
        if (!hasContact) {
            reasons.add("no contact channel provided");
        }
        if (riskKeywords) {
            reasons.add("risk-related keywords present");
        }
        if (reasons.isEmpty()) {
            return "No elevated risk signals detected.";
        }
        return "Elevated risk because " + String.join(", ", reasons) + ".";
    }

    @SuppressWarnings("unchecked")
    private static void flatten(String prefix, Object value, Map<String, Object> out) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                flatten(prefix + "." + entry.getKey(), entry.getValue(), out);
            }
        } else if (value instanceof Iterable<?> iterable) {
            int i = 0;
            for (Object item : iterable) {
                flatten(prefix + "[" + i++ + "]", item, out);
            }
        } else {
            out.put(prefix, value);
        }
    }

    private static boolean isKeyOf(String dottedKey, Set<String> tokens) {
        String leaf = dottedKey.substring(dottedKey.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return tokens.stream().anyMatch(leaf::contains);
    }

    private static double asAmount(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        String digits = String.valueOf(value).replaceAll("[^0-9.\\-]", "");
        if (digits.isBlank() || "-".equals(digits) || ".".equals(digits)) {
            return 0.0;
        }
        try {
            return Double.parseDouble(digits);
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private static boolean containsRiskWord(Object value) {
        if (!notBlank(value)) {
            return false;
        }
        String text = String.valueOf(value).toLowerCase(Locale.ROOT);
        return RISK_WORDS.stream().anyMatch(text::contains);
    }

    private static boolean notBlank(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
