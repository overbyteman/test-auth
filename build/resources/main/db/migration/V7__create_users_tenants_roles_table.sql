-- ===========================================
-- Flyway Migration: Create Users Tenants Roles Junction Table
-- Version: V7
-- Description: Multi-tenancy core table - users, tenants, and roles relationship
-- ===========================================

-- Drop existing table to ensure clean state (ACID - Atomicity)
DROP TABLE IF EXISTS users_tenants_roles CASCADE;

-- Create users_tenants_roles junction table (heart of multi-tenancy)
CREATE TABLE users_tenants_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, tenant_id, role_id)
);

-- Create high-performance indexes for multi-tenant queries
CREATE INDEX IF NOT EXISTS idx_users_tenants_roles_user_id ON users_tenants_roles USING btree (user_id);
CREATE INDEX IF NOT EXISTS idx_users_tenants_roles_tenant_id ON users_tenants_roles USING btree (tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_tenants_roles_role_id ON users_tenants_roles USING btree (role_id);
CREATE INDEX IF NOT EXISTS idx_users_tenants_roles_user_tenant ON users_tenants_roles USING btree (user_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_tenants_roles_tenant_role ON users_tenants_roles USING btree (tenant_id, role_id);

-- Add table and column comments for documentation
COMMENT ON TABLE users_tenants_roles IS 'Multi-tenancy core table: defines user roles within specific tenants';
COMMENT ON COLUMN users_tenants_roles.user_id IS 'Reference to users table UUID';
COMMENT ON COLUMN users_tenants_roles.tenant_id IS 'Reference to tenants table UUID';
COMMENT ON COLUMN users_tenants_roles.role_id IS 'Reference to roles table UUID';
