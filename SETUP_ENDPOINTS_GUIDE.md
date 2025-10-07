# 🚀 Guia de Endpoints para Setup de Redes de Academias

## 📋 Visão Geral

Este guia apresenta todos os endpoints disponíveis para configurar novas redes de academias de luta no sistema SaaS.

## 🎯 Endpoints Disponíveis

### 1. **Adicionar Filial à Rede**
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
  "landlordId": "uuid-do-landlord",
  "landlordName": "Academia Central",
  "active": true,
  "config": { ... },
  "createdAt": "2024-01-15T10:35:00",
  "updatedAt": "2024-01-15T10:35:00"
}
```

### 2. **Configurar Roles Padrões**
```http
POST /api/setup/network/{landlordId}/roles
```

**Descrição**: Adiciona todos os roles padrões para academias de luta a um landlord existente.

**Response**:
```json
{
  "message": "Roles padrões configurados com sucesso",
  "rolesCreatedCount": 11,
  "executionTimeMs": 250
}
```

### 3. **Listar Todas as Redes**
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

## 🎯 Casos de Uso Típicos

### 📋 **CENÁRIO 1: Provisionar uma rede recém-criada**

Supondo que o landlord já foi cadastrado por outro fluxo (ex.: dashboard administrativo), utilize os endpoints de setup para deixá-lo pronto para uso.

1. **Configurar roles padrões**:
```bash
curl -X POST http://localhost:8080/api/setup/network/{landlordId}/roles
```

2. **Adicionar a primeira filial**:
```bash
curl -X POST http://localhost:8080/api/setup/network/{landlordId}/tenant \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Matriz Centro",
    "config": {
      "address": "Av. Principal, 100",
      "phone": "(11) 88888-8888"
    }
  }'
```

### 📋 **CENÁRIO 2: Expandir uma rede existente com novas filiais**

Repita a chamada de criação de tenant para cada nova unidade que o cliente abrir.

```bash
curl -X POST http://localhost:8080/api/setup/network/{landlordId}/tenant \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Filial Zona Sul",
    "config": {
      "address": "Rua das Flores, 123",
      "phone": "(11) 97777-7777"
    }
  }'
```

### 📋 **CENÁRIO 3: Auditar redes cadastradas**

Recupere todas as redes e seus agregados atuais para relatórios rápidos ou integrações.

```bash
curl -X GET http://localhost:8080/api/setup/networks
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
