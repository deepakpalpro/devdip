# MCP Technical Guide — UAT & Local Development

**Audience:** Developers, QA, solution architects validating LLM agent integration  
**Branch:** `phase5-mcp`  
**Companion:** [`MCP_INTEGRATION.md`](MCP_INTEGRATION.md) (architecture & tool reference)

This guide explains how to run **every server** needed for MCP UAT and how to verify each MCP capability step-by-step.

---

## 1. What runs where

| Process | Port | Required for MCP UAT? | How it starts |
|---------|------|----------------------|---------------|
| **Spring Boot backend** | `8080` | **Yes** — MCP calls consumer API | `./gradlew bootRun` |
| **MCP server (stdio)** | — | **Yes** (Cursor path) | Cursor MCP config → `node mcp-server/dist/index.js` |
| **MCP server (HTTP)** | `3100` | Optional (Inspector / HTTP clients) | `docker compose --profile mcp up -d` |
| Consumer portal | `5173` | Optional — visual verification | `npm run dev:consumer` |
| Admin portal | `5174` | Optional — review submissions | `npm run dev:admin` |
| Ollama | `11434` | No (MCP uses platform API only) | `./scripts/docker-up.sh` |
| Kafka | `9092` | No | `./scripts/docker-up.sh` |
| LocalStack | `4566` | No | `./scripts/docker-up.sh` |
| Prometheus / Grafana | `9090` / `3000` | No | `./scripts/docker-up.sh --obs` |

**Minimum MCP UAT:** backend `:8080` + MCP server (stdio in Cursor **or** HTTP on `:3100`).

---

## 2. Run all servers (copy-paste)

### Option A — One script (recommended)

From the repo root:

```bash
# Starts: Docker (Ollama/Kafka/LocalStack), backend, both frontends, MCP HTTP
./scripts/start-all-dev.sh

# Full stack including observability + MCP HTTP:
./scripts/start-all-dev.sh --obs --mcp

# Stop everything started by the script:
./scripts/stop-all-dev.sh
```

Logs: `/tmp/banking-forms-*.log`

### Option B — Manual (separate terminals)

**Terminal 1 — Docker integrations (optional but typical):**

```bash
cp .env.example .env
./scripts/docker-up.sh              # Ollama :11434, Kafka :9092, LocalStack :4566
# Or with observability:
./scripts/docker-up.sh --obs
# Or with MCP HTTP container:
./scripts/docker-up.sh --mcp
```

**Terminal 2 — Backend (required):**

```bash
./gradlew bootRun
# Verify:
curl -s http://localhost:8080/actuator/health
```

**Terminal 3 — Frontends (optional, for visual UAT):**

```bash
cd frontend && npm install
npm run dev:consumer    # http://localhost:5173
# new terminal:
npm run dev:admin       # http://localhost:5174
```

**Terminal 4 — MCP server (HTTP mode, if not using Docker profile):**

```bash
cd mcp-server && npm install && npm run build
export MCP_API_KEY=dev-mcp-key
export BANKING_API_URL=http://localhost:8080
npm run start:http
# Verify:
curl -s http://localhost:3100/health
```

**Cursor MCP (stdio mode — no Terminal 4 needed):**

1. Build once: `cd mcp-server && npm install && npm run build`
2. Add to Cursor MCP settings (see §4)
3. Restart Cursor / reload MCP servers

---

## 3. Environment reference

Copy and adjust:

```bash
cp .env.example .env
```

| Variable | Default | Used by |
|----------|---------|---------|
| `BANKING_API_URL` | `http://localhost:8080` | MCP server |
| `BANKING_TENANT_ID` | `11111111-1111-1111-1111-111111111111` | MCP → `X-Tenant-Id` |
| `BANKING_USER_ID` | `44444444-4444-4444-4444-444444444444` | MCP → `X-Dev-User-Id` |
| `MCP_API_KEY` | `dev-mcp-key` | HTTP MCP Bearer auth |
| `MCP_HTTP_PORT` | `3100` | HTTP MCP listen port |

---

## 4. Cursor MCP configuration (stdio UAT)

1. Build the server:

```bash
cd mcp-server && npm install && npm run build
```

2. Project config is already wired at **`.cursor/mcp.json`** (paths relative to repo root).  
   To override globally, edit `~/.cursor/mcp.json` instead.

3. Build once if `mcp-server/dist/` is missing:

```bash
cd mcp-server && npm install && npm run build
```

4. Ensure `./gradlew bootRun` is running on `:8080`.

