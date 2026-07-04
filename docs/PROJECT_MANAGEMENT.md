# Project Management — Banking Forms Platform

**Audience:** Project Manager, Product Owner, Scrum Master, Delivery Lead
**Purpose:** The delivery view — product vision, epics, user stories with acceptance criteria, sprint plan, milestones, and a **traceability matrix that maps every user story to the technical components** in [`TECHNICAL_GUIDE.md`](TECHNICAL_GUIDE.md).

> Companion documents: [`TECHNICAL_GUIDE.md`](TECHNICAL_GUIDE.md) (component detail — story→component mapping lives here and there), [`SOLUTION_ARCHITECTURE.md`](SOLUTION_ARCHITECTURE.md) (architecture & roadmap).
>
> **Story ID scheme:** `US-<epic>.<n>` (e.g. `US-2.1`). Component IDs (e.g. `M-FORMDEF`, `FE-CONSUMER`) refer to §13 of the Technical Guide.
> **Status legend:** ✅ Done · 🟡 Partial · ⏳ Planned.

---

## 1. Product Vision & Goals

> Enable banks to build, publish, and process customer forms without code — with strong tenant isolation, PII protection, and an auditable review workflow — while giving customers a guided, resumable application experience.

**Goals**
- G1 — No-code form authoring & versioning.
- G2 — Guided discovery + resumable, validated form filling.
- G3 — Automated, fail-safe processing pipeline (validate → PII scrub → downstream).
- G4 — Efficient, auditable back-office review.
- G5 — Production-grade security, integrations, and observability.

---

## 2. Personas

| Persona | Description | Primary needs |
|---------|-------------|---------------|
| **Customer** (Consumer) | Bank customer applying via a form | Find the right form, fill it easily, save & resume, track status |
| **Form Author** (Admin) | Product/ops staff who design forms | Create/version/publish forms without code |
| **Reviewer** (Admin) | Back-office case handler | Review submissions, decide, request info, see history |
| **Platform Engineer** | Runs & extends the platform | Clear modules, tests, observability, safe deploys |
| **Compliance/Risk** | Oversight | PII protection, audit trail, tenant isolation |

---

## 3. Epic Overview

| Epic | Name | Goal | Milestone | Status |
|------|------|------|-----------|--------|
| E1 | Platform Foundation | Monolith, DB, security scaffold, FE scaffold | M1 | ✅ |
| E2 | Form Authoring | Author/version/publish forms | M2 | ✅ (🟡 visual builder) |
| E3 | Form Discovery | Triage → recommend → prefill | M3 | ✅ |
| E4 | Form Filling & Submission | Render, save, validate, submit | M2/M3 | ✅ |
| E5 | Consumer Application Lifecycle | List, resume, track status | M3 | ✅ |
| E6 | Processing Pipeline | Validate → PII scrub → downstream | M4 | ✅ (🟡 real downstream) |
| E7 | Admin Review & Operations | Queue, review, audit, pipeline report | M4 | ✅ |
| E8 | Advanced Integrations | Connectors, eventing, AI, notifications | M5 | ⏳ |
| E9 | Security & Observability Hardening | OIDC, dashboards, testing, analytics | M5/M6 | ⏳ |
| E10 | Form Import (AI-assisted) | Import a form from PDF/CSV/XLS/HTML/URL/image via configurable extractors + human review | M4.5 (Phase 3) | ✅ (🟡 hosted-LLM seam) |

---

## 4. User Stories & Acceptance Criteria

### E1 — Platform Foundation (M1) ✅

**US-1.1 — Modular monolith bootstrap** · ✅ · `APP-CORE`
> As a *platform engineer*, I want a modular Spring Boot backend so that modules stay decoupled and independently evolvable.
- **AC1** App boots on `:8080`, `local` profile, `/actuator/health` returns `UP`.
- **AC2** Modules are separate Gradle projects with no circular dependencies.
- **AC3** Grouped OpenAPI (consumer + admin) available at `/swagger-ui.html`.

**US-1.2 — Multi-tenant identity foundation** · ✅ · `M-IDENTITY`, `APP-CORE`
> As a *platform engineer*, I want tenant/user reference data and tenant-scoped requests so that data is isolated per bank.
- **AC1** All business tables carry `tenant_id`; queries are tenant-scoped.
- **AC2** Requests carry `X-Tenant-Id`; actor via `X-Dev-User-Id` (dev).
- **AC3** Seed tenant + user exist.

