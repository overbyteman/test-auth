# 🥊 Sistema de Roles para Academias de Luta

## 📋 Visão Geral

Este documento define a estrutura completa de roles, permissões e políticas para um sistema SaaS de gestão de academias de luta, considerando a arquitetura multi-tenant com **Landlord** (matriz) e **Tenants** (filiais).

## 🏗️ Arquitetura do Sistema

```
LANDLORD (Matriz - Academia Central)
├── TENANT 1 (Academia Centro)
├── TENANT 2 (Academia Zona Sul)  
├── TENANT 3 (Academia Zona Norte)
└── ...

Cada TENANT herda os ROLES do LANDLORD
```

## 👥 Hierarquia de Roles

### 🎯 **NÍVEL 1 - PROPRIETÁRIO/ADMINISTRADOR**
| Role | Código | Descrição | Acesso |
|------|--------|-----------|--------|
| **PROPRIETÁRIO** | `owner` | Proprietário da academia com acesso total | ✅ Total |

### 🎯 **NÍVEL 2 - GESTÃO**
| Role | Código | Descrição | Acesso |
|------|--------|-----------|--------|
| **GERENTE GERAL** | `general_manager` | Operação geral da academia | ✅ Quase total |
| **GERENTE FINANCEIRO** | `financial_manager` | Gestão financeira | 💰 Financeiro + Operacional |

### 🎯 **NÍVEL 3 - TÉCNICO/ESPORTIVO**
| Role | Código | Descrição | Acesso |
|------|--------|-----------|--------|
| **INSTRUTOR CHEFE** | `head_instructor` | Coordenação técnica | 🥋 Técnico + Gestão de instrutores |
| **INSTRUTOR** | `instructor` | Ministra aulas | 🥋 Técnico básico |

### 🎯 **NÍVEL 4 - OPERACIONAL**
| Role | Código | Descrição | Acesso |
|------|--------|-----------|--------|
| **RECEPCIONISTA** | `receptionist` | Atendimento ao público | 📞 Atendimento + Cadastros |
| **ASSISTENTE ADMINISTRATIVO** | `admin_assistant` | Suporte administrativo | 📋 Administrativo básico |

### 🎯 **NÍVEL 5 - ESPECIALIZADO**
| Role | Código | Descrição | Acesso |
|------|--------|-----------|--------|
| **TÉCNICO DE EQUIPAMENTOS** | `equipment_technician` | Manutenção de equipamentos | 🔧 Equipamentos |
| **SEGURANÇA** | `security` | Segurança da academia | 🛡️ Segurança |

### 🎯 **NÍVEL 6 - MEMBROS**
| Role | Código | Descrição | Acesso |
|------|--------|-----------|--------|
| **MEMBRO VIP** | `vip_member` | Membro com privilégios especiais | ⭐ Acesso premium |
| **MEMBRO REGULAR** | `regular_member` | Membro comum | 👤 Acesso básico |

## 🔐 Matriz de Permissões

### 📊 **RECURSOS DO SISTEMA**

| Recurso | Descrição | Exemplos |
|---------|-----------|----------|
| `users` | Gestão de usuários do sistema | Criar, editar, excluir usuários |
| `members` | Gestão de membros da academia | Cadastro, histórico, pagamentos |
| `classes` | Gestão de aulas | Criar horários, gerenciar presença |
| `payments` | Gestão de pagamentos | Processar pagamentos, gerar cobranças |
| `equipment` | Gestão de equipamentos | Controle de estoque, manutenção |
| `competitions` | Gestão de competições | Organizar eventos, inscrições |
| `reports` | Relatórios gerenciais | Relatórios financeiros, de frequência |
| `settings` | Configurações do sistema | Configurar horários, preços |

### 🎯 **AÇÕES DISPONÍVEIS**

| Ação | Descrição | Exemplo |
|------|-----------|---------|
| `create` | Criar novos registros | Cadastrar novo membro |
| `read` | Visualizar informações | Ver lista de membros |
| `update` | Modificar registros existentes | Editar dados do membro |
| `delete` | Excluir registros | Remover membro inativo |
| `manage` | Gerenciar completamente | Acesso total ao recurso |

## 🏢 Departamentos e Responsabilidades

### 1. **ADMINISTRAÇÃO**
- **PROPRIETÁRIO**: Controle total do sistema
- **GERENTE GERAL**: Operação geral, supervisão
- **ASSISTENTE ADMINISTRATIVO**: Suporte administrativo

### 2. **FINANCEIRO**
- **GERENTE FINANCEIRO**: Gestão completa das finanças
- **RECEPCIONISTA**: Processamento básico de pagamentos

### 3. **OPERACIONAL**
- **RECEPCIONISTA**: Atendimento, cadastros, agendamentos
- **SEGURANÇA**: Vigilância, controle de acesso

