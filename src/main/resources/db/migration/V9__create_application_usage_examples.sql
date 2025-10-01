-- ===========================================
-- Flyway Migration: High-Performance Query Functions and Usage Examples
-- Version: V9
-- Description: High-performance query functions and usage examples for UUID-based system
-- ===========================================

-- ===========================================
-- HIGH-PERFORMANCE QUERY FUNCTIONS
-- ===========================================

-- Function to get user permissions within a tenant (high-performance multi-tenant query)
CREATE OR REPLACE FUNCTION get_user_permissions_in_tenant(
    p_user_id UUID,
    p_tenant_id UUID
) RETURNS TABLE (
    permission_id UUID,
    action TEXT,
    resource TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT DISTINCT
        p.id,
        p.action,
        p.resource
    FROM users_tenants_roles utr
    JOIN roles_permissions rp ON utr.role_id = rp.role_id
    JOIN permissions p ON rp.permission_id = p.id
    WHERE utr.user_id = p_user_id
      AND utr.tenant_id = p_tenant_id;
END;
$$ LANGUAGE plpgsql STABLE;

-- Function to check if user has specific permission in tenant (optimized for frequent checks)
CREATE OR REPLACE FUNCTION user_has_permission_in_tenant(
    p_user_id UUID,
    p_tenant_id UUID,
    p_action TEXT,
    p_resource TEXT
) RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1
        FROM users_tenants_roles utr
        JOIN roles_permissions rp ON utr.role_id = rp.role_id
        JOIN permissions p ON rp.permission_id = p.id
        WHERE utr.user_id = p_user_id
          AND utr.tenant_id = p_tenant_id
          AND p.action = p_action
          AND p.resource = p_resource
    );
END;
$$ LANGUAGE plpgsql STABLE;

