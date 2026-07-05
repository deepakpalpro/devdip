# MCP Server Integration — Banking Forms Platform

**Branch:** `phase5-mcp`  
**Purpose:** Enable LLM agents (Cursor, Claude Desktop, custom bots) to suggest and fill banking forms programmatically via the [Model Context Protocol (MCP)](https://modelcontextprotocol.io), without users navigating the consumer portal.

---

## Architecture

```
LLM Agent (Cursor / Claude / custom)
        │  MCP tools (stdio or HTTP)
        ▼
  mcp-server/  (@banking-forms/mcp-server)
        │  REST (X-Tenant-Id, X-Dev-User-Id)
        ▼
  Banking Forms Platform (:8080)
        └── Consumer BFF — forms, discovery, submissions
```

The MCP server is a thin **agent bridge** — it does not duplicate business logic. All persistence, validation, and pipeline processing remain in the Spring Boot monolith.

---

## MCP Tools

| Tool | Phase | Description |
|------|-------|-------------|
| `list_forms` | 2 | List published forms for the tenant |
| `get_form_definition` | 2 | Full schema + flattened field catalog for agents |
| `suggest_forms` | 3 | NLU keyword intent → ranked form suggestions |
| `evaluate_discovery` | 3 | Structured discovery questionnaire evaluation |
| `preview_prefill` | 4 | Entity extraction + field mapping preview (no write) |
| `create_draft_submission` | 4 | Create DRAFT submission |
| `save_section` | 4 | Persist section data on a draft |
| `fill_from_conversation` | 4 | Extract → map → draft → save sections in one step |
| `get_submission` | 4 | Review status + section data |
| `submit_submission` | 4 | Submit after `confirmed=true` (user approval gate) |

---

## Quick Start

### 1. Backend running

```bash
./gradlew bootRun   # :8080
```

### 2. Build MCP server

```bash
cd mcp-server && npm install && npm run build && npm test
```

### 3. Cursor / Claude Desktop (stdio)

Copy `mcp-server/cursor-mcp.example.json` into your MCP config and adjust paths:

```json
{
  "mcpServers": {
    "banking-forms": {
      "command": "node",
      "args": ["/absolute/path/to/mcp-server/dist/index.js"],
      "env": {
        "BANKING_API_URL": "http://localhost:8080",
        "BANKING_TENANT_ID": "11111111-1111-1111-1111-111111111111",
        "BANKING_USER_ID": "44444444-4444-4444-4444-444444444444"
      }
    }
  }
}
```

### 4. HTTP mode (Docker)

```bash
export MCP_API_KEY=dev-mcp-key
docker compose --profile mcp up -d mcp-server
# MCP endpoint: http://localhost:3100/mcp  (Bearer dev-mcp-key)
```

---

## Agent Workflow Example

1. User: *"I want a personal loan for $25,000, my name is Jane Doe"*
2. Agent calls `suggest_forms` → `LOAN_APPLICATION`
3. Agent calls `fill_from_conversation` → draft created with pre-filled sections
4. Agent presents prefill to user → user confirms
5. Agent calls `submit_submission` with `confirmed: true`

---

## Security

| Control | Implementation |
|---------|----------------|
| HTTP auth | `MCP_API_KEY` Bearer token (required in HTTP mode when set) |
| Tenant isolation | `BANKING_TENANT_ID` on every platform API call |
| User scoping | `BANKING_USER_ID` for submission ownership |
| Submit gate | `submit_submission` requires `confirmed=true` |
| PII | Agent sees form data; platform enforces validation + PII scrub on submit |

Production: replace dev headers with OIDC (final phase US-9.1).

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `BANKING_API_URL` | `http://localhost:8080` | Platform base URL |
| `BANKING_TENANT_ID` | demo tenant UUID | `X-Tenant-Id` |
| `BANKING_USER_ID` | dev user UUID | `X-Dev-User-Id` |
| `MCP_API_KEY` | — | HTTP Bearer token |
| `MCP_HTTP_PORT` | `3100` | HTTP listen port |
| `MCP_TRANSPORT` | `stdio` | `stdio` or `http` |

---

## Task Completion Status

### Phase 1: MCP Server Setup ✅
- [✔] Task 1.1 — Docker + npm package provisioned
- [✔] Task 1.2 — `@modelcontextprotocol/sdk` server (stdio + HTTP)
- [✔] Task 1.3 — Bearer auth on HTTP endpoints

### Phase 2: Form Retrieval API ✅
- [✔] Task 2.1 — Agent form schema (`AgentFormDefinition` + flat fields)
- [✔] Task 2.2 — `list_forms`, `get_form_definition` tools
- [✔] Task 2.3 — Structured error responses on all tools

### Phase 3: Form Suggestion ✅
- [✔] Task 3.1 — NLU intent matcher (`intent-matcher.ts`)
- [✔] Task 3.2 — Integration with form catalog + discovery API
- [✔] Task 3.3 — `suggest_forms` tool with ranked recommendations

### Phase 4: Form Filling ✅
- [✔] Task 4.1 — Entity extractor (`entity-extractor.ts`)
- [✔] Task 4.2 — Field mapper (`field-mapper.ts`)
- [✔] Task 4.3 — `fill_from_conversation`, `save_section` tools
- [✔] Task 4.4 — `preview_prefill` + `submit_submission(confirmed=true)` gate

### Phase 5: Testing ✅
- [✔] Task 5.1 — Unit tests (intent, entities, field mapping)
- [✔] Task 5.2 — Documented end-to-end agent workflow
- [ ] Task 5.3 — UAT (manual — run with Cursor MCP config)
