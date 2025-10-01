-- ===========================================
-- Flyway Migration: Create Roles Permissions Junction Table
-- Version: V6
-- Description: Many-to-many relationship between roles and permissions
-- ===========================================

-- Drop existing table to ensure clean state (ACID - Atomicity)
DROP TABLE IF EXISTS roles_permissions CASCADE;

-- Create roles_permissions junction table for many-to-many relationship
CREATE TABLE roles_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Create high-performance indexes for junction table
CREATE INDEX IF NOT EXISTS idx_roles_permissions_role_id ON roles_permissions USING btree (role_id);
CREATE INDEX IF NOT EXISTS idx_roles_permissions_permission_id ON roles_permissions USING btree (permission_id);

-- Add table and column comments for documentation
COMMENT ON TABLE roles_permissions IS 'Junction table for many-to-many relationship between roles and permissions';
COMMENT ON COLUMN roles_permissions.role_id IS 'Reference to roles table UUID';
COMMENT ON COLUMN roles_permissions.permission_id IS 'Reference to permissions table UUID';
