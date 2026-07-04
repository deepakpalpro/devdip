# Technical Guide — Banking Forms Platform

**Audience:** Technical Lead, Backend/Frontend Developers
**Purpose:** A component-by-component walkthrough of the entire application — what each module/package does, its key classes, data model, invariants, and how the pieces fit together. Use this as the onboarding and implementation reference.

> Companion documents:
> - [`SOLUTION_ARCHITECTURE.md`](SOLUTION_ARCHITECTURE.md) — architecture views, decisions, NFRs (for Solution Architects).
> - [`PROJECT_MANAGEMENT.md`](PROJECT_MANAGEMENT.md) — epics, user stories, sprints (for PMs). Story IDs (e.g. `US-2.1`) referenced here map back to that document.
> - [`ARCHITECTURE.md`](ARCHITECTURE.md) — original full design/source of truth.

Each component below is tagged with a **Component ID** (e.g. `M-FORMDEF`) so user stories and the traceability matrix can reference it.

---

## 1. Quick Start (Developer)

```bash
# Backend (in-memory H2, MySQL-compat, Flyway migrations, local profile)
./gradlew bootRun                        # http://localhost:8080
# Health: GET /actuator/health   |   API docs: /swagger-ui.html

# Frontend (npm workspaces monorepo)
cd frontend && npm install
npm run dev:consumer                     # http://localhost:5173
npm run dev:admin                        # http://localhost:5174
```

- Dev tenant id (seeded): `11111111-1111-1111-1111-111111111111` (sent as `X-Tenant-Id`).
- Dev user id (default): `44444444-4444-4444-4444-444444444444` (optional `X-Dev-User-Id`).
- MySQL profile: `SPRING_PROFILES_ACTIVE=mysql DB_HOST=localhost DB_NAME=banking_forms ./gradlew bootRun`.

---

## 2. Technology Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.x, Spring Web, Spring Data JPA/Hibernate, Bean Validation |
| Migrations | Flyway (`V1`–`V9`) |
| Database | MySQL 8.x (prod), H2 in MySQL mode (local/tests) |
| API docs | springdoc-openapi (grouped: consumer + admin) |
| Frontend | React 18, TypeScript, Vite 6, React Router, TanStack Query |
| Frontend monorepo | npm workspaces (`apps/*`, `packages/*`) |
| Build | Gradle (multi-module) |

---

## 3. Repository & Module Layout

```
banking-forms-platform/
├── app/                        # APP-CORE   — bootstrap, security, config, error handling
├── bff-consumer/               # BFF-CONSUMER — consumer REST API (/api/consumer/v1)
├── bff-admin/                  # BFF-ADMIN    — admin REST API (/api/admin/v1)
├── module-identity/            # M-IDENTITY   — tenants, users, roles
├── module-form-definition/     # M-FORMDEF    — form templates, versions, schema composition
├── module-submission/          # M-SUBMISSION — drafts, submissions, section storage, audit
├── module-discovery/           # M-DISCOVERY  — triage questionnaire + recommendations + prefill
├── module-pipeline/            # M-PIPELINE   — automated processing orchestration
├── module-transformation/      # M-TRANSFORM  — PII scrubbing
├── module-processing/          # M-PROCESSING — manual review state machine
├── module-observability/       # M-OBSERV     — metrics/tracing config
├── module-notification/        # (placeholder — Phase 3/4)
├── module-downstream/          # (placeholder — Phase 4)
├── module-service-integration/ # (placeholder — Phase 4)
├── module-analytics/           # (placeholder — Phase 5)
├── docs/                       # documentation
└── frontend/                   # FE — React monorepo (apps + packages)
```

**Layering per module (DDD-ish):**
- `domain/` — JPA entities + enums, business invariants (no Spring web concerns).
- `application/` — services, use-case orchestration, DTO/view records, validators, exceptions.
- `infrastructure/` — Spring Data repositories.

