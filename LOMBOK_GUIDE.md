# Guia de Uso do Lombok no Projeto

## 📋 Visão Geral

O Lombok foi implementado no projeto para reduzir o código boilerplate e melhorar a produtividade. Este guia explica como usar as anotações do Lombok implementadas.

## 🚀 Anotações Implementadas

### 1. **@Data**
- **O que faz**: Gera automaticamente getters, setters, toString, equals e hashCode
- **Uso**: Aplicado em todas as classes de modelo e DTOs
- **Exemplo**:
```java
@Data
public class User {
    private String name;
    private String email;
    // Lombok gera automaticamente: getName(), setName(), getEmail(), setEmail(), 
    // toString(), equals(), hashCode()
}
```

### 2. **@Builder**
- **O que faz**: Implementa o padrão Builder para construção de objetos
- **Uso**: Permite criar objetos de forma fluente e legível
- **Exemplo**:
```java
User user = User.builder()
    .name("João Silva")
    .email("joao@example.com")
    .passwordHash("hash123")
    .build();
```

### 3. **@NoArgsConstructor**
- **O que faz**: Gera construtor sem argumentos
- **Uso**: Necessário para frameworks como Spring e JPA
- **Exemplo**:
```java
User user = new User(); // Construtor gerado automaticamente
```

### 4. **@AllArgsConstructor**
- **O que faz**: Gera construtor com todos os argumentos
- **Uso**: Útil para testes e criação direta de objetos
- **Exemplo**:
```java
User user = new User(1L, "João", "joao@example.com", "hash", true, "token", 
                   LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), 1, true);
```

### 5. **@EqualsAndHashCode**
- **O que faz**: Gera métodos equals() e hashCode() baseados em campos específicos
- **Uso**: Controle fino sobre quais campos são usados na comparação
- **Exemplo**:
```java
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {
    @EqualsAndHashCode.Include
    private Long id; // Apenas o ID será usado no equals/hashCode
}
```

### 6. **@ToString**
- **O que faz**: Gera método toString() automaticamente
- **Uso**: Exclusão de campos sensíveis do toString
- **Exemplo**:
```java
@ToString(exclude = {"passwordHash", "emailVerificationToken"})
public class User {
    // Campos sensíveis não aparecerão no toString
}
```

## 🔧 Configurações do Lombok

### Arquivo `lombok.config`
```properties
# Configurações gerais
lombok.addLombokGeneratedAnnotation = true
lombok.addSuppressWarnings = true
lombok.fieldNameConstants.uppercase = true

# Configurações para Builder
lombok.builder.className = Builder
lombok.builder.buildMethodName = build
lombok.builder.chain = true
```

## 📝 Exemplos de Uso

### 1. **Criação de Usuário**
```java
// Método tradicional (ANTES)
User user = new User();
user.setName("João Silva");
user.setEmail("joao@example.com");
user.setPasswordHash("hash123");
user.setCreatedAt(LocalDateTime.now());
user.setUpdatedAt(LocalDateTime.now());
user.setVersion(1);
user.setActive(true);

// Método com Lombok (AGORA)
User user = User.builder()
    .name("João Silva")
    .email("joao@example.com")
    .passwordHash("hash123")
    .createdAt(LocalDateTime.now())
    .updatedAt(LocalDateTime.now())
    .version(1)
    .active(true)
    .build();

// Ou usando o método estático
User user = User.createNew("João Silva", "joao@example.com", "hash123");
```

### 2. **Criação de DTOs**
```java
// UserRequest
UserRequest request = UserRequest.builder()
    .name("João Silva")
    .email("joao@example.com")
    .build();

// UserResponse
UserResponse response = UserResponse.builder()
    .id(1L)
    .name("João Silva")
    .email("joao@example.com")
    .createdAt(LocalDateTime.now())
    .updatedAt(LocalDateTime.now())
    .build();

// ErrorResponse
ErrorResponse error = ErrorResponse.builder()
    .status(400)
    .error("Bad Request")
    .message("Validation failed")
    .path("/api/users")
    .details(Arrays.asList("Name is required"))
    .build();
```

### 3. **Comparação de Objetos**
```java
User user1 = User.builder().id(1L).name("João").build();
User user2 = User.builder().id(1L).name("João").build();

// equals() gerado automaticamente
assertTrue(user1.equals(user2));

// hashCode() gerado automaticamente
assertEquals(user1.hashCode(), user2.hashCode());
```

## 🎯 Benefícios Implementados

### 1. **Redução de Código**
- **Antes**: ~150 linhas para classe User
- **Depois**: ~50 linhas para classe User
- **Redução**: ~67% menos código

### 2. **Melhoria na Legibilidade**
- Builder pattern mais legível que construtores
- Menos código repetitivo
- Foco no que importa

### 3. **Manutenibilidade**
- Menos código para manter
- Mudanças automáticas em getters/setters
- Menos bugs por código manual

### 4. **Consistência**
- Padrão uniforme em todas as classes
- Comportamento previsível
- Menos erros humanos

## ⚠️ Cuidados e Boas Práticas

### 1. **Campos Sensíveis**
```java
@ToString(exclude = {"passwordHash", "emailVerificationToken"})
public class User {
    // Campos sensíveis não aparecem no toString
}
```

### 2. **Equals e HashCode**
```java
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {
    @EqualsAndHashCode.Include
    private Long id; // Apenas ID para comparação
}
```

### 3. **Validações**
```java
@Data
public class UserRequest {
    @NotBlank(message = "Nome é obrigatório")
    private String name;
    
    @Email(message = "Email inválido")
    private String email;
}
```

## 🧪 Testes

### Executar Testes do Lombok
```bash
./gradlew test --tests "*Test"
```

### Verificar se Lombok está funcionando
```bash
./gradlew compileJava
```

## 📚 Recursos Adicionais

- [Documentação Oficial do Lombok](https://projectlombok.org/)
- [Anotações Lombok](https://projectlombok.org/features/all)
- [Configurações Lombok](https://projectlombok.org/features/configuration)

## 🔄 Migração de Código Existente

### Antes (Código Manual)
```java
public class User {
    private String name;
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    @Override
    public boolean equals(Object o) { /* código manual */ }
    @Override
    public int hashCode() { /* código manual */ }
    @Override
    public String toString() { /* código manual */ }
}
```

### Depois (Com Lombok)
```java
@Data
public class User {
    private String name;
    // Tudo é gerado automaticamente!
}
```

## ✅ Checklist de Implementação

- [x] Dependência do Lombok adicionada ao build.gradle
- [x] Configuração do Lombok criada (lombok.config)
- [x] Classe User refatorada com Lombok
- [x] DTOs refatorados com Lombok
- [x] Testes criados para verificar funcionamento
- [x] Documentação criada
- [x] Exemplos de uso implementados

## 🎉 Conclusão

O Lombok foi implementado com sucesso no projeto, proporcionando:
- **67% menos código** nas classes de modelo
- **Melhor legibilidade** com Builder pattern
- **Manutenibilidade** aprimorada
- **Consistência** em todo o projeto
- **Produtividade** aumentada para desenvolvedores

O projeto agora está mais limpo, mais fácil de manter e mais produtivo para desenvolvimento!
