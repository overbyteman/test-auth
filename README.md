# 🔐 Auth Service - Sistema Avançado de Autenticação e Autorização

## 📋 Índice

- [Visão Geral](#-visão-geral)
- [Funcionalidades](#-funcionalidades)
- [Tecnologias](#️-tecnologias)
- [Instalação](#-instalação)
- [Configuração](#️-configuração)
- [API Reference](#-api-reference)
- [Segurança](#️-segurança)
- [Exemplos de Uso](#-exemplos-de-uso)
- [Swagger/OpenAPI](#-swaggeropenapi)
- [Contribuição](#-contribuição)

## 🎯 Visão Geral

**Auth Service** é um sistema completo de autenticação e autorização desenvolvido em **Spring Boot** com suporte a:

- 🏢 **Multi-tenancy** (múltiplas organizações)
- 🛡️ **RBAC** (Role-Based Access Control)
- 🎭 **ABAC** (Attribute-Based Access Control) 
- 🔒 **JWT** com Access e Refresh Tokens
- 🚦 **Rate Limiting** inteligente
- 📊 **Audit Logs** completos
- 🛡️ **Validações de Segurança** avançadas

## ✨ Funcionalidades

### 🔐 Autenticação
- ✅ Login/Logout com JWT
- ✅ Registro de usuários
- ✅ Recuperação de senha
- ✅ Refresh Token automático
- ✅ Validação de força de senha
- ✅ Proteção contra SQL Injection/XSS

### 👥 Gestão de Usuários
- ✅ CRUD completo de usuários
- ✅ Perfis de usuário
- ✅ Associação a múltiplos tenants
- ✅ Ativação/Desativação
- ✅ Gestão de sessões

### 🏢 Multi-Tenancy
- ✅ Isolamento por tenant
- ✅ Configurações personalizáveis
- ✅ Roles específicos por tenant
- ✅ Políticas de acesso por tenant

### 🛡️ Segurança Avançada
- ✅ Rate Limiting por usuário/IP
- ✅ Validação de domínios de email
- ✅ Detecção de ataques automatizada
- ✅ Logs de auditoria completos
- ✅ Criptografia BCrypt

### 📊 Relatórios e Métricas
- ✅ Dashboard administrativo
- ✅ Métricas de uso em tempo real
- ✅ Relatórios de segurança
- ✅ Análise de sessões

## 🛠️ Tecnologias

| Categoria | Tecnologia | Versão |
|-----------|------------|--------|
| **Backend** | Java | 21 |
| **Framework** | Spring Boot | 3.2+ |
| **Segurança** | Spring Security | 6.x |
| **Banco** | PostgreSQL | 15+ |
| **Cache** | Redis (Opcional) | 7+ |
| **Migração** | Flyway | 9.x |
| **Build** | Gradle | 8.5 |
| **Container** | Docker & Docker Compose | Latest |
| **Documentação** | OpenAPI/Swagger | 3.0 |

## 🚀 Instalação

### Pré-requisitos

- ☕ **Java 21+**
- 🐘 **PostgreSQL 15+**
- 🐳 **Docker & Docker Compose**
- 🔧 **Git**

### 1. Clone o Repositório

```bash
git clone https://github.com/seu-usuario/auth-service.git
cd auth-service
```

### 2. Configuração com Docker (Recomendado)

```bash
# Inicia toda a infraestrutura
docker-compose up -d

# Verifica se está funcionando
curl http://localhost:8080/actuator/health
```

### 3. Configuração Manual

```bash
# 1. Configure o PostgreSQL
createdb auth_service

# 2. Configure as variáveis de ambiente
cp config/dev.env .env

# 3. Execute as migrações
./gradlew flywayMigrate

# 4. Inicie a aplicação
./gradlew bootRun
```

## ⚙️ Configuração

### Variáveis de Ambiente

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/auth_service
DB_USERNAME=postgres
DB_PASSWORD=postgres

# JWT
JWT_SECRET=seu-jwt-secret-super-seguro-aqui
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# Server
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev

# Rate Limiting
RATE_LIMIT_ENABLED=true
RATE_LIMIT_DEFAULT_REQUESTS=100
RATE_LIMIT_DEFAULT_WINDOW_MINUTES=1
```

### Configuração de Perfis

```yaml
# application-dev.yml
spring:
  profile: dev
  jpa:
    show-sql: true
  logging:
    level:
      com.seccreto: DEBUG

# application-prod.yml  
spring:
  profile: prod
  jpa:
    show-sql: false
  logging:
    level:
      com.seccreto: INFO
```

## 📚 API Reference

### Base URL
```
http://localhost:8080/api
```

### 🔐 Autenticação

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "usuario@empresa.com",
  "password": "MinhaSenh@987!"
}
```

**Resposta:**
```json
{
  "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
  "refreshToken": "eyJhbGciOiJIUzM4NCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "userName": "João Silva",
  "userEmail": "usuario@empresa.com",
  "loginTime": "2025-10-02T15:30:45"
}
```

#### Registro
```http
POST /api/auth/register
Content-Type: application/json

{
  "name": "João Silva",
  "email": "joao@empresa.com", 
  "password": "MinhaSenh@987!"
}
```

#### Refresh Token
```http
POST /api/auth/refresh-token
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzM4NCJ9..."
}
```

### 👥 Gestão de Usuários

#### Listar Usuários
```http
GET /api/users
Authorization: Bearer {accessToken}
```

#### Criar Usuário
```http
POST /api/users
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "Maria Santos",
  "email": "maria@empresa.com",
  "password": "SenhaSegur@123!"
}
```

#### Buscar Usuário
```http
GET /api/users/{userId}
Authorization: Bearer {accessToken}
```

#### Atualizar Usuário
```http
PUT /api/users/{userId}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "Maria Santos Silva",
  "email": "maria.santos@empresa.com"
}
```

### 🏢 Gestão de Tenants

#### Listar Tenants
```http
GET /api/tenants
Authorization: Bearer {accessToken}
```

#### Criar Tenant
```http
POST /api/tenants
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "Empresa XYZ",
  "config": {
    "theme": "dark",
    "features": ["advanced_reports", "api_access"]
  }
}
```

### 🛡️ Roles e Permissões

#### Listar Roles
```http
GET /api/roles
Authorization: Bearer {accessToken}
```

#### Associar Role a Usuário
```http
POST /api/users-tenants-roles
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "550e8400-e29b-41d4-a716-446655440001", 
  "roleId": "550e8400-e29b-41d4-a716-446655440002"
}
```

### 📊 Dashboard e Métricas

#### Métricas Gerais
```http
GET /api/dashboard/metrics?type=overview
Authorization: Bearer {accessToken}
```

#### Relatório de Usuários
```http
GET /api/dashboard/reports?type=users&period=last_30_days
Authorization: Bearer {accessToken}
```

## 🛡️ Segurança

### Validações Implementadas

#### 🔐 Força de Senha
- ✅ Mínimo 8 caracteres
- ✅ Pelo menos 1 maiúscula (A-Z)
- ✅ Pelo menos 1 minúscula (a-z)
- ✅ Pelo menos 1 número (0-9)
- ✅ Pelo menos 1 caractere especial (!@#$...)
- ❌ Senhas comuns bloqueadas (password, 123456)
- ❌ Sequências bloqueadas (123, abc)
- ❌ Repetições excessivas (aaa, 111)

#### 📧 Domínios de Email
- ❌ Emails temporários bloqueados (10minutemail.com)
- ❌ Domínios específicos bloqueados
- ✅ Lista branca configurável
- ✅ Modo corporativo (bloqueia gmail, yahoo)

#### 🚫 Proteções Gerais
- ❌ SQL Injection
- ❌ XSS (Cross-Site Scripting)
- ❌ Code Injection
- ❌ Palavras proibidas (admin, root, test)
- ✅ Rate Limiting inteligente
- ✅ Sanitização automática

### Rate Limiting

| Endpoint | Limite | Janela |
|----------|--------|--------|
| `/api/auth/login` | 5 req | 1 min |
| `/api/auth/register` | 3 req | 5 min |
| `/api/auth/refresh-token` | 10 req | 1 min |
| `/api/users` (POST) | 5 req | 1 min |
| Outros endpoints | 100 req | 1 min |

## 💡 Exemplos de Uso

### Cenário 1: Sistema Corporativo

```bash
# 1. Registrar empresa
curl -X POST http://localhost:8080/api/tenants \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corp",
    "config": {
      "domain": "acme.com",
      "features": ["sso", "ldap_sync"]
    }
  }'

# 2. Criar administrador
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Admin Acme",
    "email": "admin@acme.com",
    "password": "AdminSecur@2024!"
  }'

# 3. Associar role de admin
curl -X POST http://localhost:8080/api/users-tenants-roles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "{userId}",
    "tenantId": "{tenantId}",
    "roleId": "{adminRoleId}"
  }'
```

### Cenário 2: Aplicação SaaS

```bash
# 1. Login do usuário
LOGIN_RESPONSE=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "usuario@empresa.com",
    "password": "MinhaSenh@987!"
  }')

# 2. Extrair token
TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.accessToken')

# 3. Acessar recursos protegidos
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN"

# 4. Renovar token quando expirar
curl -X POST http://localhost:8080/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "'{refreshToken}'"
  }'
```

### Cenário 3: Auditoria e Compliance

```bash
# 1. Consultar logs de auditoria
curl -X GET "http://localhost:8080/api/dashboard/reports?type=audit&startDate=2025-10-01&endDate=2025-10-02" \
  -H "Authorization: Bearer $TOKEN"

# 2. Relatório de acessos por usuário
curl -X GET "http://localhost:8080/api/dashboard/reports?type=user_access&userId={userId}" \
  -H "Authorization: Bearer $TOKEN"

# 3. Métricas de segurança
curl -X GET "http://localhost:8080/api/dashboard/metrics?type=security" \
  -H "Authorization: Bearer $TOKEN"
```

## 📖 Swagger/OpenAPI

Acesse a documentação interativa da API:

```
http://localhost:8080/swagger-ui.html
```

### Credenciais para Teste

Use as credenciais padrão criadas pelo `dummy.sql`:

| Usuário | Email | Senha | Role |
|---------|-------|-------|------|
| Super Admin | superadmin@empresa.com | SuperAdmin@2024! | SUPER_ADMIN |
| Administrador | admin@empresa.com | Admin@2024! | ADMIN |
| Gerente | manager@empresa.com | Manager@2024! | MANAGER |
| Usuário | user@empresa.com | User@2024! | USER |

## 🧪 Testes

### Executar Testes

```bash
# Todos os testes
./gradlew test

# Testes de integração
./gradlew integrationTest

# Testes de segurança
./gradlew securityTest

# Coverage report
./gradlew jacocoTestReport
```

### Testes de Carga

```bash
# Usando Apache Bench
ab -n 1000 -c 10 -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/users

# Usando wrk
wrk -t12 -c400 -d30s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/dashboard/metrics
```

## 🐳 Docker

### Comandos Úteis

```bash
# Build da imagem
docker build -t auth-service:latest .

# Executar apenas a aplicação
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/auth_service \
  auth-service:latest

# Ver logs
docker-compose logs -f auth-service

# Backup do banco
docker-compose exec postgres pg_dump -U postgres auth_service > backup.sql

# Restore do banco
cat backup.sql | docker-compose exec -T postgres psql -U postgres auth_service
```

## 📊 Monitoramento

### Health Checks

```bash
# Saúde da aplicação
curl http://localhost:8080/actuator/health

# Métricas detalhadas
curl http://localhost:8080/actuator/metrics

# Info da aplicação
curl http://localhost:8080/actuator/info
```

### Prometheus Metrics

```bash
# Metrics para Prometheus
curl http://localhost:8080/actuator/prometheus
```

## 🔧 Troubleshooting

### Problemas Comuns

#### 1. Erro de Conexão com Banco
```bash
# Verificar se PostgreSQL está rodando
docker-compose ps postgres

# Ver logs do banco
docker-compose logs postgres
```

#### 2. JWT Token Inválido
```bash
# Verificar configuração do JWT_SECRET
echo $JWT_SECRET

# Regenerar token
curl -X POST http://localhost:8080/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "seu_refresh_token"}'
```

#### 3. Rate Limit Atingido
```bash
# Aguardar janela de tempo ou aumentar limite
# Verificar headers de rate limit na resposta
curl -I http://localhost:8080/api/auth/login
```

## 📈 Performance

### Benchmarks

| Endpoint | Latência P95 | Throughput |
|----------|--------------|------------|
| `/api/auth/login` | < 200ms | 500 RPS |
| `/api/users` (GET) | < 100ms | 1000 RPS |
| `/api/dashboard/metrics` | < 300ms | 200 RPS |

### Otimizações

- ✅ Indexes otimizados no PostgreSQL
- ✅ Pool de conexões configurado
- ✅ Cache de queries frequentes
- ✅ Compressão gzip habilitada
- ✅ Lazy loading de relacionamentos

## 🤝 Contribuição

1. **Fork** o projeto
2. **Clone** seu fork
3. **Crie** uma branch para sua feature (`git checkout -b feature/amazing-feature`)
4. **Commit** suas mudanças (`git commit -m 'Add amazing feature'`)
5. **Push** para a branch (`git push origin feature/amazing-feature`)
6. **Abra** um Pull Request

### Padrões de Código

- ✅ Java 21+ features
- ✅ Spring Boot best practices
- ✅ Clean Architecture principles
- ✅ SOLID principles
- ✅ 80%+ test coverage
- ✅ Javadoc para APIs públicas

## 📄 Licença

Este projeto está licenciado sob a **MIT License** - veja o arquivo [LICENSE](LICENSE) para detalhes.

## 👥 Equipe

- **Desenvolvimento Backend**: [Seu Nome](https://github.com/seu-usuario)
- **Arquitetura de Segurança**: [Seu Nome](https://github.com/seu-usuario)
- **DevOps**: [Seu Nome](https://github.com/seu-usuario)

## 📞 Suporte

- 📧 **Email**: suporte@empresa.com
- 💬 **Slack**: #auth-service
- 🐛 **Issues**: [GitHub Issues](https://github.com/seu-usuario/auth-service/issues)
- 📚 **Wiki**: [Confluence](https://empresa.atlassian.net/wiki/auth-service)

---

**🔐 Auth Service** - *Segurança e Autenticação de Classe Empresarial*

*Desenvolvido com ❤️ usando Spring Boot 3.2+ e Java 21*