### 4. **TÉCNICO/ESPORTIVO**
- **INSTRUTOR CHEFE**: Coordenação técnica, supervisão de instrutores
- **INSTRUTOR**: Ministrar aulas, acompanhar alunos

### 5. **MANUTENÇÃO**
- **TÉCNICO DE EQUIPAMENTOS**: Manutenção, controle de estoque

### 6. **MEMBROS**
- **MEMBRO VIP**: Acesso a aulas especiais, descontos
- **MEMBRO REGULAR**: Acesso às aulas regulares

## 🔒 Políticas de Segurança (ABAC)

### 🛡️ **POLÍTICAS IMPLEMENTADAS**

#### 1. **Admin Full Access**
```json
{
  "effect": "ALLOW",
  "actions": ["create", "read", "update", "delete", "manage"],
  "resources": ["users", "roles", "permissions", "policies", "members", "classes", "payments", "reports", "settings"],
  "conditions": {
    "time_restriction": "business_hours",
    "ip_whitelist": true
  }
}
```

#### 2. **Instructor Access**
```json
{
  "effect": "ALLOW",
  "actions": ["read", "update", "create"],
  "resources": ["members", "classes", "attendance", "progress"],
  "conditions": {
    "role_restriction": "instructor",
    "class_time_access": true
  }
}
```

#### 3. **Financial Access**
```json
{
  "effect": "ALLOW",
  "actions": ["read", "create", "update"],
  "resources": ["payments", "invoices", "financial_reports", "members"],
  "conditions": {
    "department": "financial",
    "data_sensitivity": "high"
  }
}
```

#### 4. **Reception Access**
```json
{
  "effect": "ALLOW",
  "actions": ["read", "create"],
  "resources": ["members", "classes", "schedules", "basic_reports"],
  "conditions": {
    "role_restriction": "reception",
    "public_facing": true
  }
}
```

## 🎯 Casos de Uso Típicos

### 📋 **CENÁRIO 1: Nova Academia (Tenant)**
1. **Landlord** cria novo **Tenant** (filial)
2. **Sistema** herda automaticamente todos os **Roles** do Landlord
3. **Proprietário** atribui roles aos funcionários da nova filial
4. **Funcionários** têm acesso baseado em seus roles

### 📋 **CENÁRIO 2: Contratação de Novo Instrutor**
1. **Gerente Geral** cria usuário para novo instrutor
2. **Sistema** atribui role `instructor`
3. **Instrutor** recebe permissões para:
   - Ver membros
   - Gerenciar suas aulas
   - Acessar equipamentos
   - Ver competições

### 📋 **CENÁRIO 3: Promoção de Instrutor**
1. **Proprietário** promove instrutor a instrutor chefe
2. **Sistema** altera role de `instructor` para `head_instructor`
3. **Instrutor Chefe** recebe permissões adicionais:
   - Gerenciar outros instrutores
   - Criar/deletar aulas
   - Gerenciar competições

### 📋 **CENÁRIO 4: Acesso Financeiro**
1. **Gerente Financeiro** precisa acessar relatórios
2. **Sistema** verifica role `financial_manager`
3. **Política** permite acesso a dados financeiros
4. **Condições** verificam departamento e sensibilidade dos dados

## 🔄 Fluxo de Autorização

```
1. USUÁRIO faz login
   ↓
2. SISTEMA identifica USER + TENANT
   ↓
3. SISTEMA busca ROLES do usuário no tenant
   ↓
4. SISTEMA busca PERMISSIONS dos roles
   ↓
5. SISTEMA verifica POLICIES das permissions
   ↓
6. SISTEMA avalia CONDIÇÕES (horário, IP, departamento)
   ↓
7. SISTEMA autoriza/nega a ação
```

## 📈 Benefícios da Estrutura

### ✅ **PARA O LANDLORD (Matriz)**
- Controle centralizado de todas as filiais
- Padronização de roles e permissões
- Facilidade para abrir novas academias
- Relatórios consolidados

### ✅ **PARA OS TENANTS (Filiais)**
- Autonomia operacional
- Roles específicos para cada filial
- Isolamento de dados entre filiais
- Configurações personalizáveis

### ✅ **PARA OS USUÁRIOS**
- Acesso baseado em função
- Segurança granular
- Facilidade de gestão
- Auditoria completa

## 🚀 Implementação

O sistema é inicializado automaticamente com:
- ✅ 1 Landlord padrão
- ✅ 4 Policies de segurança
- ✅ 20+ Permissions específicas
- ✅ 11 Roles hierárquicos
- ✅ Associações automáticas

**Resultado**: Sistema pronto para uso em academias de luta com controle total de acesso e segurança!
