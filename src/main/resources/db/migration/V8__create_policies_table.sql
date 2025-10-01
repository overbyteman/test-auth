-- ===========================================
-- Flyway Migration: Create Policies Table for ABAC
-- Version: V8
-- Description: Attribute-Based Access Control (ABAC) policies with high performance
-- ===========================================

-- Create policy_effect enum type (idempotent)
DROP TYPE IF EXISTS policy_effect CASCADE;
CREATE TYPE policy_effect AS ENUM ('allow', 'deny');

-- Drop existing table to ensure clean state (ACID - Atomicity)
DROP TABLE IF EXISTS policies CASCADE;

-- Create policies table for ABAC system
CREATE TABLE policies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT UNIQUE NOT NULL,
    description TEXT,
    effect policy_effect NOT NULL DEFAULT 'allow',
    actions TEXT[] NOT NULL,
    resources TEXT[] NOT NULL,
    conditions JSONB, -- ABAC rules
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create high-performance indexes for ABAC queries
CREATE UNIQUE INDEX IF NOT EXISTS uq_policies_name ON policies USING btree (LOWER(name));
CREATE INDEX IF NOT EXISTS idx_policies_effect ON policies USING btree (effect);
CREATE INDEX IF NOT EXISTS idx_policies_created_at ON policies USING btree (created_at);
CREATE INDEX IF NOT EXISTS idx_policies_actions ON policies USING gin (actions);
CREATE INDEX IF NOT EXISTS idx_policies_resources ON policies USING gin (resources);
CREATE INDEX IF NOT EXISTS idx_policies_conditions ON policies USING gin (conditions);

-- Add constraints for data integrity (ACID - Consistency)
DO $$
BEGIN
    -- Name length constraint
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'chk_policies_name_length'
                   AND table_name = 'policies') THEN
        ALTER TABLE policies ADD CONSTRAINT chk_policies_name_length CHECK (LENGTH(TRIM(name)) >= 2);
    END IF;

    -- Actions not empty constraint
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'chk_policies_actions_not_empty'
                   AND table_name = 'policies') THEN
        ALTER TABLE policies ADD CONSTRAINT chk_policies_actions_not_empty CHECK (array_length(actions, 1) > 0);
    END IF;

    -- Resources not empty constraint
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'chk_policies_resources_not_empty'
                   AND table_name = 'policies') THEN
        ALTER TABLE policies ADD CONSTRAINT chk_policies_resources_not_empty CHECK (array_length(resources, 1) > 0);
    END IF;
END $$;

-- Add table and column comments for documentation
COMMENT ON TABLE policies IS 'Attribute-Based Access Control (ABAC) policies with JSONB conditions';
COMMENT ON COLUMN policies.id IS 'UUID primary key for distributed systems compatibility';
COMMENT ON COLUMN policies.name IS 'Policy name, must be unique and minimum 2 characters';
COMMENT ON COLUMN policies.description IS 'Optional policy description for documentation';
COMMENT ON COLUMN policies.effect IS 'Policy effect: allow or deny access';
COMMENT ON COLUMN policies.actions IS 'Array of actions this policy applies to';
COMMENT ON COLUMN policies.resources IS 'Array of resources this policy applies to';
COMMENT ON COLUMN policies.conditions IS 'ABAC rules and conditions as JSONB for flexibility';
COMMENT ON COLUMN policies.created_at IS 'Policy creation timestamp';
