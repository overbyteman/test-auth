#!/bin/bash

# ===========================================
# Script de Setup de Segurança
# Configura automaticamente as variáveis de ambiente necessárias
# ===========================================

set -e  # Parar em caso de erro

echo "🔒 Setup de Segurança - Auth Service"
echo "===================================="

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

# Verificar se Docker está instalado
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker não encontrado. Instale o Docker primeiro.${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo -e "${RED}❌ Docker Compose não encontrado. Instale o Docker Compose primeiro.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Dependências verificadas${NC}"

echo -e "\n${BLUE}2. Configurando variáveis de ambiente...${NC}"

# Criar arquivo .env se não existir
if [ ! -f .env ]; then
    echo -e "${YELLOW}📝 Criando arquivo .env...${NC}"
    
    # Gerar valores seguros
    JWT_SECRET=$(generate_jwt_secret)
    DB_PASSWORD=$(generate_password)
    
    cat > .env << EOF
# ===========================================
# Configuração de Segurança - Gerado automaticamente
# Data: $(date)
# ===========================================

# Perfil ativo
SPRING_PROFILES_ACTIVE=dev

# Configurações do Banco de Dados
DB_URL=jdbc:postgresql://localhost:5432/usersdb
DB_USERNAME=authuser
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
LOGGING_LEVEL_COM_EXAMPLE=DEBUG

# Configurações do Actuator
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=when-authorized

# DevTools (apenas desenvolvimento)
SPRING_DEVTOOLS_RESTART_ENABLED=true
SPRING_DEVTOOLS_LIVERELOAD_ENABLED=true
EOF

    echo -e "${GREEN}✅ Arquivo .env criado com valores seguros${NC}"
    echo -e "${YELLOW}📋 Credenciais geradas:${NC}"
    echo -e "   DB User: authuser"
    echo -e "   DB Password: ${DB_PASSWORD}"
    echo -e "   JWT Secret: ${JWT_SECRET:0:10}... (32 caracteres)"
else
    echo -e "${YELLOW}⚠️  Arquivo .env já existe. Verificando configurações...${NC}"
    
    # Verificar se JWT_SECRET existe e tem tamanho adequado
    if grep -q "JWT_SECRET=" .env; then
        JWT_SECRET_VALUE=$(grep "JWT_SECRET=" .env | cut -d'=' -f2)
        if [ ${#JWT_SECRET_VALUE} -lt 32 ]; then
            echo -e "${RED}❌ JWT_SECRET muito curto (${#JWT_SECRET_VALUE} caracteres). Mínimo: 32${NC}"
            NEW_JWT_SECRET=$(generate_jwt_secret)
            sed -i.bak "s/JWT_SECRET=.*/JWT_SECRET=${NEW_JWT_SECRET}/" .env
            echo -e "${GREEN}✅ JWT_SECRET atualizado${NC}"
        else
            echo -e "${GREEN}✅ JWT_SECRET válido${NC}"
        fi
    else
        echo -e "${RED}❌ JWT_SECRET não encontrado${NC}"
        NEW_JWT_SECRET=$(generate_jwt_secret)
        echo "JWT_SECRET=${NEW_JWT_SECRET}" >> .env
        echo -e "${GREEN}✅ JWT_SECRET adicionado${NC}"
    fi
fi

echo -e "\n${BLUE}3. Verificando configurações de segurança...${NC}"

# Verificar se CORS está configurado corretamente
if grep -q "allowedOriginPatterns.*\*" src/main/java/com/seccreto/service/auth/config/SecurityConfig.java 2>/dev/null; then
    echo -e "${RED}❌ CORS ainda permite todas as origens (*)${NC}"
    echo -e "${YELLOW}   Edite SecurityConfig.java para configurar origens específicas${NC}"
else
    echo -e "${GREEN}✅ CORS configurado de forma restritiva${NC}"
fi

# Verificar se não há System.out.println
SYSTEM_OUT_COUNT=$(find src -name "*.java" -exec grep -l "System\.out\." {} \; 2>/dev/null | wc -l)
if [ "$SYSTEM_OUT_COUNT" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  Encontrados ${SYSTEM_OUT_COUNT} arquivos com System.out${NC}"
else
    echo -e "${GREEN}✅ Nenhum System.out.println encontrado${NC}"
fi

echo -e "\n${BLUE}4. Testando configuração...${NC}"

# Testar se o arquivo .env está válido
if [ -f .env ]; then
    source .env
    if [ -z "$JWT_SECRET" ]; then
        echo -e "${RED}❌ JWT_SECRET não está definido${NC}"
        exit 1
    fi
    
    if [ ${#JWT_SECRET} -lt 32 ]; then
        echo -e "${RED}❌ JWT_SECRET muito curto: ${#JWT_SECRET} caracteres${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ Configurações válidas${NC}"
fi

echo -e "\n${BLUE}5. Próximos passos...${NC}"
echo -e "${GREEN}✅ Setup de segurança concluído!${NC}"
echo ""
echo -e "${YELLOW}📋 Para iniciar a aplicação:${NC}"
echo "   docker-compose --env-file .env up -d"
echo ""
echo -e "${YELLOW}📋 Para verificar logs:${NC}"
echo "   docker-compose logs -f auth-service"
echo ""
echo -e "${YELLOW}📋 Para testar a API:${NC}"
echo "   curl http://localhost:8080/api/auth/health"
echo ""
echo -e "${YELLOW}📋 Para produção:${NC}"
echo "   1. Edite .env com suas configurações reais"
echo "   2. Configure CORS no SecurityConfig.java"
echo "   3. Use SPRING_PROFILES_ACTIVE=prod"
echo ""
echo -e "${RED}⚠️  IMPORTANTE:${NC}"
echo -e "   - Nunca commite o arquivo .env"
echo -e "   - Mantenha as credenciais seguras"
echo -e "   - Configure backup das chaves JWT"
echo ""
echo -e "${GREEN}🔒 Sistema configurado com segurança!${NC}"
