# ✅ Checklist de Segurança - Auth Service

## 🔒 Verificações Obrigatórias Antes do Deploy

### 1. Configuração JWT
- [ ] `JWT_SECRET` configurado com pelo menos 32 caracteres
- [ ] `JWT_SECRET` único para cada ambiente (dev/stage/prod)
- [ ] Tempo de expiração adequado (1h para access, 7d para refresh)
- [ ] Validação de token implementada corretamente

### 2. Credenciais e Variáveis de Ambiente
- [ ] Nenhuma credencial hardcoded no código
- [ ] Arquivo `.env` configurado corretamente
- [ ] Credenciais de banco fortes e únicas
- [ ] Variáveis de ambiente obrigatórias definidas

### 3. CORS e Rede
- [ ] CORS configurado apenas para domínios necessários
- [ ] Nenhum wildcard (`*`) em produção
- [ ] Headers de segurança configurados
- [ ] Endpoints públicos minimizados

### 4. Autenticação e Autorização
- [ ] Política de senha forte implementada (8+ chars, maiúscula, minúscula, número, símbolo)
- [ ] Rate limiting ativo em endpoints críticos
- [ ] Sessões invalidadas corretamente no logout
- [ ] Reset de senha com tokens seguros e expiração

### 5. Logs e Auditoria
- [ ] Nenhum `System.out.println` no código
- [ ] Logs não expõem informações sensíveis
- [ ] Auditoria de ações críticas implementada
- [ ] Mensagens de erro sanitizadas

### 6. Banco de Dados
- [ ] Conexões com pool configurado
- [ ] Credenciais de banco não são padrão
- [ ] Migrações de banco versionadas
- [ ] Backup automático configurado

## 🧪 Testes de Segurança

### Comandos de Verificação

```bash
# 1. Verificar se JWT_SECRET está configurado
grep "JWT_SECRET" .env | wc -c  # Deve ser > 40

# 2. Testar rate limiting
for i in {1..10}; do curl -X POST http://localhost:8080/api/auth/login -d '{"email":"test","password":"wrong"}' -H "Content-Type: application/json"; done

# 3. Testar política de senha
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{"name":"Test","email":"test@test.com","password":"123"}'

# 4. Testar CORS
curl -H "Origin: http://malicious.com" -X OPTIONS http://localhost:8080/api/auth/login

# 5. Verificar endpoints protegidos
curl http://localhost:8080/api/users  # Deve retornar 401/403
```

### Resultados Esperados

| Teste | Resultado Esperado |
|-------|-------------------|
| JWT_SECRET length | > 32 caracteres |
| Rate limiting | HTTP 429 após 5 tentativas |
| Senha fraca | HTTP 400 com mensagem de erro |
| CORS malicioso | Bloqueado |
| Endpoint protegido | HTTP 401/403 |

## 🚨 Indicadores de Problemas

### ❌ Sinais de Alerta

- Logs com senhas ou tokens em texto claro
- Mensagens de erro com stack traces completos
- CORS permitindo qualquer origem
- Endpoints críticos sem autenticação
- Rate limiting não funcionando
- JWT_SECRET padrão ou muito curto

### ✅ Sinais de Segurança Adequada

- Logs sanitizados sem informações sensíveis
- Mensagens de erro genéricas para o cliente
- CORS restritivo apenas para domínios necessários
- Todos endpoints críticos protegidos
- Rate limiting ativo e funcionando
- JWT_SECRET único e forte

## 📊 Monitoramento Contínuo

### Métricas de Segurança

```bash
# Tentativas de login falhadas (últimas 24h)
docker-compose logs --since 24h auth-service | grep "Failed login" | wc -l

# Rate limiting ativado (últimas 24h)
docker-compose logs --since 24h auth-service | grep "Rate limit exceeded" | wc -l

# Tentativas de acesso não autorizado
docker-compose logs --since 24h auth-service | grep -E "(401|403)" | wc -l

# Resets de senha solicitados
docker-compose logs --since 24h auth-service | grep "Password recovery" | wc -l
```

### Alertas Recomendados

1. **> 100 tentativas de login falhadas/hora**
2. **> 50 rate limits ativados/hora**
3. **> 20 tentativas de acesso não autorizado/hora**
4. **> 10 resets de senha/hora**
5. **Qualquer log com "SECURITY_VIOLATION"**

## 🔧 Ferramentas de Verificação

### Script de Verificação Automática

```bash
#!/bin/bash
# security-check.sh

echo "🔍 Verificação de Segurança"
echo "=========================="

# 1. Verificar JWT_SECRET
JWT_LEN=$(grep "JWT_SECRET=" .env | cut -d'=' -f2 | wc -c)
if [ $JWT_LEN -gt 32 ]; then
    echo "✅ JWT_SECRET: OK ($JWT_LEN caracteres)"
else
    echo "❌ JWT_SECRET: Muito curto ($JWT_LEN caracteres)"
fi

# 2. Verificar System.out
SYSTEM_OUT=$(find src -name "*.java" -exec grep -l "System\.out\." {} \; | wc -l)
if [ $SYSTEM_OUT -eq 0 ]; then
    echo "✅ System.out: Nenhum encontrado"
else
    echo "❌ System.out: $SYSTEM_OUT arquivos encontrados"
fi

# 3. Verificar CORS
CORS_WILDCARD=$(grep -r "allowedOriginPatterns.*\*" src/ | wc -l)
if [ $CORS_WILDCARD -eq 0 ]; then
    echo "✅ CORS: Configuração restritiva"
else
    echo "❌ CORS: Wildcard detectado"
fi

# 4. Verificar credenciais padrão
DEFAULT_CREDS=$(grep -E "(password|user)" docker-compose.yml | grep -v "\${" | wc -l)
if [ $DEFAULT_CREDS -eq 0 ]; then
    echo "✅ Credenciais: Usando variáveis de ambiente"
else
    echo "❌ Credenciais: Valores hardcoded detectados"
fi

echo ""
echo "🔒 Verificação concluída!"
```

### Integração com CI/CD

```yaml
# .github/workflows/security-check.yml
name: Security Check
on: [push, pull_request]

jobs:
  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Run Security Checklist
        run: |
          chmod +x security-check.sh
          ./security-check.sh
          
      - name: Check for hardcoded secrets
        run: |
          if grep -r "password.*=" src/ --include="*.java"; then
            echo "❌ Hardcoded passwords found"
            exit 1
          fi
          
      - name: Verify JWT configuration
        run: |
          if ! grep -q "JWT_SECRET.*\${" src/main/resources/application.yml; then
            echo "❌ JWT_SECRET not using environment variable"
            exit 1
          fi
```

## 📚 Recursos Adicionais

### Documentação de Segurança

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)

### Ferramentas Recomendadas

- **SAST**: SonarQube, Checkmarx
- **DAST**: OWASP ZAP, Burp Suite
- **Dependency Check**: OWASP Dependency Check
- **Container Security**: Trivy, Clair

---

## ✋ Antes de Fazer Deploy

**PARE e verifique:**

1. [ ] Executei `./setup-security.sh`?
2. [ ] Configurei todas as variáveis de ambiente?
3. [ ] Testei os endpoints de segurança?
4. [ ] Verifiquei os logs por informações sensíveis?
5. [ ] Configurei CORS para produção?
6. [ ] Configurei backup do banco de dados?
7. [ ] Configurei monitoramento de segurança?

**🚨 SE ALGUM ITEM ACIMA NÃO ESTIVER MARCADO, NÃO FAÇA DEPLOY!**
