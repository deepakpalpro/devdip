# Banking Forms Platform

Multi-tenant banking support platform — form building, submission, processing, and downstream integration.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full technical design.

## Prerequisites

- Java 21
- Gradle 8.x (or use `./gradlew` after generating the wrapper)

## Quick start

```bash
./gradlew bootRun
```

The app starts on port **8080** with the `local` profile (in-memory H2, MySQL compatibility mode, Flyway migrations).

Health check: `GET http://localhost:8080/actuator/health`

## Modules

| Module | Purpose |
|--------|---------|
| `app` | Bootstrap, Flyway, security, actuator |
| `bff-consumer` | Consumer-facing REST API (`/api/consumer/v1`) |
| `bff-admin` | Admin REST API (`/api/admin/v1`) |
| `module-identity` | Tenants, users, roles |
| `module-form-definition` | Form templates and versions |
| `module-submission` | Drafts and submissions |
| `module-pipeline` | Pipeline orchestration |
| `module-*` | Processing, PII, integrations, notifications, analytics, observability |

## Build

```bash
./gradlew build
```

## MySQL profile

```bash
SPRING_PROFILES_ACTIVE=mysql DB_HOST=localhost DB_NAME=banking_forms ./gradlew bootRun
```

## Frontend (React)

Monorepo under `frontend/` with consumer and admin portals.

```bash
cd frontend
npm install
npm run dev:consumer   # http://localhost:5173
npm run dev:admin      # http://localhost:5174
```

Both apps proxy `/api` to the backend on port 8080. Start the backend first:

```bash
./gradlew bootRun
```

Dev tenant ID (seed data): `11111111-1111-1111-1111-111111111111`
