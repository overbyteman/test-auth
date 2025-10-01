-- ===========================================
-- Flyway Migration: Create Daily Usage Metrics (Functions & Table)
-- Version: V11
-- Description: ACID-compliant daily usage aggregation for users/tenants + helper functions
-- ===========================================

-- Tabela de uso diário agregada (granularidade por dia, usuário, tenant)
CREATE TABLE IF NOT EXISTS daily_user_usage (
    usage_date DATE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    logins INT NOT NULL DEFAULT 0,
    actions INT NOT NULL DEFAULT 0,
    last_action_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (usage_date, user_id, tenant_id)
);

-- Índices auxiliares para queries frequentes
CREATE INDEX IF NOT EXISTS idx_daily_usage_user ON daily_user_usage (user_id);
CREATE INDEX IF NOT EXISTS idx_daily_usage_tenant ON daily_user_usage (tenant_id);
CREATE INDEX IF NOT EXISTS idx_daily_usage_user_tenant ON daily_user_usage (user_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_daily_usage_last_action ON daily_user_usage (last_action_at DESC NULLS LAST);

-- Função de trigger para updated_at
CREATE OR REPLACE FUNCTION trg_daily_user_usage_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_daily_user_usage_set_updated_at ON daily_user_usage;
CREATE TRIGGER trg_daily_user_usage_set_updated_at
    BEFORE UPDATE ON daily_user_usage
    FOR EACH ROW
    EXECUTE FUNCTION trg_daily_user_usage_updated_at();

-- ===========================================
-- FUNÇÃO: registrar login de usuário (idempotente por incremento)
-- ===========================================
CREATE OR REPLACE FUNCTION record_user_login(
    p_user_id UUID,
    p_tenant_id UUID,
    p_usage_date DATE DEFAULT CURRENT_DATE
) RETURNS VOID AS $$
BEGIN
    INSERT INTO daily_user_usage (usage_date, user_id, tenant_id, logins, actions, last_action_at)
    VALUES (p_usage_date, p_user_id, p_tenant_id, 1, 0, NULL)
    ON CONFLICT (usage_date, user_id, tenant_id) DO UPDATE
        SET logins = daily_user_usage.logins + 1,
            updated_at = NOW();
END;$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION record_user_login IS 'Incrementa contador de logins diários de um usuário em um tenant (UPSERT).';

-- ===========================================
-- FUNÇÃO: registrar ação (qualquer ação de negócio)
-- ===========================================
CREATE OR REPLACE FUNCTION record_user_action(
    p_user_id UUID,
    p_tenant_id UUID,
    p_usage_date DATE DEFAULT CURRENT_DATE,
    p_action_at TIMESTAMPTZ DEFAULT NOW()
) RETURNS VOID AS $$
BEGIN
    INSERT INTO daily_user_usage (usage_date, user_id, tenant_id, logins, actions, last_action_at)
    VALUES (p_usage_date, p_user_id, p_tenant_id, 0, 1, p_action_at)
    ON CONFLICT (usage_date, user_id, tenant_id) DO UPDATE
        SET actions = daily_user_usage.actions + 1,
            last_action_at = GREATEST(daily_user_usage.last_action_at, p_action_at),
            updated_at = NOW();
END;$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION record_user_action IS 'Incrementa contador de ações diárias e atualiza last_action_at (UPSERT).';

-- ===========================================
-- FUNÇÃO: obter uso agregado em intervalo (retorno tabular)
-- ===========================================
CREATE OR REPLACE FUNCTION get_user_daily_usage(
    p_user_id UUID,
    p_start DATE,
    p_end DATE,
    p_tenant_id UUID DEFAULT NULL
) RETURNS TABLE (
    usage_date DATE,
    tenant_id UUID,
    logins INT,
    actions INT,
    last_action_at TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        d.usage_date,
        d.tenant_id,
        d.logins,
        d.actions,
        d.last_action_at
    FROM daily_user_usage d
    WHERE d.user_id = p_user_id
      AND d.usage_date BETWEEN p_start AND p_end
      AND (p_tenant_id IS NULL OR d.tenant_id = p_tenant_id)
    ORDER BY d.usage_date ASC;
END;$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION get_user_daily_usage IS 'Retorna métricas diárias (logins, ações) para um usuário em intervalo, opcionalmente filtrando por tenant.';

-- ===========================================
-- FUNÇÃO: top usuários por ações em intervalo
-- ===========================================
CREATE OR REPLACE FUNCTION get_top_active_users(
    p_start DATE,
    p_end DATE,
    p_tenant_id UUID DEFAULT NULL,
    p_limit INT DEFAULT 10
) RETURNS TABLE (
    user_id UUID,
    total_actions BIGINT,
    total_logins BIGINT,
    last_action_at TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        d.user_id,
        SUM(d.actions) AS total_actions,
        SUM(d.logins) AS total_logins,
        MAX(d.last_action_at) AS last_action_at
    FROM daily_user_usage d
    WHERE d.usage_date BETWEEN p_start AND p_end
      AND (p_tenant_id IS NULL OR d.tenant_id = p_tenant_id)
    GROUP BY d.user_id
    ORDER BY total_actions DESC, total_logins DESC
    LIMIT p_limit;
END;$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION get_top_active_users IS 'Lista top usuários por ações em intervalo de datas, opcionalmente filtrando por tenant.';

-- ===========================================
-- FUNÇÃO: limpeza de linhas antigas (retenção)
-- ===========================================
CREATE OR REPLACE FUNCTION cleanup_old_usage(
    p_keep_days INT DEFAULT 180
) RETURNS INTEGER AS $$
DECLARE
    v_deleted INT;
BEGIN
    DELETE FROM daily_user_usage
    WHERE usage_date < CURRENT_DATE - p_keep_days;
    GET DIAGNOSTICS v_deleted = ROW_COUNT;
    RAISE NOTICE 'Deleted % old usage rows (retention % days)', v_deleted, p_keep_days;
    RETURN v_deleted;
END;$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_old_usage IS 'Remove métricas antigas além da retenção definida (default 180 dias).';

-- Comentários de tabela / colunas
COMMENT ON TABLE daily_user_usage IS 'Agregação diária de logins e ações por usuário e tenant.';
COMMENT ON COLUMN daily_user_usage.usage_date IS 'Data (UTC) da agregação.';
COMMENT ON COLUMN daily_user_usage.user_id IS 'Usuário (FK).';
COMMENT ON COLUMN daily_user_usage.tenant_id IS 'Tenant (FK).';
COMMENT ON COLUMN daily_user_usage.logins IS 'Contagem de logins no dia.';
COMMENT ON COLUMN daily_user_usage.actions IS 'Contagem de ações registradas no dia.';
COMMENT ON COLUMN daily_user_usage.last_action_at IS 'Timestamp da ação mais recente no dia.';
COMMENT ON COLUMN daily_user_usage.created_at IS 'Criado em.';
COMMENT ON COLUMN daily_user_usage.updated_at IS 'Atualizado em.';