**US-1.3 — Database schema & migrations** · ✅ · DB (`M-*` domain), `APP-CORE`
> As a *developer*, I want Flyway-managed schema so that DB changes are versioned and repeatable.
- **AC1** `V1`–`V9` run cleanly on H2 (local) and MySQL.
- **AC2** Core + dynamic-storage + pipeline + seed/sample tables created.

**US-1.4 — Frontend scaffold & design system** · ✅ · `FE-PKG-UI`, `FE-CONSUMER`, `FE-ADMIN`
> As a *developer*, I want two React apps + shared UI so that both portals are consistent.
- **AC1** Consumer (:5173) and Admin (:5174) run; `/api` proxied to backend.
- **AC2** Shared `ui` package provides shell, states, badges, tokens.

**US-1.5 — API standards & error handling** · ✅ · `APP-CORE`
> As a *developer*, I want a consistent error envelope so that clients handle failures uniformly.
- **AC1** Domain exceptions map to 400/404/409 with `{error:{code,message}}`.
- **AC2** Bean Validation errors return 400.

**US-1.6 — Testing baseline** · ✅ · (backend tests, `FE` typecheck/build)
> As a *tech lead*, I want automated tests so that regressions are caught.
- **AC1** JUnit/Mockito unit tests + `MockMvc` controller tests pass.
- **AC2** Frontend `typecheck` + `build` pass.

### E2 — Form Authoring (M2) ✅

**US-2.1 — Create a form** · ✅ · `M-FORMDEF`, `BFF-ADMIN`, `FE-ADMIN`
> As a *form author*, I want to create a new form (unique code) so that I can start authoring.
- **AC1** `POST /api/admin/v1/forms` creates a definition + empty `DRAFT` v1.
- **AC2** Duplicate code per tenant → 409.
- **AC3** New form appears in the admin list.

**US-2.2 — Edit draft schema** · ✅ · `M-FORMDEF`, `FE-ADMIN`
> As a *form author*, I want to edit a draft's JSON schema so that I can define sections/fields.
- **AC1** `PUT .../versions/{id}` saves only when status is `DRAFT`.
- **AC2** Invalid schema (missing sections/fields, dup keys, dotted keys) → 400.

**US-2.3 — Version a form** · ✅ · `M-FORMDEF`
> As a *form author*, I want to create a new draft version so that I can change a published form safely.
- **AC1** `POST .../versions` creates a new `DRAFT` (optionally cloning schema).
- **AC2** Existing published version stays live until publish.

**US-2.4 — Publish a version** · ✅ · `M-FORMDEF`, `FE-ADMIN`
> As a *form author*, I want to publish a draft so that customers can use it.
- **AC1** `POST .../publish` sets `PUBLISHED` + `publishedAt`; prior published → `DEPRECATED`.
- **AC2** Published form appears in consumer catalog.

**US-2.5 — Visual drag-and-drop builder** · ⏳ · `FE-PKG-BUILDER`, `FE-ADMIN`
> As a *form author*, I want a visual builder so that I don't edit raw JSON.
- **AC1** Add/reorder/configure sections & fields via UI.
- **AC2** Produces the same schema contract as the JSON editor.
- *(Currently a placeholder; JSON editor is the working authoring path.)*

### E3 — Form Discovery (M3) ✅

**US-3.1 — Triage questionnaire** · ✅ · `M-DISCOVERY`, `FE-CONSUMER`
> As a *customer*, I want a guided questionnaire so that I find the right form.
- **AC1** `GET /discovery/{code}` returns questions; wizard renders them.

**US-3.2 — Form recommendation** · ✅ · `M-DISCOVERY`
> As a *customer*, I want ranked recommendations based on my answers.
- **AC1** `POST /discovery/{code}/evaluate` returns scored/ranked forms per triage rules.

**US-3.3 — Prefill from discovery** · ✅ · `M-DISCOVERY`, `M-SUBMISSION`
> As a *customer*, I want my answers pre-filled into the chosen form so that I re-enter less.
- **AC1** Field mappings seed the draft; prefill skips required-field validation.

### E4 — Form Filling & Submission (M2/M3) ✅

**US-4.1 — Start a draft** · ✅ · `M-SUBMISSION`, `BFF-CONSUMER`
> As a *customer*, I want to start an application so that I can fill it over time.
- **AC1** `POST /submissions` creates a `DRAFT` for the chosen published form.

**US-4.2 — Dynamic form rendering** · ✅ · `FE-PKG-RENDERER`, `FE-CONSUMER`
> As a *customer*, I want the form rendered from its schema so that fields/validation match the definition.
- **AC1** text/number/select controls render with labels + inline errors.

