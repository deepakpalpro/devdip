# Banking Forms Platform

Multi-tenant banking support platform — form building, submission, processing, and downstream integration.

## Documentation

| Document | Audience | Contents |
|----------|----------|----------|
| [docs/TECHNICAL_GUIDE.md](docs/TECHNICAL_GUIDE.md) | Tech Lead / Developers | Component-by-component walkthrough, component IDs, flows, extension points |
| [docs/SOLUTION_ARCHITECTURE.md](docs/SOLUTION_ARCHITECTURE.md) | Solution Architect | C4 views, ADRs, NFRs, security/data/integration architecture, roadmap |
| [docs/PROJECT_MANAGEMENT.md](docs/PROJECT_MANAGEMENT.md) | Project Manager / PO | Epics, user stories + acceptance criteria, sprint plan, milestones, story→component traceability |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | All | Original full technical design (source of truth) |
| [docs/MCP_INTEGRATION.md](docs/MCP_INTEGRATION.md) | Developers / QA | MCP agent integration — architecture & tools (Phase 5) |
| [docs/MCP_TECHNICAL_GUIDE.md](docs/MCP_TECHNICAL_GUIDE.md) | Developers / QA | MCP UAT checklist, run-all-servers commands |

User stories in the PM doc map to technical components via IDs (e.g. `US-2.1` → `M-FORMDEF`); see the traceability matrix in [PROJECT_MANAGEMENT.md §5](docs/PROJECT_MANAGEMENT.md) and the component index in [TECHNICAL_GUIDE.md §13](docs/TECHNICAL_GUIDE.md).

## Prerequisites

- Java 21
- Gradle 8.x (or use `./gradlew` after generating the wrapper)

## Quick start

**Run all servers (backend, frontends, Docker integrations):**

```bash
./scripts/start-all-dev.sh              # full dev stack
./scripts/start-all-dev.sh --obs --mcp  # + Prometheus/Grafana + MCP HTTP
./scripts/stop-all-dev.sh               # tear down
```

**Backend only:**

```bash
./gradlew bootRun
```

The app starts on port **8080** with the `local` profile (in-memory H2, MySQL compatibility mode, Flyway migrations).

Health check: `GET http://localhost:8080/actuator/health`

**MCP agent UAT:** see [docs/MCP_TECHNICAL_GUIDE.md](docs/MCP_TECHNICAL_GUIDE.md)

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
| `mcp-server/` | MCP agent bridge — LLM tools for form suggest/fill (Phase 5) |

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
