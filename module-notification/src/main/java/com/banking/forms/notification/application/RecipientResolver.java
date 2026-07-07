package com.banking.forms.notification.application;

import com.banking.forms.formdefinition.application.PublishedFormView;
import com.banking.forms.submission.application.SectionStorageRouter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Derives a customer's contact points (email, phone, consent, locale) from a submission's section
 * data. Keys are matched heuristically by leaf name (e.g. any field whose name contains {@code email}),
 * mirroring the form-agnostic approach used by the heuristic AI evaluator. Fail-safe: any error yields
 * an empty recipient so notifications are simply skipped, never breaking the caller.
 */
@Component
public class RecipientResolver {

    private static final Logger log = LoggerFactory.getLogger(RecipientResolver.class);

    private static final List<String> EMAIL_KEYS = List.of("email", "e-mail", "mail");
    private static final List<String> PHONE_KEYS = List.of("phone", "mobile", "msisdn", "whatsapp", "cell");
    private static final List<String> CONSENT_KEYS =
            List.of("consent", "optin", "opt_in", "opt-in", "notifyme", "marketing");
    private static final List<String> LOCALE_KEYS = List.of("locale", "language", "lang", "preferredlanguage");

    private final SectionStorageRouter sectionStorageRouter;

    public RecipientResolver(SectionStorageRouter sectionStorageRouter) {
        this.sectionStorageRouter = sectionStorageRouter;
    }

    public Recipient resolve(PublishedFormView form, UUID submissionId) {
        Map<String, Object> flat;
        try {
            Map<String, Map<String, Object>> sections =
                    sectionStorageRouter.resolve(form.storageStrategy()).loadAllSections(submissionId);
            flat = flatten(sections);
        } catch (Exception ex) {
            log.warn("Recipient resolution failed for submission {}: {}", submissionId, ex.getMessage());
            return Recipient.empty();
        }

        String email = firstMatch(flat, EMAIL_KEYS, RecipientResolver::looksLikeEmail);
        String phone = firstMatch(flat, PHONE_KEYS, RecipientResolver::looksLikePhone);
        Boolean consent = resolveConsent(flat);
        String locale = firstMatch(flat, LOCALE_KEYS, value -> !value.isBlank());
        return new Recipient(email, phone, consent, normalizeLocale(locale));
    }

    private static Boolean resolveConsent(Map<String, Object> flat) {
        for (Map.Entry<String, Object> entry : flat.entrySet()) {
            if (isKeyOf(entry.getKey(), CONSENT_KEYS)) {
                return isTruthy(entry.getValue());
            }
        }
        return null;
    }

    private static String firstMatch(Map<String, Object> flat, List<String> keys, java.util.function.Predicate<String> valid) {
        return flat.entrySet().stream()
                .filter(e -> isKeyOf(e.getKey(), keys))
                .map(e -> e.getValue() == null ? "" : String.valueOf(e.getValue()).trim())
                .filter(v -> !v.isBlank() && valid.test(v))
                .findFirst()
                .orElse(null);
    }

    private static boolean isKeyOf(String dottedKey, List<String> tokens) {
        String leaf = dottedKey.substring(dottedKey.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return tokens.stream().anyMatch(leaf::contains);
    }

    private static boolean looksLikeEmail(String value) {
        return value.contains("@") && value.indexOf('@') < value.lastIndexOf('.');
    }

    private static boolean looksLikePhone(String value) {
        long digits = value.chars().filter(Character::isDigit).count();
        return digits >= 7;
    }

    private static boolean isTruthy(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value == null) {
            return false;
        }
        String s = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return s.equals("true") || s.equals("yes") || s.equals("y") || s.equals("1") || s.equals("on");
    }

    private static String normalizeLocale(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT).split("[_-]")[0];
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> flatten(Map<String, Map<String, Object>> sections) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (sections == null) {
            return out;
        }
        sections.forEach((section, fields) -> flatten(section, fields, out));
        return out;
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
}
