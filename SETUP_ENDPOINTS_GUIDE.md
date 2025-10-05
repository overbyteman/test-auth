# 🚀 Guia de Endpoints para Setup de Redes de Academias

## 📋 Visão Geral

Este guia apresenta todos os endpoints disponíveis para configurar novas redes de academias de luta no sistema SaaS.

## 🎯 Endpoints Disponíveis

### 1. **Criar Nova Rede (Landlord + Roles)**
```http
POST /api/setup/network
```

**Descrição**: Cria um novo landlord (matriz) com todos os roles padrões para academias de luta.

**Request Body**:
```json
{
  "name": "Academia Central",
  "description": "Rede de academias de artes marciais",
  "config": {
    "type": "martial_arts_academy",
    "business_model": "franchise",
    "default_currency": "BRL",
    "timezone": "America/Sao_Paulo",
    "features": [
      "member_management",
      "class_scheduling",
      "payment_processing",
      "instructor_management",
      "equipment_tracking",
      "competition_management"
    ]
  }
}
```

**Response**:
```json
{
  "id": "uuid-do-landlord",
  "name": "Academia Central",
  "config": { ... },
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00",
  "tenantsCount": 0,
  "rolesCount": 11
}
```

### 2. **Adicionar Filial à Rede**
```http
POST /api/setup/network/{landlordId}/tenant
```

**Descrição**: Cria uma nova filial (tenant) para um landlord existente.

**Request Body**:
```json
{
  "name": "Academia Centro",
  "config": {
    "address": "Rua das Flores, 123",
    "phone": "(11) 99999-9999",
    "instructor_capacity": 50,
    "equipment": ["tatame", "saco_pesado", "luvas"]
  }
}
```

**Response**:
```json
{
  "id": "uuid-do-tenant",
  "name": "Academia Centro",
  "config": { ... },
  "createdAt": "2024-01-15T10:35:00",
  "updatedAt": "2024-01-15T10:35:00"
}
```

### 3. **Configurar Roles Padrões**
```http
POST /api/setup/network/{landlordId}/roles
```

**Descrição**: Adiciona todos os roles padrões para academias de luta a um landlord existente.

**Response**:
```json
{
  "message": "Roles padrões configurados com sucesso",
  "rolesCreated": 11,
  "executionTimeMs": 250
}
```

### 4. **Verificar Status da Rede**
```http
GET /api/setup/network/{landlordId}/status
```

**Descrição**: Retorna informações sobre o status de configuração de uma rede.

**Response**:
```json
{
  "networkStatus": {
    "hasRoles": true,
    "hasPolicies": true,
    "hasPermissions": true,
    "rolesCount": 11,
    "tenantsCount": 3,
    "policiesCount": 4,
    "permissionsCount": 20
  },
  "executionTimeMs": 45
}
```

### 5. **Listar Todas as Redes**
```http
GET /api/setup/networks
```

**Descrição**: Lista todas as redes de academias cadastradas.

**Response**:
```json
[
  {
    "id": "uuid-landlord-1",
    "name": "Academia Central",
    "tenantsCount": 3,
    "rolesCount": 11,
    "createdAt": "2024-01-15T10:30:00"
  },
  {
    "id": "uuid-landlord-2", 
    "name": "Rede de Lutas",
    "tenantsCount": 5,
    "rolesCount": 11,
    "createdAt": "2024-01-14T15:20:00"
  }
]
```

### 6. **Setup Completo de Rede**
```http
POST /api/setup/network/complete
```

**Descrição**: Cria uma rede completa com landlord, roles padrões e filial inicial.

**Request Body**:
```json
{
  "networkName": "Academia Central",
  "networkDescription": "Rede de academias de artes marciais",
  "firstTenantName": "Academia Centro",
  "networkConfig": {
    "type": "martial_arts_academy",
    "business_model": "franchise",
    "default_currency": "BRL"
  },
  "tenantConfig": {
    "address": "Rua das Flores, 123",
    "phone": "(11) 99999-9999"
  }
}
```

**Response**:
```json
{
  "landlord": {
    "id": "uuid-do-landlord",
    "name": "Academia Central",
    "tenantsCount": 1,
    "rolesCount": 11
  },
  "firstTenant": {
    "id": "uuid-do-tenant",
    "name": "Academia Centro"
  },
  "rolesCreated": 11,
  "policiesCreated": 4,
  "permissionsCreated": 20,
  "totalExecutionTime": 1250
}
```