5. **Refresh Cursor** (pick one — you do not need “Reload Window” specifically):
   - **Easiest:** quit Cursor fully (**Cmd+Q** on Mac), reopen this project folder
   - **Settings:** **Cursor Settings → Tools & MCP** (or **Features → MCP**) → find **banking-forms** → toggle off/on or click the refresh icon
   - **Command palette:** **Cmd+Shift+P** → type `reload` → choose **Developer: Reload Window** (if it appears; name varies by Cursor version)

6. **banking-forms** MCP row should show green. If red: **View → Output** → channel **MCP** for startup errors.

7. In Cursor chat (**Agent** mode), confirm tools appear: `list_forms`, `suggest_forms`, `fill_from_conversation`, etc.

Legacy template (absolute path): `mcp-server/cursor-mcp.example.json`

---

## 5. MCP UAT test plan

Run checks in order. Each step maps to a feature-plan task.

### Pre-flight

```bash
curl -s http://localhost:8080/actuator/health          # → {"status":"UP"}
cd mcp-server && npm test                              # → 4 tests pass
```

### UAT-1 — Form retrieval (Phase 2)

**Via API (what MCP tools call):**

```bash
TENANT=11111111-1111-1111-1111-111111111111

# list_forms
curl -s -H "X-Tenant-Id: $TENANT" http://localhost:8080/api/consumer/v1/forms | python3 -m json.tool

# get_form_definition
curl -s -H "X-Tenant-Id: $TENANT" \
  http://localhost:8080/api/consumer/v1/forms/LOAN_APPLICATION | python3 -m json.tool
```

**Via Cursor agent:** Ask *"List available banking forms"* → agent should call `list_forms`.  
**Pass criteria:** Returns `LOAN_APPLICATION`, `ACCOUNT_OPENING` (seeded forms).

---

### UAT-2 — Form suggestion (Phase 3)

**Via Cursor agent prompts:**

| Prompt | Expected top suggestion |
|--------|-------------------------|
| *"I want a personal loan for $25,000"* | `LOAN_APPLICATION` |
| *"I need to open a savings account"* | `ACCOUNT_OPENING` |
| *"What's the weather?"* | Empty / ask to clarify |

**Pass criteria:** `suggest_forms` returns ranked list; top match aligns with intent.

**Discovery API (optional):**

```bash
curl -s -X POST -H "X-Tenant-Id: $TENANT" -H "Content-Type: application/json" \
  -H "X-Dev-User-Id: 44444444-4444-4444-4444-444444444444" \
  -d '{"answers":{"need":"loan","amount":25000}}' \
  http://localhost:8080/api/consumer/v1/discovery/BANKING_NEEDS/evaluate | python3 -m json.tool
```

---

### UAT-3 — Prefill preview (Phase 4, no writes)

**Via Cursor agent:**

> *"Preview how you would fill LOAN_APPLICATION from: My name is Jane Doe and I need $25000"*

Agent should call `preview_prefill` and show:
- Extracted entities (`firstName`, `amount`, …)
- Mapped section data
- `missingRequired` fields (e.g. `lastName`, address if embedded)
- `requiresUserConfirmation: true`

**Pass criteria:** No submission created; preview JSON returned.

---

### UAT-4 — Fill from conversation (Phase 4)

**Via Cursor agent:**

> *"Fill a loan application for Jane Doe, $25000"*

Agent flow:
1. `suggest_forms` → `LOAN_APPLICATION`
2. `fill_from_conversation` → returns `submissionId`, `sectionData`, `missingRequired`
3. Agent presents data to user

**Verify in admin portal or API:**

```bash
# Replace SUBMISSION_ID from agent response
curl -s -H "X-Tenant-Id: $TENANT" \
  http://localhost:8080/api/admin/v1/submissions/SUBMISSION_ID | python3 -m json.tool
```

**Pass criteria:** Draft exists; `personal-info.firstName` = Jane; `loan-details.amount` = 25000.

**Note:** `LOAN_APPLICATION` has an embedded address section — agent may need follow-up prompts to complete `residence` before submit succeeds.

---

### UAT-5 — Submit with confirmation gate (Phase 4)

**Via Cursor agent:**

1. After prefill, user says: *"Yes, submit it"*
2. Agent must call `submit_submission` with `confirmed: true`

**Without confirmation:**

Agent calling `submit_submission` with `confirmed: false` must **fail** with confirmation error.

**After submit:**

```bash
# Wait ~3s for async pipeline
curl -s -H "X-Tenant-Id: $TENANT" \
  http://localhost:8080/api/admin/v1/submissions/SUBMISSION_ID/pipeline | python3 -m json.tool
```

