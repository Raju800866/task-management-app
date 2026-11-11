# TaskFlow — project guide for AI assistants

Task management app built as microservices. Read this before making changes.

## Structure
- `auth-service/` — Python 3.12, FastAPI, SQLAlchemy 2, PostgreSQL. Register, login, issues JWT (HS256, `sub` = user id, 60 min).
  - `app/routes.py` (endpoints under `/api/auth`), `app/models.py`, `app/schemas.py`, `app/security.py`, `app/config.py`
- `task-service/` — Java 17, Spring Boot 3.5, Spring Data JPA, PostgreSQL. Task CRUD under `/api/tasks`, protected by JWT.
  - Package `com.taskapp.taskservice`: `task/Task.java` (entity), `task/dto/TaskRequest.java`, `task/dto/TaskResponse.java`,
    `task/TaskService.java`, `task/TaskController.java`, `task/TaskRepository.java`, `config/SecurityConfig.java`,
    `exception/GlobalExceptionHandler.java`. Tests: `src/test/.../TaskControllerTest.java`.
- `frontend/` — React 19 + Vite + TailwindCSS 4. `src/api.js` (all API calls, relative `/api/...` URLs),
  `src/pages/` (Login, Register, Dashboard), `src/components/` (TaskCard, TaskFormModal, …), `src/constants.js` (status/priority lists).
- `docker-compose.yml` — local stack (2 Postgres + both services + frontend/nginx on http://localhost:3000).
- `deploy/` — AWS: `docker-compose.prod.yml` (EC2: both services + nginx gateway), `nginx.conf`, `aws/setup-cloudfront.sh`.
- `deploy.sh` — one-command redeploy to AWS (`./deploy.sh`, `./deploy.sh backend`, `./deploy.sh frontend`).

## Key rules (keep these when changing code)
- The user id ALWAYS comes from the JWT (`jwt.getSubject()`), never from the request body.
- Every task query is scoped by owner (`findByIdAndUserId`); another user's task returns 404.
- Each service has its own database (`auth_db`, `task_db`); no cross-database foreign keys.
- New DB columns must be NULLABLE or have a default — tables are auto-updated (`ddl-auto: update` / `create_all`) and production RDS already has rows.
- A new task field must be added in ALL layers: `Task.java` → `TaskRequest` → `TaskResponse` (+ `TaskResponse.from`) → `TaskService.apply()` → frontend `TaskFormModal.jsx` and `TaskCard.jsx`.
- Enum values (status, priority) must match between Java enums and `frontend/src/constants.js`.
- Validation errors are returned as RFC 7807 JSON with an `errors` map; the frontend shows them per field.
- Java must stay compatible with Java 17. Keep code style consistent with the existing files.
- Never commit secrets. Real values live only in `deploy/.env` on the server.

## Commands
- Task service tests: `cd task-service && mvn test`
- Run everything locally: `docker-compose up --build` → http://localhost:3000
- Frontend dev server: `cd frontend && npm run dev` → http://localhost:5173 (proxies /api to 8000/8080)
- Deploy to AWS (after committing): `./deploy.sh`

## Live URL
https://d2mdtqoz9h6pyu.cloudfront.net (CloudFront → S3 frontend, /api/* → EC2 in ap-southeast-2, RDS PostgreSQL 16)