**US-4.3 — Section-wise save (dual storage)** · ✅ · `M-SUBMISSION`
> As a *customer*, I want to save a section so that progress persists.
- **AC1** `PUT .../sections/{key}` persists via the form's storage strategy (JSON_BLOB or KEY_VALUE) transparently.
- **AC2** Saves are **partial** — an incomplete section can be saved (draft), so long multi-section forms can be left and resumed.

**US-4.4 — Server-side validation** · ✅ · `M-SUBMISSION`
> As a *compliance owner*, I want server validation so that invalid data can't be saved/submitted.
- **AC1** Per-section draft save accepts partial data (structure only, no required-field gate); **submit** validates all sections (incl. missing) → 400 with field errors, and the wizard jumps to the first incomplete section.

**US-4.5 — Submit application** · ✅ · `M-SUBMISSION`, `M-PIPELINE`, `BFF-CONSUMER`
> As a *customer*, I want to submit so that the bank processes my application.
- **AC1** `POST .../submit` validates all, stamps idempotency key + `submitted_at`, returns `202` with status.
- **AC2** Re-submit with same idempotency key is safe.

**US-4.6 — Embedded / reusable forms** · ✅ · `M-FORMDEF` (`FormSchemaComposer`), `FE-PKG-RENDERER`, `M-SUBMISSION`
> As a *form author*, I want to embed a building-block form (e.g. Address) so that I reuse definitions.
- **AC1** `embedded_form` fields are inlined on read (cycle-safe, depth-limited).
- **AC2** Renderer recurses; KEY_VALUE flattens nested leaves to dotted keys.

### E5 — Consumer Application Lifecycle (M3) ✅

**US-5.1 — My applications list** · ✅ · `M-SUBMISSION`, `BFF-CONSUMER`, `FE-CONSUMER`
> As a *customer*, I want to see my applications so that I can manage them.
- **AC1** `GET /submissions` returns only the current user's applications with status.

**US-5.2 — Resume a draft** · ✅ · `M-SUBMISSION`, `FE-CONSUMER`
> As a *customer*, I want to resume a draft so that I continue where I left off.
- **AC1** Opening `?submission={id}` loads server-stored draft data (server state preferred over local).
- **AC2** Resume restores the last section position (`current_section_key`, migration `V9`), landing the user on the page they left off — including partially-filled sections.

**US-5.3 — Track application status** · ✅ · `FE-CONSUMER`
> As a *customer*, I want a persistent status page so that I know where my application stands.
- **AC1** Status page shows current status + read-only summary and survives reload.

### E6 — Processing Pipeline (M4) ✅

**US-6.1 — Automated validation step** · ✅ · `M-PIPELINE`, `M-SUBMISSION`
> As the *platform*, I want to re-validate on submit so that only valid data proceeds.
- **AC1** VALIDATE step advances `SUBMITTED→VALIDATING`, records `VALIDATED` event.

**US-6.2 — PII scrubbing step** · ✅ · `M-PIPELINE`, `M-TRANSFORM`
> As *compliance*, I want PII scrubbed before downstream so that sensitive data is protected.
- **AC1** PII_SCRUB writes a sanitized payload to `submission_sanitized_payload`; advances to `PROCESSING`.

**US-6.3 — Downstream dispatch & fail-safe** · ✅ (🟡 real connector) · `M-PIPELINE`
> As the *platform*, I want downstream dispatch that never fails the customer's submit.
- **AC1** Success → `PENDING_REVIEW`, `PIPELINE_COMPLETED`.
- **AC2** Failure → submission reverts to `SUBMITTED`, execution `FAILED` with error details, `PIPELINE_FAILED` event.
- *(Real connectors = US-8.1.)*

### E7 — Admin Review & Operations (M4) ✅

**US-7.1 — Audit timeline** · ✅ · `M-SUBMISSION` (`submission_event`)
> As a *reviewer*, I want a full audit trail so that every change is traceable.
- **AC1** All transitions append events (`from`,`to`,`note`,`actor`); shown as a timeline.

**US-7.2 — Review queue & detail** · ✅ · `M-PROCESSING`, `BFF-ADMIN`, `FE-ADMIN`
> As a *reviewer*, I want a queue and detail view so that I can process submissions.
- **AC1** `GET /submissions` (admin) + detail shows sections + timeline.

