package com.banking.forms.notification.application;

/**
 * The customer's resolved contact points, discovered from the submission's field data.
 *
 * @param email        resolved email address, or null
 * @param phone        resolved E.164-ish phone number, or null
 * @param consentGiven TRUE/FALSE if a consent field was present, null if unknown (no consent field)
 * @param locale       preferred locale if the form captured one, else null
 */
public record Recipient(String email, String phone, Boolean consentGiven, String locale) {

    public static Recipient empty() {
        return new Recipient(null, null, null, null);
    }

    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }

    public boolean hasPhone() {
        return phone != null && !phone.isBlank();
    }

    public boolean hasAnyContact() {
        return hasEmail() || hasPhone();
    }

    /** Consent is granted when explicitly true; an absent consent field counts as not-granted. */
    public boolean consentGranted() {
        return Boolean.TRUE.equals(consentGiven);
    }
}
