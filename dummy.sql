-- Dummy seed script for manual execution
-- Run manually when you need baseline data in local/dev environments.

-- =====================================================
-- Schema guardrails for compatibility
-- Ensures required columns and indexes exist before seeding
-- =====================================================
DO $$
	DECLARE
		has_code_column BOOLEAN;
		has_tenant_column BOOLEAN;
		has_landlord_column BOOLEAN;
BEGIN
	SELECT EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema = current_schema()
		  AND table_name = 'roles'
		  AND column_name = 'code'
	) INTO has_code_column;

	IF NOT has_code_column THEN
		EXECUTE 'ALTER TABLE roles ADD COLUMN code VARCHAR(100)';
		EXECUTE $guard_update_code$
			UPDATE roles
			SET code = LOWER(REGEXP_REPLACE(name, '[^a-z0-9]+', '-', 'g'))
			WHERE code IS NULL
		$guard_update_code$;
		has_code_column := TRUE;
	END IF;

	SELECT EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema = current_schema()
		  AND table_name = 'roles'
		  AND column_name = 'tenant_id'
	) INTO has_tenant_column;

	SELECT EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema = current_schema()
		  AND table_name = 'roles'
		  AND column_name = 'landlord_id'
	) INTO has_landlord_column;

	IF has_code_column THEN
		EXECUTE $guard_fill_codes$
			WITH missing_codes AS (
				SELECT id,
				       CONCAT('role-', SUBSTRING(id::text, 1, 8)) AS new_code
				FROM roles
				WHERE COALESCE(TRIM(code), '') = ''
			)
			UPDATE roles AS r
			SET code = missing_codes.new_code
			FROM missing_codes
			WHERE r.id = missing_codes.id
		$guard_fill_codes$;

		IF has_landlord_column THEN
			-- If a prior global or tenant-scoped unique index exists, drop it to avoid conflicts
			-- when moving to landlord-scoped uniqueness. This makes the guardrail idempotent
			-- across schema migrations where older indexes might remain.
			IF EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relkind = 'i' AND c.relname = 'uidx_roles_code') THEN
				EXECUTE 'DROP INDEX IF EXISTS uidx_roles_code';
			END IF;
			IF EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relkind = 'i' AND c.relname = 'uidx_roles_name') THEN
				EXECUTE 'DROP INDEX IF EXISTS uidx_roles_name';
			END IF;
			IF EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relkind = 'i' AND c.relname = 'uidx_roles_code_tenant') THEN
				EXECUTE 'DROP INDEX IF EXISTS uidx_roles_code_tenant';
			END IF;
			IF EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relkind = 'i' AND c.relname = 'uidx_roles_name_tenant') THEN
				EXECUTE 'DROP INDEX IF EXISTS uidx_roles_name_tenant';
			END IF;
		
			EXECUTE $guard_dedupe_landlord$
				WITH duplicate_codes AS (
					SELECT id,
					       landlord_id,
					       code,
					       ROW_NUMBER() OVER (PARTITION BY landlord_id, code ORDER BY id) AS rn
					FROM roles
					WHERE COALESCE(TRIM(code), '') <> ''
				),
				renamed AS (
					SELECT id,
					       CONCAT(code, '-', rn) AS new_code
					FROM duplicate_codes
					WHERE rn > 1
				)
				UPDATE roles AS r
				SET code = renamed.new_code
				FROM renamed
				WHERE r.id = renamed.id
			$guard_dedupe_landlord$;

				-- Ensure landlord-scoped unique indexes exist (idempotent)
				EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uidx_roles_code_landlord ON roles (code, landlord_id)';
				EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uidx_roles_name_landlord ON roles (name, landlord_id)';
				-- Ensure tenant/global indexes are not recreated when landlord scoping is active
				-- (they were dropped above if present). No further action needed.
		ELSIF has_tenant_column THEN
			EXECUTE $guard_dedupe_with_tenant$
				WITH duplicate_codes AS (
					SELECT id,
					       tenant_id,
					       code,
					       ROW_NUMBER() OVER (PARTITION BY tenant_id, code ORDER BY id) AS rn
					FROM roles
					WHERE COALESCE(TRIM(code), '') <> ''
				),
				renamed AS (
					SELECT id,
					       CONCAT(code, '-', rn) AS new_code
					FROM duplicate_codes
					WHERE rn > 1
				)
				UPDATE roles AS r
				SET code = renamed.new_code
				FROM renamed
				WHERE r.id = renamed.id
			$guard_dedupe_with_tenant$;

			EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uidx_roles_code_tenant ON roles (code, tenant_id)';
			EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uidx_roles_name_tenant ON roles (name, tenant_id)';
		ELSE
			EXECUTE $guard_dedupe_global$
				WITH duplicate_codes AS (
					SELECT id,
					       code,
					       ROW_NUMBER() OVER (PARTITION BY code ORDER BY id) AS rn
					FROM roles
					WHERE COALESCE(TRIM(code), '') <> ''
				),
				renamed AS (
					SELECT id,
					       CONCAT(code, '-', rn) AS new_code
					FROM duplicate_codes
					WHERE rn > 1
				)
				UPDATE roles AS r
				SET code = renamed.new_code
				FROM renamed
				WHERE r.id = renamed.id
			$guard_dedupe_global$;

			EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uidx_roles_code ON roles (code)';
			EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uidx_roles_name ON roles (name)';
		END IF;
	END IF;
END$$;

-- =====================================================
-- Landlords (Matriz controladora)
-- =====================================================

