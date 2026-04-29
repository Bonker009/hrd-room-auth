ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS request_path VARCHAR(1024);

CREATE INDEX IF NOT EXISTS idx_audit_logs_request_path ON audit_logs (request_path);
