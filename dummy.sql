-- Dummy seed script for manual execution
-- Run manually when you need baseline data in local/dev environments.

-- =====================================================
-- Schema guardrails for compatibility
-- Ensures required columns and indexes exist before seeding
-- =====================================================
DO $$
BEGIN
	IF NOT EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema = current_schema()
		  AND table_name = 'roles'
		  AND column_name = 'code'
	) THEN
		ALTER TABLE roles ADD COLUMN code VARCHAR(100);
		UPDATE roles
		SET code = LOWER(REGEXP_REPLACE(name, '[^a-z0-9]+', '-', 'g'))
		WHERE code IS NULL;
	END IF;
END$$;

-- Ensure all roles have non-empty codes before enforcing uniqueness
UPDATE roles
SET code = CONCAT('role-', SUBSTRING(id::text, 1, 8))
WHERE code IS NULL OR TRIM(code) = '';

-- Resolve duplicate codes within the same tenant by suffixing an ordinal
WITH duplicate_codes AS (
		SELECT id,
					 tenant_id,
					 code,
					 ROW_NUMBER() OVER (PARTITION BY tenant_id, code ORDER BY id) AS rn
		FROM roles
)
UPDATE roles r
SET code = CONCAT(r.code, '-', duplicate_codes.rn)
FROM duplicate_codes
WHERE r.id = duplicate_codes.id
	AND duplicate_codes.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uidx_roles_code_tenant ON roles (code, tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_roles_name_tenant ON roles (name, tenant_id);

-- =====================================================
-- System root tenant + Super Admin seed
-- =====================================================
INSERT INTO tenants (id, name, config, created_at, updated_at)
VALUES (
	'00000000-0000-0000-0000-000000000000',
	'System Root Tenant',
	'{"type": "system", "default": true}',
	NOW(),
	NOW()
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO roles (id, code, name, description, tenant_id)
VALUES (
	'99999999-9999-9999-9999-999999999999',
	'root-super-admin',
	'SUPER_ADMIN',
	'Global super administrator with unrestricted access',
	'00000000-0000-0000-0000-000000000000'
)
ON CONFLICT (name, tenant_id) DO NOTHING;

INSERT INTO users (id, name, email, password_hash, is_active, email_verified_at, created_at, updated_at)
-- Senha padrão: ChangeMeNow!123 (hash Argon2id gerado com PostQuantumPasswordEncoder)
VALUES (
	'aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb',
	'System Super Admin',
	'super.admin@system.local',
	'$argon2id$v=19$m=65536,t=3,p=4$UBPfdzWRImPL7316HUF3SwjezhzHk3mFcYwiYeSB364$fL5gXZFM133bNfenk0hdM8MfudaUbz0l/EcrUyMHbtbPCh/7vU6hZG6uAiTx2BdwA17epgHsq3wIUkxZr3baCw',
	true,
	NOW(),
	NOW(),
	NOW()
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO users_tenants_roles (user_id, tenant_id, role_id)
VALUES (
	'aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb',
	'00000000-0000-0000-0000-000000000000',
	'99999999-9999-9999-9999-999999999999'
)
ON CONFLICT (user_id, tenant_id, role_id) DO NOTHING;

-- Default password hint: override hash before production use.

-- Tenants
INSERT INTO tenants (id, name, config, created_at, updated_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'Acme Corp', '{"type": "enterprise", "active": true}', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO tenants (id, name, config, created_at, updated_at)
VALUES ('22222222-2222-2222-2222-222222222222', 'Beta Ltd', '{"type": "startup", "active": true}', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Roles per tenant
INSERT INTO roles (id, code, name, description, tenant_id)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'platform-admin', 'ADMIN', 'Administrator role', '11111111-1111-1111-1111-111111111111')
ON CONFLICT (code, tenant_id) DO NOTHING;

INSERT INTO roles (id, code, name, description, tenant_id)
VALUES ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'platform-user', 'USER', 'Standard user role', '11111111-1111-1111-1111-111111111111')
ON CONFLICT (code, tenant_id) DO NOTHING;

INSERT INTO roles (id, code, name, description, tenant_id)
VALUES ('c0c0c0c0-c0c0-c0c0-c0c0-c0c0c0c0c0c0', 'branch-staff', 'STAFF', 'Staff role for Beta Ltd', '22222222-2222-2222-2222-222222222222')
ON CONFLICT (code, tenant_id) DO NOTHING;

-- Policies per tenant
INSERT INTO policies (id, tenant_id, code, name, description, effect, actions, resources, conditions, created_at)
VALUES ('12121212-1212-1212-1212-121212121212', '11111111-1111-1111-1111-111111111111', 'admin-access', 'Admin Full Access', 'Full administrative access for Acme', 'ALLOW', ARRAY['create', 'read', 'update', 'delete'], ARRAY['users', 'roles', 'policies'], '{}'::jsonb, NOW())
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO policies (id, tenant_id, code, name, description, effect, actions, resources, conditions, created_at)
VALUES ('34343434-3434-3434-3434-343434343434', '22222222-2222-2222-2222-222222222222', 'staff-dashboard', 'Staff Dashboard Access', 'Restrict staff to tenant-specific dashboards', 'ALLOW', ARRAY['read'], ARRAY['dashboard'], '{}'::jsonb, NOW())
ON CONFLICT (tenant_id, code) DO NOTHING;

-- Permissions
INSERT INTO permissions (id, action, resource)
VALUES ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'read', 'users')
ON CONFLICT (action, resource) DO NOTHING;

INSERT INTO permissions (id, action, resource)
VALUES ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'update', 'users')
ON CONFLICT (action, resource) DO NOTHING;

-- Role-permission associations (with tenant-specific policies)
INSERT INTO roles_permissions (role_id, permission_id, policy_id, created_at, updated_at)
SELECT r.id, p.id, pol.id, NOW(), NOW()
FROM roles r
JOIN permissions p ON p.action = 'read' AND p.resource = 'users'
JOIN policies pol ON pol.code = 'admin-access' AND pol.tenant_id = '11111111-1111-1111-1111-111111111111'
WHERE r.code = 'platform-admin' AND r.tenant_id = '11111111-1111-1111-1111-111111111111'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO roles_permissions (role_id, permission_id, policy_id, created_at, updated_at)
SELECT r.id, p.id, NULL, NOW(), NOW()
FROM roles r
JOIN permissions p ON p.action = 'read' AND p.resource = 'users'
WHERE r.code = 'platform-user' AND r.tenant_id = '11111111-1111-1111-1111-111111111111'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO roles_permissions (role_id, permission_id, policy_id, created_at, updated_at)
SELECT r.id, p.id, pol.id, NOW(), NOW()
FROM roles r
JOIN permissions p ON p.action = 'update' AND p.resource = 'users'
JOIN policies pol ON pol.code = 'staff-dashboard' AND pol.tenant_id = '22222222-2222-2222-2222-222222222222'
WHERE r.code = 'branch-staff' AND r.tenant_id = '22222222-2222-2222-2222-222222222222'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Users
INSERT INTO users (id, name, email, password_hash, is_active, created_at, updated_at)
-- Senha padrão: ChangeMeNow!123 (hash Argon2id gerado com PostQuantumPasswordEncoder)
VALUES ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'Admin User', 'admin@acme.test', '$argon2id$v=19$m=65536,t=3,p=4$UBPfdzWRImPL7316HUF3SwjezhzHk3mFcYwiYeSB364$fL5gXZFM133bNfenk0hdM8MfudaUbz0l/EcrUyMHbtbPCh/7vU6hZG6uAiTx2BdwA17epgHsq3wIUkxZr3baCw', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, name, email, password_hash, is_active, created_at, updated_at)
-- Senha padrão: ChangeMeNow!123 (hash Argon2id gerado com PostQuantumPasswordEncoder)
VALUES ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'Regular User', 'user@acme.test', '$argon2id$v=19$m=65536,t=3,p=4$UBPfdzWRImPL7316HUF3SwjezhzHk3mFcYwiYeSB364$fL5gXZFM133bNfenk0hdM8MfudaUbz0l/EcrUyMHbtbPCh/7vU6hZG6uAiTx2BdwA17epgHsq3wIUkxZr3baCw', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- User-tenant-role associations
INSERT INTO users_tenants_roles (user_id, tenant_id, role_id)
VALUES ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', '11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa')
ON CONFLICT (user_id, tenant_id, role_id) DO NOTHING;

INSERT INTO users_tenants_roles (user_id, tenant_id, role_id)
VALUES ('ffffffff-ffff-ffff-ffff-ffffffffffff', '11111111-1111-1111-1111-111111111111', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb')
ON CONFLICT (user_id, tenant_id, role_id) DO NOTHING;

INSERT INTO users_tenants_roles (user_id, tenant_id, role_id)
VALUES ('ffffffff-ffff-ffff-ffff-ffffffffffff', '22222222-2222-2222-2222-222222222222', 'c0c0c0c0-c0c0-c0c0-c0c0-c0c0c0c0c0c0')
ON CONFLICT (user_id, tenant_id, role_id) DO NOTHING;