INSERT INTO landlords (id, name, config, created_at, updated_at)
VALUES
	('00000000-0000-0000-0000-000000000000', 'System Root Landlord', '{"scope": "system", "default": true}', NOW(), NOW()),
	('11111111-aaaa-bbbb-cccc-111111111111', 'Consórcio Dragão Dourado', '{"brand": "Dragão Dourado", "segment": "martial-arts"}', NOW(), NOW()),
	('66666666-aaaa-bbbb-cccc-222222222222', 'Holding Tigre Escarlate', '{"brand": "Tigre Escarlate", "network": true}', NOW(), NOW()),
	('bbbbbbbb-aaaa-bbbb-cccc-333333333333', 'Grupo Lótus Azul', '{"brand": "Lótus Azul", "tier": "premium"}', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- =====================================================
-- System root tenant + Super Admin seed
-- =====================================================
INSERT INTO tenants (id, name, config, is_active, landlord_id, created_at, updated_at)
VALUES (
	'00000000-0000-0000-0000-000000000000',
	'System Root Tenant',
	'{"type": "system", "default": true}',
 	true,
	'00000000-0000-0000-0000-000000000000',
	NOW(),
	NOW()
)
ON CONFLICT DO NOTHING;

INSERT INTO roles (id, code, name, description, landlord_id)
VALUES (
	'99999999-9999-9999-9999-999999999999',
	'root-super-admin',
	'SUPER_ADMIN',
	'Global super administrator with unrestricted access',
	'00000000-0000-0000-0000-000000000000'
)
ON CONFLICT DO NOTHING;

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
ON CONFLICT DO NOTHING;

INSERT INTO users_tenants_roles (user_id, tenant_id, role_id)
VALUES (
	'aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb',
	'00000000-0000-0000-0000-000000000000',
	'99999999-9999-9999-9999-999999999999'
)
ON CONFLICT DO NOTHING;

-- Default password hint: override hash before production use.

-- =====================================================
-- Martial Arts Academies (Tenants)
-- =====================================================

INSERT INTO tenants (id, name, config, is_active, landlord_id, created_at, updated_at)
VALUES
	('11111111-2222-3333-4444-555555555555', 'Academia Dragão Dourado', '{"type": "academy", "mode": "solo", "slug": "dragao-dourado"}', true, '11111111-aaaa-bbbb-cccc-111111111111', NOW(), NOW()),
	('66666666-7777-8888-9999-aaaaaaaaaaaa', 'Rede Tigre Escarlate', '{"type": "academy", "mode": "network", "slug": "tigre-escarlate", "branches": [{"code": "zona-sul", "name": "Unidade Zona Sul"}]}', true, '66666666-aaaa-bbbb-cccc-222222222222', NOW(), NOW()),
	('bbbbbbbb-cccc-dddd-eeee-ffffffffffff', 'Clube Lótus Azul', '{"type": "academy", "mode": "premium", "slug": "lotus-azul"}', true, 'bbbbbbbb-aaaa-bbbb-cccc-333333333333', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- =====================================================
-- Roles per tenant aligned with hierarchy
-- =====================================================

INSERT INTO roles (id, code, name, description, landlord_id)
VALUES
	('11111111-2222-3333-4444-000000000001', 'owner', 'PROPRIETARIO', 'Controle total sobre todas as operações da academia', '11111111-aaaa-bbbb-cccc-111111111111'),
	('11111111-2222-3333-4444-000000000002', 'general_manager', 'GERENTE_GERAL', 'Supervisiona operações diárias e filiais', '11111111-aaaa-bbbb-cccc-111111111111'),
	('11111111-2222-3333-4444-000000000003', 'financial_manager', 'GERENTE_FINANCEIRO', 'Gestão financeira e relatórios', '11111111-aaaa-bbbb-cccc-111111111111'),
	('11111111-2222-3333-4444-000000000004', 'head_instructor', 'INSTRUTOR_CHEFE', 'Coordena o time técnico e metodologia', '11111111-aaaa-bbbb-cccc-111111111111'),
	('11111111-2222-3333-4444-000000000005', 'instructor', 'INSTRUTOR', 'Ministra aulas e acompanha alunos', '11111111-aaaa-bbbb-cccc-111111111111'),
	('11111111-2222-3333-4444-000000000006', 'receptionist', 'RECEPCIONISTA', 'Atendimento, agendamentos e suporte', '11111111-aaaa-bbbb-cccc-111111111111'),
	('11111111-2222-3333-4444-000000000007', 'admin_assistant', 'ASSISTENTE_ADMINISTRATIVO', 'Suporte administrativo às operações', '11111111-aaaa-bbbb-cccc-111111111111'),
	('11111111-2222-3333-4444-000000000008', 'equipment_technician', 'TECNICO_EQUIPAMENTOS', 'Manutenção e controle de equipamentos', '11111111-aaaa-bbbb-cccc-111111111111'),
	('11111111-2222-3333-4444-000000000009', 'security', 'SEGURANCA', 'Controle de acesso e segurança física', '11111111-aaaa-bbbb-cccc-111111111111'),
	('11111111-2222-3333-4444-00000000000a', 'vip_member', 'MEMBRO_VIP', 'Aluno com benefícios e agenda flexível', '11111111-aaaa-bbbb-cccc-111111111111'),
	('11111111-2222-3333-4444-00000000000b', 'regular_member', 'MEMBRO_REGULAR', 'Aluno com acesso padrão às aulas', '11111111-aaaa-bbbb-cccc-111111111111'),
	('66666666-7777-8888-9999-000000000001', 'owner', 'PROPRIETARIO', 'Controle total sobre todas as operações da rede', '66666666-aaaa-bbbb-cccc-222222222222'),
	('66666666-7777-8888-9999-000000000002', 'general_manager', 'GERENTE_GERAL', 'Responsável pelas unidades e resultados da rede', '66666666-aaaa-bbbb-cccc-222222222222'),
	('66666666-7777-8888-9999-000000000003', 'financial_manager', 'GERENTE_FINANCEIRO', 'Controle de finanças, contratos e repasses', '66666666-aaaa-bbbb-cccc-222222222222'),
	('66666666-7777-8888-9999-000000000004', 'head_instructor', 'INSTRUTOR_CHEFE', 'Coordena metodologia unificada da rede', '66666666-aaaa-bbbb-cccc-222222222222'),
	('66666666-7777-8888-9999-000000000005', 'instructor', 'INSTRUTOR', 'Ministra aulas nas unidades da rede', '66666666-aaaa-bbbb-cccc-222222222222'),
	('66666666-7777-8888-9999-000000000006', 'receptionist', 'RECEPCIONISTA', 'Atendimento e cobrança nas unidades', '66666666-aaaa-bbbb-cccc-222222222222'),
	('66666666-7777-8888-9999-000000000007', 'admin_assistant', 'ASSISTENTE_ADMINISTRATIVO', 'Suporte aos gestores de filial', '66666666-aaaa-bbbb-cccc-222222222222'),
	('66666666-7777-8888-9999-000000000008', 'equipment_technician', 'TECNICO_EQUIPAMENTOS', 'Inventário e manutenção compartilhada', '66666666-aaaa-bbbb-cccc-222222222222'),
	('66666666-7777-8888-9999-000000000009', 'security', 'SEGURANCA', 'Segurança e controle de acesso das unidades', '66666666-aaaa-bbbb-cccc-222222222222'),
	('66666666-7777-8888-9999-00000000000a', 'vip_member', 'MEMBRO_VIP', 'Aluno premium com acesso a todas as unidades', '66666666-aaaa-bbbb-cccc-222222222222'),
	('66666666-7777-8888-9999-00000000000b', 'regular_member', 'MEMBRO_REGULAR', 'Aluno com plano tradicional da rede', '66666666-aaaa-bbbb-cccc-222222222222'),
	('bbbbbbbb-cccc-dddd-eeee-000000000001', 'owner', 'PROPRIETARIO', 'Controle total sobre a academia boutique', 'bbbbbbbb-aaaa-bbbb-cccc-333333333333'),
	('bbbbbbbb-cccc-dddd-eeee-000000000002', 'general_manager', 'GERENTE_GERAL', 'Gestão da experiência premium dos alunos', 'bbbbbbbb-aaaa-bbbb-cccc-333333333333'),
	('bbbbbbbb-cccc-dddd-eeee-000000000003', 'financial_manager', 'GERENTE_FINANCEIRO', 'Planejamento financeiro e parcerias', 'bbbbbbbb-aaaa-bbbb-cccc-333333333333'),
	('bbbbbbbb-cccc-dddd-eeee-000000000004', 'head_instructor', 'INSTRUTOR_CHEFE', 'Curadoria técnica e eventos exclusivos', 'bbbbbbbb-aaaa-bbbb-cccc-333333333333'),
	('bbbbbbbb-cccc-dddd-eeee-000000000005', 'instructor', 'INSTRUTOR', 'Mentoria personalizada aos alunos', 'bbbbbbbb-aaaa-bbbb-cccc-333333333333'),
	('bbbbbbbb-cccc-dddd-eeee-000000000006', 'receptionist', 'RECEPCIONISTA', 'Atendimento personalizado aos membros', 'bbbbbbbb-aaaa-bbbb-cccc-333333333333'),
	('bbbbbbbb-cccc-dddd-eeee-000000000007', 'admin_assistant', 'ASSISTENTE_ADMINISTRATIVO', 'Operações administrativas e concierge', 'bbbbbbbb-aaaa-bbbb-cccc-333333333333'),
	('bbbbbbbb-cccc-dddd-eeee-000000000008', 'equipment_technician', 'TECNICO_EQUIPAMENTOS', 'Cuidados com equipamentos premium', 'bbbbbbbb-aaaa-bbbb-cccc-333333333333'),
	('bbbbbbbb-cccc-dddd-eeee-000000000009', 'security', 'SEGURANCA', 'Controle de acesso vip e segurança', 'bbbbbbbb-aaaa-bbbb-cccc-333333333333'),
	('bbbbbbbb-cccc-dddd-eeee-00000000000a', 'vip_member', 'MEMBRO_VIP', 'Aluno premium com serviços personalizados', 'bbbbbbbb-aaaa-bbbb-cccc-333333333333'),
	('bbbbbbbb-cccc-dddd-eeee-00000000000b', 'regular_member', 'MEMBRO_REGULAR', 'Aluno com pacote essencial', 'bbbbbbbb-aaaa-bbbb-cccc-333333333333')
ON CONFLICT DO NOTHING;

-- =====================================================
-- Policies per tenant
-- =====================================================

INSERT INTO policies (id, tenant_id, code, name, description, effect, actions, resources, conditions, created_at)
VALUES
	('11111111-2222-3333-4444-aaaa00000001', '11111111-2222-3333-4444-555555555555', 'admin-full-access', 'Admin Full Access', 'Controle total de todas as áreas da academia', 'ALLOW', ARRAY['create', 'read', 'update', 'delete', 'manage'], ARRAY['members', 'classes', 'payments', 'equipment', 'competitions', 'reports', 'settings', 'users', 'permissions'], '{"mfa_required": true, "device_posture": "managed", "allowed_ip_ranges": ["10.0.0.0/16", "192.168.0.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"], "start": "00:00", "end": "23:59"}]}}'::jsonb, NOW()),
	('11111111-2222-3333-4444-aaaa00000002', '11111111-2222-3333-4444-555555555555', 'management-access', 'Management Access', 'Gestão executiva sem acesso a configurações críticas', 'ALLOW', ARRAY['create', 'read', 'update', 'delete'], ARRAY['members', 'classes', 'payments', 'competitions', 'reports', 'equipment', 'settings', 'users', 'permissions'], '{"mfa_required": true, "allowed_ip_ranges": ["10.0.10.0/24", "10.0.20.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"], "start": "07:00", "end": "22:00"}]}, "risk_level": "medium"}'::jsonb, NOW()),
	('11111111-2222-3333-4444-aaaa00000003', '11111111-2222-3333-4444-555555555555', 'financial-access', 'Financial Access', 'Gestão de cobranças e finanças', 'ALLOW', ARRAY['create', 'read', 'update'], ARRAY['payments', 'invoices', 'financial_reports', 'members', 'classes', 'equipment', 'competitions'], '{"allowed_ip_ranges": ["10.0.30.0/24", "172.16.30.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"], "start": "08:00", "end": "18:30"}]}, "requires_dual_approval": true}'::jsonb, NOW()),
	('11111111-2222-3333-4444-aaaa00000004', '11111111-2222-3333-4444-555555555555', 'instructor-access', 'Instructor Access', 'Acesso pedagógico aos dados de alunos', 'ALLOW', ARRAY['create', 'read', 'update'], ARRAY['members', 'classes', 'competitions'], '{"department": "technical"}'::jsonb, NOW()),
	('11111111-2222-3333-4444-aaaa00000005', '11111111-2222-3333-4444-555555555555', 'reception-access', 'Reception Access', 'Atendimento e cadastros de alunos', 'ALLOW', ARRAY['create', 'read'], ARRAY['members', 'classes', 'payments', 'schedules', 'basic_reports', 'competitions'], '{"department": "frontdesk", "allowed_ip_ranges": ["10.0.50.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"], "start": "07:00", "end": "21:30"}]}}'::jsonb, NOW()),
	('11111111-2222-3333-4444-aaaa00000006', '11111111-2222-3333-4444-555555555555', 'operations-access', 'Operations Access', 'Suporte administrativo interno', 'ALLOW', ARRAY['create', 'read', 'update'], ARRAY['members', 'classes', 'equipment', 'payments', 'competitions'], '{"department": "operations", "allowed_ip_ranges": ["10.0.40.0/24", "172.16.20.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"], "start": "06:00", "end": "22:00"}]}}'::jsonb, NOW()),
	('11111111-2222-3333-4444-aaaa00000007', '11111111-2222-3333-4444-555555555555', 'equipment-maintenance', 'Equipment Maintenance', 'Manutenção e estoque de equipamentos', 'ALLOW', ARRAY['create', 'read', 'update', 'delete'], ARRAY['equipment', 'inventory'], '{"department": "maintenance", "safety_training_required": true, "allowed_ip_ranges": ["10.0.70.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"], "start": "08:00", "end": "18:00"}]}}'::jsonb, NOW()),
	('11111111-2222-3333-4444-aaaa00000008', '11111111-2222-3333-4444-555555555555', 'security-access', 'Security Access', 'Controle de acesso e incidentes', 'ALLOW', ARRAY['read'], ARRAY['members', 'classes', 'equipment', 'competitions', 'access_logs', 'facilities'], '{"department": "security", "mfa_required": true, "monitoring_level": "critical", "allowed_ip_ranges": ["10.0.60.0/24", "172.16.40.0/24"], "allowed_schedule": {"timezone": "UTC", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"], "start": "00:00", "end": "23:59"}]}}'::jsonb, NOW()),
	('11111111-2222-3333-4444-aaaa00000009', '11111111-2222-3333-4444-555555555555', 'member-vip-access', 'Member VIP Access', 'Benefícios de membros VIP', 'ALLOW', ARRAY['read'], ARRAY['members', 'classes', 'competitions', 'perks'], '{"tier": "vip", "allowed_ip_ranges": ["0.0.0.0/0"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"], "start": "06:00", "end": "23:00"}]}}'::jsonb, NOW()),
	('11111111-2222-3333-4444-aaaa0000000a', '11111111-2222-3333-4444-555555555555', 'member-basic-access', 'Member Basic Access', 'Acesso essencial dos alunos regulares', 'ALLOW', ARRAY['read'], ARRAY['members', 'classes', 'competitions'], '{"tier": "regular", "allowed_ip_ranges": ["0.0.0.0/0"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"], "start": "06:00", "end": "22:00"}]}}'::jsonb, NOW()),
	('11111111-2222-3333-4444-aaaa0000000b', '11111111-2222-3333-4444-555555555555', 'after-hours-support', 'After Hours Support', 'Suporte emergencial fora do expediente', 'ALLOW', ARRAY['read', 'update'], ARRAY['members', 'classes', 'competitions', 'reports'], '{"allowed_ip_ranges": ["10.0.80.0/24"], "incident_priority": ["P0", "P1"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"], "start": "18:00", "end": "23:59"}, {"days": ["SATURDAY", "SUNDAY"], "start": "08:00", "end": "22:00"}]}}'::jsonb, NOW()),
	('11111111-2222-3333-4444-aaaa0000000c', '11111111-2222-3333-4444-555555555555', 'trusted-network-access', 'Trusted Network Access', 'Acesso restrito a redes corporativas confiáveis', 'ALLOW', ARRAY['read'], ARRAY['reports', 'payments', 'members'], '{"mfa_required": true, "device_posture": "corporate-managed", "geo_restrictions": ["BR", "PT"], "allowed_ip_ranges": ["203.0.113.0/24", "198.51.100.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"], "start": "07:00", "end": "20:00"}]}}'::jsonb, NOW()),
	('66666666-7777-8888-9999-aaaa00000001', '66666666-7777-8888-9999-aaaaaaaaaaaa', 'admin-full-access', 'Admin Full Access', 'Controle total sobre todas as unidades da rede', 'ALLOW', ARRAY['create', 'read', 'update', 'delete', 'manage'], ARRAY['members', 'classes', 'payments', 'equipment', 'competitions', 'reports', 'settings', 'users', 'permissions'], '{"mfa_required": true, "device_posture": "managed", "allowed_ip_ranges": ["10.0.0.0/16", "192.168.0.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"], "start": "00:00", "end": "23:59"}]}}'::jsonb, NOW()),
	('66666666-7777-8888-9999-aaaa00000002', '66666666-7777-8888-9999-aaaaaaaaaaaa', 'management-access', 'Management Access', 'Gestão das filiais e rotinas operacionais', 'ALLOW', ARRAY['create', 'read', 'update', 'delete'], ARRAY['members', 'classes', 'payments', 'competitions', 'reports', 'equipment', 'settings', 'users', 'permissions'], '{"mfa_required": true, "allowed_ip_ranges": ["10.0.10.0/24", "10.0.20.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"], "start": "07:00", "end": "22:00"}]}, "risk_level": "medium"}'::jsonb, NOW()),
	('66666666-7777-8888-9999-aaaa00000003', '66666666-7777-8888-9999-aaaaaaaaaaaa', 'financial-access', 'Financial Access', 'Controle financeiro consolidado da rede', 'ALLOW', ARRAY['create', 'read', 'update'], ARRAY['payments', 'invoices', 'financial_reports', 'members', 'classes', 'equipment', 'competitions'], '{"allowed_ip_ranges": ["10.0.30.0/24", "172.16.30.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"], "start": "08:00", "end": "18:30"}]}, "requires_dual_approval": true}'::jsonb, NOW()),
	('66666666-7777-8888-9999-aaaa00000004', '66666666-7777-8888-9999-aaaaaaaaaaaa', 'instructor-access', 'Instructor Access', 'Padronização técnica entre unidades', 'ALLOW', ARRAY['create', 'read', 'update'], ARRAY['members', 'classes', 'competitions'], '{"department": "technical"}'::jsonb, NOW()),
	('66666666-7777-8888-9999-aaaa00000005', '66666666-7777-8888-9999-aaaaaaaaaaaa', 'reception-access', 'Reception Access', 'Atendimento integrado ao CRM da rede', 'ALLOW', ARRAY['create', 'read'], ARRAY['members', 'classes', 'payments', 'schedules', 'basic_reports', 'competitions'], '{"department": "frontdesk", "allowed_ip_ranges": ["10.0.50.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"], "start": "07:00", "end": "21:30"}]}}'::jsonb, NOW()),
	('66666666-7777-8888-9999-aaaa00000006', '66666666-7777-8888-9999-aaaaaaaaaaaa', 'operations-access', 'Operations Access', 'Suporte administrativo compartilhado', 'ALLOW', ARRAY['create', 'read', 'update'], ARRAY['members', 'classes', 'equipment', 'payments', 'competitions'], '{"department": "operations", "allowed_ip_ranges": ["10.0.40.0/24", "172.16.20.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"], "start": "06:00", "end": "22:00"}]}}'::jsonb, NOW()),
	('66666666-7777-8888-9999-aaaa00000007', '66666666-7777-8888-9999-aaaaaaaaaaaa', 'equipment-maintenance', 'Equipment Maintenance', 'Logística de equipamento por unidade', 'ALLOW', ARRAY['create', 'read', 'update', 'delete'], ARRAY['equipment', 'inventory'], '{"department": "maintenance", "safety_training_required": true, "allowed_ip_ranges": ["10.0.70.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"], "start": "08:00", "end": "18:00"}]}}'::jsonb, NOW()),
	('66666666-7777-8888-9999-aaaa00000008', '66666666-7777-8888-9999-aaaaaaaaaaaa', 'security-access', 'Security Access', 'Monitoramento de acesso entre unidades', 'ALLOW', ARRAY['read'], ARRAY['members', 'classes', 'equipment', 'competitions', 'access_logs', 'facilities'], '{"department": "security", "mfa_required": true, "monitoring_level": "critical", "allowed_ip_ranges": ["10.0.60.0/24", "172.16.40.0/24"], "allowed_schedule": {"timezone": "UTC", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"], "start": "00:00", "end": "23:59"}]}}'::jsonb, NOW()),
	('66666666-7777-8888-9999-aaaa00000009', '66666666-7777-8888-9999-aaaaaaaaaaaa', 'member-vip-access', 'Member VIP Access', 'Acesso vip multiunidade', 'ALLOW', ARRAY['read'], ARRAY['members', 'classes', 'competitions', 'perks'], '{"tier": "vip", "allowed_ip_ranges": ["0.0.0.0/0"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"], "start": "06:00", "end": "23:00"}]}}'::jsonb, NOW()),
	('66666666-7777-8888-9999-aaaa0000000a', '66666666-7777-8888-9999-aaaaaaaaaaaa', 'member-basic-access', 'Member Basic Access', 'Acesso regular às unidades cadastradas', 'ALLOW', ARRAY['read'], ARRAY['members', 'classes', 'competitions'], '{"tier": "regular", "allowed_ip_ranges": ["0.0.0.0/0"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"], "start": "06:00", "end": "22:00"}]}}'::jsonb, NOW()),
	('66666666-7777-8888-9999-aaaa0000000b', '66666666-7777-8888-9999-aaaaaaaaaaaa', 'after-hours-support', 'After Hours Support', 'Suporte emergencial fora do expediente', 'ALLOW', ARRAY['read', 'update'], ARRAY['members', 'classes', 'competitions', 'reports'], '{"allowed_ip_ranges": ["10.0.80.0/24"], "incident_priority": ["P0", "P1"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"], "start": "18:00", "end": "23:59"}, {"days": ["SATURDAY", "SUNDAY"], "start": "08:00", "end": "22:00"}]}}'::jsonb, NOW()),
	('66666666-7777-8888-9999-aaaa0000000c', '66666666-7777-8888-9999-aaaaaaaaaaaa', 'trusted-network-access', 'Trusted Network Access', 'Acesso restrito a redes corporativas confiáveis', 'ALLOW', ARRAY['read'], ARRAY['reports', 'payments', 'members'], '{"mfa_required": true, "device_posture": "corporate-managed", "geo_restrictions": ["BR", "PT"], "allowed_ip_ranges": ["203.0.113.0/24", "198.51.100.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"], "start": "07:00", "end": "20:00"}]}}'::jsonb, NOW()),
	('bbbbbbbb-cccc-dddd-eeee-aaaa00000001', 'bbbbbbbb-cccc-dddd-eeee-ffffffffffff', 'admin-full-access', 'Admin Full Access', 'Controle total da academia boutique', 'ALLOW', ARRAY['create', 'read', 'update', 'delete', 'manage'], ARRAY['members', 'classes', 'payments', 'equipment', 'competitions', 'reports', 'settings', 'users', 'permissions'], '{"mfa_required": true, "device_posture": "managed", "allowed_ip_ranges": ["10.0.0.0/16", "192.168.0.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"], "start": "00:00", "end": "23:59"}]}}'::jsonb, NOW()),
	('bbbbbbbb-cccc-dddd-eeee-aaaa00000002', 'bbbbbbbb-cccc-dddd-eeee-ffffffffffff', 'management-access', 'Management Access', 'Curadoria da experiência premium', 'ALLOW', ARRAY['create', 'read', 'update', 'delete'], ARRAY['members', 'classes', 'payments', 'competitions', 'reports', 'equipment', 'settings', 'users', 'permissions'], '{"mfa_required": true, "allowed_ip_ranges": ["10.0.10.0/24", "10.0.20.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"], "start": "07:00", "end": "22:00"}]}, "risk_level": "medium"}'::jsonb, NOW()),
	('bbbbbbbb-cccc-dddd-eeee-aaaa00000003', 'bbbbbbbb-cccc-dddd-eeee-ffffffffffff', 'financial-access', 'Financial Access', 'Gestão de pacotes premium e faturamento', 'ALLOW', ARRAY['create', 'read', 'update'], ARRAY['payments', 'invoices', 'financial_reports', 'members', 'classes', 'equipment', 'competitions'], '{"allowed_ip_ranges": ["10.0.30.0/24", "172.16.30.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"], "start": "08:00", "end": "18:30"}]}, "requires_dual_approval": true}'::jsonb, NOW()),
	('bbbbbbbb-cccc-dddd-eeee-aaaa00000004', 'bbbbbbbb-cccc-dddd-eeee-ffffffffffff', 'instructor-access', 'Instructor Access', 'Execução de treinos personalizados', 'ALLOW', ARRAY['create', 'read', 'update'], ARRAY['members', 'classes', 'competitions'], '{"department": "technical"}'::jsonb, NOW()),
	('bbbbbbbb-cccc-dddd-eeee-aaaa00000005', 'bbbbbbbb-cccc-dddd-eeee-ffffffffffff', 'reception-access', 'Reception Access', 'Atendimento concierge dos membros', 'ALLOW', ARRAY['create', 'read'], ARRAY['members', 'classes', 'payments', 'schedules', 'basic_reports', 'competitions'], '{"department": "frontdesk", "allowed_ip_ranges": ["10.0.50.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"], "start": "07:00", "end": "21:30"}]}}'::jsonb, NOW()),
	('bbbbbbbb-cccc-dddd-eeee-aaaa00000006', 'bbbbbbbb-cccc-dddd-eeee-ffffffffffff', 'operations-access', 'Operations Access', 'Backoffice e suporte ao cliente', 'ALLOW', ARRAY['create', 'read', 'update'], ARRAY['members', 'classes', 'equipment', 'payments', 'competitions'], '{"department": "operations", "allowed_ip_ranges": ["10.0.40.0/24", "172.16.20.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"], "start": "06:00", "end": "22:00"}]}}'::jsonb, NOW()),
	('bbbbbbbb-cccc-dddd-eeee-aaaa00000007', 'bbbbbbbb-cccc-dddd-eeee-ffffffffffff', 'equipment-maintenance', 'Equipment Maintenance', 'Manutenção de equipamentos premium', 'ALLOW', ARRAY['create', 'read', 'update', 'delete'], ARRAY['equipment', 'inventory'], '{"department": "maintenance", "safety_training_required": true, "allowed_ip_ranges": ["10.0.70.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"], "start": "08:00", "end": "18:00"}]}}'::jsonb, NOW()),
	('bbbbbbbb-cccc-dddd-eeee-aaaa00000008', 'bbbbbbbb-cccc-dddd-eeee-ffffffffffff', 'security-access', 'Security Access', 'Controle de acesso dos espaços exclusivos', 'ALLOW', ARRAY['read'], ARRAY['members', 'classes', 'equipment', 'competitions', 'access_logs', 'facilities'], '{"department": "security", "mfa_required": true, "monitoring_level": "critical", "allowed_ip_ranges": ["10.0.60.0/24", "172.16.40.0/24"], "allowed_schedule": {"timezone": "UTC", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"], "start": "00:00", "end": "23:59"}]}}'::jsonb, NOW()),
	('bbbbbbbb-cccc-dddd-eeee-aaaa00000009', 'bbbbbbbb-cccc-dddd-eeee-ffffffffffff', 'member-vip-access', 'Member VIP Access', 'Benefícios exclusivos para membros VIP', 'ALLOW', ARRAY['read'], ARRAY['members', 'classes', 'competitions', 'perks'], '{"tier": "vip", "allowed_ip_ranges": ["0.0.0.0/0"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"], "start": "06:00", "end": "23:00"}]}}'::jsonb, NOW()),
	('bbbbbbbb-cccc-dddd-eeee-aaaa0000000a', 'bbbbbbbb-cccc-dddd-eeee-ffffffffffff', 'member-basic-access', 'Member Basic Access', 'Acesso padrão dos membros essenciais', 'ALLOW', ARRAY['read'], ARRAY['members', 'classes', 'competitions'], '{"tier": "regular", "allowed_ip_ranges": ["0.0.0.0/0"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"], "start": "06:00", "end": "22:00"}]}}'::jsonb, NOW()),
	('bbbbbbbb-cccc-dddd-eeee-aaaa0000000b', 'bbbbbbbb-cccc-dddd-eeee-ffffffffffff', 'after-hours-support', 'After Hours Support', 'Suporte emergencial fora do expediente', 'ALLOW', ARRAY['read', 'update'], ARRAY['members', 'classes', 'competitions', 'reports'], '{"allowed_ip_ranges": ["10.0.80.0/24"], "incident_priority": ["P0", "P1"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"], "start": "18:00", "end": "23:59"}, {"days": ["SATURDAY", "SUNDAY"], "start": "08:00", "end": "22:00"}]}}'::jsonb, NOW()),
	('bbbbbbbb-cccc-dddd-eeee-aaaa0000000c', 'bbbbbbbb-cccc-dddd-eeee-ffffffffffff', 'trusted-network-access', 'Trusted Network Access', 'Acesso restrito a redes corporativas confiáveis', 'ALLOW', ARRAY['read'], ARRAY['reports', 'payments', 'members'], '{"mfa_required": true, "device_posture": "corporate-managed", "geo_restrictions": ["BR", "PT"], "allowed_ip_ranges": ["203.0.113.0/24", "198.51.100.0/24"], "allowed_schedule": {"timezone": "America/Sao_Paulo", "windows": [{"days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"], "start": "07:00", "end": "20:00"}]}}'::jsonb, NOW())
ON CONFLICT DO NOTHING;

-- =====================================================
-- Permissions catalog
-- =====================================================

INSERT INTO permissions (id, action, resource, landlord_id)
VALUES
	('20000000-0000-0000-0000-000000000001', 'create', 'members', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000002', 'read', 'members', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000003', 'update', 'members', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000004', 'delete', 'members', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000005', 'create', 'classes', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000006', 'read', 'classes', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000007', 'update', 'classes', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000008', 'delete', 'classes', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000009', 'create', 'payments', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-00000000000a', 'read', 'payments', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-00000000000b', 'update', 'payments', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-00000000000c', 'delete', 'payments', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-00000000000d', 'create', 'equipment', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-00000000000e', 'read', 'equipment', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-00000000000f', 'update', 'equipment', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000010', 'delete', 'equipment', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000011', 'create', 'competitions', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000012', 'read', 'competitions', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000013', 'update', 'competitions', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000014', 'delete', 'competitions', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000015', 'create', 'reports', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000016', 'read', 'reports', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000017', 'update', 'reports', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000018', 'delete', 'reports', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000019', 'create', 'settings', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-00000000001a', 'read', 'settings', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-00000000001b', 'update', 'settings', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-00000000001c', 'delete', 'settings', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-00000000001d', 'manage', 'settings', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-00000000001e', 'create', 'users', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-00000000001f', 'read', 'users', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000020', 'update', 'users', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000021', 'delete', 'users', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000022', 'create', 'permissions', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000023', 'read', 'permissions', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000024', 'update', 'permissions', '00000000-0000-0000-0000-000000000000'),
	('20000000-0000-0000-0000-000000000025', 'delete', 'permissions', '00000000-0000-0000-0000-000000000000')
ON CONFLICT DO NOTHING;

-- =====================================================
-- Role-permission associations
-- =====================================================

WITH role_permissions AS (
	SELECT 'owner' AS role_code, 'admin-full-access' AS policy_code, resources.resource, actions.action
	FROM UNNEST(ARRAY['members', 'classes', 'payments', 'equipment', 'competitions', 'reports', 'settings', 'users', 'permissions']) AS resources(resource)
	CROSS JOIN UNNEST(ARRAY['create', 'read', 'update', 'delete']) AS actions(action)
	UNION ALL
	SELECT 'owner', 'admin-full-access', 'settings', 'manage'
	UNION ALL
	SELECT 'general_manager', 'management-access', resources.resource, actions.action
	FROM UNNEST(ARRAY['members', 'classes', 'payments', 'competitions', 'reports']) AS resources(resource)
	CROSS JOIN UNNEST(ARRAY['create', 'read', 'update', 'delete']) AS actions(action)
	UNION ALL
	SELECT 'general_manager', 'management-access', 'equipment', 'read'
	UNION ALL
	SELECT 'general_manager', 'management-access', 'settings', 'read'
	UNION ALL
	SELECT 'general_manager', 'management-access', 'users', 'read'
	UNION ALL
	SELECT 'general_manager', 'management-access', 'permissions', 'read'
	UNION ALL
	SELECT 'financial_manager', 'financial-access', 'members', action
	FROM UNNEST(ARRAY['read', 'update']) AS action(action)
	UNION ALL
	SELECT 'financial_manager', 'financial-access', 'classes', 'read'
	UNION ALL
	SELECT 'financial_manager', 'financial-access', 'payments', action
	FROM UNNEST(ARRAY['create', 'read', 'update']) AS action(action)
	UNION ALL
	SELECT 'financial_manager', 'financial-access', 'equipment', 'read'
	UNION ALL
	SELECT 'financial_manager', 'financial-access', 'competitions', 'read'
	UNION ALL
	SELECT 'financial_manager', 'financial-access', 'reports', action
	FROM UNNEST(ARRAY['create', 'read', 'update']) AS action(action)
	UNION ALL
	SELECT 'financial_manager', 'financial-access', 'settings', 'read'
	UNION ALL
	SELECT 'financial_manager', 'financial-access', 'users', 'read'
	UNION ALL
	SELECT 'head_instructor', 'instructor-access', resources.resource, actions.action
	FROM UNNEST(ARRAY['members', 'classes', 'competitions']) AS resources(resource)
	CROSS JOIN UNNEST(ARRAY['create', 'read', 'update', 'delete']) AS actions(action)
	UNION ALL
	SELECT 'head_instructor', 'instructor-access', 'payments', 'read'
	UNION ALL
	SELECT 'head_instructor', 'instructor-access', 'equipment', 'read'
	UNION ALL
	SELECT 'head_instructor', 'instructor-access', 'reports', 'read'
	UNION ALL
	SELECT 'instructor', 'instructor-access', 'members', action
	FROM UNNEST(ARRAY['read', 'update']) AS action(action)
	UNION ALL
	SELECT 'instructor', 'instructor-access', 'classes', action
	FROM UNNEST(ARRAY['create', 'read', 'update']) AS action(action)
	UNION ALL
	SELECT 'instructor', 'instructor-access', 'payments', 'read'
	UNION ALL
	SELECT 'instructor', 'instructor-access', 'equipment', 'read'
	UNION ALL
	SELECT 'instructor', 'instructor-access', 'competitions', 'read'
	UNION ALL
	SELECT 'instructor', 'instructor-access', 'reports', 'read'
	UNION ALL
	SELECT 'receptionist', 'reception-access', 'members', action
	FROM UNNEST(ARRAY['create', 'read', 'update', 'delete']) AS action(action)
	UNION ALL
	SELECT 'receptionist', 'reception-access', 'classes', 'read'
	UNION ALL
	SELECT 'receptionist', 'reception-access', 'payments', 'read'
	UNION ALL
	SELECT 'receptionist', 'reception-access', 'equipment', 'read'
	UNION ALL
	SELECT 'receptionist', 'reception-access', 'competitions', 'read'
	UNION ALL
	SELECT 'receptionist', 'reception-access', 'reports', 'read'
	UNION ALL
	SELECT 'admin_assistant', 'operations-access', 'members', action
	FROM UNNEST(ARRAY['read', 'update']) AS action(action)
	UNION ALL
	SELECT 'admin_assistant', 'operations-access', 'classes', action
	FROM UNNEST(ARRAY['read', 'update']) AS action(action)
	UNION ALL
	SELECT 'admin_assistant', 'operations-access', 'payments', 'read'
	UNION ALL
	SELECT 'admin_assistant', 'operations-access', 'equipment', 'read'
	UNION ALL
	SELECT 'admin_assistant', 'operations-access', 'competitions', 'read'
	UNION ALL
	SELECT 'admin_assistant', 'operations-access', 'reports', 'read'
	UNION ALL
	SELECT 'equipment_technician', 'equipment-maintenance', 'equipment', action
	FROM UNNEST(ARRAY['create', 'read', 'update', 'delete']) AS action(action)
	UNION ALL
	SELECT 'security', 'security-access', 'members', 'read'
	UNION ALL
	SELECT 'security', 'security-access', 'classes', 'read'
	UNION ALL
	SELECT 'security', 'security-access', 'equipment', 'read'
	UNION ALL
	SELECT 'security', 'security-access', 'competitions', 'read'
	UNION ALL
	SELECT 'vip_member', 'member-vip-access', 'members', 'read'
	UNION ALL
	SELECT 'vip_member', 'member-vip-access', 'classes', 'read'
	UNION ALL
	SELECT 'vip_member', 'member-vip-access', 'competitions', 'read'
	UNION ALL
	SELECT 'regular_member', 'member-basic-access', 'members', 'read'
	UNION ALL
	SELECT 'regular_member', 'member-basic-access', 'classes', 'read'
	UNION ALL
	SELECT 'regular_member', 'member-basic-access', 'competitions', 'read'
)
INSERT INTO roles_permissions (role_id, permission_id, policy_id, created_at, updated_at)
SELECT r.id,
	   p.id,
	   pol.id,
	   NOW(),
	   NOW()
FROM role_permissions rp
JOIN roles r ON r.code = rp.role_code AND r.landlord_id IN ('11111111-aaaa-bbbb-cccc-111111111111', '66666666-aaaa-bbbb-cccc-222222222222', 'bbbbbbbb-aaaa-bbbb-cccc-333333333333')
JOIN permissions p ON p.resource = rp.resource AND p.action = rp.action
JOIN policies pol ON pol.tenant_id IN ('11111111-2222-3333-4444-555555555555', '66666666-7777-8888-9999-aaaaaaaaaaaa', 'bbbbbbbb-cccc-dddd-eeee-ffffffffffff') AND pol.code = rp.policy_code
ON CONFLICT DO NOTHING;

-- =====================================================
-- Users and assignments
-- =====================================================

INSERT INTO users (id, name, email, password_hash, is_active, email_verified_at, created_at, updated_at)
VALUES
	('aaaabbbb-1111-2222-3333-444444444444', 'Helena Sato', 'helena.sato@dragao.local', '$argon2id$v=19$m=65536,t=3,p=4$UBPfdzWRImPL7316HUF3SwjezhzHk3mFcYwiYeSB364$fL5gXZFM133bNfenk0hdM8MfudaUbz0l/EcrUyMHbtbPCh/7vU6hZG6uAiTx2BdwA17epgHsq3wIUkxZr3baCw', true, NOW(), NOW(), NOW()),
	('aaaabbbb-1111-2222-3333-555555555555', 'Marcos Vidal', 'marcos.vidal@dragao.local', '$argon2id$v=19$m=65536,t=3,p=4$UBPfdzWRImPL7316HUF3SwjezhzHk3mFcYwiYeSB364$fL5gXZFM133bNfenk0hdM8MfudaUbz0l/EcrUyMHbtbPCh/7vU6hZG6uAiTx2BdwA17epgHsq3wIUkxZr3baCw', true, NOW(), NOW(), NOW()),
	('aaaabbbb-1111-2222-3333-666666666666', 'Luiza Campos', 'luiza.campos@dragao.local', '$argon2id$v=19$m=65536,t=3,p=4$UBPfdzWRImPL7316HUF3SwjezhzHk3mFcYwiYeSB364$fL5gXZFM133bNfenk0hdM8MfudaUbz0l/EcrUyMHbtbPCh/7vU6hZG6uAiTx2BdwA17epgHsq3wIUkxZr3baCw', true, NOW(), NOW(), NOW()),
	('aaaabbbb-1111-2222-3333-777777777777', 'Ricardo Menezes', 'ricardo.menezes@dragao.local', '$argon2id$v=19$m=65536,t=3,p=4$UBPfdzWRImPL7316HUF3SwjezhzHk3mFcYwiYeSB364$fL5gXZFM133bNfenk0hdM8MfudaUbz0l/EcrUyMHbtbPCh/7vU6hZG6uAiTx2BdwA17epgHsq3wIUkxZr3baCw', true, NULL, NOW(), NOW()),
	('aaaabbbb-5555-6666-7777-888888888888', 'Joana Azevedo', 'joana.azevedo@tigreescarlate.local', '$argon2id$v=19$m=65536,t=3,p=4$UBPfdzWRImPL7316HUF3SwjezhzHk3mFcYwiYeSB364$fL5gXZFM133bNfenk0hdM8MfudaUbz0l/EcrUyMHbtbPCh/7vU6hZG6uAiTx2BdwA17epgHsq3wIUkxZr3baCw', true, NOW(), NOW(), NOW()),
	('aaaabbbb-5555-6666-7777-999999999999', 'Diego Farias', 'diego.farias@tigreescarlate.local', '$argon2id$v=19$m=65536,t=3,p=4$UBPfdzWRImPL7316HUF3SwjezhzHk3mFcYwiYeSB364$fL5gXZFM133bNfenk0hdM8MfudaUbz0l/EcrUyMHbtbPCh/7vU6hZG6uAiTx2BdwA17epgHsq3wIUkxZr3baCw', true, NOW(), NOW(), NOW()),
	('aaaabbbb-5555-6666-7777-aaaaaaaaaaaa', 'Sofia Leal', 'sofia.leal@tigreescarlate.local', '$argon2id$v=19$m=65536,t=3,p=4$UBPfdzWRImPL7316HUF3SwjezhzHk3mFcYwiYeSB364$fL5gXZFM133bNfenk0hdM8MfudaUbz0l/EcrUyMHbtbPCh/7vU6hZG6uAiTx2BdwA17epgHsq3wIUkxZr3baCw', true, NOW(), NOW(), NOW()),
	('aaaabbbb-5555-6666-7777-bbbbbbbbbbbb', 'Letícia Mauro', 'leticia.mauro@tigreescarlate.local', '$argon2id$v=19$m=65536,t=3,p=4$UBPfdzWRImPL7316HUF3SwjezhzHk3mFcYwiYeSB364$fL5gXZFM133bNfenk0hdM8MfudaUbz0l/EcrUyMHbtbPCh/7vU6hZG6uAiTx2BdwA17epgHsq3wIUkxZr3baCw', true, NOW(), NOW(), NOW()),
	('aaaabbbb-5555-6666-7777-cccccccccccc', 'Rafael Couto', 'rafael.couto@zonasul.tigreescarlate.local', '$argon2id$v=19$m=65536,t=3,p=4$UBPfdzWRImPL7316HUF3SwjezhzHk3mFcYwiYeSB364$fL5gXZFM133bNfenk0hdM8MfudaUbz0l/EcrUyMHbtbPCh/7vU6hZG6uAiTx2BdwA17epgHsq3wIUkxZr3baCw', true, NOW(), NOW(), NOW()),
	('aaaabbbb-9999-aaaa-bbbb-cccccccccccc', 'Bianca Ohara', 'bianca.ohara@lotusazul.local', '$argon2id$v=19$m=65536,t=3,p=4$UBPfdzWRImPL7316HUF3SwjezhzHk3mFcYwiYeSB364$fL5gXZFM133bNfenk0hdM8MfudaUbz0l/EcrUyMHbtbPCh/7vU6hZG6uAiTx2BdwA17epgHsq3wIUkxZr3baCw', true, NOW(), NOW(), NOW()),
	('aaaabbbb-9999-aaaa-bbbb-dddddddddddd', 'Fernando Lim', 'fernando.lim@lotusazul.local', '$argon2id$v=19$m=65536,t=3,p=4$UBPfdzWRImPL7316HUF3SwjezhzHk3mFcYwiYeSB364$fL5gXZFM133bNfenk0hdM8MfudaUbz0l/EcrUyMHbtbPCh/7vU6hZG6uAiTx2BdwA17epgHsq3wIUkxZr3baCw', true, NOW(), NOW(), NOW()),
	('aaaabbbb-9999-aaaa-bbbb-eeeeeeeeeeee', 'Paula Torres', 'paula.torres@lotusazul.local', '$argon2id$v=19$m=65536,t=3,p=4$UBPfdzWRImPL7316HUF3SwjezhzHk3mFcYwiYeSB364$fL5gXZFM133bNfenk0hdM8MfudaUbz0l/EcrUyMHbtbPCh/7vU6hZG6uAiTx2BdwA17epgHsq3wIUkxZr3baCw', true, NOW(), NOW(), NOW()),
	('aaaabbbb-9999-aaaa-bbbb-ffffffffffff', 'Igor Mishima', 'igor.mishima@lotusazul.local', '$argon2id$v=19$m=65536,t=3,p=4$UBPfdzWRImPL7316HUF3SwjezhzHk3mFcYwiYeSB364$fL5gXZFM133bNfenk0hdM8MfudaUbz0l/EcrUyMHbtbPCh/7vU6hZG6uAiTx2BdwA17epgHsq3wIUkxZr3baCw', true, NULL, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO users_tenants_roles (user_id, tenant_id, role_id)
VALUES
	('aaaabbbb-1111-2222-3333-444444444444', '11111111-2222-3333-4444-555555555555', '11111111-2222-3333-4444-000000000001'),
	('aaaabbbb-1111-2222-3333-555555555555', '11111111-2222-3333-4444-555555555555', '11111111-2222-3333-4444-000000000004'),
	('aaaabbbb-1111-2222-3333-666666666666', '11111111-2222-3333-4444-555555555555', '11111111-2222-3333-4444-000000000006'),
	('aaaabbbb-1111-2222-3333-777777777777', '11111111-2222-3333-4444-555555555555', '11111111-2222-3333-4444-00000000000a'),
	('aaaabbbb-5555-6666-7777-888888888888', '66666666-7777-8888-9999-aaaaaaaaaaaa', '66666666-7777-8888-9999-000000000001'),
	('aaaabbbb-5555-6666-7777-999999999999', '66666666-7777-8888-9999-aaaaaaaaaaaa', '66666666-7777-8888-9999-000000000002'),
	('aaaabbbb-5555-6666-7777-aaaaaaaaaaaa', '66666666-7777-8888-9999-aaaaaaaaaaaa', '66666666-7777-8888-9999-000000000005'),
	('aaaabbbb-5555-6666-7777-bbbbbbbbbbbb', '66666666-7777-8888-9999-aaaaaaaaaaaa', '66666666-7777-8888-9999-000000000006'),
	('aaaabbbb-5555-6666-7777-cccccccccccc', '66666666-7777-8888-9999-aaaaaaaaaaaa', '66666666-7777-8888-9999-000000000009'),
	('aaaabbbb-9999-aaaa-bbbb-cccccccccccc', 'bbbbbbbb-cccc-dddd-eeee-ffffffffffff', 'bbbbbbbb-cccc-dddd-eeee-000000000001'),
	('aaaabbbb-9999-aaaa-bbbb-dddddddddddd', 'bbbbbbbb-cccc-dddd-eeee-ffffffffffff', 'bbbbbbbb-cccc-dddd-eeee-000000000003'),
	('aaaabbbb-9999-aaaa-bbbb-eeeeeeeeeeee', 'bbbbbbbb-cccc-dddd-eeee-ffffffffffff', 'bbbbbbbb-cccc-dddd-eeee-000000000007'),
	('aaaabbbb-9999-aaaa-bbbb-ffffffffffff', 'bbbbbbbb-cccc-dddd-eeee-ffffffffffff', 'bbbbbbbb-cccc-dddd-eeee-00000000000b')
ON CONFLICT DO NOTHING;
