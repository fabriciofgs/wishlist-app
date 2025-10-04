# Wishlist API

API REST simples para gerenciar wishlists de clientes, construída com Java 17, Spring Boot e MongoDB.

## Visão geral do projeto

- **Objetivo**: CRUD de itens de wishlist por cliente, regras de negócio (máx 20 itens, sem duplicidade) e geração de IDs sequenciais para MongoDB.

- **Stack**: Java 17, Spring Boot, Spring Web, Spring Data MongoDB, Lombok, Hibernate Validator, JUnit5, Mockito, Docker, Docker Compose


## Estrutura principal do código

- package com.wishlist.controller

    - WishlistController (REST, mapeia endpoints)

- package com.wishlist.service

    - WishlistService (lógica de negócio, lança exceções)

    - SequenceGeneratorService (gera sequência para ids no MongoDB via MongoOperations)

- package com.wishlist.domain.model

    - Wishlist (campo itens: List<WishlistItem>)

    - WishlistItem (entidade Mongo, id Long, campos productId, clientId, etc.)

    - DatabaseSequence (coleção de sequência utilizada pelo SequenceGeneratorService)

- package com.wishlist.domain.repository

    - WishlistRepository extends MongoRepository<WishlistItem, Long>

- package com.wishlist.infra.exception

    - NotFoundException (RuntimeException)

    - BadRequestException (RuntimeException)

    - RestExceptionHandler (@ControllerAdvice; mapeia exceções para ResponseEntity JSON)


## Configuração (application.properties / environment)

Arquivo: src/main/resources/application.properties (exemplo mínimo para desenvolvimento)

properties

```
# MongoDB
spring.data.mongodb.uri=mongodb://admin:passwd@mongo:27017/wishlistdb?authSource=admin

# Porta da aplicação
server.port=8080

# Logging
logging.level.org.springframework=INFO
logging.level.com.wishlist=DEBUG

# Perfil ativo de dev
spring.profiles.active=local
```

Ambiente com Docker Compose (variáveis definidas via compose): a app resolve host `mongo` automaticamente quando executada no mesmo compose network.

## Endpoints API (contrato atual)

Base: /wishlist

- GET /wishlist/{id}

    - **Resposta 200**: JSON do WishlistItem

    - **Resposta 404**: JSON padronizado pelo ControllerAdvice (ex.: status, message, timestamp)

- GET /wishlist/client/{clientId}

    - **Resposta 200**: JSON do objeto Wishlist { itens: [...] }

    - **Resposta 404**: JSON padronizado (cliente sem itens)

- GET /wishlist/client/{clientId}/product/{productId}

    - **Resposta 200**: JSON do WishlistItem

    - **Resposta 404**: JSON padronizado (item não encontrado)

- POST /wishlist/add_item

    - **Request body**: JSON do WishlistItem (campo id omitido/nullable)

    - **Validações**: usar anotações JSR‑303 (ex.: @NotNull em productId e clientId) — payload inválido retorna 400 com detalhes de validação (via ControllerAdvice)

    - **Resposta 200**: JSON do WishlistItem salvo

    - **Resposta 400**: JSON padronizado (limite de 20 itens ou duplicidade)

- POST /wishlist/add_list

    - **Request body**: JSON do Wishlist { itens: [...] }

    - **Comportamento**: itera e chama service para cada item; falhas lançam exceções tratadas pelo ControllerAdvice

    - **Resposta 200**: texto "Itens adicionados a wishlist com sucesso"

- DELETE /wishlist/delete/{id}

    - **Resposta 200**: texto "Item removido com sucesso"

- DELETE /wishlist/delete

    - **Resposta 200**: texto "Todos os items foram removidos da wishlist com sucesso"


Exemplo de payload add_item

json

```
{
  "productId": 100,
  "productName": "Bota",
  "clientId": 10,
  "clientName": "Maria"
}
```

## Docker & Deployment (dev/demo)

dockerfile

```
# --- build stage: compile the project and produce the fat JAR ---
FROM maven:3-openjdk-17-slim AS build
WORKDIR /workspace

# copy only what is needed to leverage Docker layer cache for dependencies
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN mvn -B -f pom.xml -N dependency:go-offline

# copy source and build
COPY src ./src
RUN mvn -B -DskipTests package

# --- runtime stage: minimal image to run the JAR as non-root ---
FROM openjdk:17-jdk-slim
ARG APP_JAR_NAME
WORKDIR /app

# create non-root user
RUN useradd --create-home --shell /bin/bash appuser \
  && mkdir /app/logs \
  && chown -R appuser:appuser /app

# copy jar from build stage (matches target/*.jar)
COPY --from=build /workspace/target/*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar

USER appuser
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]

```

Docker Compose (sobe mongo + app)

yaml

```
services:
  mongo:
    image: mongo:latest
    container_name: wishlist-mongo
    restart: unless-stopped
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: passwd
      MONGO_INITDB_DATABASE: wishlistdb
    volumes:
      - wishlist-data:/data/db
    ports:
      - "27017:27017"
    networks:
      - wishlist-net
    healthcheck:
      test: ["CMD", "mongosh", "--quiet", "--eval", "db.adminCommand('ping')"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build:
      context: .
      dockerfile: Dockerfile
    image: wishlist-app:latest
    container_name: wishlist-app
    restart: unless-stopped
    depends_on:
      - mongo
    environment:
      # String de conexão que a aplicação espera
      SPRING_DATA_MONGODB_URI: mongodb://admin:passwd@mongo:27017/wishlistdb?authSource=admin
      SERVER_PORT: 8080
      SPRING_PROFILES_ACTIVE: local
    ports:
      - "8080:8080"
    networks:
      - wishlist-net

volumes:
  wishlist-data:

networks:
  wishlist-net:
    driver: bridge

```

Comandos úteis:

- Build & up: docker compose up -d --build

- Logs: docker compose logs -f app

- Tear down: docker compose down -v



## Como começar (quickstart)

### Docker compose

1. Subir com Docker Compose:

    - docker compose up -d --build

2. Testar endpoint:

    - curl http://localhost:8080/wishlist/client/10

### Docker

1. Crie um network:
   - docker network create NET1

2. Crie um volume:
   - docker volume create VOL1

3. Subir o Container com mongodb:
   - docker run -d --network NET1 -h mongo --name mongo -e MONGO_INITDB_ROOT_USERNAME=admin -e MONGO_INITDB_ROOT_PASSWORD=passwd -p 27017:27017 -v VOL1:/data/db mongo:latest

4. Build o container da app:
   - docker build -t wishlist-app .

5. Subir o container da app:
   - docker run -d --network NET1 -p 8000:8000 wishlist-app

6. Testar endpoint:

    - curl http://localhost:8080/wishlist/client/10

### Sem Docker

1. Ajuste application.properties ou defina variáveis de ambiente (SPRING_DATA_MONGODB_URI, SERVER_PORT).

2. Build: mvn clean package

3. Execute o .jar gerado na ./target

4. Testar endpoint:

    - curl http://localhost:8080/wishlist/client/10