# MCP Server Integration — Banking Forms Platform

**Branch:** `phase5-mcp`  
**Purpose:** Enable LLM agents (Cursor, Claude Desktop, custom bots) to suggest and fill banking forms programmatically via the [Model Context Protocol (MCP)](https://modelcontextprotocol.io), without users navigating the consumer portal.

> **UAT & run-all-servers:** [`MCP_TECHNICAL_GUIDE.md`](MCP_TECHNICAL_GUIDE.md)  
> **Platform overview:** [`TECHNICAL_GUIDE.md`](TECHNICAL_GUIDE.md) §5.14

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

## Quick start

```bash
# Full stack (backend + frontends + docker + optional MCP HTTP)
./scripts/start-all-dev.sh --mcp

# MCP only needs backend + stdio config in Cursor:
./gradlew bootRun
cd mcp-server && npm install && npm run build
# → project MCP config: .cursor/mcp.json (see MCP_TECHNICAL_GUIDE.md §4)
```

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

## Agent workflow example

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

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `BANKING_API_URL` | `http://localhost:8080` | Platform base URL |
| `BANKING_TENANT_ID` | demo tenant UUID | `X-Tenant-Id` |
| `BANKING_USER_ID` | dev user UUID | `X-Dev-User-Id` |
| `MCP_API_KEY` | — | HTTP Bearer token |
| `MCP_HTTP_PORT` | `3100` | HTTP listen port |
| `MCP_TRANSPORT` | `stdio` | `stdio` or `http` |

---

## Task completion status

| Phase | Status |
|-------|--------|
| 1 — MCP server setup | ✅ |
| 2 — Form retrieval | ✅ |
| 3 — Form suggestion | ✅ |
| 4 — Form filling + confirmation | ✅ |
| 5 — Unit tests + UAT guide | ✅ ([`MCP_TECHNICAL_GUIDE.md`](MCP_TECHNICAL_GUIDE.md)) |
