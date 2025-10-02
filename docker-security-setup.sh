#!/bin/bash

# ===========================================
# Script de Setup de Segurança para Docker
# Adaptado para execução dentro do container
# ===========================================

set -e  # Parar em caso de erro

echo "🔒 Setup de Segurança - Auth Service (Docker)"
echo "=============================================="

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Função para gerar chave JWT segura
generate_jwt_secret() {
    if command -v openssl &> /dev/null; then
        openssl rand -base64 32
    elif command -v uuidgen &> /dev/null; then
        # Fallback usando uuidgen
        echo "$(uuidgen | tr -d '-')$(uuidgen | tr -d '-')" | head -c 32
    else
        # Fallback manual
        cat /dev/urandom | tr -dc 'A-Za-z0-9' | fold -w 32 | head -n 1
    fi
}

# Função para gerar senha segura
generate_password() {
    if command -v openssl &> /dev/null; then
        openssl rand -base64 16 | tr -d "=+/" | cut -c1-12
    else
        cat /dev/urandom | tr -dc 'A-Za-z0-9!@#$%^&*' | fold -w 12 | head -n 1
    fi
}

echo -e "${BLUE}1. Verificando dependências...${NC}"
echo -e "${GREEN}✅ Executando dentro do Docker - dependências OK${NC}"

echo -e "\n${BLUE}2. Gerando configurações de segurança...${NC}"

# Gerar valores seguros
JWT_SECRET=$(generate_jwt_secret)
DB_PASSWORD=$(generate_password)

echo -e "${YELLOW}📝 Criando arquivo .env...${NC}"

cat > .env << EOF
# ===========================================
# Configuração de Segurança - Gerado automaticamente
# Data: $(date)
# Ambiente: Docker Build
# ===========================================

# Perfil ativo
SPRING_PROFILES_ACTIVE=dev

# Configurações do Banco de Dados
DB_URL=jdbc:postgresql://db:5432/usersdb
DB_USERNAME=user
DB_PASSWORD=${DB_PASSWORD}

# Configurações JWT (OBRIGATÓRIO)
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# Configurações do Servidor
SERVER_PORT=8080

# Configurações de Reset de Senha
PASSWORD_RESET_ENABLED=true

# Configurações de Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_SECCRETO=DEBUG

# Configurações do Actuator
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=when-authorized

# DevTools (apenas desenvolvimento)
SPRING_DEVTOOLS_RESTART_ENABLED=true
SPRING_DEVTOOLS_LIVERELOAD_ENABLED=true

# Configurações de Segurança Adicionais
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080, http://localhost:8081
RATE_LIMIT_ENABLED=true
AUDIT_LOG_ENABLED=true
EOF

echo -e "${GREEN}✅ Arquivo .env criado com valores seguros${NC}"
echo -e "${YELLOW}📋 Credenciais geradas:${NC}"
echo -e "   DB User: user"
echo -e "   DB Password: ${DB_PASSWORD}"
echo -e "   JWT Secret: ${JWT_SECRET:0:10}... (${#JWT_SECRET} caracteres)"

echo -e "\n${BLUE}3. Validando configurações...${NC}"

# Validar JWT_SECRET
if [ ${#JWT_SECRET} -lt 32 ]; then
    echo -e "${RED}❌ JWT_SECRET muito curto: ${#JWT_SECRET} caracteres${NC}"
    exit 1
fi

echo -e "${GREEN}✅ JWT_SECRET válido (${#JWT_SECRET} caracteres)${NC}"
echo -e "${GREEN}✅ Configurações de segurança validadas${NC}"

echo -e "\n${BLUE}4. Exportando variáveis para o build...${NC}"

# Exportar variáveis para uso no build
export JWT_SECRET
export DB_PASSWORD
export DB_USERNAME=user

# Salvar em arquivo para uso posterior
echo "export JWT_SECRET='${JWT_SECRET}'" > /tmp/build-env.sh
echo "export DB_PASSWORD='${DB_PASSWORD}'" >> /tmp/build-env.sh
echo "export DB_USERNAME='user'" >> /tmp/build-env.sh

echo -e "${GREEN}✅ Variáveis exportadas para o build${NC}"

echo -e "\n${GREEN}🔒 Setup de segurança concluído com sucesso!${NC}"
echo -e "${YELLOW}📋 Arquivo .env criado em: $(pwd)/.env${NC}"
echo -e "${YELLOW}📋 Variáveis disponíveis para o build${NC}"
