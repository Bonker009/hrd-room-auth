CREATE TABLE IF NOT EXISTS audit_logs (
    audit_log_id   UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    occurred_at    TIMESTAMP    NOT NULL DEFAULT now(),
    action         VARCHAR(16)  NOT NULL,
    entity_type    VARCHAR(128) NOT NULL,
    entity_id      VARCHAR(64)  NOT NULL,
    actor_id       UUID,
    request_id     VARCHAR(64),
    summary        TEXT,
    details_json   TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_occurred_at ON audit_logs (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs (actor_id);
