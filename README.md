# User Service - API REST Spring Boot

Uma API REST moderna construída com Spring Boot, seguindo padrões de arquitetura sênior e boas práticas de desenvolvimento.

## 🚀 Características

- **Arquitetura MVC** bem estruturada
- **ACID e Idempotência** garantidas
- **Undertow** como servidor web de alta performance
- **HikariCP** com pool de conexões otimizado
- **PostgreSQL** como banco de dados
- **Flyway** para migrações de banco
- **TestContainers** para testes de integração
- **Mockito** para testes unitários
- **Docker** para containerização
- **Monitoramento** com Actuator e Prometheus

## 📁 Estrutura do Projeto

```
src/
├── main/java/com/example/
│   ├── api/                    # Camada de Apresentação
│   │   ├── dto/               # Data Transfer Objects
│   │   ├── exception/         # Exception Handler Global
│   │   └── mapper/            # Mappers DTO ↔ Entity
│   ├── config/                # Configurações
│   ├── controller/            # Controllers REST
│   ├── model/                 # Camada de Dados
│   └── service/               # Camada de Negócio
└── test/                      # Testes
    ├── java/com/example/
    │   ├── controller/        # Testes de Controller
    │   ├── service/           # Testes Unitários
    └── integration/           # Testes de Integração
```

## 🛠️ Configuração e Execução

### Pré-requisitos

- Java 21+
- PostgreSQL 16+
- Docker (opcional)
- Gradle 8.5+

### Configuração de Ambiente

O projeto possui configurações específicas para cada ambiente:

- **Desenvolvimento**: `config/dev.env`
- **Staging**: `config/stage.env`
- **Produção**: `config/prod.env`

### Execução Rápida

```bash
# Tornar script executável
chmod +x run.sh

# Desenvolvimento
./run.sh dev start

# Staging
./run.sh stage start

# Produção
./run.sh prod start

# Executar testes
./run.sh dev test

# Executar com Docker
./run.sh prod docker
```

### Execução Manual

```bash
# Desenvolvimento
./gradlew bootRun --args='--spring.profiles.active=dev'

# Staging
./gradlew bootRun --args='--spring.profiles.active=stage'

# Produção
./gradlew bootRun --args='--spring.profiles.active=prod'

# Compilar
./gradlew clean build

# Testes
./gradlew test
```

## 🐳 Docker

### Executar com Docker Compose

```bash
# Desenvolvimento
docker-compose up

# Produção
SPRING_PROFILES_ACTIVE=prod docker-compose up
```

### Build da Imagem

```bash
# Build da imagem
docker build -t users-service:latest .

# Executar container
docker run -p 8080:8080 users-service:latest
```

## 🧪 Testes

### Executar Todos os Testes

```bash
./gradlew test
```

### Tipos de Testes

- **Testes Unitários**: Usando Mockito
- **Testes de Integração**: Usando TestContainers
- **Testes de Controller**: Usando MockMvc

### Cobertura de Testes

```bash
./gradlew jacocoTestReport
```

## 📊 Monitoramento

### Endpoints de Monitoramento

- **Health Check**: `http://localhost:8080/actuator/health`
- **Info**: `http://localhost:8080/actuator/info`
- **Métricas**: `http://localhost:8080/actuator/metrics`
- **Prometheus**: `http://localhost:8080/actuator/prometheus`

### Swagger/OpenAPI

- **Documentação**: `http://localhost:8080/swagger-ui.html`

## 🔧 Configurações

### Perfis de Configuração

- **dev**: Desenvolvimento local
- **stage**: Ambiente de staging
- **prod**: Produção

### Variáveis de Ambiente

```bash
# Banco de dados
DB_URL=jdbc:postgresql://localhost:5432/usersdb
DB_USERNAME=users
DB_PASSWORD=password

# Servidor
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_EXAMPLE=DEBUG
```

## 📈 Performance

### Otimizações Implementadas

- **Undertow**: Servidor web de alta performance
- **HikariCP**: Pool de conexões otimizado
- **Cache**: Configuração de cache simples
- **Compressão**: Gzip habilitado
- **JVM**: Otimizações para containers

### Configurações por Ambiente

- **DEV**: Pool pequeno, logs detalhados
- **STAGE**: Pool médio, logs moderados
- **PROD**: Pool otimizado, logs mínimos

## 🏗️ Arquitetura

### Padrões Implementados

- **MVC**: Model-View-Controller
- **Repository**: Padrão Repository
- **Service Layer**: Camada de serviços
- **DTO**: Data Transfer Objects
- **Exception Handling**: Tratamento global de exceções

### Princípios SOLID

- **SRP**: Responsabilidade única
- **OCP**: Aberto para extensão
- **LSP**: Substituição de Liskov
- **ISP**: Segregação de interfaces
- **DIP**: Inversão de dependência

## 🔒 Segurança

### Configurações de Segurança

- **CORS**: Configurado para desenvolvimento
- **Headers**: Headers de segurança
- **Validação**: Validação de entrada
- **Sanitização**: Sanitização de dados

## 📝 API Endpoints

### Usuários

- `GET /api/users` - Listar usuários
- `GET /api/users/{id}` - Buscar usuário por ID
- `POST /api/users` - Criar usuário
- `PUT /api/users/{id}` - Atualizar usuário
- `DELETE /api/users/{id}` - Deletar usuário
- `GET /api/users/search?name={name}` - Buscar por nome
- `GET /api/users/email/{email}` - Buscar por email
- `GET /api/users/stats` - Estatísticas

### Características da API

- **Idempotência**: Operações idempotentes
- **Validação**: Validação de entrada
- **Tratamento de Erros**: Respostas consistentes
- **Documentação**: Swagger/OpenAPI

## 🚀 Deploy

### Ambiente de Produção

1. Configure as variáveis de ambiente
2. Execute o build: `./gradlew clean build`
3. Execute com Docker: `docker-compose up -d`
4. Monitore com Actuator

### Variáveis de Produção

```bash
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://localhost:5432/usersdb
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}
```

## 📚 Documentação Adicional

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Docker Documentation](https://docs.docker.com/)
- [TestContainers Documentation](https://www.testcontainers.org/)

## 🤝 Contribuição

1. Fork o projeto
2. Crie uma branch para sua feature
3. Commit suas mudanças
4. Push para a branch
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo LICENSE para mais detalhes.
