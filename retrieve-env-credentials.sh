#!/bin/bash

# ===========================================
# Script Unificado de Credenciais
# Extrai e configura credenciais do build Docker
# ===========================================

echo "🔐 Configurando Credenciais do Auth Service"
echo "==========================================="

# Tentar extrair do container em execução
if docker exec auth-service cat /build/.env > .env 2>/dev/null; then
    echo "✅ Credenciais extraídas do container em execução!"
    SOURCE="container em execução"
# Tentar extrair via container temporário  
elif docker create --name temp-extract auth-service:latest >/dev/null 2>&1 && \
     docker cp temp-extract:/build/.env .env 2>/dev/null; then
    docker rm temp-extract >/dev/null 2>&1
    echo "✅ Credenciais extraídas via container temporário!"
    SOURCE="container temporário"
# Usar credenciais conhecidas do último build
else
    echo "🔧 Usando credenciais do último build conhecido..."
    cat > .env << 'EOF'
# ===========================================
# Credenciais do Build Docker
# ===========================================
SPRING_PROFILES_ACTIVE=dev
DB_USERNAME=user
DB_PASSWORD=Q1CNHOSBWrCT
JWT_SECRET=1j57iyoDDBBR/Ecw1ZHW7raYEDe1U4RnW+kS5HV3pVE=
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080
RATE_LIMIT_ENABLED=true
AUDIT_LOG_ENABLED=true
EOF
    SOURCE="build conhecido"
fi

# Extrair e mostrar credenciais
if [ -f .env ]; then
    DB_USER=$(grep "^DB_USERNAME=" .env | cut -d'=' -f2)
    DB_PASS=$(grep "^DB_PASSWORD=" .env | cut -d'=' -f2)
    JWT_SEC=$(grep "^JWT_SECRET=" .env | cut -d'=' -f2)
    
    echo ""
    echo "📋 CREDENCIAIS CONFIGURADAS ($SOURCE)"
    echo "=================================="
    echo "DB_USERNAME: $DB_USER"
    echo "DB_PASSWORD: $DB_PASS"
    echo "JWT_SECRET: ${JWT_SEC:0:10}... (${#JWT_SEC} chars)"
    echo "=================================="
    echo ""
    echo "🚀 Para iniciar o sistema:"
    echo "   docker-compose --env-file .env up -d"
    echo ""
    echo "🔍 Para verificar:"
    echo "   docker-compose ps"
    echo "   curl http://localhost:8080/actuator/health"
    echo ""
    echo "✅ Sistema pronto para uso!"
else
    echo "❌ Erro ao criar arquivo .env"
    exit 1
fi
