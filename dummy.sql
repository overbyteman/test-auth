-- =====================================================
-- DUMMY.SQL - Dados de Exemplo para Auth Service
-- =====================================================
-- Autor: Auth Service Team
-- Data: 2025-10-02
-- Versão: 1.0
-- Descrição: Popula a aplicação com dados realísticos para demonstração
--           Inclui cenários complexos de autenticação e autorização
-- =====================================================

-- Habilitar extensões necessárias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =====================================================
-- 1. TENANTS - Organizações/Empresas
-- =====================================================

-- Limpar dados existentes (cuidado em produção!)
TRUNCATE TABLE users_tenants_roles CASCADE;
TRUNCATE TABLE roles_permissions CASCADE;
TRUNCATE TABLE sessions CASCADE;
TRUNCATE TABLE audit_logs CASCADE;
TRUNCATE TABLE users CASCADE;
TRUNCATE TABLE permissions CASCADE;
TRUNCATE TABLE roles CASCADE;
TRUNCATE TABLE tenants CASCADE;
TRUNCATE TABLE policies CASCADE;

-- Inserir Tenants
INSERT INTO tenants (id, name, config, created_at, updated_at) VALUES
    -- Empresa Principal
    ('11111111-1111-1111-1111-111111111111', 'Seccreto Corporation', 
     '{"domain": "seccreto.com", "features": ["sso", "ldap_sync", "advanced_reports"], "theme": "corporate", "max_users": 1000}',
     NOW() - INTERVAL '90 days', NOW()),
    
    -- Cliente Corporativo
    ('22222222-2222-2222-2222-222222222222', 'TechStart Solutions',
     '{"domain": "techstart.com", "features": ["api_access", "webhooks"], "theme": "startup", "max_users": 100}',
     NOW() - INTERVAL '60 days', NOW()),
     
    -- Cliente SaaS
    ('33333333-3333-3333-3333-333333333333', 'Global Finance Corp',
     '{"domain": "globalfinance.com", "features": ["compliance", "audit_logs", "sso"], "theme": "finance", "max_users": 500}',
     NOW() - INTERVAL '30 days', NOW()),
     
    -- Tenant para Testes
    ('44444444-4444-4444-4444-444444444444', 'Dev Testing Environment',
     '{"domain": "dev.test", "features": ["all"], "theme": "dev", "max_users": 50}',
     NOW() - INTERVAL '1 day', NOW()),
     
    -- Tenant Multi-Regional
    ('55555555-5555-5555-5555-555555555555', 'International Holdings',
     '{"domain": "intl.com", "features": ["multi_region", "compliance", "advanced_reports"], "theme": "enterprise", "max_users": 2000, "regions": ["US", "EU", "APAC"]}',
     NOW() - INTERVAL '120 days', NOW());

-- =====================================================
-- 2. PERMISSIONS - Permissões Granulares
-- =====================================================

