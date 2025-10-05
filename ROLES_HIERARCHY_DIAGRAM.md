# 🥊 Diagrama da Hierarquia de Roles - Academia de Luta

## 📊 Estrutura Hierárquica

```
┌─────────────────────────────────────────────────────────────────────┐
│                    LANDLORD (ACADEMIA CENTRAL)                     │
│                     🏢 Matriz - Controle Total                      │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                │ 1:N
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        TENANTS (FILIAIS)                            │
│  🏢 Academia Centro  🏢 Academia Zona Sul  🏢 Academia Zona Norte  │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                │ Herda Roles
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                           ROLES SYSTEM                              │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                        NÍVEL 1 - PROPRIETÁRIO                       │
│  👑 PROPRIETÁRIO (owner) - Acesso Total ao Sistema                 │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        NÍVEL 2 - GESTÃO                             │
│  📊 GERENTE GERAL (general_manager) - Operação Geral               │
│  💰 GERENTE FINANCEIRO (financial_manager) - Gestão Financeira    │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      NÍVEL 3 - TÉCNICO/ESPORTIVO                   │
│  🥋 INSTRUTOR CHEFE (head_instructor) - Coordenação Técnica        │
│  🥋 INSTRUTOR (instructor) - Ministra Aulas                         │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       NÍVEL 4 - OPERACIONAL                        │
│  📞 RECEPCIONISTA (receptionist) - Atendimento ao Público         │
│  📋 ASSISTENTE ADMINISTRATIVO (admin_assistant) - Suporte Admin    │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      NÍVEL 5 - ESPECIALIZADO                        │
│  🔧 TÉCNICO DE EQUIPAMENTOS (equipment_technician) - Manutenção    │
│  🛡️ SEGURANÇA (security) - Segurança da Academia                   │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         NÍVEL 6 - MEMBROS                          │
│  ⭐ MEMBRO VIP (vip_member) - Privilégios Especiais                 │
│  👤 MEMBRO REGULAR (regular_member) - Acesso Básico                │
└─────────────────────────────────────────────────────────────────────┘
```

## 🔐 Matriz de Permissões por Role

```
┌─────────────────────┬─────────┬─────────┬─────────┬─────────┬─────────┬─────────┬─────────┬─────────┐
│        ROLE         │ Members │ Classes │Payments │Equipment│Competit.│ Reports │ Settings│  Users │
├─────────────────────┼─────────┼─────────┼─────────┼─────────┼─────────┼─────────┼─────────┼─────────┤
│ 👑 PROPRIETÁRIO     │   CRUD  │   CRUD  │   CRUD  │   CRUD  │   CRUD  │   CRUD  │   CRUD  │   CRUD  │
│ 📊 GERENTE GERAL    │   CRUD  │   CRUD  │   CRUD  │    R    │   CRUD  │   CRUD  │    R    │    R    │
│ 💰 GERENTE FINANCEIRO│   RU   │    R    │   CRUD  │    R    │    R    │   CRUD  │    R    │    R    │
│ 🥋 INSTRUTOR CHEFE  │   CRUD  │   CRUD  │    R    │    R    │   CRUD  │    R    │    -    │    -    │
│ 🥋 INSTRUTOR        │   RU    │   CRUD  │    R    │    R    │    R    │    R    │    -    │    -    │
│ 📞 RECEPCIONISTA    │   CRUD  │    R    │    R    │    R    │    R    │    R    │    -    │    -    │
│ 📋 ASSISTENTE ADMIN │   RU    │   RU    │    R    │    R    │    R    │    R    │    -    │    -    │
│ 🔧 TÉCNICO EQUIP.   │    R    │    -    │    -    │   CRUD  │    -    │    -    │    -    │    -    │
│ 🛡️ SEGURANÇA       │    R    │    R    │    -    │    R    │    -    │    -    │    -    │    -    │
│ ⭐ MEMBRO VIP       │    R    │    R    │    -    │    -    │    R    │    -    │    -    │    -    │
│ 👤 MEMBRO REGULAR  │    R    │    R    │    -    │    -    │    R    │    -    │    -    │    -    │
└─────────────────────┴─────────┴─────────┴─────────┴─────────┴─────────┴─────────┴─────────┴─────────┘

Legenda: C=Create, R=Read, U=Update, D=Delete, -=Sem Acesso
```

## 🏢 Departamentos e Responsabilidades

