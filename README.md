# Smart Lead

Receives contact messages (an email or phone number plus free text), classifies them with an LLM
(Hugging Face, `openai/gpt-oss-120b:groq`) and stores the ones that look like sales leads.

## How it works

1. A client sends `POST /api/v1/messages`. The message is validated and saved, and the API answers right away.
2. In the background, a `PENDING` row is added to `hf_completions` for that message.
3. A scheduled job (every 60 seconds after the previous run finished) takes up to `batch-size` pending rows
   and asks the model to classify each message with a JSON schema.
4. The answer is stored on the completion (`SUCCESS`, with token usage). If the message is a lead, a row is added to `leads`.
5. Temporary failures (rate limit, timeouts) leave the row `PENDING` for the next run. Permanent ones (HTTP 400, 413, 422)
   mark it `ERROR`. When the daily request limit is used up, the job pauses until it resets.

## Architecture

```mermaid
flowchart LR
    client([Client]) -->|X-API-KEY| security
    browser([Swagger UI]) -->|HTTP Basic| security

    subgraph app [Spring Boot application]
        security[Security filters]
        security --> mc[MessageController<br/>POST/GET /api/v1/messages]
        security --> lc[LeadController<br/>GET /api/v1/leads]
        security --> cc[CompletionController<br/>GET /api/v1/completions]

        mc --> ms[MessageService]
        lc --> ls[LeadService]
        cc --> cs[CompletionService]

        ms -->|"submitMessageAsync (async)"| hf[HuggingFaceService]
        job([Scheduled job<br/>every 60 s]) --> hf
        hf --> rl[Rate limiter]
        hf -->|"submitLeadAsync (async)"| ls
    end

    ms --> db[(PostgreSQL<br/>messages, hf_completions, leads)]
    ls --> db
    cs --> db
    hf --> db
    hf -->|chat completions| hfapi[[Hugging Face router]]
```

```mermaid
sequenceDiagram
    participant C as Client
    participant API as MessageController
    participant DB as PostgreSQL
    participant Job as Scheduled job
    participant HF as Hugging Face

    C->>API: POST /api/v1/messages
    API->>DB: insert message
    API-->>C: 201 Created
    API--)DB: (async) insert completion = PENDING
    loop every 60 s
        Job->>DB: pending completions (oldest first)
        Job->>HF: classify message
        HF-->>Job: JSON result + usage
        Job->>DB: completion = SUCCESS
        Job--)DB: (async) insert lead, if it is one
    end
```

| Package | Contents |
|---|---|
| `controller` | REST endpoints |
| `service.message`, `service.lead`, `service.completion` | Business logic, DTOs and filters |
| `service.hgface` | Hugging Face client, rate limiter and the scheduled processing |
| `domain` | JPA entities and Spring Data repositories |
| `config` | Security, OpenAPI, async executor, HTTP client |
| `exceptions` | Error types and the global exception handler |

Database schema changes are Liquibase changelogs in `src/main/resources/db/changelog/<yyyy>/<mm>/<ddHHMM>-<name>.xml`.
Hibernate only validates them (`ddl-auto: validate`).

## Requirements