**Pass criteria:**
- Submit returns `SUBMITTED` then pipeline moves to `PENDING_REVIEW`
- Timeline includes `AI_EVALUATED`, `SERVICE_CALL_*`, `DOWNSTREAM_*`

---

### UAT-6 — HTTP MCP auth (Phase 1 security)

```bash
# Health — no auth
curl -s http://localhost:3100/health

# MCP without token — should 401 when MCP_API_KEY is set
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:3100/mcp

# With token
curl -s -H "Authorization: Bearer dev-mcp-key" http://localhost:3100/mcp
```

**Pass criteria:** Unauthenticated requests rejected when key configured.

---

### UAT-7 — MCP Inspector (optional)

For HTTP transport debugging:

```bash
npx @modelcontextprotocol/inspector
# Connect to: http://localhost:3100/mcp
# Headers: Authorization: Bearer dev-mcp-key
```

Use Inspector to invoke `list_forms`, `suggest_forms`, etc. interactively.

---

### UAT-8 — Observability & downstream (optional)

Requires `./scripts/start-all-dev.sh --obs` (or `--obs --mcp`).

```bash
# Prometheus target UP
curl -s http://localhost:9090/api/v1/targets | grep -o '"health":"[^"]*"'

# Grafana dashboard
open http://localhost:3000   # admin/admin → Banking Forms → Banking Forms Platform

# Downstream providers (after configure-dev-downstream)
curl -s http://localhost:8080/api/admin/v1/downstream-providers \
  -H "X-Tenant-Id: 11111111-1111-1111-1111-111111111111"

# Webhook sink logs (after a submission)
docker compose logs webhook-sink | tail -10
```

**Pass criteria:** Prometheus scrape `health:"up"`; Grafana dashboard shows pipeline metrics; after submit, outbox shows `log-sink`, `rest-webhook`, and `kafka-stream` as `DISPATCHED`.

---

## 6. UAT checklist (sign-off)

| # | Test | Pass |
|---|------|------|
| 1 | Backend health UP | ☐ |
| 2 | `npm test` in mcp-server (4/4) | ☐ |
| 3 | Cursor MCP server connected (green) | ☐ |
| 4 | `list_forms` returns seeded forms | ☐ |
| 5 | `suggest_forms` — loan intent → LOAN_APPLICATION | ☐ |
| 6 | `preview_prefill` — no side effects | ☐ |
| 7 | `fill_from_conversation` — draft created | ☐ |
| 8 | `submit_submission` blocked without `confirmed=true` | ☐ |
| 9 | Submit + async pipeline completes | ☐ |
| 10 | Admin portal shows submission timeline | ☐ |
| 11 | Grafana dashboard shows pipeline metrics (`--obs`) | ☐ |
| 12 | Downstream fan-out: log + webhook + kafka (`configure-dev-downstream.sh`) | ☐ |

---

## 7. Troubleshooting

| Symptom | Fix |
|---------|-----|
| MCP tools not in Cursor | Rebuild `mcp-server/dist`; check absolute path in MCP config; restart Cursor |
| `Connection refused :8080` | Start `./gradlew bootRun` first |
| Submit returns 400 | Complete all required sections (check embedded address nesting) |
| Docker MCP can't reach backend | Backend on host; Docker uses `host.docker.internal:8080` |
| Grafana shows no metrics | Run `./scripts/docker-up.sh --obs` (sets `PROMETHEUS_SCRAPE_HOST` on Mac); confirm target UP at http://localhost:9090/targets |
| Downstream webhook/kafka empty | Run `./scripts/configure-dev-downstream.sh` after backend restart (H2 resets provider config) |
| `401` on `:3100/mcp` | Set header `Authorization: Bearer dev-mcp-key` |
| Ollama slow / unrelated | MCP UAT does not require Ollama — skip docker if only testing MCP |

---

## 8. Architecture quick reference

```
User ↔ Cursor Agent
         │ MCP (stdio)
         ▼
    mcp-server/
    ├── intent-matcher.ts      → suggest_forms
    ├── entity-extractor.ts    → preview_prefill / fill_from_conversation
    ├── field-mapper.ts        → map entities → schema fields
    └── client/banking-api.ts  → REST to :8080
         │
         ▼
    Consumer BFF (/api/consumer/v1/*)
         │
         ▼
    Submissions, pipeline, review (existing platform)
```

See [`MCP_INTEGRATION.md`](MCP_INTEGRATION.md) for the full tool catalog and security model.