**US-7.3 — Review decisions** · ✅ · `M-PROCESSING`
> As a *reviewer*, I want to start review and approve/reject/request-info.
- **AC1** Valid transitions only (`SUBMITTED|NEEDS_INFO→PENDING_REVIEW→APPROVED|REJECTED|NEEDS_INFO`); illegal → 409.
- **AC2** Each decision writes an audit event.

**US-7.4 — Pipeline report** · ✅ · `M-PIPELINE`, `BFF-ADMIN`, `FE-ADMIN`
> As a *reviewer/ops*, I want to see pipeline execution + sanitized payload.
- **AC1** `GET /submissions/{id}/pipeline` returns execution status, current step, transformed fields.

### E8 — Advanced Integrations (M5) ⏳

- **US-8.1 — Downstream connectors** ⏳ · `module-downstream` — real Kafka/S3/REST/core-banking delivery + `outbox_event` reliability.
- **US-8.2 — Event-driven pipeline** ⏳ · `M-PIPELINE` — outbox → message broker → async step workers.
- **US-8.3 — AI evaluation step** ⏳ · `module-service-integration`, `M-PIPELINE` — `AI_EVALUATE` via adapter + guardrails.
- **US-8.4 — Service-integration adapters** ⏳ · `module-service-integration` — external API adapter registry.
- **US-8.5 — Notifications** ⏳ · `module-notification` — event-triggered customer/staff notifications.

### E10 — Form Import (AI-assisted) (Phase 3) ✅

**US-10.1 — Import a form from a document** · ✅ · `M-FORMIMPORT`, `BFF-ADMIN`, `FE-ADMIN`
> As a *form author*, I want to upload a PDF/CSV/XLS/HTML file (or point at a URL) so that a draft form is generated for me instead of building it by hand.
- **AC1** `POST /form-imports` (file) / `POST /form-imports/from-url` (URL) creates a job; the source type is auto-detected (filename/MIME/URL).
- **AC2** The right extractor runs (PDFBox/POI/jsoup) and the job reaches `NEEDS_REVIEW` with a mapped schema + confidence signal.
- **AC3** Duplicate sources are de-duplicated via a source hash.

**US-10.2 — Configurable extraction providers** · ✅ · `M-FORMIMPORT`, `BFF-ADMIN`, `FE-ADMIN`
> As a *platform admin*, I want to manage which extractor handles each source type so that I can tune/extend extraction without a code change.
- **AC1** `GET/PUT /form-import-providers` lists providers and updates `enabled`, `priority`, and `config` JSON (Settings page).
- **AC2** For a source type, the highest-priority **enabled** provider **with an available implementation** is chosen; a provider can be seeded but disabled/unavailable.

**US-10.3 — Import from an image (AI/vision)** · ✅ (🟡 hosted-LLM seam) · `M-SVCINT`, `M-FORMIMPORT`
> As a *form author*, I want to import a form from a scanned image so that even non-digital forms can be onboarded.
- **AC1** `ollama-vision` provider sends the (downscaled) image to a local Ollama vision model (`llava`) and returns fields for review.
- **AC2** Provider is disabled by default and enabled from Settings once Ollama is running; endpoint/model/prompt/timeout are configurable.
- *(Hosted-LLM `llm-vision` seam exists but is disabled pending a real provider.)*

**US-10.4 — Review & accept an import (human-in-the-loop)** · ✅ · `M-FORMIMPORT`, `M-FORMDEF`, `FE-ADMIN`
> As a *form author*, I want to review/edit the extracted schema before it becomes a form so that AI output is never trusted blindly.
- **AC1** Job lands in `NEEDS_REVIEW` (or `FAILED`); `POST /form-imports/{id}/accept` creates a `DRAFT` form (via `M-FORMDEF`) with the reviewed schema.
- **AC2** Nothing is published automatically — accept only produces a draft for normal authoring/publish.

### E9 — Security & Observability Hardening (M5/M6) ⏳

- **US-9.1 — OIDC authentication & RBAC** ⏳ · `APP-CORE` (`SecurityConfig`), `M-IDENTITY` — replace dev headers with JWT/OIDC + role enforcement.
- **US-9.2 — Observability** 🟡 · `M-OBSERV` — metrics/tracing/structured logs, Grafana dashboards, alerts.
- **US-9.3 — Load & security testing** ⏳ — performance baselines, pen-test, dependency scanning.
- **US-9.4 — Analytics export** ⏳ · `module-analytics` — export from sanitized payloads.

---

## 5. Story → Component Traceability Matrix