**Dependency rule:** BFFs depend on module `application` APIs; modules never depend on BFFs; no circular module dependencies. Cross-module reads go through a module's public service (e.g. submission/pipeline call `FormQueryService`).

---

## 4. Cross-Cutting Concerns (`APP-CORE`)

Module: `app/` — package `com.banking.forms.app` + `com.banking.forms.shared`.

| Class | Responsibility |
|-------|----------------|
| `BankingFormsApplication` | Spring Boot entrypoint; component-scans all modules. |
| `config/SecurityConfig` | Non-local security: OAuth2 resource server (JWT), role-based access (planned/active for non-local profiles). |
| `config/LocalSecurityConfig` | `local` profile: permits `/api/**` for frictionless dev. |
| `config/CorsConfig` | CORS for the Vite dev servers. |
| `config/OpenApiConfig` | springdoc setup + two grouped specs: `consumer` (`/api/consumer/v1/**`) and `admin` (`/api/admin/v1/**`). |
| `config/GlobalExceptionHandler` | `@RestControllerAdvice` mapping domain exceptions → HTTP status + `ErrorResponse`. |
| `shared/api/ApiError`, `ErrorResponse` | Standard error envelope `{ error: { code, message } }`. |
| `shared/Uuids` | UUID helpers. |

**Exception → HTTP mapping (representative):**

| Exception | HTTP |
|-----------|------|
| `FormNotFoundException`, `SubmissionNotFoundException`, `DiscoveryNotFoundException` | 404 |
| `FormConflictException` (dup code / illegal state transition) | 409 |
| `FormSchemaException`, `SubmissionValidationException` | 400 |
| `ReviewException` (illegal review transition) | 409 |

**Multi-tenancy:** every request carries `X-Tenant-Id`; repositories and services are tenant-scoped (`findBy...TenantId...`). Actor identity is currently the `X-Dev-User-Id` header (`DevRequestContext`/`AdminRequestContext`) pending OIDC.

*Implements:* `US-1.1`, `US-1.2`, `US-1.5`.

---

## 5. Backend Modules

### 5.1 `M-IDENTITY` — Identity (`module-identity`)
Owns the tenant/user reference data used for tenant scoping and audit actor references.

| Kind | Classes |
|------|---------|
| Domain | `Tenant`, `AppUser`, `UserRole`, `UserStatus` |
| Infra | `TenantRepository`, `AppUserRepository` |

- Seeded in `V2` (tenant `demo-bank`, user `dev@demo-bank.local`).
- OIDC-based provisioning is future work; today identities are seeded/dev-headers.

*Implements:* `US-1.2`. *(OIDC login → `US-9.x`, planned.)*

---

### 5.2 `M-FORMDEF` — Form Definition (`module-form-definition`)
The authoring domain: form templates, immutable-per-status versions, JSON schema, and composition of embedded forms.

| Kind | Classes |
|------|---------|
| Domain | `FormDefinition`, `FormVersion`, `FormVersionStatus` (`DRAFT`/`PUBLISHED`/`DEPRECATED`), `StorageStrategy` (`JSON_BLOB`/`KEY_VALUE`) |
| Application | `FormCommandService` (author/version/publish), `FormQueryService` (read published), `FormSchemaComposer` (inline embedded forms), views: `FormDetailView`, `FormVersionView`, `FormAdminSummaryView`, `PublishedFormView`; exceptions: `FormNotFoundException`, `FormConflictException`, `FormSchemaException` |
| Infra | `FormDefinitionRepository`, `FormVersionRepository` |

