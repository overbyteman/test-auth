-- ===========================================
-- Flyway Migration: Create Users Table
-- Version: V1
-- Description: Initial schema for users management with UUID and high performance
-- ===========================================

-- Enable UUID extension for high performance UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Drop existing table to ensure clean state (ACID - Atomicity)
DROP TABLE IF EXISTS users CASCADE;

-- Create users table with optimized structure using UUIDs
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    email_verification_token TEXT,
    email_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create high-performance indexes
CREATE INDEX IF NOT EXISTS idx_users_name ON users USING btree (LOWER(name));
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email ON users USING btree (LOWER(email));
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users USING btree (created_at);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users USING btree (is_active);
CREATE INDEX IF NOT EXISTS idx_users_email_verified_at ON users USING btree (email_verified_at) WHERE email_verified_at IS NOT NULL;

-- Add constraints for data integrity (ACID - Consistency)
DO $$
BEGIN
    -- Name length constraint
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'chk_users_name_length'
                   AND table_name = 'users') THEN
        ALTER TABLE users ADD CONSTRAINT chk_users_name_length CHECK (LENGTH(TRIM(name)) >= 2);
    END IF;

    -- Email format constraint
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'chk_users_email_format'
                   AND table_name = 'users') THEN
        ALTER TABLE users ADD CONSTRAINT chk_users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');
    END IF;

    -- Password hash length constraint
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'chk_users_password_hash_length'
                   AND table_name = 'users') THEN
        ALTER TABLE users ADD CONSTRAINT chk_users_password_hash_length CHECK (LENGTH(password_hash) >= 60);
    END IF;
END $$;

-- Create high-performance trigger function for updated_at
CREATE OR REPLACE FUNCTION update_users_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for automatic updated_at management (idempotent)
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_users_updated_at();

-- Add table and column comments for documentation
COMMENT ON TABLE users IS 'Users table with UUID primary keys for high performance';
COMMENT ON COLUMN users.id IS 'UUID primary key for distributed systems compatibility';
COMMENT ON COLUMN users.name IS 'User full name, minimum 2 characters';
COMMENT ON COLUMN users.email IS 'User email address, must be unique and valid format';
COMMENT ON COLUMN users.password_hash IS 'Hashed user password using bcrypt';
COMMENT ON COLUMN users.is_active IS 'User activation status';
COMMENT ON COLUMN users.email_verification_token IS 'Token for email verification process';
COMMENT ON COLUMN users.email_verified_at IS 'Timestamp when email was verified';
COMMENT ON COLUMN users.created_at IS 'Record creation timestamp with timezone';
COMMENT ON COLUMN users.updated_at IS 'Record last update timestamp with timezone';