Maps each user story to the implementing technical component(s) (see [`TECHNICAL_GUIDE.md` §13](TECHNICAL_GUIDE.md)) and delivery status/sprint.

| Story | Title | Component(s) | Status | Sprint |
|-------|-------|--------------|--------|--------|
| US-1.1 | Monolith bootstrap | APP-CORE | ✅ | S1 |
| US-1.2 | Multi-tenant identity | M-IDENTITY, APP-CORE | ✅ | S1 |
| US-1.3 | Schema & migrations | DB / M-* | ✅ | S1 |
| US-1.4 | FE scaffold & design system | FE-PKG-UI, FE-CONSUMER, FE-ADMIN | ✅ | S1 |
| US-1.5 | API standards & errors | APP-CORE | ✅ | S1 |
| US-1.6 | Testing baseline | backend tests, FE build | ✅ | S1 |
| US-2.1 | Create form | M-FORMDEF, BFF-ADMIN, FE-ADMIN | ✅ | S2 |
| US-2.2 | Edit draft schema | M-FORMDEF, FE-ADMIN | ✅ | S2 |
| US-2.3 | Version a form | M-FORMDEF | ✅ | S2 |
| US-2.4 | Publish version | M-FORMDEF, FE-ADMIN | ✅ | S2 |
| US-2.5 | Visual builder | FE-PKG-BUILDER, FE-ADMIN | ⏳ | S8+ |
| US-3.1 | Triage questionnaire | M-DISCOVERY, FE-CONSUMER | ✅ | S3 |
| US-3.2 | Recommendation | M-DISCOVERY | ✅ | S3 |
| US-3.3 | Prefill | M-DISCOVERY, M-SUBMISSION | ✅ | S3 |
| US-4.1 | Start draft | M-SUBMISSION, BFF-CONSUMER | ✅ | S3 |
| US-4.2 | Dynamic rendering | FE-PKG-RENDERER, FE-CONSUMER | ✅ | S3 |
| US-4.3 | Section save (dual storage) | M-SUBMISSION | ✅ | S3 |
| US-4.4 | Server validation | M-SUBMISSION | ✅ | S3 |
| US-4.5 | Submit | M-SUBMISSION, M-PIPELINE, BFF-CONSUMER | ✅ | S3 |
| US-4.6 | Embedded forms | M-FORMDEF, FE-PKG-RENDERER, M-SUBMISSION | ✅ | S3 |
| US-5.1 | My applications | M-SUBMISSION, BFF-CONSUMER, FE-CONSUMER | ✅ | S4 |
| US-5.2 | Resume draft | M-SUBMISSION, FE-CONSUMER | ✅ | S4 |
| US-5.3 | Track status | FE-CONSUMER | ✅ | S4 |
| US-6.1 | Validate step | M-PIPELINE, M-SUBMISSION | ✅ | S5 |
| US-6.2 | PII scrub step | M-PIPELINE, M-TRANSFORM | ✅ | S5 |
| US-6.3 | Downstream + fail-safe | M-PIPELINE | ✅ / 🟡 | S5 |
| US-7.1 | Audit timeline | M-SUBMISSION | ✅ | S6 |
| US-7.2 | Review queue & detail | M-PROCESSING, BFF-ADMIN, FE-ADMIN | ✅ | S6 |
| US-7.3 | Review decisions | M-PROCESSING | ✅ | S6 |
| US-7.4 | Pipeline report | M-PIPELINE, BFF-ADMIN, FE-ADMIN | ✅ | S6 |
| US-8.1 | Downstream connectors | module-downstream | ⏳ | S7 |
| US-8.2 | Event-driven pipeline | M-PIPELINE (outbox→broker) | ⏳ | S7 |
| US-8.3 | AI evaluation | module-service-integration, M-PIPELINE | ⏳ | S8 |
| US-8.4 | Service adapters | module-service-integration | ⏳ | S8 |
| US-8.5 | Notifications | module-notification | ⏳ | S8 |
| US-9.1 | OIDC auth & RBAC | APP-CORE, M-IDENTITY | ⏳ | S7 |
| US-9.2 | Observability | M-OBSERV | 🟡 | S8 |
| US-9.3 | Load & security testing | (cross-cutting) | ⏳ | S8 |
| US-9.4 | Analytics export | module-analytics | ⏳ | S8 |
| US-10.1 | Import form from document | M-FORMIMPORT, BFF-ADMIN, FE-ADMIN | ✅ | S6.5 |
| US-10.2 | Configurable extraction providers | M-FORMIMPORT, BFF-ADMIN, FE-ADMIN | ✅ | S6.5 |
| US-10.3 | Import from image (AI/vision) | M-SVCINT, M-FORMIMPORT | ✅ / 🟡 | S6.5 |
| US-10.4 | Review & accept import | M-FORMIMPORT, M-FORMDEF, FE-ADMIN | ✅ | S6.5 |

