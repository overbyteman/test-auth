# Documentação de Exceções - Sistema de Autenticação

## Visão Geral

Este documento descreve todas as exceções tratadas pelo `GlobalExceptionHandler` do sistema de autenticação, organizadas por categoria e com exemplos de uso.

## Categorias de Exceções

### 1. Exceções de Validação (400 - Bad Request)

#### `ValidationException`
- **Uso**: Validação de dados de entrada nos services
- **HTTP Status**: 400
- **Exemplo**: "Nome não pode ser vazio"

#### `IllegalArgumentException`
- **Uso**: Argumentos inválidos passados para métodos
- **HTTP Status**: 400
- **Exemplo**: "ID deve ser maior que zero"

#### `ConstraintViolationException`
- **Uso**: Violação de constraints de validação Bean Validation
- **HTTP Status**: 400
- **Exemplo**: "Email deve ter formato válido"

#### `MethodArgumentNotValidException`
- **Uso**: Validação de DTOs com @Valid
- **HTTP Status**: 400
- **Exemplo**: Campos obrigatórios ausentes

### 2. Exceções de Recurso Não Encontrado (404 - Not Found)

#### `ResourceNotFoundException`
- **Uso**: Recursos não encontrados nos services
- **HTTP Status**: 404
- **Exemplo**: "Usuário não encontrado com ID: 123"

#### `TenantNotFoundException`
- **Uso**: Tenant específico não encontrado
- **HTTP Status**: 404
- **Exemplo**: "Tenant não encontrado"

#### `NoHandlerFoundException`
- **Uso**: Endpoint não encontrado
- **HTTP Status**: 404
- **Exemplo**: "Endpoint não encontrado"

### 3. Exceções de Conflito (409 - Conflict)

#### `ConflictException`
- **Uso**: Conflitos de negócio (duplicidade, etc.)
- **HTTP Status**: 409
- **Exemplo**: "Já existe um usuário com este email"

#### `DataIntegrityViolationException`
- **Uso**: Violação de integridade no banco de dados
- **HTTP Status**: 409
- **Exemplo**: "Violação de chave única"

#### `OptimisticLockingFailureException`
- **Uso**: Conflito de concorrência otimista
- **HTTP Status**: 409
- **Exemplo**: "Dados foram modificados por outro usuário"

### 4. Exceções de Autorização (401 - Unauthorized)

#### `SessionExpiredException`
- **Uso**: Sessão expirada
- **HTTP Status**: 401
- **Exemplo**: "Sessão expirada"

#### `InvalidTokenException`
- **Uso**: Token inválido ou malformado
- **HTTP Status**: 401
- **Exemplo**: "Token inválido"

### 5. Exceções de Permissão (403 - Forbidden)

#### `InsufficientPermissionsException`
- **Uso**: Permissões insuficientes para operação
- **HTTP Status**: 403
- **Exemplo**: "Permissões insuficientes"

#### `SecurityException`
- **Uso**: Erros de segurança
- **HTTP Status**: 403
- **Exemplo**: "Erro de segurança"

#### `IllegalAccessException`
- **Uso**: Acesso negado
- **HTTP Status**: 403
- **Exemplo**: "Acesso negado"

### 6. Exceções de Banco de Dados (500 - Internal Server Error)

#### `DataAccessException`
- **Uso**: Erros gerais de acesso aos dados
- **HTTP Status**: 500
- **Exemplo**: "Erro de acesso aos dados"

#### `SQLException`
- **Uso**: Erros específicos de SQL
- **HTTP Status**: 500
- **Exemplo**: "Erro de banco de dados"

### 7. Exceções de Processamento (400 - Bad Request)

#### `JsonProcessingException`
- **Uso**: Erro ao processar JSON
- **HTTP Status**: 400
- **Exemplo**: "Erro ao processar JSON"

#### `HttpMessageNotReadableException`
- **Uso**: Erro ao ler mensagem HTTP
- **HTTP Status**: 400
- **Exemplo**: "Erro ao ler mensagem HTTP"

### 8. Exceções de Rede (400 - Bad Request)

#### `UnknownHostException`
- **Uso**: Endereço de rede inválido
- **HTTP Status**: 400
- **Exemplo**: "Endereço de rede inválido"

### 9. Exceções de Requisição HTTP

#### `HttpRequestMethodNotSupportedException`
- **Uso**: Método HTTP não suportado
- **HTTP Status**: 405
- **Exemplo**: "Método HTTP não suportado"

#### `MissingServletRequestParameterException`
- **Uso**: Parâmetro obrigatório ausente
- **HTTP Status**: 400
- **Exemplo**: "Parâmetro obrigatório ausente"

#### `MethodArgumentTypeMismatchException`
- **Uso**: Tipo de parâmetro inválido
- **HTTP Status**: 400
- **Exemplo**: "Tipo de parâmetro inválido"

### 10. Exceções de Negócio (422 - Unprocessable Entity)

#### `BusinessException`
- **Uso**: Regras de negócio violadas
- **HTTP Status**: 422
- **Exemplo**: "Erro de regra de negócio"

#### `UnsupportedOperationException`
- **Uso**: Operação não suportada
- **HTTP Status**: 501
- **Exemplo**: "Operação não suportada"

#### `IllegalStateException`
- **Uso**: Estado inválido para operação
- **HTTP Status**: 409
- **Exemplo**: "Estado inválido para a operação"

### 11. Exceções de Timeout (408 - Request Timeout)

#### `TimeoutException`
- **Uso**: Timeout na operação
- **HTTP Status**: 408
- **Exemplo**: "Timeout na operação"

### 12. Exceção Genérica (500 - Internal Server Error)

#### `Exception`
- **Uso**: Qualquer exceção não tratada
- **HTTP Status**: 500
- **Exemplo**: "Erro interno do servidor"

## Estrutura da Resposta de Erro

Todas as exceções retornam um `ErrorResponse` com a seguinte estrutura:

```json
{
  "timestamp": "2024-01-30T12:34:56.789Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Mensagem de erro",
  "path": "/api/users",
  "details": ["Detalhe 1", "Detalhe 2"]
}
```

## Boas Práticas

1. **Use exceções específicas**: Prefira exceções específicas ao invés de genéricas
2. **Mensagens claras**: Sempre forneça mensagens de erro claras e úteis
3. **Logs apropriados**: Registre exceções com nível de log apropriado
4. **Tratamento consistente**: Use o GlobalExceptionHandler para tratamento consistente
5. **Documentação**: Documente exceções customizadas

## Exemplos de Uso

### No Service Layer
```java
if (user == null) {
    throw new ResourceNotFoundException("Usuário não encontrado com ID: " + id);
}

if (emailExists) {
    throw new ConflictException("Já existe um usuário com este email");
}
```

### No Controller Layer
```java
@PostMapping
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
    // O GlobalExceptionHandler tratará ValidationException automaticamente
    User user = userService.createUser(request.getName(), request.getEmail());
    return ResponseEntity.ok(UserMapper.toResponse(user));
}
```
