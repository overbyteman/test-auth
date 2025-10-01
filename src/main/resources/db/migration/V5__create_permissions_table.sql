-- ===========================================
-- Flyway Migration: Create Permissions Table
-- Version: V5
-- Description: Permissions for RBAC with UUIDs and high performance
-- ===========================================

-- Drop existing table to ensure clean state (ACID - Atomicity)
DROP TABLE IF EXISTS permissions CASCADE;

-- Create permissions table for RBAC system
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    action TEXT NOT NULL, -- e.g., create, read, update, delete
    resource TEXT NOT NULL -- e.g., articles, users
);

-- Add unique constraint for action+resource combination
ALTER TABLE permissions ADD CONSTRAINT uq_permissions_action_resource UNIQUE (action, resource);

-- Create high-performance indexes
CREATE UNIQUE INDEX IF NOT EXISTS uq_permissions_action_resource_idx ON permissions USING btree (action, resource);
CREATE INDEX IF NOT EXISTS idx_permissions_action ON permissions USING btree (action);
CREATE INDEX IF NOT EXISTS idx_permissions_resource ON permissions USING btree (resource);

-- Add constraints for data integrity (ACID - Consistency)
DO $$
BEGIN
    -- Action length constraint
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'chk_permissions_action_length'
                   AND table_name = 'permissions') THEN
        ALTER TABLE permissions ADD CONSTRAINT chk_permissions_action_length CHECK (LENGTH(TRIM(action)) >= 2);
    END IF;

    -- Resource length constraint
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'chk_permissions_resource_length'
                   AND table_name = 'permissions') THEN
        ALTER TABLE permissions ADD CONSTRAINT chk_permissions_resource_length CHECK (LENGTH(TRIM(resource)) >= 2);
    END IF;
END $$;

-- Add table and column comments for documentation
COMMENT ON TABLE permissions IS 'Permissions for Role-Based Access Control (RBAC)';
COMMENT ON COLUMN permissions.id IS 'UUID primary key for distributed systems compatibility';
COMMENT ON COLUMN permissions.action IS 'Permission action (create, read, update, delete)';
COMMENT ON COLUMN permissions.resource IS 'Resource being accessed (users, articles, etc.)';
