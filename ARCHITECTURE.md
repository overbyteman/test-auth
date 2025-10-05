# 🏗️ Arquitetura do Sistema - Multi-Tenancy com RBAC e ABAC

## 📊 Modelo de Dados

### Estrutura de Relacionamentos

```
┌─────────────────────────────────────────────────────────────────────┐
│                     MULTI-TENANCY ARCHITECTURE                       │
└─────────────────────────────────────────────────────────────────────┘

┌──────────┐         ┌─────────────────────┐         ┌──────────┐
│  TENANT  │────────>│ USERS_TENANTS_ROLES │<────────│   USER   │
└──────────┘         └─────────────────────┘         └──────────┘
     │                        │
     │ 1:N                    │ N:1
     │                        │
     ↓                        ↓
┌──────────┐              ┌──────────┐
│   ROLE   │─────────────>│PERMISSION│
└──────────┘   N:N        └──────────┘
                               │
                               │ N:1
                               ↓
                          ┌──────────┐
                          │  POLICY  │
                          └──────────┘
```

## 🔑 Entidades Principais

### 1. **Tenant** (Organização)
- **Descrição**: Representa uma organização/empresa no sistema
- **Características**:
  - Cada tenant é isolado dos demais
  - Pode ter múltiplas roles específicas
  - Configuração personalizável via JSON

**Campos:**
- `id` (UUID) - Identificador único
- `name` (String) - Nome único do tenant
- `config` (JSONB) - Configurações personalizadas
- `roles` (Set<Role>) - Roles pertencentes a este tenant
- `created_at`, `updated_at` - Timestamps

### 2. **Role** (Papel/Função)
- **Descrição**: Define um papel dentro de um tenant
- **Características**:
  - Pertence a um tenant específico
  - Contém múltiplas permissions
  - Nome único por tenant (pode repetir entre tenants diferentes)

**Campos:**
- `id` (UUID) - Identificador único
- `name` (String) - Nome do role (ex: "ADMIN", "MANAGER")
- `description` (String) - Descrição opcional
- `tenant` (Tenant) - Tenant proprietário
- `permissions` (Set<Permission>) - Permissions associadas

**Relacionamentos:**
- `N:1` com `Tenant` - Cada role pertence a um tenant
- `N:N` com `Permission` - Um role tem várias permissions

### 3. **Permission** (Permissão)
- **Descrição**: Define uma ação específica em um recurso
- **Características**:
  - Combinação única de `action` + `resource`
  - Pode ter uma policy ABAC associada
  - Compartilhada entre múltiplos roles

**Campos:**
- `id` (UUID) - Identificador único
- `action` (String) - Ação (ex: "create", "read", "update", "delete")
- `resource` (String) - Recurso (ex: "users", "articles", "reports")
- `policy` (Policy) - Policy ABAC opcional
- `roles` (Set<Role>) - Roles que possuem esta permission

**Relacionamentos:**
- `N:N` com `Role` - Várias permissions para vários roles
- `N:1` com `Policy` - Cada permission pode ter uma policy

### 4. **Policy** (Política ABAC)
- **Descrição**: Define regras de acesso baseadas em atributos
- **Características**:
  - Permite/nega acesso baseado em condições
  - Suporta múltiplas actions e resources
  - Condições flexíveis em JSON

**Campos:**
- `id` (UUID) - Identificador único
- `name` (String) - Nome único da policy
- `description` (String) - Descrição opcional
- `effect` (Enum) - "ALLOW" ou "DENY"
- `actions` (Array<String>) - Lista de ações
- `resources` (Array<String>) - Lista de recursos
- `conditions` (JSONB) - Condições ABAC em JSON
- `permissions` (Set<Permission>) - Permissions usando esta policy

**Exemplo de Condições:**
```json
{
  "ip_range": ["192.168.1.0/24"],
  "time_of_day": {"start": "08:00", "end": "18:00"},
  "user_department": "engineering",
  "resource_owner": "${user.id}"
}
```

### 5. **User** (Usuário)
- **Descrição**: Usuário do sistema
- **Características**:
  - Pode participar de múltiplos tenants
  - Pode ter diferentes roles em cada tenant
  - Email único global

**Campos:**
- `id` (UUID) - Identificador único
- `name` (String) - Nome completo
- `email` (String) - Email único
- `password_hash` (String) - Senha criptografada
- `is_active` (Boolean) - Status do usuário
- `tenantRoles` (Set<UsersTenantsRoles>) - Relacionamentos tenant-role

### 6. **UsersTenantsRoles** (Tabela de Junção)
- **Descrição**: Relaciona usuários com tenants e roles
- **Características**:
  - Chave primária composta (user_id, tenant_id, role_id)
  - Permite que um usuário tenha múltiplos roles em um tenant
  - Permite que um usuário participe de múltiplos tenants