INSERT INTO permissions (id, action, resource, created_at, updated_at) VALUES
    -- User Management
    ('a0000000-0000-0000-0000-000000000001', 'create', 'users', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000002', 'read', 'users', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000003', 'update', 'users', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000004', 'delete', 'users', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000005', 'suspend', 'users', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000006', 'activate', 'users', NOW(), NOW()),
    
    -- Profile Management
    ('a0000000-0000-0000-0000-000000000007', 'read', 'profile', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000008', 'update', 'profile', NOW(), NOW()),
    
    -- Role Management
    ('a0000000-0000-0000-0000-000000000009', 'create', 'roles', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000010', 'read', 'roles', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000011', 'update', 'roles', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000012', 'delete', 'roles', NOW(), NOW()),
    
    -- Permission Management
    ('a0000000-0000-0000-0000-000000000013', 'create', 'permissions', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000014', 'read', 'permissions', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000015', 'update', 'permissions', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000016', 'delete', 'permissions', NOW(), NOW()),
    
    -- Tenant Management
    ('a0000000-0000-0000-0000-000000000017', 'create', 'tenants', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000018', 'read', 'tenants', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000019', 'update', 'tenants', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000020', 'delete', 'tenants', NOW(), NOW()),
    
    -- Session Management
    ('a0000000-0000-0000-0000-000000000021', 'read', 'sessions', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000022', 'delete', 'sessions', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000023', 'manage', 'sessions', NOW(), NOW()),
    
    -- System & Monitoring
    ('a0000000-0000-0000-0000-000000000024', 'read', 'metrics', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000025', 'read', 'health', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000026', 'manage', 'system', NOW(), NOW()),
    
    -- Audit & Compliance
    ('a0000000-0000-0000-0000-000000000027', 'read', 'audit_logs', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000028', 'export', 'audit_logs', NOW(), NOW()),
    
    -- API Access
    ('a0000000-0000-0000-0000-000000000029', 'access', 'api', NOW(), NOW()),
    ('a0000000-0000-0000-0000-000000000030', 'manage', 'api_keys', NOW(), NOW());

-- =====================================================
-- 3. ROLES - Funções Hierárquicas
-- =====================================================

INSERT INTO roles (id, name, description) VALUES
    -- Hierarquia de Roles
    ('b0000000-0000-0000-0000-000000000001', 'SUPER_ADMIN', 'Administrador supremo com acesso total ao sistema'),
    ('b0000000-0000-0000-0000-000000000002', 'ADMIN', 'Administrador com acesso completo ao tenant'),
    ('b0000000-0000-0000-0000-000000000003', 'MANAGER', 'Gerente com permissões de gestão limitadas'),
    ('b0000000-0000-0000-0000-000000000004', 'USER', 'Usuário padrão com acesso básico'),
    ('b0000000-0000-0000-0000-000000000005', 'GUEST', 'Usuário convidado com acesso limitado'),
    
    -- Roles Especializados
    ('b0000000-0000-0000-0000-000000000006', 'SECURITY_OFFICER', 'Oficial de segurança com acesso a logs e auditoria'),
    ('b0000000-0000-0000-0000-000000000007', 'API_USER', 'Usuário especializado para acesso via API'),
    ('b0000000-0000-0000-0000-000000000008', 'COMPLIANCE_AUDITOR', 'Auditor com acesso a relatórios de compliance'),
    ('b0000000-0000-0000-0000-000000000009', 'SUPPORT_AGENT', 'Agente de suporte com permissões específicas'),
    ('b0000000-0000-0000-0000-000000000010', 'READ_ONLY', 'Usuário apenas leitura para relatórios');

-- =====================================================
-- 4. ROLES_PERMISSIONS - Associações Role-Permissão
-- =====================================================

-- SUPER_ADMIN - Todas as permissões
INSERT INTO roles_permissions (role_id, permission_id)
SELECT 'b0000000-0000-0000-0000-000000000001', id FROM permissions;

-- ADMIN - Gerenciamento completo exceto sistema
INSERT INTO roles_permissions (role_id, permission_id) VALUES
    -- User Management
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000003'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000004'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000005'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000006'),
    -- Role & Permission Management
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000009'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000010'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000011'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000014'),
    -- Tenant Management
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000018'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000019'),
    -- Session & Monitoring
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000021'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000022'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000023'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000024'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000025');

-- MANAGER - Leitura de usuários e gestão limitada
INSERT INTO roles_permissions (role_id, permission_id) VALUES
    ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000002'), -- read users
    ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000007'), -- read profile
    ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000008'), -- update profile
    ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000010'), -- read roles
    ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000014'), -- read permissions
    ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000018'), -- read tenants
    ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000021'), -- read sessions
    ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000024'), -- read metrics
    ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000025'); -- read health

-- USER - Permissões básicas
INSERT INTO roles_permissions (role_id, permission_id) VALUES
    ('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000007'), -- read profile
    ('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000008'), -- update profile
    ('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000025'); -- read health

-- SECURITY_OFFICER - Segurança e auditoria
INSERT INTO roles_permissions (role_id, permission_id) VALUES
    ('b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000002'), -- read users
    ('b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000021'), -- read sessions
    ('b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000022'), -- delete sessions
    ('b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000024'), -- read metrics
    ('b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000027'), -- read audit_logs
    ('b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000028'); -- export audit_logs

-- API_USER - Acesso via API
INSERT INTO roles_permissions (role_id, permission_id) VALUES
    ('b0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000029'), -- access api
    ('b0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000007'), -- read profile
    ('b0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000002'); -- read users

-- =====================================================
-- 5. USERS - Usuários Realísticos
-- =====================================================

INSERT INTO users (id, name, email, password_hash, is_active, email_verified_at, created_at, updated_at) VALUES
    -- Super Administrador
    ('c0000000-0000-0000-0000-000000000001', 
     'Super Administrator', 
     'superadmin@empresa.com',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqfcwKpDOr6vTq4/Ng9V2dG', -- SuperAdmin@2024!
     true, 
     NOW() - INTERVAL '90 days',
     NOW() - INTERVAL '90 days', 
     NOW()),
     
    -- Administrador Principal
    ('c0000000-0000-0000-0000-000000000002', 
     'João Silva', 
     'admin@empresa.com',
     '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- Admin@2024!
     true, 
     NOW() - INTERVAL '85 days',
     NOW() - INTERVAL '85 days', 
     NOW()),
     
    -- Gerente
    ('c0000000-0000-0000-0000-000000000003', 
     'Maria Santos', 
     'manager@empresa.com',
     '$2a$12$GTFmPA/Klx/LcPxnfaJ.7u42zE4Z5.vVsQLK7LkzQd8yD5EmS5N.a', -- Manager@2024!
     true, 
     NOW() - INTERVAL '60 days',
     NOW() - INTERVAL '60 days', 
     NOW()),
     
    -- Usuário Padrão
    ('c0000000-0000-0000-0000-000000000004', 
     'Carlos Oliveira', 
     'user@empresa.com',
     '$2a$12$8YUf5OyCrON8F0tXd.zVkOZJdcXCU1qscrHxqgMXCtMDKl1OZQ8sG', -- User@2024!
     true, 
     NOW() - INTERVAL '30 days',
     NOW() - INTERVAL '30 days', 
     NOW()),
     
    -- Oficial de Segurança
    ('c0000000-0000-0000-0000-000000000005', 
     'Ana Security', 
     'security@empresa.com',
     '$2a$12$VJeNF/xOvZ7DhF9m3KGTre6pCzHJjL2IyG4HgJzHg3cLkQs2vQxMa', -- Security@2024!
     true, 
     NOW() - INTERVAL '45 days',
     NOW() - INTERVAL '45 days', 
     NOW()),
     
    -- Usuário API
    ('c0000000-0000-0000-0000-000000000006', 
     'API Service Account', 
     'api@empresa.com',
     '$2a$12$7ZvOZUq3f6Y.VQOGd8.fzO6SqE6Mk4gKzJhG4/QP8L3MNJr5L2k6K', -- ApiUser@2024!
     true, 
     NOW() - INTERVAL '20 days',
     NOW() - INTERVAL '20 days', 
     NOW()),
     
    -- Usuários de Diferentes Tenants
    ('c0000000-0000-0000-0000-000000000007', 
     'Tech Admin', 
     'admin@techstart.com',
     '$2a$12$NFbcPQ5r8vX9/.YGfJ3cIe7ZgRqxP2v3k4GvE5Hm7Jz/Nx8Lz9mNq', -- TechAdmin@2024!
     true, 
     NOW() - INTERVAL '60 days',
     NOW() - INTERVAL '60 days', 
     NOW()),
     
    ('c0000000-0000-0000-0000-000000000008', 
     'Finance Manager', 
     'manager@globalfinance.com',
     '$2a$12$OPdAQT6u9aY0/.ZHgK4dJf8AhSrxQ3w4l5HwF6In8Ka/Oy9Ma0oOr', -- FinanceManager@2024!
     true, 
     NOW() - INTERVAL '30 days',
     NOW() - INTERVAL '30 days', 
     NOW()),
     
    -- Usuários Inativos/Suspensos para Testes
    ('c0000000-0000-0000-0000-000000000009', 
     'Suspended User', 
     'suspended@empresa.com',
     '$2a$12$QReBS7r0bZ1/.AIhL5eEKg9BiTsxR4x5m6IxG7Jo9Lb/Pz0Nb1pPs', -- Suspended@2024!
     false, 
     NOW() - INTERVAL '15 days',
     NOW() - INTERVAL '15 days', 
     NOW() - INTERVAL '5 days'),
     
    -- Usuário Internacional
    ('c0000000-0000-0000-0000-000000000010', 
     'International User', 
     'global@intl.com',
     '$2a$12$RSfCT8s1cA2/.BJiM6fFLh0CjUtxS5y6n7JyH8Kp0Mc/Qa1Oc2qQt', -- International@2024!
     true, 
     NOW() - INTERVAL '120 days',
     NOW() - INTERVAL '120 days', 
     NOW());

-- =====================================================
-- 6. USERS_TENANTS_ROLES - Associações Complexas
-- =====================================================

INSERT INTO users_tenants_roles (user_id, tenant_id, role_id) VALUES
    -- Super Admin em todos os tenants
    ('c0000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'b0000000-0000-0000-0000-000000000001'),
    ('c0000000-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'b0000000-0000-0000-0000-000000000001'),
    ('c0000000-0000-0000-0000-000000000001', '33333333-3333-3333-3333-333333333333', 'b0000000-0000-0000-0000-000000000001'),
    ('c0000000-0000-0000-0000-000000000001', '44444444-4444-4444-4444-444444444444', 'b0000000-0000-0000-0000-000000000001'),
    ('c0000000-0000-0000-0000-000000000001', '55555555-5555-5555-5555-555555555555', 'b0000000-0000-0000-0000-000000000001'),
    
    -- Admin Principal da Seccreto Corp
    ('c0000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'b0000000-0000-0000-0000-000000000002'),
    
    -- Gerente em múltiplos tenants
    ('c0000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'b0000000-0000-0000-0000-000000000003'),
    ('c0000000-0000-0000-0000-000000000003', '22222222-2222-2222-2222-222222222222', 'b0000000-0000-0000-0000-000000000003'),
    
    -- Usuário padrão
    ('c0000000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'b0000000-0000-0000-0000-000000000004'),
    
    -- Oficial de Segurança
    ('c0000000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'b0000000-0000-0000-0000-000000000006'),
    ('c0000000-0000-0000-0000-000000000005', '33333333-3333-3333-3333-333333333333', 'b0000000-0000-0000-0000-000000000006'),
    
    -- API User
    ('c0000000-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'b0000000-0000-0000-0000-000000000007'),
    
    -- Admins específicos de cada tenant
    ('c0000000-0000-0000-0000-000000000007', '22222222-2222-2222-2222-222222222222', 'b0000000-0000-0000-0000-000000000002'),
    ('c0000000-0000-0000-0000-000000000008', '33333333-3333-3333-3333-333333333333', 'b0000000-0000-0000-0000-000000000003'),
    
    -- Usuário internacional em multiple tenants com roles diferentes
    ('c0000000-0000-0000-0000-000000000010', '55555555-5555-5555-5555-555555555555', 'b0000000-0000-0000-0000-000000000002'),
    ('c0000000-0000-0000-0000-000000000010', '11111111-1111-1111-1111-111111111111', 'b0000000-0000-0000-0000-000000000003'),
    ('c0000000-0000-0000-0000-000000000010', '33333333-3333-3333-3333-333333333333', 'b0000000-0000-0000-0000-000000000004');

-- =====================================================
-- 7. SESSIONS - Sessões Ativas e Históricas
-- =====================================================

INSERT INTO sessions (id, user_id, tenant_id, token, refresh_token, user_agent, ip_address, expires_at, created_at, updated_at) VALUES
    -- Sessões ativas recentes
    ('d0000000-0000-0000-0000-000000000001', 
     'c0000000-0000-0000-0000-000000000001', 
     '11111111-1111-1111-1111-111111111111',
     'eyJhbGciOiJIUzM4NCJ9.super_admin_active_token',
     'eyJhbGciOiJIUzM4NCJ9.super_admin_refresh_token',
     'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
     '192.168.1.100',
     NOW() + INTERVAL '1 hour',
     NOW() - INTERVAL '30 minutes',
     NOW()),
     
    ('d0000000-0000-0000-0000-000000000002', 
     'c0000000-0000-0000-0000-000000000002', 
     '11111111-1111-1111-1111-111111111111',
     'eyJhbGciOiJIUzM4NCJ9.admin_active_token',
     'eyJhbGciOiJIUzM4NCJ9.admin_refresh_token',
     'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
     '10.0.0.50',
     NOW() + INTERVAL '45 minutes',
     NOW() - INTERVAL '15 minutes',
     NOW()),
     
    -- Sessão API ativa
    ('d0000000-0000-0000-0000-000000000003', 
     'c0000000-0000-0000-0000-000000000006', 
     '11111111-1111-1111-1111-111111111111',
     'eyJhbGciOiJIUzM4NCJ9.api_user_token',
     'eyJhbGciOiJIUzM4NCJ9.api_user_refresh_token',
     'APIClient/1.0 (automated-service)',
     '203.0.113.25',
     NOW() + INTERVAL '2 hours',
     NOW() - INTERVAL '5 minutes',
     NOW()),
     
    -- Sessões expiradas para histórico
    ('d0000000-0000-0000-0000-000000000004', 
     'c0000000-0000-0000-0000-000000000003', 
     '11111111-1111-1111-1111-111111111111',
     'eyJhbGciOiJIUzM4NCJ9.expired_manager_token',
     'eyJhbGciOiJIUzM4NCJ9.expired_manager_refresh',
     'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)',
     '172.16.0.123',
     NOW() - INTERVAL '2 hours',
     NOW() - INTERVAL '3 hours',
     NOW() - INTERVAL '2 hours'),
     
    -- Sessão de tenant diferente
    ('d0000000-0000-0000-0000-000000000005', 
     'c0000000-0000-0000-0000-000000000007', 
     '22222222-2222-2222-2222-222222222222',
     'eyJhbGciOiJIUzM4NCJ9.techstart_admin_token',
     'eyJhbGciOiJIUzM4NCJ9.techstart_admin_refresh',
     'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36',
     '198.51.100.75',
     NOW() + INTERVAL '30 minutes',
     NOW() - INTERVAL '10 minutes',
     NOW());

-- =====================================================
-- 8. POLICIES - Políticas ABAC Complexas
-- =====================================================

INSERT INTO policies (id, name, effect, actions, resources, conditions, created_at, updated_at) VALUES
    -- Política de horário comercial
    ('e0000000-0000-0000-0000-000000000001',
     'Business Hours Only',
     'ALLOW',
     ARRAY['login', 'access'],
     ARRAY['system'],
     '{"time": {"start": "08:00", "end": "18:00"}, "timezone": "UTC", "days": ["monday", "tuesday", "wednesday", "thursday", "friday"]}',
     NOW() - INTERVAL '30 days',
     NOW()),
     
    -- Política de localização geográfica
    ('e0000000-0000-0000-0000-000000000002',
     'Geo-Location Restriction',
     'DENY',
     ARRAY['login', 'api_access'],
     ARRAY['system'],
     '{"geo": {"blocked_countries": ["CN", "RU", "KP"], "allowed_regions": ["US", "EU", "BR"]}}',
     NOW() - INTERVAL '25 days',
     NOW()),
     
    -- Política de acesso sensível
    ('e0000000-0000-0000-0000-000000000003',
     'Sensitive Data Access',
     'ALLOW',
     ARRAY['read', 'export'],
     ARRAY['audit_logs', 'financial_data'],
     '{"user": {"min_role": "MANAGER", "requires_mfa": true}, "time": {"max_session_duration": 3600}}',
     NOW() - INTERVAL '20 days',
     NOW()),
     
    -- Política de API rate limiting
    ('e0000000-0000-0000-0000-000000000004',
     'API Rate Limiting',
     'DENY',
     ARRAY['api_access'],
     ARRAY['users', 'tenants'],
     '{"rate_limit": {"requests_per_minute": 100, "burst": 150}, "user": {"type": "api_user"}}',
     NOW() - INTERVAL '15 days',
     NOW()),
     
    -- Política de acesso administrativo
    ('e0000000-0000-0000-0000-000000000005',
     'Admin Access Control',
     'ALLOW',
     ARRAY['create', 'update', 'delete'],
     ARRAY['users', 'roles', 'permissions'],
     '{"user": {"role": "ADMIN", "tenant_admin": true}, "source": {"ip_whitelist": ["10.0.0.0/8", "192.168.0.0/16"]}}',
     NOW() - INTERVAL '10 days',
     NOW());

-- =====================================================
-- 9. AUDIT_LOGS - Logs de Auditoria Realísticos
-- =====================================================

INSERT INTO audit_logs (id, user_id, session_id, action, resource_type, resource_id, details, ip_address, user_agent, success, error_message, timestamp) VALUES
    -- Login bem-sucedido do Super Admin
    ('f0000000-0000-0000-0000-000000000001',
     'c0000000-0000-0000-0000-000000000001',
     'd0000000-0000-0000-0000-000000000001',
     'LOGIN',
     'SESSION',
     'd0000000-0000-0000-0000-000000000001',
     'Super admin login successful',
     '192.168.1.100',
     'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
     true,
     NULL,
     NOW() - INTERVAL '30 minutes'),
     
    -- Criação de usuário pelo Admin
    ('f0000000-0000-0000-0000-000000000002',
     'c0000000-0000-0000-0000-000000000002',
     'd0000000-0000-0000-0000-000000000002',
     'CREATE_USER',
     'USER',
     'c0000000-0000-0000-0000-000000000004',
     'Admin created new user: Carlos Oliveira',
     '10.0.0.50',
     'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
     true,
     NULL,
     NOW() - INTERVAL '30 days'),
     
    -- Tentativa de login falhada
    ('f0000000-0000-0000-0000-000000000003',
     NULL,
     NULL,
     'LOGIN_FAILED',
     'SESSION',
     NULL,
     'Failed login attempt for email: hacker@evil.com',
     '185.220.101.50',
     'curl/7.68.0',
     false,
     'Invalid credentials',
     NOW() - INTERVAL '2 hours'),
     
    -- Acesso API bem-sucedido
    ('f0000000-0000-0000-0000-000000000004',
     'c0000000-0000-0000-0000-000000000006',
     'd0000000-0000-0000-0000-000000000003',
     'API_ACCESS',
     'USER',
     NULL,
     'API user accessed user list endpoint',
     '203.0.113.25',
     'APIClient/1.0 (automated-service)',
     true,
     NULL,
     NOW() - INTERVAL '5 minutes'),
     
    -- Suspensão de usuário por segurança
    ('f0000000-0000-0000-0000-000000000005',
     'c0000000-0000-0000-0000-000000000005',
     NULL,
     'SUSPEND_USER',
     'USER',
     'c0000000-0000-0000-0000-000000000009',
     'Security officer suspended user due to suspicious activity',
     '10.0.0.75',
     'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36',
     true,
     NULL,
     NOW() - INTERVAL '5 days'),
     
    -- Exportação de logs de auditoria
    ('f0000000-0000-0000-0000-000000000006',
     'c0000000-0000-0000-0000-000000000005',
     NULL,
     'EXPORT_AUDIT_LOGS',
     'AUDIT_LOG',
     NULL,
     'Security officer exported audit logs for compliance review - Period: last 30 days',
     '10.0.0.75',
     'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36',
     true,
     NULL,
     NOW() - INTERVAL '1 day'),
     
    -- Múltiplas tentativas de força bruta (blocked)
    ('f0000000-0000-0000-0000-000000000007',
     NULL,
     NULL,
     'RATE_LIMIT_EXCEEDED',
     'SESSION',
     NULL,
     'Rate limit exceeded for login attempts from IP: 185.220.101.50',
     '185.220.101.50',
     'python-requests/2.31.0',
     false,
     'Too many login attempts. IP temporarily blocked.',
     NOW() - INTERVAL '1 hour'),
     
    -- Atualização de configuração de tenant
    ('f0000000-0000-0000-0000-000000000008',
     'c0000000-0000-0000-0000-000000000007',
     'd0000000-0000-0000-0000-000000000005',
     'UPDATE_TENANT_CONFIG',
     'TENANT',
     '22222222-2222-2222-2222-222222222222',
     'TechStart admin updated tenant configuration - Added API webhook endpoint',
     '198.51.100.75',
     'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36',
     true,
     NULL,
     NOW() - INTERVAL '6 hours'),
     
    -- Acesso negado por política ABAC
    ('f0000000-0000-0000-0000-000000000009',
     'c0000000-0000-0000-0000-000000000004',
     NULL,
     'ACCESS_DENIED',
     'AUDIT_LOG',
     NULL,
     'User attempted to access audit logs but denied by policy: insufficient role level',
     '172.16.0.123',
     'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)',
     false,
     'Policy violation: User role USER insufficient for resource audit_logs',
     NOW() - INTERVAL '3 hours'),
     
    -- Refresh token usado com sucesso
    ('f0000000-0000-0000-0000-000000000010',
     'c0000000-0000-0000-0000-000000000010',
     NULL,
     'REFRESH_TOKEN',
     'SESSION',
     NULL,
     'International user refreshed access token across multiple tenants',
     '203.0.113.100',
     'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
     true,
     NULL,
     NOW() - INTERVAL '2 hours');

-- =====================================================
-- 10. CENÁRIOS COMPLEXOS ADICIONAIS
-- =====================================================

-- Criar sessões expiradas adicionais para demonstrar limpeza
INSERT INTO sessions (id, user_id, tenant_id, token, refresh_token, user_agent, ip_address, expires_at, created_at, updated_at) 
SELECT 
    gen_random_uuid(),
    'c0000000-0000-0000-0000-000000000004',
    '11111111-1111-1111-1111-111111111111',
    'expired_token_' || generate_series,
    'expired_refresh_' || generate_series,
    'Mozilla/5.0 (Test Browser)',
    '192.168.1.' || (100 + generate_series),
    NOW() - INTERVAL '1 day' - (generate_series || ' hours')::INTERVAL,
    NOW() - INTERVAL '2 days' - (generate_series || ' hours')::INTERVAL,
    NOW() - INTERVAL '1 day' - (generate_series || ' hours')::INTERVAL
FROM generate_series(1, 5);

-- Criar logs de auditoria de tentativas de ataque
INSERT INTO audit_logs (id, user_id, session_id, action, resource_type, resource_id, details, ip_address, user_agent, success, error_message, timestamp)
SELECT 
    gen_random_uuid(),
    NULL,
    NULL,
    'SECURITY_VIOLATION',
    'SYSTEM',
    NULL,
    'SQL injection attempt detected in user input: ' || 
    (ARRAY['DROP TABLE users', 'UNION SELECT * FROM', '<script>alert()', 'OR 1=1--'])[generate_series % 4 + 1],
    '185.220.101.' || (50 + generate_series),
    'AttackBot/' || generate_series || '.0',
    false,
    'Input validation failed - potentially malicious content',
    NOW() - INTERVAL '7 days' + (generate_series || ' hours')::INTERVAL
FROM generate_series(1, 10);

-- =====================================================
-- 11. COMENTÁRIOS E DOCUMENTAÇÃO
-- =====================================================

-- Adicionar comentários às tabelas para documentação
COMMENT ON TABLE tenants IS 'Multi-tenant organizations with complex configurations and feature sets';
COMMENT ON TABLE users IS 'System users with encrypted passwords and comprehensive audit trail';
COMMENT ON TABLE roles IS 'Hierarchical role system supporting RBAC with granular permissions';
COMMENT ON TABLE permissions IS 'Granular permissions for fine-grained access control';
COMMENT ON TABLE users_tenants_roles IS 'Multi-tenant role assignments enabling complex organizational structures';
COMMENT ON TABLE sessions IS 'User sessions with JWT tokens and comprehensive tracking';
COMMENT ON TABLE policies IS 'ABAC policies for complex conditional access control';
COMMENT ON TABLE audit_logs IS 'Comprehensive audit trail for security and compliance';

-- =====================================================
-- 12. ESTATÍSTICAS E VERIFICAÇÕES
-- =====================================================

-- Mostrar estatísticas dos dados inseridos
DO $$
BEGIN
    RAISE NOTICE '================================';
    RAISE NOTICE 'DUMMY DATA INSERTION COMPLETED';
    RAISE NOTICE '================================';
    RAISE NOTICE 'Tenants created: %', (SELECT COUNT(*) FROM tenants);
    RAISE NOTICE 'Users created: %', (SELECT COUNT(*) FROM users);
    RAISE NOTICE 'Roles created: %', (SELECT COUNT(*) FROM roles);
    RAISE NOTICE 'Permissions created: %', (SELECT COUNT(*) FROM permissions);
    RAISE NOTICE 'Role-Permission associations: %', (SELECT COUNT(*) FROM roles_permissions);
    RAISE NOTICE 'User-Tenant-Role associations: %', (SELECT COUNT(*) FROM users_tenants_roles);
    RAISE NOTICE 'Active sessions: %', (SELECT COUNT(*) FROM sessions WHERE expires_at > NOW());
    RAISE NOTICE 'Expired sessions: %', (SELECT COUNT(*) FROM sessions WHERE expires_at <= NOW());
    RAISE NOTICE 'Policies created: %', (SELECT COUNT(*) FROM policies);
    RAISE NOTICE 'Audit log entries: %', (SELECT COUNT(*) FROM audit_logs);
    RAISE NOTICE '================================';
    RAISE NOTICE 'READY FOR TESTING!';
    RAISE NOTICE 'Use credentials from README.md';
    RAISE NOTICE '================================';
END $$;

-- =====================================================
-- FIM DO ARQUIVO DUMMY.SQL
-- =====================================================
