package com.banking.forms.transformation.application;

import com.banking.forms.transformation.domain.PiiStrategy;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Heuristic PII classifier: matches on the normalized leaf field key. This keeps the demo
 * config-free; a production system would layer per-form/per-tenant overrides on top (the
 * {@code formCode} argument is threaded through for exactly that).
 *
 * <p>Names are intentionally left {@link PiiStrategy#NONE} because reviewers need to see the
 * applicant's name; identifiers, contact details, and dates of birth are transformed.
 */
@Component
public class DefaultPiiFieldRegistry implements PiiFieldRegistry {

    private record Pattern(List<String> needles, PiiStrategy strategy) {}

    private static final List<Pattern> PATTERNS = List.of(
            new Pattern(List.of("ssn", "socialsecurity", "tin", "taxid", "nationalid"), PiiStrategy.MASK),
            new Pattern(List.of("accountnumber", "accountno", "acctno", "iban", "cardnumber"), PiiStrategy.MASK),
            new Pattern(List.of("routing", "sortcode"), PiiStrategy.MASK),
            new Pattern(List.of("passport", "license", "licence"), PiiStrategy.MASK),
            new Pattern(List.of("phone", "mobile", "telephone"), PiiStrategy.MASK),
            new Pattern(List.of("email"), PiiStrategy.HASH),
            new Pattern(List.of("dob", "dateofbirth", "birthdate"), PiiStrategy.REDACT));

    @Override
    public PiiStrategy strategyFor(String formCode, String fieldKey) {
        if (fieldKey == null) {
            return PiiStrategy.NONE;
        }
        String normalized = normalize(fieldKey);
        for (Pattern pattern : PATTERNS) {
            for (String needle : pattern.needles()) {
                if (normalized.contains(needle)) {
                    return pattern.strategy();
                }
            }
        }
        return PiiStrategy.NONE;
    }

    private String normalize(String fieldKey) {
        StringBuilder sb = new StringBuilder(fieldKey.length());
        for (char c : fieldKey.toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Exposed for documentation/tests: the built-in sensitive-field patterns. */
    public Map<String, PiiStrategy> describe() {
        return PATTERNS.stream()
                .flatMap(p -> p.needles().stream().map(n -> Map.entry(n, p.strategy())))
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
    }
}