**Campos:**
- `user_id` (UUID) - ID do usuário
- `tenant_id` (UUID) - ID do tenant
- `role_id` (UUID) - ID do role

## 🎯 Fluxo de Autorização

### Exemplo Prático

**Cenário:** João precisa criar um artigo

```
1. João faz login
   └─> Sistema identifica User (João)

2. Sistema verifica contexto atual
   └─> Tenant: "Empresa ABC"
   └─> User ID: uuid-joao

3. Busca roles de João no tenant "Empresa ABC"
   └─> Consulta: UsersTenantsRoles
   └─> Resultado: Role "EDITOR"

4. Busca permissions do role "EDITOR"
   └─> Consulta: Role.permissions
   └─> Resultado: Permission(action="create", resource="articles")

5. Verifica se permission tem policy associada
   └─> Consulta: Permission.policy
   └─> Resultado: Policy "Editor Articles Policy"

6. Avalia condições da policy
   └─> Verifica: IP, horário, departamento, etc.
   └─> Resultado: ALLOW

7. Autoriza a ação
   └─> João pode criar artigos
```

## 📋 Exemplos de Uso

### Exemplo 1: Criar Tenant com Roles

```java
// 1. Criar tenant
Tenant tenant = Tenant.createNew("Empresa XYZ", config);
tenantRepository.save(tenant);

// 2. Criar roles para o tenant
Role adminRole = Role.createNew("ADMIN", "Administrador", tenant);
Role editorRole = Role.createNew("EDITOR", "Editor", tenant);
Role viewerRole = Role.createNew("VIEWER", "Visualizador", tenant);

roleRepository.saveAll(List.of(adminRole, editorRole, viewerRole));

// 3. Criar permissions
Permission createArticles = Permission.createNew("create", "articles");
Permission readArticles = Permission.createNew("read", "articles");
Permission updateArticles = Permission.createNew("update", "articles");
Permission deleteArticles = Permission.createNew("delete", "articles");

permissionRepository.saveAll(List.of(createArticles, readArticles, updateArticles, deleteArticles));

// 4. Associar permissions aos roles
adminRole.addPermission(createArticles);
adminRole.addPermission(readArticles);
adminRole.addPermission(updateArticles);
adminRole.addPermission(deleteArticles);

editorRole.addPermission(createArticles);
editorRole.addPermission(readArticles);
editorRole.addPermission(updateArticles);

viewerRole.addPermission(readArticles);

roleRepository.saveAll(List.of(adminRole, editorRole, viewerRole));
```

### Exemplo 2: Associar Usuário a Tenant com Role

```java
// 1. Buscar entidades
User user = userRepository.findByEmail("joao@email.com");
Tenant tenant = tenantRepository.findByName("Empresa XYZ");
Role role = roleRepository.findByNameAndTenant("EDITOR", tenant);

// 2. Criar relacionamento
UsersTenantsRoles utr = UsersTenantsRoles.of(user, tenant, role);
usersTenantsRolesRepository.save(utr);

// Agora João é EDITOR na Empresa XYZ
```

### Exemplo 3: Permission com Policy ABAC

```java
// 1. Criar policy
Policy policy = Policy.createNew(
    "Time-Based Edit Policy",
    "Permite edição apenas durante horário comercial",
    PolicyEffect.ALLOW,
    List.of("update", "delete"),
    List.of("articles"),
    conditions // JSON com regras
);
policyRepository.save(policy);

// 2. Associar policy à permission
Permission updateArticles = permissionRepository.findByActionAndResource("update", "articles");
updateArticles.setPolicy(policy);
permissionRepository.save(updateArticles);
```

### Exemplo 4: Verificar Permissões de um Usuário

```java
// Buscar todas as permissions de um usuário em um tenant específico
public Set<Permission> getUserPermissions(UUID userId, UUID tenantId) {
    // 1. Buscar roles do usuário no tenant
    List<UsersTenantsRoles> userTenantRoles = usersTenantsRolesRepository
        .findByUserIdAndTenantId(userId, tenantId);
    
    // 2. Buscar todas as permissions dos roles
    Set<Permission> permissions = new HashSet<>();
    for (UsersTenantsRoles utr : userTenantRoles) {
        Role role = roleRepository.findById(utr.getRoleId()).orElseThrow();
        permissions.addAll(role.getPermissions());
    }
    
    return permissions;
}

// Verificar se usuário pode executar uma ação
public boolean canUserPerformAction(UUID userId, UUID tenantId, String action, String resource) {
    Set<Permission> permissions = getUserPermissions(userId, tenantId);
    
    for (Permission permission : permissions) {
        if (permission.getAction().equals(action) && 
            permission.getResource().equals(resource)) {
            
            // Se tem policy, avaliar condições
            if (permission.hasPolicy()) {
                return evaluatePolicy(permission.getPolicy(), userId, tenantId);
            }
            
            return true; // Sem policy, permite
        }
    }
    
    return false; // Não tem permissão
}
```