-- Function to get all tenants for a user with their roles
CREATE OR REPLACE FUNCTION get_user_tenants_with_roles(
    p_user_id UUID
) RETURNS TABLE (
    tenant_id UUID,
    tenant_name TEXT,
    role_id UUID,
    role_name TEXT,
    role_description TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        t.id,
        t.name,
        r.id,
        r.name,
        r.description
    FROM users_tenants_roles utr
    JOIN tenants t ON utr.tenant_id = t.id
    JOIN roles r ON utr.role_id = r.id
    WHERE utr.user_id = p_user_id
    ORDER BY t.name, r.name;
END;
$$ LANGUAGE plpgsql STABLE;

-- Function for ABAC policy evaluation (high-performance policy matching)
CREATE OR REPLACE FUNCTION evaluate_abac_policies(
    p_action TEXT,
    p_resource TEXT,
    p_context JSONB DEFAULT '{}'::JSONB
) RETURNS TABLE (
    policy_id UUID,
    policy_name TEXT,
    effect policy_effect,
    conditions_match BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        pol.id,
        pol.name,
        pol.effect,
        CASE
            WHEN pol.conditions IS NULL THEN TRUE
            WHEN pol.conditions @> p_context THEN TRUE
            ELSE FALSE
        END as conditions_match
    FROM policies pol
    WHERE p_action = ANY(pol.actions)
      AND p_resource = ANY(pol.resources)
    ORDER BY pol.effect DESC; -- 'deny' comes before 'allow'
END;
$$ LANGUAGE plpgsql STABLE;

-- Function to cleanup expired sessions (for scheduled maintenance)
CREATE OR REPLACE FUNCTION cleanup_expired_sessions()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM sessions WHERE expires_at <= NOW();
    GET DIAGNOSTICS deleted_count = ROW_COUNT;

    -- Log cleanup activity
    RAISE NOTICE 'Cleaned up % expired sessions at %', deleted_count, NOW();

    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- ===========================================
-- USAGE EXAMPLES AND SAMPLE DATA
-- ===========================================

-- Insert some sample roles and permissions for testing
DO $$
DECLARE
    admin_role_id UUID;
    user_role_id UUID;
    create_users_perm_id UUID;
    read_users_perm_id UUID;
    update_users_perm_id UUID;
    delete_users_perm_id UUID;
BEGIN
    -- Insert sample roles (case-insensitive uniqueness em LOWER(name))
    INSERT INTO roles (name, description)
    SELECT 'ADMIN', 'Administrator with full access'
    WHERE NOT EXISTS (
        SELECT 1 FROM roles WHERE LOWER(name) = LOWER('ADMIN')
    )
    RETURNING id INTO admin_role_id;

    IF admin_role_id IS NULL THEN
        SELECT id INTO admin_role_id FROM roles WHERE LOWER(name) = 'admin';
        -- Atualiza descrição se quiser manter consistência
        UPDATE roles SET description = 'Administrator with full access' WHERE id = admin_role_id;
    END IF;

    INSERT INTO roles (name, description)
    SELECT 'USER', 'Regular user with limited access'
    WHERE NOT EXISTS (
        SELECT 1 FROM roles WHERE LOWER(name) = LOWER('USER')
    )
    RETURNING id INTO user_role_id;

    IF user_role_id IS NULL THEN
        SELECT id INTO user_role_id FROM roles WHERE LOWER(name) = 'user';
        UPDATE roles SET description = 'Regular user with limited access' WHERE id = user_role_id;
    END IF;

    -- Permissions (possui UNIQUE constraint (action, resource), então ON CONFLICT é válido)
    INSERT INTO permissions (action, resource)
    VALUES ('create', 'users')
    ON CONFLICT (action, resource) DO NOTHING
    RETURNING id INTO create_users_perm_id;

    INSERT INTO permissions (action, resource)
    VALUES ('read', 'users')
    ON CONFLICT (action, resource) DO NOTHING
    RETURNING id INTO read_users_perm_id;

    INSERT INTO permissions (action, resource)
    VALUES ('update', 'users')
    ON CONFLICT (action, resource) DO NOTHING
    RETURNING id INTO update_users_perm_id;

    INSERT INTO permissions (action, resource)
    VALUES ('delete', 'users')
    ON CONFLICT (action, resource) DO NOTHING
    RETURNING id INTO delete_users_perm_id;

    -- Garantir IDs caso já existissem
    SELECT id INTO create_users_perm_id FROM permissions WHERE action='create' AND resource='users';
    SELECT id INTO read_users_perm_id FROM permissions WHERE action='read' AND resource='users';
    SELECT id INTO update_users_perm_id FROM permissions WHERE action='update' AND resource='users';
    SELECT id INTO delete_users_perm_id FROM permissions WHERE action='delete' AND resource='users';

    -- Relacionamentos role -> permissions
    INSERT INTO roles_permissions (role_id, permission_id)
    VALUES
        (admin_role_id, create_users_perm_id),
        (admin_role_id, read_users_perm_id),
        (admin_role_id, update_users_perm_id),
        (admin_role_id, delete_users_perm_id),
        (user_role_id, read_users_perm_id)
    ON CONFLICT (role_id, permission_id) DO NOTHING;

    RAISE NOTICE 'Sample roles and permissions created successfully (idempotent).';
END $$;

-- Add comments for all functions
COMMENT ON FUNCTION get_user_permissions_in_tenant IS 'High-performance function to get all user permissions within a specific tenant';
COMMENT ON FUNCTION user_has_permission_in_tenant IS 'Optimized function for frequent permission checks in multi-tenant context';
COMMENT ON FUNCTION get_user_tenants_with_roles IS 'Get all tenants accessible to user with their assigned roles';
COMMENT ON FUNCTION evaluate_abac_policies IS 'High-performance ABAC policy evaluation with JSONB context matching';
COMMENT ON FUNCTION cleanup_expired_sessions IS 'Maintenance function to remove expired sessions and free up space';