- Java 25
- Docker (PostgreSQL for local runs, and Testcontainers for the tests)
- A [Hugging Face access token](https://huggingface.co/settings/tokens)

The Maven wrapper (`./mvnw`) is included, so Maven does not need to be installed.

## Setup

### 1. Clone

```bash
git clone https://github.com/muzaffar-mz/smart-lead.git
cd smart-lead
java -version   # must print 25
```

### 2. Start PostgreSQL

```bash
docker run -d --name smart-lead-db \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=smart-lead \
  -p 5432:5432 postgres:17-alpine
```

Port 5432 must be free. To start over later: `docker rm -f smart-lead-db`.

### 3. Configure

The application reads these environment variables:

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `API_KEY` | yes | | Value clients send in the `X-API-KEY` header |
| `SWAGGER_LOGIN` | yes | | Swagger UI user name |
| `SWAGGER_PASSWORD` | yes | | Swagger UI password |
| `HUGGING_FACE_TOKEN` | yes | | Hugging Face access token |
| `TEST_KEY` | no | empty (endpoint disabled) | Value for the `X-TEST-KEY` header of `/api/v1/test/**` |
| `DATABASE_URL` | no | `jdbc:postgresql://localhost:5432/smart-lead` | JDBC URL |
| `POSTGRES_USER` | no | `postgres` | Database user |
| `POSTGRES_PASSWORD` | no | `postgres` | Database password |
| `PORT` | no | `7695` | HTTP port |

For local work you can put the values in `src/main/resources/application-local.yaml` (git-ignored) and run with the `local` profile:

```yaml
security:
  api-key: local-api-key
spring:
  security:
    user:
      name: swagger
      password: swagger
hugging-face:
  token: <your token>
```

Never commit tokens.

### 4. Build

```bash
./mvnw clean package -DskipTests
```

This compiles the code and creates `target/smart-lead-0.0.1-SNAPSHOT.jar`.
Leave out `-DskipTests` to run the tests as well (Docker must be running, see [Tests](#tests)).

### 5. Run

With environment variables:

```bash
export API_KEY=local-api-key SWAGGER_LOGIN=swagger SWAGGER_PASSWORD=swagger HUGGING_FACE_TOKEN=<token>
java -jar target/smart-lead-0.0.1-SNAPSHOT.jar
```

Or with the `local` profile file from step 3:

```bash
java -jar target/smart-lead-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

During development you can skip the build step and start it with Maven instead:
`./mvnw spring-boot:run` (add `-Dspring-boot.run.profiles=local` for the profile).

The application listens on <http://localhost:7695>. Check it with
`curl http://localhost:7695/actuator/health`, which should contain `"status":"UP"`.
On the first start Liquibase creates the tables.

### 6. Try it

```bash
curl -X POST http://localhost:7695/api/v1/messages \
  -H 'X-API-KEY: local-api-key' -H 'Content-Type: application/json' \
  -d '{"contact": "john@acme.com", "message": "Hi, we would like a demo for 50 users."}'

curl -H 'X-API-KEY: local-api-key' http://localhost:7695/api/v1/completions
curl -H 'X-API-KEY: local-api-key' http://localhost:7695/api/v1/leads
```

After the next scheduled run (up to a minute) the message appears as a lead if the model classified it as one.

## Web UI

Open <http://localhost:7695/>. The main page is a public contact form (it posts to `POST /api/v1/messages`, which needs no key).
The **Admin** button in the top right asks for the same login and password as Swagger (`SWAGGER_LOGIN` / `SWAGGER_PASSWORD`).
After a successful login the leads, messages and completions views open, with filters and a side panel that shows the
message and model answer behind each row. The files are plain HTML, CSS and JavaScript in `src/main/resources/static`.

`POST /api/public/admin/login` checks the login and returns the `API_KEY`, which the page keeps in the browser tab
(`sessionStorage`) and sends as `X-API-KEY`. There is no rate limit on the login yet.

## API

All endpoints require the `X-API-KEY` header, except `/actuator/health`, `POST /api/v1/messages` (the public contact form) and the admin login.
Swagger UI is at `/swagger-ui.html` (HTTP Basic with `SWAGGER_LOGIN` / `SWAGGER_PASSWORD`),
and the OpenAPI document at `/v3/api-docs`.

| Method and path | Description |
|---|---|
| `POST /api/v1/messages` | Submit a message |
| `GET /api/v1/messages` | Search messages (id, email, phone, ip, date range; paged) |
| `GET /api/v1/messages/{id}` | One message |
| `GET /api/v1/leads` | Search leads (type, urgency, message id, date range; paged) |
| `GET /api/v1/leads/{id}` | One lead |
| `GET /api/v1/completions` | Search completions (status, message id, date range; paged) |
| `GET /api/v1/completions/{id}` | One completion |
| `GET /api/v1/completions/message/{messageId}` | The completion of a message |
| `GET /api/v1/test/seed-messages?count=10` | Submits `count` (1-1000, default 10) test messages. Needs `X-API-KEY` and `X-TEST-KEY` |

List endpoints take `page` (default 0), `size` (default 100, max 5000), `sortBy` and `direction` (`asc` or `desc`).
Every response is wrapped in `BaseResponse`: the data is in `payload`, and errors have a negative `code`
(`-10` not found, `-20` invalid request, `-30` unauthorized, `-99` internal error).

## Tests

```bash
./mvnw test          # unit, JPA and integration tests
./mvnw verify
```

Docker must be running. One PostgreSQL container (Testcontainers) is shared by all database tests,
and the schema comes from the same Liquibase changelogs as in production. The Hugging Face client is mocked.

## Docker

```bash
docker build -t smart-lead .
docker run --rm -p 7695:7695 \
  -e API_KEY=... -e SWAGGER_LOGIN=... -e SWAGGER_PASSWORD=... -e HUGGING_FACE_TOKEN=... \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/smart-lead \
  smart-lead
```

## CI

`.github/workflows/ci.yml` builds and tests on pull requests to `main` and `develop` and on pushes to `main`,
then checks that the Docker image builds.
