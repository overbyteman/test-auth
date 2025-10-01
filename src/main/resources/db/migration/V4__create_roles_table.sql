-- ===========================================
-- Flyway Migration: Create Roles Table
-- Version: V4
-- Description: User roles for RBAC with UUIDs and high performance
-- ===========================================

-- Drop existing table to ensure clean state (ACID - Atomicity)
DROP TABLE IF EXISTS roles CASCADE;

-- Create roles table for RBAC system
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT UNIQUE NOT NULL,
    description TEXT
);

-- Create high-performance indexes
CREATE UNIQUE INDEX IF NOT EXISTS uq_roles_name ON roles USING btree (LOWER(name));
CREATE INDEX IF NOT EXISTS idx_roles_name_text ON roles USING btree (name text_pattern_ops);

-- Add constraints for data integrity (ACID - Consistency)
DO $$
BEGIN
    -- Name length constraint
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'chk_roles_name_length'
                   AND table_name = 'roles') THEN
        ALTER TABLE roles ADD CONSTRAINT chk_roles_name_length CHECK (LENGTH(TRIM(name)) >= 2);
    END IF;
END $$;

-- Add table and column comments for documentation
COMMENT ON TABLE roles IS 'User roles for Role-Based Access Control (RBAC)';
COMMENT ON COLUMN roles.id IS 'UUID primary key for distributed systems compatibility';
COMMENT ON COLUMN roles.name IS 'Role name, must be unique and minimum 2 characters';
COMMENT ON COLUMN roles.description IS 'Optional role description for documentation';
