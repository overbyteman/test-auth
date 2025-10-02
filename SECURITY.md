# 🔒 Guia de Segurança

## ✅ Correções de Segurança Implementadas

### 1. **JWT Seguro**
- ❌ **Problema**: Chave JWT padrão insegura
- ✅ **Solução**: Configuração obrigatória via variável de ambiente `JWT_SECRET`
- 🔧 **Configuração**: Mínimo 32 caracteres, gerada com `openssl rand -base64 32`

### 2. **CORS Restritivo**
- ❌ **Problema**: CORS permitindo qualquer origem (`*`)
- ✅ **Solução**: Origens específicas configuradas
- 🔧 **Configuração**: Apenas `localhost:3000`, `localhost:8080` e domínio de produção

### 3. **Logs Seguros**
- ❌ **Problema**: `System.out.println` expondo dados sensíveis
- ✅ **Solução**: Logger SLF4J com sanitização de dados
- 🔧 **Implementação**: Não loga tokens, senhas ou emails completos

### 4. **Política de Senha Forte**
- ❌ **Problema**: Senhas fracas (mínimo 6 caracteres)
- ✅ **Solução**: Política robusta implementada
- 🔧 **Requisitos**:
  - Mínimo 8 caracteres, máximo 128
  - Pelo menos 1 maiúscula, 1 minúscula, 1 número, 1 símbolo
  - Não permite sequências comuns (123456, qwerty, etc.)
  - Não permite mais de 2 caracteres iguais consecutivos

### 5. **Reset de Senha Seguro**
- ❌ **Problema**: Funcionalidade não implementada
- ✅ **Solução**: Sistema completo com tokens seguros
- 🔧 **Características**:
  - Tokens de 32 bytes gerados com `SecureRandom`
  - Expiração em 15 minutos
  - Invalidação automática após uso
  - Invalidação de todas as sessões após reset

### 6. **Rate Limiting**
- ❌ **Problema**: Sem proteção contra força bruta
- ✅ **Solução**: Rate limiting por endpoint
- 🔧 **Configuração**:
  - Login: 5 tentativas/minuto
  - Registro: 3 tentativas/5 minutos
  - Reset senha: 3 tentativas/15 minutos
  - Refresh token: 10 tentativas/minuto

### 7. **Mensagens de Erro Sanitizadas**
- ❌ **Problema**: Vazamento de informações internas
- ✅ **Solução**: Mensagens genéricas para o cliente
- 🔧 **Implementação**: Logs detalhados para debug, respostas sanitizadas

### 8. **Invalidação de Tokens**
- ❌ **Problema**: Logout não invalidava tokens
- ✅ **Solução**: Invalidação completa de sessões
- 🔧 **Implementação**: Remove sessão do banco ao fazer logout

### 9. **Credenciais Seguras**
- ❌ **Problema**: Credenciais hardcoded
- ✅ **Solução**: Variáveis de ambiente obrigatórias
- 🔧 **Configuração**: Arquivo `config/env.example` como template

### 10. **Endpoints Protegidos**
- ❌ **Problema**: Endpoint `/api/users` público
- ✅ **Solução**: Removido acesso público
- 🔧 **Implementação**: Apenas endpoints de auth são públicos

## 🚀 Configuração para Produção

### 1. Variáveis de Ambiente Obrigatórias

```bash
# JWT (OBRIGATÓRIO)
JWT_SECRET=sua_chave_jwt_de_pelo_menos_32_caracteres

# Banco de Dados
DB_URL=jdbc:postgresql://seu-host:5432/database
DB_USERNAME=seu_usuario
DB_PASSWORD=sua_senha_forte

# Aplicação
SPRING_PROFILES_ACTIVE=prod
```

### 2. Gerar Chave JWT Segura

```bash
# Gerar chave de 256 bits (32 bytes)
openssl rand -base64 32

# Ou usar uuidgen para uma alternativa
uuidgen | tr -d '-' | head -c 32
```

### 3. Configurar CORS para Produção

Edite `SecurityConfig.java` e substitua:
```java
.allowedOrigins(
    "http://localhost:3000",
    "http://localhost:8080",
    "https://seudominio.com"  // ← Seu domínio real
)
```

### 4. Deploy Seguro

```bash
# 1. Copiar arquivo de exemplo
cp config/env.example .env

# 2. Editar variáveis
nano .env

# 3. Executar com variáveis
docker-compose --env-file .env up -d
```

## 🔍 Verificações de Segurança

### Checklist Pré-Deploy

- [ ] `JWT_SECRET` configurado com pelo menos 32 caracteres
- [ ] Credenciais de banco não são padrão (`user`/`password`)
- [ ] CORS configurado apenas para domínios necessários
- [ ] Logs não expõem informações sensíveis
- [ ] Rate limiting ativo nos endpoints críticos
- [ ] Endpoints públicos revisados e minimizados

### Testes de Segurança

```bash
# Testar rate limiting
for i in {1..10}; do curl -X POST http://localhost:8080/api/auth/login; done

# Testar CORS
curl -H "Origin: http://malicious-site.com" http://localhost:8080/api/auth/login

# Testar política de senha
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"password": "123456"}'
```

## 🚨 Monitoramento

### Logs de Segurança

Monitore os seguintes padrões nos logs:

```
# Rate limiting ativado
WARN - Rate limit exceeded for user:123

# Tentativas de login falhadas
WARN - Password reset failed: Token inválido

# Tokens inválidos
DEBUG - JWT validation failed: Token expirado
```

### Métricas Importantes

- Taxa de tentativas de login falhadas
- Número de rate limits ativados
- Frequência de resets de senha
- Sessões ativas vs. expiradas

## 📞 Suporte

Para questões de segurança:
1. Verifique este documento primeiro
2. Consulte os logs da aplicação
3. Teste em ambiente de desenvolvimento
4. Contate a equipe de segurança se necessário

---

**⚠️ IMPORTANTE**: Nunca commite arquivos `.env` ou credenciais reais no repositório!
