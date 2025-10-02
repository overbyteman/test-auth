# 🚀 Guia de Deployment Seguro

## ⚡ Setup Rápido (Desenvolvimento)

```bash
# 1. Executar script de configuração automática
./setup-security.sh

# 2. Iniciar aplicação
docker-compose --env-file .env up -d

# 3. Verificar saúde
curl http://localhost:8080/api/auth/health
```

## 🏭 Deployment em Produção

### 1. Preparação do Ambiente

```bash
# Clonar repositório
git clone <seu-repositorio>
cd pasta-sem-titulo

# Executar setup inicial
./setup-security.sh
```

### 2. Configuração de Produção

```bash
# Editar variáveis para produção
nano .env
```

**Configurações obrigatórias para produção:**

```env
# Perfil de produção
SPRING_PROFILES_ACTIVE=prod

# Banco de dados de produção
DB_URL=jdbc:postgresql://seu-host-prod:5432/authdb_prod
DB_USERNAME=auth_user_prod
DB_PASSWORD=SuaSenhaForte123!@#

# JWT com chave única para produção
JWT_SECRET=SuaChaveJWTUnicaDe32CaracteresOuMais

# Configurações de produção
LOGGING_LEVEL_ROOT=WARN
MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=never
```

### 3. Configurar CORS para Produção

Edite `src/main/java/com/seccreto/service/auth/config/SecurityConfig.java`:

```java
.allowedOrigins(
    "https://seuapp.com",
    "https://www.seuapp.com",
    "https://admin.seuapp.com"
)
```

### 4. Build e Deploy

```bash
# Build da aplicação
./gradlew clean build

# Deploy com Docker
docker-compose --env-file .env up -d --build

# Verificar status
docker-compose ps
docker-compose logs -f auth-service
```

## 🔍 Verificações Pós-Deploy

### Testes de Segurança

```bash
# 1. Testar rate limiting (deve bloquear após 5 tentativas)
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@test.com","password":"wrong"}'
done

# 2. Testar CORS (deve rejeitar origem não permitida)
curl -H "Origin: http://malicious-site.com" \
     -H "Access-Control-Request-Method: POST" \
     -H "Access-Control-Request-Headers: Content-Type" \
     -X OPTIONS http://localhost:8080/api/auth/login

# 3. Testar política de senha forte
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "password": "123456"
  }'
```

### Monitoramento

```bash
# Logs de segurança
docker-compose logs auth-service | grep -E "(Rate limit|JWT|Authentication)"

# Métricas de saúde
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics

# Status do banco
docker-compose exec db psql -U $DB_USERNAME -d usersdb -c "SELECT COUNT(*) FROM users;"
```

## 🔧 Configurações Avançadas

### SSL/TLS (Recomendado para Produção)

```yaml
# docker-compose.prod.yml
services:
  nginx:
    image: nginx:alpine
    ports:
      - "443:443"
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/ssl/certs
    depends_on:
      - auth-service

  auth-service:
    expose:
      - "8080"
    # Remove ports para não expor diretamente
```

### Backup Automático

```bash
# Script de backup (backup.sh)
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
docker-compose exec -T db pg_dump -U $DB_USERNAME usersdb > backup_$DATE.sql
aws s3 cp backup_$DATE.sql s3://seu-bucket/backups/
```

### Monitoramento com Prometheus

```yaml
# Adicionar ao docker-compose.yml
  prometheus:
    image: prom/prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin123
```

## 🚨 Troubleshooting

### Problemas Comuns

**1. JWT_SECRET muito curto**
```bash
# Erro: JWT secret must be at least 32 characters
# Solução: Gerar nova chave
openssl rand -base64 32
```

**2. Conexão com banco falha**
```bash
# Verificar se banco está rodando
docker-compose ps db

# Verificar logs do banco
docker-compose logs db

# Testar conexão
docker-compose exec db psql -U $DB_USERNAME -d usersdb -c "SELECT 1;"
```

**3. Rate limiting muito agressivo**
```bash
# Ajustar no AuthController.java
@RateLimit(requests = 10, windowMinutes = 5)  # Aumentar limites
```

**4. CORS bloqueando requests**
```bash
# Verificar origem no SecurityConfig.java
# Adicionar domínio necessário na lista allowedOrigins
```

### Logs Importantes

```bash
# Logs de autenticação
docker-compose logs auth-service | grep "Authentication"

# Logs de rate limiting
docker-compose logs auth-service | grep "Rate limit"

# Logs de erro
docker-compose logs auth-service | grep "ERROR"

# Logs de JWT
docker-compose logs auth-service | grep "JWT"
```

## 📊 Métricas de Performance

### Endpoints de Monitoramento

- **Saúde**: `GET /actuator/health`
- **Métricas**: `GET /actuator/metrics`
- **Info**: `GET /actuator/info`
- **Prometheus**: `GET /actuator/prometheus`

### Alertas Recomendados

1. **Taxa de erro > 5%**
2. **Rate limiting ativado > 100 vezes/hora**
3. **Tentativas de login falhadas > 50/minuto**
4. **Uso de CPU > 80%**
5. **Uso de memória > 85%**
6. **Conexões de banco > 80% do pool**

## 🔄 Atualizações

### Deploy de Nova Versão

```bash
# 1. Backup
./backup.sh

# 2. Pull nova versão
git pull origin main

# 3. Build
./gradlew clean build

# 4. Deploy com zero downtime
docker-compose up -d --no-deps auth-service

# 5. Verificar saúde
curl http://localhost:8080/actuator/health
```

### Rollback

```bash
# Voltar para versão anterior
docker-compose down
git checkout <commit-anterior>
docker-compose up -d --build
```

---

## 📞 Suporte

- **Logs**: `docker-compose logs -f auth-service`
- **Status**: `docker-compose ps`
- **Saúde**: `curl http://localhost:8080/actuator/health`
- **Documentação**: `http://localhost:8080/swagger-ui.html`

**🔒 Lembre-se**: Sempre teste em ambiente de desenvolvimento antes de fazer deploy em produção!
