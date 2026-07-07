package com.banking.forms.notification.application;

/**
 * Output of the {@link TemplateRenderer}. For email {@code subject}/{@code body} carry the rendered
 * text and {@code templateName} is null; for WhatsApp {@code templateName} carries the provider-approved
 * template id and {@code body} a plain-text fallback.
 */
public record RenderedMessage(String subject, String body, String templateName) {}