```
┌─────────────────────────────────────────────────────────────────────┐
│                           DEPARTAMENTOS                            │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ 1️⃣ ADMINISTRAÇÃO                                                  │
│ ┌─────────────────┬─────────────────┬─────────────────────────────┐ │
│ │ 👑 PROPRIETÁRIO │ 📊 GERENTE GERAL│ 📋 ASSISTENTE ADMINISTRATIVO│ │
│ │ • Controle Total│ • Operação Geral│ • Suporte Administrativo    │ │
│ │ • Todas as Ações│ • Supervisão    │ • Tarefas Básicas           │ │
│ └─────────────────┴─────────────────┴─────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ 2️⃣ FINANCEIRO                                                     │
│ ┌─────────────────┬───────────────────────────────────────────────┐ │
│ │ 💰 GERENTE FINANCEIRO│ 📞 RECEPCIONISTA (Pagamentos Básicos)    │ │
│ │ • Gestão Completa│ • Processamento de Pagamentos                │ │
│ │ • Relatórios    │ • Cobranças Simples                          │ │
│ └─────────────────┴───────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ 3️⃣ OPERACIONAL                                                    │
│ ┌─────────────────┬───────────────────────────────────────────────┐ │
│ │ 📞 RECEPCIONISTA│ 🛡️ SEGURANÇA                                │ │
│ │ • Atendimento   │ • Vigilância                                 │ │
│ │ • Cadastros     │ • Controle de Acesso                         │ │
│ │ • Agendamentos  │ • Segurança Física                           │ │
│ └─────────────────┴───────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ 4️⃣ TÉCNICO/ESPORTIVO                                              │
│ ┌─────────────────┬───────────────────────────────────────────────┐ │
│ │ 🥋 INSTRUTOR CHEFE│ 🥋 INSTRUTOR                               │ │
│ │ • Coordenação   │ • Ministrar Aulas                            │ │
│ │ • Supervisão    │ • Acompanhar Alunos                          │ │
│ │ • Gestão Técnica│ • Aplicar Metodologia                        │ │
│ └─────────────────┴───────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ 5️⃣ MANUTENÇÃO                                                     │
│ ┌─────────────────────────────────────────────────────────────────┐ │
│ │ 🔧 TÉCNICO DE EQUIPAMENTOS                                    │ │
│ │ • Manutenção de Equipamentos                                   │ │
│ │ • Controle de Estoque                                          │ │
│ │ • Reparos e Calibração                                         │ │
│ └─────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ 6️⃣ MEMBROS                                                        │
│ ┌─────────────────┬───────────────────────────────────────────────┐ │
│ │ ⭐ MEMBRO VIP   │ 👤 MEMBRO REGULAR                            │ │
│ │ • Aulas Especiais│ • Aulas Regulares                           │ │
│ │ • Descontos     │ • Acesso Básico                              │ │
│ │ • Privilégios   │ • Horários Padrão                            │ │
│ └─────────────────┴───────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

## 🔒 Políticas de Segurança (ABAC)

```
┌─────────────────────────────────────────────────────────────────────┐
│                        POLICIES SYSTEM                             │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ 🛡️ ADMIN FULL ACCESS                                               │
│ • Effect: ALLOW                                                   │
│ • Actions: [create, read, update, delete, manage]                 │
│ • Resources: [users, roles, permissions, policies, members,        │
│   classes, payments, reports, settings]                           │
│ • Conditions: business_hours + ip_whitelist                       │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ 🥋 INSTRUCTOR ACCESS                                               │
│ • Effect: ALLOW                                                   │
│ • Actions: [read, update, create]                                 │
│ • Resources: [members, classes, attendance, progress]              │
│ • Conditions: role_restriction=instructor + class_time_access     │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ 💰 FINANCIAL ACCESS                                                │
│ • Effect: ALLOW                                                   │
│ • Actions: [read, create, update]                                 │
│ • Resources: [payments, invoices, financial_reports, members]      │
│ • Conditions: department=financial + data_sensitivity=high         │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ 📞 RECEPTION ACCESS                                                │
│ • Effect: ALLOW                                                   │
│ • Actions: [read, create]                                         │
│ • Resources: [members, classes, schedules, basic_reports]          │
│ • Conditions: role_restriction=reception + public_facing          │
└─────────────────────────────────────────────────────────────────────┘
```

## 🎯 Fluxo de Autorização

```
┌─────────────────────────────────────────────────────────────────────┐
│                    FLUXO DE AUTORIZAÇÃO                            │
└─────────────────────────────────────────────────────────────────────┘

1️⃣ USUÁRIO FAZ LOGIN
   ↓
2️⃣ SISTEMA IDENTIFICA USER + TENANT
   ↓
3️⃣ SISTEMA BUSCA ROLES DO USUÁRIO NO TENANT
   ↓
4️⃣ SISTEMA BUSCA PERMISSIONS DOS ROLES
   ↓
5️⃣ SISTEMA VERIFICA POLICIES DAS PERMISSIONS
   ↓
6️⃣ SISTEMA AVALIA CONDIÇÕES (horário, IP, departamento)
   ↓
7️⃣ SISTEMA AUTORIZA/NEGA A AÇÃO
```

## 📈 Benefícios da Estrutura

```
┌─────────────────────────────────────────────────────────────────────┐
│                        BENEFÍCIOS                                  │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ 🏢 PARA O LANDLORD (MATRIZ)                                       │
│ • Controle centralizado de todas as filiais                        │
│ • Padronização de roles e permissões                               │
│ • Facilidade para abrir novas academias                            │
│ • Relatórios consolidados                                          │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ 🏢 PARA OS TENANTS (FILIAIS)                                       │
│ • Autonomia operacional                                            │
│ • Roles específicos para cada filial                               │
│ • Isolamento de dados entre filiais                                │
│ • Configurações personalizáveis                                    │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ 👥 PARA OS USUÁRIOS                                               │
│ • Acesso baseado em função                                          │
│ • Segurança granular                                               │
│ • Facilidade de gestão                                              │
│ • Auditoria completa                                                │
└─────────────────────────────────────────────────────────────────────┘
```

## 🚀 Implementação Automática

```
┌─────────────────────────────────────────────────────────────────────┐
│                    SISTEMA INICIALIZADO                            │
└─────────────────────────────────────────────────────────────────────┘

✅ 1 LANDLORD PADRÃO (Academia Central)
✅ 4 POLICIES DE SEGURANÇA
✅ 20+ PERMISSIONS ESPECÍFICAS
✅ 11 ROLES HIERÁRQUICOS
✅ ASSOCIAÇÕES AUTOMÁTICAS

🎯 RESULTADO: Sistema pronto para uso em academias de luta
   com controle total de acesso e segurança!
```

