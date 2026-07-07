package com.banking.forms.notification.application;

/**
 * Minimal masking for recipient identifiers so emails/phone numbers are not written to logs or admin
 * views in the clear. Not reversible — display only.
 */
public final class PiiMask {

    private PiiMask() {}

    public static String recipient(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.contains("@")) {
            return email(trimmed);
        }
        return phone(trimmed);
    }

    private static String email(String value) {
        int at = value.indexOf('@');
        String local = value.substring(0, at);
        String domain = value.substring(at);
        String head = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return head + "***" + domain;
    }

    private static String phone(String value) {
        String digits = value.replaceAll("\\s+", "");
        if (digits.length() <= 4) {
            return "****";
        }
        return "***" + digits.substring(digits.length() - 4);
    }
}