## 🔐 Regras de Negócio

### Multi-Tenancy
1. ✅ Um tenant pode ter múltiplas roles
2. ✅ Roles são específicas por tenant (mesmo nome pode existir em tenants diferentes)
3. ✅ Um usuário pode participar de múltiplos tenants
4. ✅ Um usuário pode ter múltiplos roles no mesmo tenant
5. ✅ Isolamento total de dados entre tenants

### RBAC (Role-Based Access Control)
1. ✅ Um role pertence a um único tenant
2. ✅ Um role pode ter múltiplas permissions
3. ✅ Uma permission pode estar em múltiplos roles
4. ✅ Permissions são globais (compartilhadas entre tenants)

### ABAC (Attribute-Based Access Control)
1. ✅ Uma permission pode ter uma policy associada
2. ✅ Uma policy pode ser usada por múltiplas permissions
3. ✅ Policy define condições adicionais (IP, horário, atributos, etc.)
4. ✅ Policy pode ALLOW ou DENY acesso

### Hierarquia de Decisão
```
1. Usuário tem role no tenant? 
   └─> NÃO: DENY
   └─> SIM: Continue

2. Role tem a permission necessária?
   └─> NÃO: DENY
   └─> SIM: Continue

3. Permission tem policy associada?
   └─> NÃO: ALLOW (apenas RBAC)
   └─> SIM: Continue

4. Policy permite acesso? (avaliar condições)
   └─> DENY: DENY
   └─> ALLOW: ALLOW
```

## 🎨 Casos de Uso Comuns

### Caso 1: Sistema SaaS Multi-Empresa
```
Tenant: "Empresa A"
  ├─ Role: ADMIN (todas permissions)
  ├─ Role: MANAGER (create, read, update)
  └─ Role: USER (read)

Tenant: "Empresa B"
  ├─ Role: SUPER_ADMIN (todas permissions + admin features)
  ├─ Role: TEAM_LEAD (create, read, update projects)
  └─ Role: DEVELOPER (read, update tasks)

User: João
  ├─ Empresa A: MANAGER
  └─ Empresa B: DEVELOPER
```

### Caso 2: Sistema Corporativo com Departamentos
```
Tenant: "Corporação XYZ"
  ├─ Role: HR_ADMIN (permissions em employees)
  ├─ Role: FINANCE_ADMIN (permissions em invoices)
  └─ Role: IT_ADMIN (permissions em systems)

Permissions com Policies:
  ├─ delete:employees → Policy: "Apenas durante expediente + aprovação gerente"
  ├─ approve:invoices → Policy: "Valor < $10k OU tem role FINANCE_DIRECTOR"
  └─ access:systems → Policy: "IP interno + VPN ativa"
```

## 📈 Performance e Otimização

### Índices Recomendados
```sql
-- Roles
CREATE INDEX idx_roles_tenant_id ON roles(tenant_id);
CREATE INDEX idx_roles_name_tenant ON roles(name, tenant_id);

-- Permissions
CREATE INDEX idx_permissions_action ON permissions(action);
CREATE INDEX idx_permissions_resource ON permissions(resource);
CREATE INDEX idx_permissions_policy_id ON permissions(policy_id);

-- UsersTenantsRoles
CREATE INDEX idx_utr_user_tenant ON users_tenants_roles(user_id, tenant_id);
CREATE INDEX idx_utr_tenant_role ON users_tenants_roles(tenant_id, role_id);

-- Roles_Permissions (tabela de junção JPA)
CREATE INDEX idx_rp_role_id ON roles_permissions(role_id);
CREATE INDEX idx_rp_permission_id ON roles_permissions(permission_id);
```

### Cache Estratégico
```java
// Cachear permissions de usuários por tenant
@Cacheable(value = "user-permissions", key = "#userId + '-' + #tenantId")
public Set<Permission> getUserPermissions(UUID userId, UUID tenantId) { ... }

// Cachear roles de tenant
@Cacheable(value = "tenant-roles", key = "#tenantId")
public List<Role> getTenantRoles(UUID tenantId) { ... }
```

## 🚀 Migração do Modelo Antigo

Se você tinha um modelo diferente, o Hibernate irá:
1. Adicionar coluna `tenant_id` na tabela `roles`
2. Adicionar coluna `policy_id` na tabela `permissions`
3. Manter tabela `roles_permissions` (gerenciada pelo JPA)
4. Manter tabela `users_tenants_roles`

**Atenção:** Roles existentes precisarão ser associados a um tenant!

---

**Última atualização:** 04/10/2025
**Versão:** 2.0
**Status:** ✅ Implementado