**Key behaviours & invariants**
- `FormVersion` lifecycle methods enforce transitions: `updateSchema()` only on `DRAFT`; `publish()` `DRAFT→PUBLISHED` (stamps `publishedAt`); `deprecate()` `PUBLISHED→DEPRECATED`.
- `FormCommandService.createDefinition` creates the definition + an empty `DRAFT` v1; unique `code` per tenant (else `FormConflictException`).
- `createVersion` opens a new `DRAFT` (optionally cloning a prior version's schema).
- `updateDraftSchema` validates schema shape (sections/fields present, unique field keys, no dots in keys) → `FormSchemaException` on violation.
- `publish` auto-deprecates the previously published version for that definition.
- `FormSchemaComposer` inlines referenced published forms into `embedded_form` fields (recursive, cycle detection, `MAX_DEPTH=5`, sets `embeddedUnavailable` when unresolved).

*Implements:* `US-2.1`, `US-2.2`, `US-2.3`, `US-2.4`, `US-4.6` (embedded composition).

---

### 5.3 `M-SUBMISSION` — Submission (`module-submission`)
Owns drafts, submitted applications, per-section value storage (dual strategy), validation, and the audit timeline.

| Kind | Classes |
|------|---------|
| Domain | `Submission`, `SubmissionStatus` (8 states), `SubmissionSection`, `SubmissionFieldValue`, `SubmissionEvent` |
| Application | `SubmissionService`, `SectionValidator`, `SectionStorageStrategy` + `SectionStorageRouter`, `JsonBlobSectionStorage`, `KeyValueSectionStorage`, `SubmissionEventRecorder`; views: `SubmissionDetailView`, `SubmissionSummaryView`, `SubmissionEventView`; exceptions: `SubmissionNotFoundException`, `SubmissionValidationException` |
| Infra | `SubmissionRepository`, `SubmissionSectionRepository`, `SubmissionFieldValueRepository`, `SubmissionEventRepository` |

**Submission status model** (`SubmissionStatus`): `DRAFT → SUBMITTED → VALIDATING → PROCESSING → PENDING_REVIEW → {APPROVED | REJECTED | NEEDS_INFO}`. Transition methods (`markSubmitted`, `markValidating`, `markProcessing`, `markUnderReview`, `markApproved`, `markRejected`, `markNeedsInfo`, `revertToSubmitted`) guard the state machine.

**Storage strategies** (chosen per form via `FormDefinition.storageStrategy`):
- `JsonBlobSectionStorage` — one `submission_section` row per section; the whole section stored as JSON in `section_data_json`.
- `KeyValueSectionStorage` — `submission_section` row (JSON null) + one `submission_field_value` row per leaf field; nested/embedded values flattened to dotted keys (`mailingAddress.address.line1`) and unflattened on read. Enables per-field indexing/encryption for regulated forms.
- `SectionStorageRouter.resolve(strategy)` picks the implementation.

**Key services**
- `SubmissionService.createDraft(tenant, user, formCode[, prefill])` — creates `DRAFT`, optionally seeds prefill (from discovery) without required-field validation.
- `saveSection` — **partial draft save**: persists one section via the storage strategy without required-field validation (only checks the section exists) and records the resume position (`currentSectionKey`). Completeness is enforced on `submit`, not here — so long forms can be left incomplete and resumed.
- `submit` — validates **all** sections (incl. missing), stamps idempotency key + `submitted_at`, records `SUBMITTED` event.
- `listSubmissions(tenant)` — admin list (all applicants). `listSubmissions(tenant, user)` — consumer "my applications" list.
- `getSubmission` / `getTimeline` — detail + audit events.

*Implements:* `US-4.1`–`US-4.5`, `US-5.1`, `US-5.2`, `US-7.1`.

---

### 5.4 `M-DISCOVERY` — Discovery / Triage (`module-discovery`)
A questionnaire-driven "help me choose" wizard that recommends forms and pre-populates the chosen one.

| Kind | Classes |
|------|---------|
| Domain | `DiscoveryQuestionnaire` |
| Application | `DiscoveryService`, `RecommendationEngine`, `TriageRule`, `RuleCondition`, `ConditionOperator`, `RankedForm`, `RankedForm`, `FieldMapping`; views: `QuestionnaireView`, `DiscoveryEvaluationView`, `RecommendationView`; `DiscoveryNotFoundException` |
| Infra | `DiscoveryQuestionnaireRepository`, `DiscoverySessionRepository` |

- `RecommendationEngine` scores forms against answers using `TriageRule`/`RuleCondition` (`ConditionOperator`).
- `FieldMapping` maps questionnaire answers → target form section/field for **prefill** (`DiscoveryService.buildPrefill`).
- Seeded questionnaire `BANKING_NEEDS` (`V5`).

*Implements:* `US-3.1`, `US-3.2`, `US-3.3`.

---

### 5.5 `M-PIPELINE` — Processing Pipeline (`module-pipeline`)
Runs the automated post-submit pipeline and exposes a report for admins.

| Kind | Classes |
|------|---------|
| Domain | `PipelineExecution` (status `RUNNING`/`COMPLETED`/`FAILED`, `current_step`), `SanitizedPayload`, `PipelineStepType` (`VALIDATE`, `PII_SCRUB`, `AI_EVALUATE`, `SERVICE_CALL`, `DOWNSTREAM`, `NOTIFY`) |
| Application | `SubmissionPipelineService` (live pipeline), `PipelineOrchestrator`, `PipelineResult`; views: `PipelineReportView`, `PipelineExecutionView`, `TransformedFieldView` |
| Infra | `PipelineExecutionRepository`, `SanitizedPayloadRepository` |

**Live pipeline** (`SubmissionPipelineService.process`, invoked synchronously by the consumer submit endpoint):
1. **VALIDATE** — re-validate sections → `VALIDATED` event; advances `SUBMITTED → VALIDATING`.
2. **PII_SCRUB** — `PiiScrubber` produces a sanitized copy persisted to `submission_sanitized_payload`; advances `VALIDATING → PROCESSING`, `PII_SCRUBBED` event.
3. **DOWNSTREAM** — (placeholder dispatch) → `PIPELINE_COMPLETED`, submission `PROCESSING → PENDING_REVIEW`.
- **Failure handling:** failures are caught, not thrown — submission reverts to `SUBMITTED`, `PipelineExecution.fail()` records `error_details`, and a `PIPELINE_FAILED` event is written. Submit never fails because of the pipeline.

*Implements:* `US-6.1`, `US-6.2`, `US-6.3`, `US-7.4`.

---

### 5.6 `M-TRANSFORM` — PII Scrubbing (`module-transformation`)
| Kind | Classes |
|------|---------|
| Application | `PiiScrubber`, `PiiFieldRegistry` + `DefaultPiiFieldRegistry`, `ScrubResult` |
| Domain | `PiiStrategy` |

- `PiiFieldRegistry` declares which fields are sensitive and how to transform (`PiiStrategy`, e.g. mask/redact/tokenize). `PiiScrubber` applies them, producing a `ScrubResult` (sanitized payload + transformed field list). Wired into the pipeline's PII_SCRUB step.

*Implements:* `US-6.2`.

---

### 5.7 `M-PROCESSING` — Manual Review (`module-processing`)
| Kind | Classes |
|------|---------|
| Application | `ReviewService`, `ReviewWorkflow`, `ReviewAction` (`START_REVIEW`, `APPROVE`, `REJECT`, `REQUEST_INFO`), `ReviewException` |

- `ReviewWorkflow` is the review state machine: `SUBMITTED|NEEDS_INFO → PENDING_REVIEW` (START_REVIEW); `PENDING_REVIEW → APPROVED|REJECTED|NEEDS_INFO`. Illegal transitions → `ReviewException` (409).
- `ReviewService.decide()` persists the new status and appends an audit event (`REVIEW_STARTED`/`APPROVED`/`REJECTED`/`INFO_REQUESTED`). Reviews reuse the `submission_event` timeline (no separate table).

*Implements:* `US-7.2`, `US-7.3`.

---

### 5.8 `M-OBSERV` — Observability (`module-observability`)
- `ObservabilityConfig` — Micrometer/metrics wiring seed. Dashboards, tracing, alerting are Phase 5. *Implements:* `US-9.2` (partial).

### 5.9 Placeholder modules
`module-notification`, `module-downstream`, `module-service-integration`, `module-analytics` are scaffolded (build files only) for Phase 4/5 features (notifications, Kafka/S3/REST connectors, AI/service adapters, analytics export). *Maps to:* `US-8.x`, `US-9.x` (planned).

---

## 6. Backend-for-Frontend (BFF) APIs

### 6.1 `BFF-CONSUMER` (`/api/consumer/v1`)
| Controller | Endpoints |
|------------|-----------|
| `ConsumerFormsController` | `GET /forms` (published catalog) |
| `ConsumerFormDetailController` | `GET /forms/{formCode}` (composed schema) |
| `ConsumerDiscoveryController` | `GET /discovery/{code}`, `POST /discovery/{code}/evaluate` |
| `ConsumerSubmissionsController` | `GET /submissions` (my apps), `POST /submissions` (create draft), `GET /submissions/{id}`, `PUT /submissions/{id}/sections/{sectionKey}` (save section), `POST /submissions/{id}/submit` |
| `DevRequestContext` | resolves `X-Dev-User-Id` (default dev user) |

Submit runs the pipeline synchronously and returns `202 Accepted` with the resulting status.

*Implements:* `US-3.x`, `US-4.x`, `US-5.x`.

### 6.2 `BFF-ADMIN` (`/api/admin/v1`)
| Controller | Endpoints |
|------------|-----------|
| `AdminFormsController` | `GET /forms`, `GET /forms/{id}`, `POST /forms`, `POST /forms/{id}/versions`, `PUT /forms/{id}/versions/{versionId}`, `POST /forms/{id}/versions/{versionId}/publish` |
| `AdminSubmissionsController` | `GET /submissions`, `GET /submissions/{id}` (detail + timeline) |
| `AdminReviewController` | `POST /submissions/{id}/review/{start|approve|reject|request-info}` |
| `AdminPipelineController` | `GET /submissions/{id}/pipeline` (execution + sanitized payload) |
| `AdminRequestContext` | resolves admin actor id |

*Implements:* `US-2.x`, `US-7.x`.

---

## 7. Database Schema & Migrations

Flyway migrations in `app/src/main/resources/db/migration/`:

| Version | Purpose |
|---------|---------|
| `V1` | Core schema (tenant, app_user, form_definition, form_version, submission, submission_section_data, submission_event, pipeline_config, pipeline_execution, outbox_event) |
| `V2` | Dev seed (tenant, user, LOAN_APPLICATION + ACCOUNT_OPENING forms) |
| `V3` | Dynamic section storage: `storage_strategy` column, `submission_section` + `submission_field_value` tables |
| `V4` | Embedded form seed (`ADDRESS_DETAILS` building block embedded in loan/account) |
| `V5` | Discovery questionnaire seed (`BANKING_NEEDS`) |
| `V6` | `submission_sanitized_payload` (pipeline output) |
| `V7` | Sample submissions across all 8 statuses |
| `V8` | More sample submissions + a FAILED-pipeline example |
| `V9` | Draft resume progress: `current_section_key` on `submission` |

**Central tables:** `form_definition` → `form_version` (1:N, unique per version number). `submission` → `submission_section` → `submission_field_value`. `submission` → `submission_event` (append-only audit). `submission` → `pipeline_execution` + `submission_sanitized_payload`. IDs are `BINARY(16)` UUIDs. Detailed column-level design lives in [`ARCHITECTURE.md` §5](ARCHITECTURE.md).

*Implements:* `US-1.3`.

---

## 8. Key Cross-Cutting Design Topics

### 8.1 Dynamic section storage (`JSON_BLOB` vs `KEY_VALUE`)
Per-form strategy. JSON_BLOB is simple/fast for most forms; KEY_VALUE normalizes each field into a row for per-field indexing, querying, and column-level encryption on regulated forms. Both implement `SectionStorageStrategy` and are transparent to callers via `SectionStorageRouter`. *(→ `US-4.3`.)*

### 8.2 Embedded / nested forms
A field of type `embedded_form` references another published form by `formCode`. `FormSchemaComposer` inlines the referenced schema at read time; the renderer and validators recurse; KEY_VALUE storage flattens nested leaves to dotted keys. *(→ `US-4.6`.)*

### 8.3 Audit timeline
`submission_event` is an append-only log (`event_type`, `payload_json` with `{from,to,note}`, `actor_id`). Pipeline and review both write to it; the admin detail page renders it as a timeline. *(→ `US-7.1`.)*

---

## 9. Frontend Architecture

### 9.1 Monorepo layout
```
frontend/
├── apps/
│   ├── consumer-portal/   # FE-CONSUMER (:5173)
│   └── admin-portal/      # FE-ADMIN (:5174)
└── packages/
    ├── api-client/        # FE-PKG-API — typed fetch client + shared DTO types
    ├── ui/                # FE-PKG-UI  — design system (AppShell, Button, Card, states, badges)
    ├── form-renderer/     # FE-PKG-RENDERER — dynamic schema→controls renderer
    └── form-builder/      # FE-PKG-BUILDER — visual builder (placeholder/stub)
```

Both apps use TanStack Query for server state and Vite proxy (`/api` → `:8080`).

### 9.2 `FE-PKG-API` — `api-client`
`createApiClient()` returns typed methods for every endpoint (consumer + admin), injecting `X-Tenant-Id`/`X-Request-Id`. Exports shared TS interfaces (`FormSchema`, `SubmissionDetail`, `SubmissionSummary`, `AdminFormDetail`, `PipelineReport`, …) and `ApiError`. Single source of API truth for both apps.

### 9.3 `FE-PKG-RENDERER` — `form-renderer`
`SectionRenderer.tsx` renders a section's fields dynamically: `text`/`number`/`select` scalar controls with per-field error display, and recursive `embedded_form` rendering (`<fieldset>` with dotted error paths). Genuine implementation (not a stub). *(→ `US-4.2`, `US-4.6`.)*

### 9.4 `FE-PKG-UI` — `ui`
Shared presentational components (`AppShell` with nav slot, `PageHeader`, `Button`, `Card`, `EmptyState`, `LoadingState`, `ErrorState`) + `styles.css`/`tokens.css` (design tokens, `.bf-badge*`). *(→ `US-1.4`.)*

### 9.5 `FE-CONSUMER` — consumer portal
| Area | Files | Purpose |
|------|-------|---------|
| Shell/routes | `App.tsx`, `main.tsx` | Router + nav (Catalog / My applications) |
| Catalog | `pages/FormCatalog.tsx`, `hooks/useConsumerForms.ts` | Browse & start forms |
| Discovery | `pages/DiscoveryWizardPage.tsx`, `hooks/useDiscovery.ts` | Triage wizard → recommendation → prefill |
| Fill/submit | `pages/SubmissionWizardPage.tsx`, `hooks/useSubmission.ts`, `components/SubmissionReview.tsx` | Section stepper, **partial per-page save** (next/back), review, submit; **server-backed resume** via `?submission=` restoring both the saved data and the last section position |
| My applications | `pages/MyApplicationsPage.tsx` | List of the user's applications w/ status badges + continue/view actions |
| Status | `pages/ApplicationStatusPage.tsx`, `lib/submissionStatus.ts` | Persistent per-application status + read-only summary |

*Implements:* `US-3.x`, `US-4.x`, `US-5.1`, `US-5.2`, `US-5.3`.

### 9.6 `FE-ADMIN` — admin portal
| Area | Files | Purpose |
|------|-------|---------|
| Shell/routes | `App.tsx`, `main.tsx` | Router + nav |
| Forms list | `pages/FormsListPage.tsx`, `hooks/useAdminForms.ts` | List forms, create new, latest version/status |
| Builder | `pages/FormBuilderPage.tsx`, `pages/formStatus.ts` | Version selector + JSON schema editor + save draft/publish/new version |
| Submissions | `pages/SubmissionsListPage.tsx`, `pages/SubmissionDetailPage.tsx`, `hooks/useAdminSubmissions.ts` | Queue, detail (sections + timeline), review actions, pipeline report |

The visual drag-and-drop builder (`FE-PKG-BUILDER`) is currently a placeholder; the JSON editor is the working authoring UI. *Implements:* `US-2.x`, `US-7.x`. *(Visual builder → `US-2.5`, planned.)*

---

## 10. End-to-End Flows

**Author → publish → fill → submit → review**
1. Admin creates form + edits `DRAFT` schema (`AdminFormsController` → `FormCommandService`) → publishes.
2. Consumer sees it in catalog (`ConsumerFormsController`), optionally via discovery (`M-DISCOVERY` prefill).
3. Consumer creates draft, saves sections (validated), submits (`ConsumerSubmissionsController`).
4. Pipeline runs synchronously: VALIDATE → PII_SCRUB → DOWNSTREAM (`M-PIPELINE`, `M-TRANSFORM`) → `PENDING_REVIEW`.
5. Admin reviews (`AdminReviewController` → `M-PROCESSING`) → APPROVED/REJECTED/NEEDS_INFO; timeline updated.
6. Consumer tracks status on "My applications" (`FE-CONSUMER`).

---

## 11. Testing

- Backend: JUnit 5 + Mockito unit tests (e.g. `FormCommandServiceTest`), Spring `MockMvc` controller tests (e.g. `AdminFormsControllerTest`).
- Frontend: `tsc -b` typecheck across workspaces (`npm run typecheck`) + Vite production build (`npm run build`).
- Run: `./gradlew test` / `cd frontend && npm run typecheck && npm run build`.

*Implements:* `US-1.6`.

---

## 12. Extension Points (How to…)

- **Add a form field type:** extend `SectionRenderer` (control) + `SectionValidator` (validation rules) + schema-shape validation in `FormCommandService.validateSchema`.
- **Add a pipeline step:** add a `PipelineStepType`, implement the step in `SubmissionPipelineService` (or the config-driven `PipelineOrchestrator`), emit an audit event.
- **Add a downstream connector:** implement in `module-downstream` behind a connector interface (see `ARCHITECTURE.md` §12) and invoke from the DOWNSTREAM step.
- **Add an endpoint:** add controller method in the relevant BFF + api-client method + hook + page.

---

## 13. Component Index (ID → location)

| ID | Component | Path |
|----|-----------|------|
| APP-CORE | Bootstrap/config/security/errors | `app/` |
| BFF-CONSUMER | Consumer API | `bff-consumer/` |
| BFF-ADMIN | Admin API | `bff-admin/` |
| M-IDENTITY | Tenants/users | `module-identity/` |
| M-FORMDEF | Form definition/versions | `module-form-definition/` |
| M-SUBMISSION | Submissions/storage/audit | `module-submission/` |
| M-DISCOVERY | Triage/recommendation | `module-discovery/` |
| M-PIPELINE | Automated pipeline | `module-pipeline/` |
| M-TRANSFORM | PII scrubbing | `module-transformation/` |
| M-PROCESSING | Manual review | `module-processing/` |
| M-OBSERV | Observability | `module-observability/` |
| FE-CONSUMER | Consumer portal | `frontend/apps/consumer-portal/` |
| FE-ADMIN | Admin portal | `frontend/apps/admin-portal/` |
| FE-PKG-API | API client | `frontend/packages/api-client/` |
| FE-PKG-UI | Design system | `frontend/packages/ui/` |
| FE-PKG-RENDERER | Form renderer | `frontend/packages/form-renderer/` |
| FE-PKG-BUILDER | Visual builder (stub) | `frontend/packages/form-builder/` |
