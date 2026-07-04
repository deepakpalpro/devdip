-- V15: Async pipeline processing via the generic outbox_event table (US-8.2).
-- Extends the V1 placeholder so a submit transition can enqueue a PIPELINE_REQUESTED row in the
-- same durable outbox, then a @Scheduled worker runs the pipeline off the request path. The generic
-- `outbox_event` table remains available for future broker-based fan-out (US-8.2 broker seam).

ALTER TABLE outbox_event ADD COLUMN tenant_id BINARY(16);
ALTER TABLE outbox_event ADD COLUMN submission_id BINARY(16);
ALTER TABLE outbox_event ADD COLUMN attempts INT NOT NULL DEFAULT 0;
ALTER TABLE outbox_event ADD COLUMN error CLOB;
ALTER TABLE outbox_event ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX idx_outbox_submission ON outbox_event (submission_id);
