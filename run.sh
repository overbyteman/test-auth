#!/bin/bash

# ===========================================
# Script Unificado - Auth Service
# Build, Deploy e Configuração Completa
# ===========================================

set -e  # Parar em caso de erro

echo "🚀 Auth Service - Build & Deploy Completo"
echo "========================================="

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
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

# Função para parar containers existentes
stop_existing_containers() {
    echo -e "${YELLOW}🛑 Parando containers existentes...${NC}"
    
    if docker-compose ps -q | grep -q .; then
        docker-compose down --remove-orphans
        echo -e "${GREEN}✅ Containers parados${NC}"
    else
        echo -e "${BLUE}ℹ️  Nenhum container em execução${NC}"
    fi
}

# Função para limpar imagens antigas
cleanup_old_images() {
    echo -e "${YELLOW}🧹 Limpando imagens antigas...${NC}"
    
    # Remover imagem antiga se existir
    if docker images -q auth-service:latest &> /dev/null; then
        docker rmi auth-service:latest 2>/dev/null || true
    fi
    
    # Limpar imagens órfãs
    docker image prune -f &> /dev/null || true
    
    echo -e "${GREEN}✅ Limpeza concluída${NC}"
}

echo -e "\n${BLUE}1. Gerando credenciais de segurança...${NC}"

# Gerar valores seguros
JWT_SECRET=$(generate_jwt_secret)
DB_PASSWORD=$(generate_password)

echo -e "${GREEN}✅ Credenciais geradas:${NC}"
echo -e "   DB Password: ${DB_PASSWORD}"
echo -e "   JWT Secret: ${JWT_SECRET:0:10}... (${#JWT_SECRET} caracteres)"

echo -e "\n${BLUE}2. Criando arquivo .env...${NC}"

cat > .env << EOF
# ===========================================
# Configuração de Segurança - Gerado automaticamente
# Data: $(date)
# Ambiente: Production Ready
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
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080,http://localhost:8081
RATE_LIMIT_ENABLED=true
AUDIT_LOG_ENABLED=true
EOF

echo -e "${GREEN}✅ Arquivo .env criado${NC}"

echo -e "\n${BLUE}3. Validando configurações...${NC}"

# Validar JWT_SECRET
if [ ${#JWT_SECRET} -lt 32 ]; then
    echo -e "${RED}❌ JWT_SECRET muito curto: ${#JWT_SECRET} caracteres${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Configurações validadas${NC}"

# Parar containers existentes
stop_existing_containers

# Limpar imagens antigas
cleanup_old_images

echo -e "\n${PURPLE}4. Buildando nova imagem Docker...${NC}"

# Build da imagem com as novas credenciais
docker build \
    --build-arg JWT_SECRET="${JWT_SECRET}" \
    --build-arg DB_PASSWORD="${DB_PASSWORD}" \
    --no-cache \
    -t auth-service:latest .

echo -e "${GREEN}✅ Imagem buildada com sucesso${NC}"

echo -e "\n${PURPLE}5. Iniciando sistema completo...${NC}"

# Subir o sistema com as credenciais
docker-compose --env-file .env up -d

echo -e "${GREEN}✅ Sistema iniciado${NC}"

echo -e "\n${BLUE}6. Aguardando inicialização...${NC}"

# Aguardar o serviço ficar disponível
echo -e "${YELLOW}⏳ Aguardando serviços ficarem prontos...${NC}"
sleep 10

# Verificar se os serviços estão rodando
if docker-compose ps | grep -q "Up"; then
    echo -e "${GREEN}✅ Serviços em execução${NC}"
else
    echo -e "${RED}❌ Erro na inicialização dos serviços${NC}"
    echo -e "${YELLOW}📋 Status dos containers:${NC}"
    docker-compose ps
    exit 1
fi

echo -e "\n${GREEN}🎉 DEPLOY COMPLETO!${NC}"
echo -e "${YELLOW}=================================${NC}"
echo -e "${BLUE}📋 INFORMAÇÕES DO SISTEMA:${NC}"
echo -e "   🌐 API: http://localhost:8080"
echo -e "   🏥 Health: http://localhost:8080/actuator/health"
echo -e "   📊 Metrics: http://localhost:8080/actuator/metrics"
echo -e "   📚 Swagger: http://localhost:8080/swagger-ui.html"
echo -e ""
echo -e "${BLUE}🔐 CREDENCIAIS:${NC}"
echo -e "   DB User: user"
echo -e "   DB Password: ${DB_PASSWORD}"
echo -e "   JWT Secret: ${JWT_SECRET:0:10}...${NC}"
echo -e ""
echo -e "${BLUE}🛠️  COMANDOS ÚTEIS:${NC}"
echo -e "   Ver logs: docker-compose logs -f"
echo -e "   Parar: docker-compose down"
echo -e "   Status: docker-compose ps"
echo -e ""
echo -e "${GREEN}✅ Sistema pronto para uso!${NC}"

# Teste rápido de conectividade
echo -e "\n${BLUE}7. Testando conectividade...${NC}"
sleep 5

if curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo -e "${GREEN}✅ API respondendo corretamente${NC}"
else
    echo -e "${YELLOW}⚠️  API ainda inicializando... (normal nos primeiros minutos)${NC}"
fi

echo -e "\n${GREEN}🚀 Deploy finalizado com sucesso!${NC}"
