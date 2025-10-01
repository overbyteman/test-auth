-- ===========================================
-- Flyway Migration: Create Users Table
-- Version: V1
-- Description: Initial schema for users management
-- ===========================================

-- Create users table with optimized structure
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Create indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_users_name ON users (LOWER(name));
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email ON users (LOWER(email));
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users (created_at);
CREATE INDEX IF NOT EXISTS idx_users_active ON users (active);

-- Add constraints for data integrity
ALTER TABLE users ADD CONSTRAINT chk_users_name_length CHECK (LENGTH(TRIM(name)) >= 2);
ALTER TABLE users ADD CONSTRAINT chk_users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- Create function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger for automatic updated_at and version management
CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE users IS 'Users table for the application';
COMMENT ON COLUMN users.id IS 'Primary key, auto-generated';
COMMENT ON COLUMN users.name IS 'User full name, minimum 2 characters';
COMMENT ON COLUMN users.email IS 'User email address, must be unique and valid format';
COMMENT ON COLUMN users.created_at IS 'Record creation timestamp with timezone';
COMMENT ON COLUMN users.updated_at IS 'Record last update timestamp with timezone';
COMMENT ON COLUMN users.version IS 'Optimistic locking version number';
COMMENT ON COLUMN users.active IS 'Soft delete flag, TRUE for active users';