---

## 6. Sprint Plan (2-week sprints)

| Sprint | Goal | Stories | Milestone | Status |
|--------|------|---------|-----------|--------|
| **S1** | Foundation & scaffolding | US-1.1 … US-1.6 | M1 | ✅ Done |
| **S2** | Form authoring backend + admin JSON builder | US-2.1 … US-2.4 | M2 | ✅ Done |
| **S3** | Discovery + dynamic filling + submission + embedded | US-3.1–3.3, US-4.1–4.6 | M2/M3 | ✅ Done |
| **S4** | Consumer application lifecycle | US-5.1 … US-5.3 | M3 | ✅ Done |
| **S5** | Automated processing pipeline + PII | US-6.1 … US-6.3 | M4 | ✅ Done |
| **S6** | Admin review workspace + pipeline report | US-7.1 … US-7.4 | M4 | ✅ Done |
| **S6.5** | Form import (multi-source, configurable providers, Ollama vision) — Phase 3 | US-10.1 … US-10.4 | M4.5 | ✅ Done |
| **S7** | OIDC auth + real downstream + eventing | US-9.1, US-8.1, US-8.2 | M5 | ⏳ Planned |
| **S8** | AI, notifications, analytics, observability, hardening + visual builder | US-8.3–8.5, US-9.2–9.4, US-2.5 | M5/M6 | ⏳ Planned |

---

## 7. Milestones / Release Plan

| Milestone | Theme | Included epics | Exit criteria | Status |
|-----------|-------|----------------|---------------|--------|
| **M1** | Foundation | E1 | App boots, schema migrates, portals scaffolded, tests green | ✅ |
| **M2** | Authoring & Filling | E2, E4 | Author→publish→fill→submit works end-to-end | ✅ |
| **M3** | Consumer Experience | E3, E5 | Discovery + resumable drafts + status tracking | ✅ |
| **M4** | Processing & Review | E6, E7 | Automated pipeline + auditable review workflow | ✅ |
| **M4.5** | Form Import (Phase 3) | E10 | Import a form from PDF/CSV/XLS/HTML/URL/image via configurable providers + human review | ✅ |
| **M5** | Integrations & Security | E8 (part), E9 (part) | OIDC live, real downstream connector, async pipeline | ⏳ |
| **M6** | Observability & Hardening | E9 | Dashboards, alerting, load/security tested, analytics | ⏳ |

**Current state:** M1–M4 complete (MVP feature-complete for the core lifecycle); M4.5 (Form Import, Phase 3) complete. M5–M6 planned.

---

## 8. Definition of Ready / Done

**Definition of Ready (story):** clear user value; acceptance criteria written; dependencies known; component(s) identified; estimable; test approach agreed.

**Definition of Done (story):** code merged; unit/integration tests pass; frontend typecheck + build pass; acceptance criteria demonstrably met; API documented (OpenAPI); traceability matrix updated; no known P1 defects.

---

## 9. Risks, Assumptions & Dependencies

| # | Risk / Dependency | Impact | Response |
|---|-------------------|--------|----------|
| R1 | Auth still dev-headers | Blocks production | US-9.1 (OIDC) before any real deployment |
| R2 | Synchronous pipeline | Latency/throughput ceiling | US-8.2 async migration (outbox present) |
| R3 | Visual builder is a stub | Author friction | US-2.5; JSON editor mitigates now |
| R4 | Downstream/AI/notifications placeholders | No external effects | US-8.1/8.3/8.5 behind existing interfaces |
| A1 | Single shared DB acceptable at current scale | — | Read replicas; revisit DB-per-tenant later |
| D1 | OIDC IdP availability | Gates US-9.1 | Coordinate with security/infra |

---

## 10. Delivery Metrics / KPIs

- **Product:** form publish lead time, draft→submit conversion, time-to-decision (submit→approved/rejected), % submissions auto-progressed without pipeline failure.
- **Delivery:** sprint velocity, story cycle time, escaped defects, % stories with updated traceability.
- **Quality/Ops:** test pass rate, pipeline failure rate, p95 submit latency, audit completeness (100% transitions logged).
