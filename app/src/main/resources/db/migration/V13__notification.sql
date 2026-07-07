-- V13: Multi-channel customer notifications (email + WhatsApp).
-- Mirrors the form-import provider pattern: which adapter serves a logical channel — and whether an
-- external provider is used at all — is data-driven from `notification_provider`, managed in the admin
-- Settings UI. `notification_message` is both the durable outbox (PENDING rows picked up by the async
-- dispatcher) and the delivery log. Secrets are never stored here (config holds only a `secretRef`).

CREATE TABLE notification_provider (
    id           BINARY(16) PRIMARY KEY,
    code         VARCHAR(64) NOT NULL UNIQUE,
    name         VARCHAR(128) NOT NULL,
    channel_type VARCHAR(32) NOT NULL,
    enabled      BOOLEAN NOT NULL DEFAULT FALSE,
    priority     INT NOT NULL DEFAULT 100,
    config_json  CLOB,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notif_provider_channel ON notification_provider (channel_type, enabled, priority);

-- Per event + channel + locale message templates. For email `subject`/`body` hold the rendered text;
-- for WhatsApp `subject` holds the provider-approved template name and `body` a plain-text fallback.
CREATE TABLE notification_template (
    id           BINARY(16) PRIMARY KEY,
    event_type   VARCHAR(64) NOT NULL,
    channel_type VARCHAR(32) NOT NULL,
    locale       VARCHAR(16) NOT NULL DEFAULT 'en',
    subject      VARCHAR(256),
    body         CLOB NOT NULL,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_notif_template UNIQUE (event_type, channel_type, locale)
);

-- Durable outbox + delivery log. One row per (event, channel) fan-out. `recipient` is the real
-- address needed for async delivery; it is masked in admin views and timeline events.
CREATE TABLE notification_message (
    id                  BINARY(16) PRIMARY KEY,
    tenant_id           BINARY(16) NOT NULL,
    submission_id       BINARY(16),
    event_type          VARCHAR(64) NOT NULL,
    channel_type        VARCHAR(32) NOT NULL,
    provider_code       VARCHAR(64),
    recipient           VARCHAR(256) NOT NULL,
    subject             VARCHAR(256),
    body                CLOB,
    template_name       VARCHAR(128),
    status              VARCHAR(16) NOT NULL,
    attempts            INT NOT NULL DEFAULT 0,
    provider_message_id VARCHAR(128),
    error               CLOB,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notif_msg_status ON notification_message (status, created_at);
CREATE INDEX idx_notif_msg_submission ON notification_message (submission_id);

-- Email providers: the zero-setup `log-email` sink is enabled by default so notifications are
-- demoable/testable out of the box; `smtp-email` (JavaMailSender) is present but disabled until an
-- admin configures spring.mail.* / a host.
INSERT INTO notification_provider (id, code, name, channel_type, enabled, priority, config_json) VALUES
    (X'b0000000000000000000000000000001', 'log-email',      'Log sink (email, no external delivery)', 'email',    TRUE,  10, NULL),
    (X'b0000000000000000000000000000002', 'smtp-email',     'SMTP email (JavaMailSender)',            'email',    FALSE, 20,
     '{"from":"no-reply@bankingforms.local"}');

-- WhatsApp provider: present but DISABLED until an admin sets endpoint + phoneNumberId + secretRef.
INSERT INTO notification_provider (id, code, name, channel_type, enabled, priority, config_json) VALUES
    (X'b0000000000000000000000000000003', 'whatsapp-cloud', 'WhatsApp Cloud API (Meta)',              'whatsapp', FALSE, 10,
     '{"endpoint":"https://graph.facebook.com/v20.0","phoneNumberId":"","secretRef":"WHATSAPP_CLOUD_TOKEN"}');

-- Default English email templates. {{placeholders}} are substituted from message variables.
INSERT INTO notification_template (id, event_type, channel_type, locale, subject, body) VALUES
    (X'c0000000000000000000000000000001', 'APPLICATION_SUBMITTED', 'email', 'en',
     'We received your {{formName}} application',
     'Thank you — we have received your {{formName}} application (reference {{reference}}). Our team will review it and keep you updated.'),
    (X'c0000000000000000000000000000002', 'APPLICATION_APPROVED', 'email', 'en',
     'Your {{formName}} application is approved',
     'Good news — your {{formName}} application (reference {{reference}}) has been approved. Thank you for banking with us.'),
    (X'c0000000000000000000000000000003', 'APPLICATION_REJECTED', 'email', 'en',
     'Update on your {{formName}} application',
     'After review, your {{formName}} application (reference {{reference}}) was not approved at this time. Please contact us if you would like more information.'),
    (X'c0000000000000000000000000000004', 'APPLICATION_NEEDS_INFO', 'email', 'en',
     'We need more information for your {{formName}} application',
     'To continue processing your {{formName}} application (reference {{reference}}) we need some additional information. Please log in to your portal to provide the requested details.');

-- WhatsApp fallbacks (subject = provider-approved template name, body = plain-text fallback).
INSERT INTO notification_template (id, event_type, channel_type, locale, subject, body) VALUES
    (X'c0000000000000000000000000000005', 'APPLICATION_SUBMITTED',  'whatsapp', 'en', 'application_received',
     'We received your {{formName}} application (ref {{reference}}). We will keep you updated.'),
    (X'c0000000000000000000000000000006', 'APPLICATION_APPROVED',   'whatsapp', 'en', 'application_approved',
     'Good news! Your {{formName}} application (ref {{reference}}) has been approved.'),
    (X'c0000000000000000000000000000007', 'APPLICATION_REJECTED',   'whatsapp', 'en', 'application_update',
     'Update: your {{formName}} application (ref {{reference}}) was not approved at this time.'),
    (X'c0000000000000000000000000000008', 'APPLICATION_NEEDS_INFO', 'whatsapp', 'en', 'application_needs_info',
     'We need more information for your {{formName}} application (ref {{reference}}). Please check your portal.');
