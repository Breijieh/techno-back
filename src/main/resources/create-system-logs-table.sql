-- ====================================================================
-- System Logs Table Migration Script
-- Creates SYSTEM_LOGS table for audit logging and system event tracking
-- ====================================================================

CREATE TABLE IF NOT EXISTS system_logs (
    log_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action_type VARCHAR(50) NOT NULL,
    module VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    log_level VARCHAR(10) NOT NULL CHECK (log_level IN ('INFO', 'WARNING', 'ERROR', 'DEBUG')),
    ip_address VARCHAR(45),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_date TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_system_logs_user FOREIGN KEY (user_id)
        REFERENCES user_accounts(user_id) ON DELETE SET NULL
);

-- Create indexes for commonly filtered/searched columns
CREATE INDEX IF NOT EXISTS idx_system_logs_user_id ON system_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_system_logs_level ON system_logs(log_level);
CREATE INDEX IF NOT EXISTS idx_system_logs_module ON system_logs(module);
CREATE INDEX IF NOT EXISTS idx_system_logs_created_date ON system_logs(created_date DESC);
CREATE INDEX IF NOT EXISTS idx_system_logs_action_type ON system_logs(action_type);

-- Index for date range queries
CREATE INDEX IF NOT EXISTS idx_system_logs_date_range ON system_logs(created_date DESC, log_level);

