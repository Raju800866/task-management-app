# TaskFlow — Task Management App (Microservices)

A small task manager built as three independent services:

| Service | Stack | Responsibility | Port (local) |
|---|---|---|---|
| **auth-service** | Python · FastAPI · PostgreSQL | Register, login, issue JWT | 8000 |
| **task-service** | Java 17 · Spring Boot 3.5 · PostgreSQL | Task CRUD, protected by JWT | 8080 |
| **frontend** | React · Vite · TailwindCSS (served by nginx) | Login, Register, Task Dashboard | 3000 |

## Architecture

```
                 ┌─────────────── Browser ───────────────┐
                 │   React app — calls relative /api/*   │
                 └───────────────────┬───────────────────┘
                                     │
          Local: nginx (frontend container)   ·   AWS: CloudFront
                                     │
        ┌───────────── /* ───────────┼───────── /api/auth/* ───── /api/tasks/* ─┐
        ▼                            ▼                               ▼
  static React build          auth-service (FastAPI)         task-service (Spring Boot)
  (nginx / S3)                       │   signs JWT (HS256)          │   verifies JWT locally
                                     ▼                               ▼
                               auth_db (PostgreSQL)            task_db (PostgreSQL)
```

### Key design decisions
- **Database per service.** Auth and tasks each own their own PostgreSQL database. `tasks.user_id` is a plain column (no foreign key) because users live in the auth database.
- **Stateless JWT auth, no runtime coupling.** The auth service signs a JWT (`sub` = user id). The task service verifies the signature and expiry itself with the shared secret, so it never calls the auth service per request and keeps working if auth is briefly down.
- **Ownership enforced on the server.** The user id always comes from the token, never the request body. Every task query is filtered by owner; another user's task returns 404.
- **Single entry point, relative URLs.** The frontend only calls `/api/auth/*` and `/api/tasks/*`. Locally nginx routes them; on AWS CloudFront routes them to the load balancer. Same build everywhere, no CORS issues, HTTPS on AWS.
- **12-factor config.** Every service reads DB URLs, secrets and CORS origins from environment variables, so the same image runs locally and on AWS.

### Production next steps
- RS256 with a JWKS endpoint so only the auth service holds the signing key
- Flyway (task-service) and Alembic (auth-service) instead of auto schema creation
- Refresh tokens, rate limiting on login, secrets in AWS Secrets Manager

## Run locally (Task 1)

Requires Docker Desktop.

```bash
docker-compose up --build
```

Open **http://localhost:3000**, register, and start adding tasks.

Useful extras:
- Auth API docs (Swagger): http://localhost:8000/docs
- Health checks: http://localhost:8000/api/auth/health, http://localhost:8080/api/tasks/health
- Reset all data: `docker-compose down -v`
- Task-completion emails (caught by Mailpit, nothing is really sent): http://localhost:8025

## Notifications

When a task moves into **Done** (via the status dropdown or the edit form), task-service emails the owner.
- The recipient comes from the `email` claim in the JWT, so there's no call to the auth service.
- The email is sent after the database commit, on a background thread. If the mail server is down, the task still saves and the failure is only logged.
- Re-saving a task that's already done doesn't send a second email.
- Locally the emails go to Mailpit. On AWS, set `MAIL_ENABLED=true` and the SMTP settings in `deploy/.env` (see `deploy/.env.example`, e.g. AWS SES). Emails are off by default.

## API

**Auth service**
| Method | Path | Notes |
|---|---|---|
| POST | `/api/auth/register` | `{ name, email, password }` (password ≥ 8 chars) |
| POST | `/api/auth/login` | `{ email, password }` → `{ access_token, token_type, expires_in }` |
| GET | `/api/auth/me` | Current user (Bearer token) |

**Task service** (all need `Authorization: Bearer <token>`)
| Method | Path | Notes |
|---|---|---|
| GET | `/api/tasks?status=TODO` | List my tasks, optional status filter |
| GET | `/api/tasks/{id}` | One task |
| POST | `/api/tasks` | `{ title, description?, status?, priority?, dueDate? }` |
| PUT | `/api/tasks/{id}` | Full update |
| PATCH | `/api/tasks/{id}/status` | `{ status }` |
| DELETE | `/api/tasks/{id}` | Delete |

`status`: `TODO` · `IN_PROGRESS` · `DONE` — `priority`: `LOW` · `MEDIUM` · `HIGH`

## Developing without Docker
- Auth: `cd auth-service && source .venv/bin/activate && uvicorn app.main:app --reload` (needs Postgres on 5432)
- Tasks: `cd task-service && mvn spring-boot:run` (needs Postgres on 5433)
- Frontend: `cd frontend && npm install && npm run dev` → http://localhost:5173 (Vite proxies /api to the backends)
- Task service tests: `cd task-service && mvn test`
