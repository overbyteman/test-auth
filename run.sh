#!/bin/bash

# ===========================================
# Script para executar a aplicação
# ===========================================

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Função para mostrar ajuda
show_help() {
    echo -e "${BLUE}User Service - Script de Execução${NC}"
    echo ""
    echo "Uso: ./run.sh [ambiente] [comando]"
    echo ""
    echo "Ambientes disponíveis:"
    echo "  dev     - Desenvolvimento (padrão)"
    echo "  stage   - Staging"
    echo "  prod    - Produção"
    echo ""
    echo "Comandos disponíveis:"
    echo "  start   - Iniciar aplicação"
    echo "  build   - Compilar aplicação"
    echo "  test    - Executar testes"
    echo "  docker  - Executar com Docker"
    echo "  help    - Mostrar esta ajuda"
    echo ""
    echo "Exemplos:"
    echo "  ./run.sh dev start"
    echo "  ./run.sh prod docker"
    echo "  ./run.sh stage test"
}

# Função para carregar variáveis de ambiente
load_env() {
    local env=$1
    local env_file="config/${env}.env"
    
    if [ -f "$env_file" ]; then
        echo -e "${GREEN}Carregando configurações de $env...${NC}"
        export $(grep -v '^#' "$env_file" | xargs)
    else
        echo -e "${RED}Arquivo de configuração não encontrado: $env_file${NC}"
        exit 1
    fi
}

# Função para verificar e iniciar Docker
setup_docker() {
    local env=$1
    echo -e "${GREEN}Verificando Docker para $env...${NC}"
    
    # Verificar se Docker está rodando
    if ! docker info > /dev/null 2>&1; then
        echo -e "${RED}Docker não está rodando. Por favor, inicie o Docker Desktop.${NC}"
        exit 1
    fi
    
    # Verificar se a imagem do PostgreSQL existe
    if ! docker images | grep -q "postgres.*16-alpine"; then
        echo -e "${YELLOW}Baixando imagem PostgreSQL 16-alpine...${NC}"
        docker pull postgres:16-alpine
    fi
    
    # Verificar se o container já está rodando
    if docker ps | grep -q "users-postgres"; then
        echo -e "${GREEN}Container PostgreSQL já está rodando.${NC}"
    else
        echo -e "${GREEN}Iniciando container PostgreSQL...${NC}"
        docker-compose up -d db
        
        # Aguardar o banco estar pronto
        echo -e "${YELLOW}Aguardando PostgreSQL estar pronto...${NC}"
        sleep 15
        
        # Verificar se o banco principal está respondendo
        if docker exec users-postgres pg_isready -U users -d usersdb > /dev/null 2>&1; then
            echo -e "${GREEN}PostgreSQL está pronto!${NC}"
            
            # Para stage, verificar se o banco de staging também está pronto
            if [ "$env" = "stage" ]; then
                echo -e "${YELLOW}Verificando banco de staging...${NC}"
                if docker exec users-postgres pg_isready -U stage_user -d usersdb_stage > /dev/null 2>&1; then
                    echo -e "${GREEN}Banco de staging está pronto!${NC}"
                else
                    echo -e "${YELLOW}Aguardando banco de staging...${NC}"
                    sleep 5
                fi
            fi
        else
            echo -e "${RED}Erro: PostgreSQL não está respondendo.${NC}"
            exit 1
        fi
    fi
}

# Função para iniciar aplicação
start_app() {
    local env=$1
    load_env $env
    
    # Para o perfil dev, configurar Docker primeiro
    if [ "$env" = "dev" ]; then
        setup_docker_dev
    fi
    
    echo -e "${GREEN}Iniciando aplicação em modo $env...${NC}"
    ./gradlew bootRun --args="--spring.profiles.active=$env"
}

# Função para compilar aplicação
build_app() {
    echo -e "${GREEN}Compilando aplicação...${NC}"
    ./gradlew clean build -x test
}

# Função para executar testes
run_tests() {
    local env=$1
    load_env $env
    
    echo -e "${GREEN}Executando testes...${NC}"
    ./gradlew test
}

# Função para executar com Docker
run_docker() {
    local env=$1
    load_env $env
    
    echo -e "${GREEN}Executando com Docker...${NC}"
    docker-compose up --build
}

# Função para parar Docker
stop_docker() {
    local env=$1
    echo -e "${YELLOW}Parando containers Docker...${NC}"
    
    if [ "$env" = "dev" ]; then
        # Para dev, parar apenas o banco de dados
        echo -e "${GREEN}Parando container PostgreSQL...${NC}"
        docker-compose stop db
    else
        # Para outros perfis, parar todos os containers
        docker-compose down
    fi
}

# Verificar se Gradle wrapper existe
if [ ! -f "./gradlew" ]; then
    echo -e "${RED}Gradle wrapper não encontrado!${NC}"
    exit 1
fi

# Tornar gradlew executável
chmod +x ./gradlew

# Verificar argumentos
if [ $# -eq 0 ]; then
    show_help
    exit 0
fi

# Processar argumentos
ENV=${1:-dev}
COMMAND=${2:-start}

case $COMMAND in
    "start")
        start_app $ENV
        ;;
    "build")
        build_app
        ;;
    "test")
        run_tests $ENV
        ;;
    "docker")
        run_docker $ENV
        ;;
    "stop")
        stop_docker $ENV
        ;;
    "help"|"-h"|"--help")
        show_help
        ;;
    *)
        echo -e "${RED}Comando inválido: $COMMAND${NC}"
        show_help
        exit 1
        ;;
esac
