-- Migration V11: Criar tabela de logs de auditoria
-- Autor: Sistema de Auditoria
-- Data: 2025-10-02
-- Descrição: Tabela para registrar todas as ações sensíveis do sistema

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    session_id UUID,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50),
    resource_id UUID,
    details TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    success BOOLEAN NOT NULL DEFAULT true,
    error_message VARCHAR(1000),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign keys
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_session FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE SET NULL
);

-- Índices para performance
CREATE INDEX idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_success ON audit_logs(success);
CREATE INDEX idx_audit_ip ON audit_logs(ip_address);

-- Comentários
COMMENT ON TABLE audit_logs IS 'Logs de auditoria para todas as ações sensíveis do sistema';
COMMENT ON COLUMN audit_logs.user_id IS 'ID do usuário que realizou a ação (NULL para ações de sistema)';
COMMENT ON COLUMN audit_logs.session_id IS 'ID da sessão quando a ação foi realizada';
COMMENT ON COLUMN audit_logs.action IS 'Tipo de ação realizada (LOGIN, CREATE_USER, etc.)';
COMMENT ON COLUMN audit_logs.resource_type IS 'Tipo do recurso afetado (USER, ROLE, etc.)';
COMMENT ON COLUMN audit_logs.resource_id IS 'ID do recurso específico afetado';
COMMENT ON COLUMN audit_logs.details IS 'Detalhes adicionais da ação em formato livre';
COMMENT ON COLUMN audit_logs.ip_address IS 'Endereço IP de origem da ação';
COMMENT ON COLUMN audit_logs.user_agent IS 'User Agent do cliente que realizou a ação';
COMMENT ON COLUMN audit_logs.success IS 'Indica se a ação foi bem-sucedida';
COMMENT ON COLUMN audit_logs.error_message IS 'Mensagem de erro caso a ação tenha falhado';
COMMENT ON COLUMN audit_logs.timestamp IS 'Timestamp exato quando a ação ocorreu';