## 🎯 Casos de Uso Típicos

### 📋 **CENÁRIO 1: Novo Cliente Adquire o SaaS**

1. **Criar Rede Completa**:
```bash
curl -X POST http://localhost:8080/api/setup/network/complete \
  -H "Content-Type: application/json" \
  -d '{
    "networkName": "Academia do João",
    "networkDescription": "Rede de academias do João Silva",
    "firstTenantName": "Matriz Centro",
    "networkConfig": {
      "type": "martial_arts_academy",
      "business_model": "franchise"
    }
  }'
```

2. **Adicionar Filiais**:
```bash
curl -X POST http://localhost:8080/api/setup/network/{landlordId}/tenant \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Filial Zona Sul",
    "config": {
      "address": "Av. Paulista, 1000",
      "phone": "(11) 88888-8888"
    }
  }'
```

### 📋 **CENÁRIO 2: Cliente Existente Adiciona Nova Rede**

1. **Criar Nova Rede**:
```bash
curl -X POST http://localhost:8080/api/setup/network \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Segunda Rede do Cliente",
    "description": "Nova rede de academias"
  }'
```

2. **Verificar Status**:
```bash
curl -X GET http://localhost:8080/api/setup/network/{landlordId}/status
```

### 📋 **CENÁRIO 3: Migração de Dados**

1. **Criar Rede**:
```bash
curl -X POST http://localhost:8080/api/setup/network \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Academia Migrada",
    "description": "Academia migrada do sistema antigo"
  }'
```

2. **Configurar Roles**:
```bash
curl -X POST http://localhost:8080/api/setup/network/{landlordId}/roles
```

3. **Adicionar Filiais**:
```bash
# Para cada filial existente
curl -X POST http://localhost:8080/api/setup/network/{landlordId}/tenant \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Nome da Filial",
    "config": { ... }
  }'
```

## 🔐 Roles Criados Automaticamente

Quando você chama os endpoints de setup, o sistema cria automaticamente:

### 👑 **NÍVEL 1 - PROPRIETÁRIO**
- **PROPRIETÁRIO** (`owner`) - Acesso total ao sistema

### 📊 **NÍVEL 2 - GESTÃO**
- **GERENTE GERAL** (`general_manager`) - Operação geral
- **GERENTE FINANCEIRO** (`financial_manager`) - Gestão financeira

### 🥋 **NÍVEL 3 - TÉCNICO/ESPORTIVO**
- **INSTRUTOR CHEFE** (`head_instructor`) - Coordenação técnica
- **INSTRUTOR** (`instructor`) - Ministra aulas

### 📞 **NÍVEL 4 - OPERACIONAL**
- **RECEPCIONISTA** (`receptionist`) - Atendimento ao público
- **ASSISTENTE ADMINISTRATIVO** (`admin_assistant`) - Suporte administrativo

### 🔧 **NÍVEL 5 - ESPECIALIZADO**
- **TÉCNICO DE EQUIPAMENTOS** (`equipment_technician`) - Manutenção
- **SEGURANÇA** (`security`) - Segurança da academia

### 👤 **NÍVEL 6 - MEMBROS**
- **MEMBRO VIP** (`vip_member`) - Privilégios especiais
- **MEMBRO REGULAR** (`regular_member`) - Acesso básico

## 🛡️ Políticas de Segurança Criadas

- **Admin Full Access** - Acesso administrativo completo
- **Instructor Access** - Acesso para instrutores
- **Financial Access** - Acesso financeiro
- **Reception Access** - Acesso de recepção

## 📈 Benefícios do Sistema

### ✅ **PARA O SAAS**
- Onboarding automatizado de novos clientes
- Padronização de roles e permissões
- Facilidade para escalar o negócio
- Redução de tempo de configuração

### ✅ **PARA OS CLIENTES**
- Sistema pronto para uso imediatamente
- Roles específicos para academias de luta
- Facilidade para adicionar novas filiais
- Configurações personalizáveis

### ✅ **PARA OS USUÁRIOS**
- Acesso baseado em função
- Segurança granular
- Facilidade de gestão
- Auditoria completa

## 🚀 Próximos Passos

Após criar a rede, você pode:

1. **Criar usuários** e atribuir roles
2. **Configurar permissões** específicas
3. **Adicionar mais filiais** conforme necessário
4. **Personalizar configurações** por tenant
5. **Monitorar uso** através dos relatórios

O sistema está pronto para uso em produção! 🎯
