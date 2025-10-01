-- Script para criar múltiplos bancos de dados e usuários
-- Este script é executado automaticamente pelo PostgreSQL no primeiro boot

-- Criar banco para staging
CREATE DATABASE usersdb_stage;

-- Criar usuário para staging
CREATE USER stage_user WITH PASSWORD 'stage_password';
GRANT ALL PRIVILEGES ON DATABASE usersdb_stage TO stage_user;

-- Conectar ao banco de staging e criar schema
\c usersdb_stage;
CREATE SCHEMA IF NOT EXISTS public;
GRANT ALL ON SCHEMA public TO stage_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO stage_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO stage_user;

-- Voltar ao banco principal
\c usersdb;

-- Garantir que o usuário principal tenha todas as permissões
GRANT ALL PRIVILEGES ON DATABASE usersdb TO "user";
GRANT ALL PRIVILEGES ON DATABASE usersdb_stage TO "user";
