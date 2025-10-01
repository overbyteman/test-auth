-- ===========================================
-- Flyway Migration: Create Tenants Table
-- Version: V2
-- Description: Multi-tenancy support with UUIDs and high performance
-- ===========================================

-- Drop existing table to ensure clean state (ACID - Atomicity)
DROP TABLE IF EXISTS tenants CASCADE;

-- Create tenants table with optimized structure using UUIDs
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    config JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create high-performance indexes
CREATE INDEX IF NOT EXISTS idx_tenants_name ON tenants USING btree (LOWER(name));
CREATE INDEX IF NOT EXISTS idx_tenants_created_at ON tenants USING btree (created_at);
CREATE INDEX IF NOT EXISTS idx_tenants_config ON tenants USING gin (config);

-- Add constraints for data integrity (ACID - Consistency)
DO $$
BEGIN
    -- Name length constraint
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'chk_tenants_name_length'
                   AND table_name = 'tenants') THEN
        ALTER TABLE tenants ADD CONSTRAINT chk_tenants_name_length CHECK (LENGTH(TRIM(name)) >= 2);
    END IF;

    -- Name uniqueness constraint
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'uq_tenants_name'
                   AND table_name = 'tenants') THEN
        ALTER TABLE tenants ADD CONSTRAINT uq_tenants_name UNIQUE (name);
    END IF;
END $$;

-- Create high-performance trigger function for updated_at
CREATE OR REPLACE FUNCTION update_tenants_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for automatic updated_at management (idempotent)
DROP TRIGGER IF EXISTS update_tenants_updated_at ON tenants;
CREATE TRIGGER update_tenants_updated_at
    BEFORE UPDATE ON tenants
    FOR EACH ROW
    EXECUTE FUNCTION update_tenants_updated_at();

-- Add table and column comments for documentation
COMMENT ON TABLE tenants IS 'Multi-tenant organizations with JSONB configuration';
COMMENT ON COLUMN tenants.id IS 'UUID primary key for distributed systems compatibility';
COMMENT ON COLUMN tenants.name IS 'Tenant organization name, must be unique';
COMMENT ON COLUMN tenants.config IS 'Tenant-specific configuration as JSONB for flexibility';
COMMENT ON COLUMN tenants.created_at IS 'Record creation timestamp with timezone';
COMMENT ON COLUMN tenants.updated_at IS 'Record last update timestamp with timezone';
