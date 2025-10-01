-- ===========================================
-- Flyway Migration: Create Sessions Table
-- Version: V3
-- Description: User sessions and refresh tokens management with UUIDs
-- ===========================================

-- Drop existing table to ensure clean state (ACID - Atomicity)
DROP TABLE IF EXISTS sessions CASCADE;

-- Create sessions table for refresh tokens and session management
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token_hash TEXT UNIQUE NOT NULL,
    user_agent TEXT,
    ip_address INET,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create high-performance indexes for session management
CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON sessions USING btree (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_sessions_refresh_token ON sessions USING btree (refresh_token_hash);
CREATE INDEX IF NOT EXISTS idx_sessions_expires_at ON sessions USING btree (expires_at);
CREATE INDEX IF NOT EXISTS idx_sessions_created_at ON sessions USING btree (created_at);
CREATE INDEX IF NOT EXISTS idx_sessions_ip_address ON sessions USING btree (ip_address) WHERE ip_address IS NOT NULL;

-- Add constraints for data integrity (ACID - Consistency)
DO $$
BEGIN
    -- Refresh token hash length constraint
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'chk_sessions_refresh_token_length'
                   AND table_name = 'sessions') THEN
        ALTER TABLE sessions ADD CONSTRAINT chk_sessions_refresh_token_length CHECK (LENGTH(refresh_token_hash) >= 32);
    END IF;

    -- Expires at future constraint
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'chk_sessions_expires_future'
                   AND table_name = 'sessions') THEN
        ALTER TABLE sessions ADD CONSTRAINT chk_sessions_expires_future CHECK (expires_at > created_at);
    END IF;
END $$;

-- Add table and column comments for documentation
COMMENT ON TABLE sessions IS 'User authentication sessions with refresh token management';
COMMENT ON COLUMN sessions.id IS 'UUID primary key for distributed systems compatibility';
COMMENT ON COLUMN sessions.user_id IS 'Reference to users table UUID';
COMMENT ON COLUMN sessions.refresh_token_hash IS 'Hashed refresh token for security';
COMMENT ON COLUMN sessions.user_agent IS 'Client user agent string for security tracking';
COMMENT ON COLUMN sessions.ip_address IS 'Client IP address for security tracking';
COMMENT ON COLUMN sessions.expires_at IS 'Session expiration timestamp';
COMMENT ON COLUMN sessions.created_at IS 'Session creation timestamp';
