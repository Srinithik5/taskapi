# Secure Task Management REST API

![CI](https://github.com/Srinithik5/taskapi/actions/workflows/ci.yml/badge.svg)

A backend REST API where users register, log in, and manage their own tasks. Admins
can see every user and every task. Built as a hands-on security project: ownership is
enforced at the **database query level**, passwords are BCrypt-hashed, sessions are
stateless JWTs, and the first admin account is bootstrapped from environment
variables - there is deliberately no "promote yourself to admin" endpoint.

A small glossy frontend is included (served by the API itself, `http://localhost:8080`)
so the whole thing can be explored by clicking instead of typing curl commands, but
the project's real surface is the API - test it with Swagger UI or Postman.

## Tech stack

Java 21 &middot; Spring Boot 3 &middot; Spring Web &middot; Spring Data JPA (Hibernate) &middot;
Bean Validation &middot; PostgreSQL 16 &middot; Spring Security 6 + JWT (`jjwt`) + BCrypt &middot;
springdoc-openapi (Swagger) &middot; Actuator &middot; JUnit 5 + Mockito + MockMvc &middot;
H2 (tests only) &middot; Docker + Docker Compose &middot; GitHub Actions &middot; Maven
(wrapper included, no local install needed)

## Architecture

```
                 HTTP (JSON)
 ┌────────────┐  ───────────►  ┌─────────────────────────────────────────────┐
 │  Browser / │                │                Spring Boot app               │
 │  Postman / │                │                                               │
 │  Swagger   │                │  JwtAuthFilter ──► SecurityConfig (RBAC)      │
 │            │  ◄───────────  │        │                                      │
 └────────────┘   JSON / JWT   │        ▼                                      │
                                │  Controller ──► Service ──► Repository (JPA)  │
                                └───────────────────────┬───────────────────────┘
                                                         │ JDBC
                                                         ▼
                                                 ┌──────────────┐
                                                 │  PostgreSQL  │
                                                 └──────────────┘
```

Every request to `/api/tasks/**` or `/api/admin/**` passes through `JwtAuthFilter`
first (validates the JWT, loads the current user's role fresh from the database),
then `SecurityConfig`'s rules (401 if not authenticated, 403 if wrong role), and only
then reaches a controller. Controllers never touch the database directly - only
through a service, which never touches the database directly - only through a
repository. DTOs (not entities) cross the HTTP boundary in both directions.

## Run with Docker (3 commands)

```powershell
cp .env.example .env
```
```powershell
docker compose up --build -d
```
```powershell
curl http://localhost:8080/actuator/health
```

Then open `http://localhost:8080` for the UI, or `http://localhost:8080/swagger-ui/index.html`
for the interactive API docs. A first ADMIN account is created automatically from the
`ADMIN_EMAIL`/`ADMIN_PASSWORD` values in your `.env`.

## Run locally without Docker (for development)

```powershell
docker compose up -d db
```
```powershell
.\mvnw.cmd spring-boot:run
```

No system-wide Maven or Java install is strictly required beyond JDK 21 - `mvnw.cmd`
downloads the exact right Maven version itself on first run.

## Environment variables

| Variable | Used by | Dev default (do not use in production) | Purpose |
|---|---|---|---|
| `POSTGRES_DB` | `db`, `api` | `taskapi` | Database name |
| `POSTGRES_USER` | `db`, `api` | `taskapi` | Database user |
| `POSTGRES_PASSWORD` | `db`, `api` | `changeme` | Database password |
| `JWT_SECRET` | `api` | insecure placeholder | HMAC signing key for JWTs (must be ≥ 32 bytes) |
| `ADMIN_EMAIL` | `api` | `admin@example.com` | Email of the auto-created first admin |
| `ADMIN_PASSWORD` | `api` | `AdminPass123!` | Password of the auto-created first admin |

Copy `.env.example` to `.env` and override these for anything beyond local learning -
generate a real secret with `openssl rand -base64 32`.

## Endpoints

| Method | Path | Auth required | Description |
|---|---|---|---|
| POST | `/api/auth/register` | None | Register a new user |
| POST | `/api/auth/login` | None | Log in, receive a JWT |
| POST | `/api/tasks` | JWT | Create a task |
| GET | `/api/tasks` | JWT | List your own tasks (paginated, sortable, `?status=` filter) |
| GET | `/api/tasks/{id}` | JWT | Get one of your own tasks |
| PUT | `/api/tasks/{id}` | JWT | Replace a task (full update) |
| DELETE | `/api/tasks/{id}` | JWT | Delete a task |
| GET | `/api/admin/users` | JWT + ADMIN role | List every user |
| GET | `/api/admin/tasks` | JWT + ADMIN role | List every task, across all users |
| GET | `/swagger-ui/index.html` | None | Interactive API docs |
| GET | `/actuator/health` | None | Health check |
| GET | `/` | None | Glossy frontend dashboard |

A task or user you don't own always returns `404`, identical to one that doesn't
exist at all - the API never reveals which.

## Sample curl commands

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"Password123","name":"Your Name"}'

# Log in
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"Password123"}'

# Create a task (replace $TOKEN with the token from login)
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"Write resume bullets","description":"Summarize this project"}'

# List tasks, paginated and sorted
curl "http://localhost:8080/api/tasks?page=0&size=5&sort=createdAt,desc" \
  -H "Authorization: Bearer $TOKEN"
```

## Running the tests

```powershell
.\mvnw.cmd test
```

28 tests across three layers: service unit tests (Mockito), controller tests
(MockMvc + `@WithMockUser`), and a repository test (`@DataJpaTest` + H2) that proves,
against a real database, that one user can never fetch another user's task.

## CI

Every push/PR to `main` triggers [`.github/workflows/ci.yml`](.github/workflows/ci.yml):
checkout &rarr; JDK 21 + Maven cache &rarr; `mvn -B verify`. No database service is
needed in CI - the whole test suite is self-contained (Mockito + H2).

## Project layout

```
taskapi/
  src/main/java/com/srinithi/taskapi/
    controller/   HTTP layer - AuthController, TaskController, AdminController
    service/      Business logic - AuthService, TaskService, UserService
    repository/   Spring Data JPA interfaces - UserRepository, TaskRepository
    entity/       JPA entities - User, Task, Role, Status
    dto/          Request/response records - validated input, safe output
    exception/    Custom exceptions + GlobalExceptionHandler
    security/     SecurityConfig, JwtAuthFilter, JwtService, CustomUserDetailsService
    config/       OpenApiConfig, AdminInitializer
  src/main/resources/
    static/       The glossy frontend (index.html, css/, js/)
    application.properties
  src/test/java/  28 tests across service/controller/repository layers
```
