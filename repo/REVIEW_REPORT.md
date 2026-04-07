# Delivery Acceptance & Architecture Audit (Static-Only)

## Verdict

**Overall conclusion: Fail**

### Scope and Static Verification Boundary

**What was reviewed**
- Backend architecture, controllers, services, security filters/guards, entities, repositories, migrations, and configs (`src/main/java`, `src/main/resources`, `pom.xml`).
- Frontend routes/stores/views/API client and component logic (`frontend/src`, `frontend/package.json`, `frontend/vite.config.ts`).
- Documentation and run/test scripts (`README.md`, `run_tests.sh`, `API_tests/run_api_tests.sh`, `unit_tests/run_unit_tests.sh`, Docker files).
- Static tests (backend and frontend test sources under `src/test` and `frontend/src/__tests__`).

**What was not reviewed / not executed**
- Runtime execution, environment bring-up, Docker orchestration, browser flows, API calls, DB state mutation, and automated test execution.

**Intentionally not executed**
- Project startup, Docker, backend/frontend/test commands (per static-only rule).

**Claims requiring manual verification**
- Real offline behavior, actual job scheduler throughput under multi-node deployment, WeChat intranet integration behavior, performance/scalability, and real UX responsiveness.

## Repository / Requirement Mapping Summary

- Prompt core goal: offline-capable K-12 exam scheduling + roster + notification workflow with strict RBAC/ABAC, security controls, compliance review, imports/exports, versioning, and job orchestration.
- Mapped implementation areas: auth/session/security filters, role guards and scope guards, exam session workflow, import/export, notification/compliance/inbox modules, job queue/scheduler, frontend role-routed UI, and test suites.
- Major result: many modules exist, but there are multiple blocker-level frontend-backend contract mismatches and several core requirement gaps (import/export depth, data-at-rest encryption usage, full object-scope enforcement, and test coverage for critical security paths).

---

## Section-by-section Review

## 1) Hard Gates

### 1.1 Documentation and static verifiability
**Conclusion: Partial Pass**

**Rationale**
- Basic run instructions exist, but they are incomplete/inaccurate against code and include contradictions.
- Documentation does not provide a reliable, statically consistent verification path for key flows.

**Evidence**
- Docker-based startup docs present: `README.md:21`.
- README references `repo/frontend/Dockerfile` and a global `.gitignore`, but structure statements are inconsistent/misleading for current root and no local `.gitignore` in root listing: `README.md:7`, `README.md:12`, `README.md:65`.
- Critical frontend-backend endpoint mismatches make docs insufficient for verification without code rewrite (examples below in issues): `frontend/src/stores/auth.ts:271`, `src/main/java/com/exam/system/controller/AuthController.java:31`.

**Manual verification note**
- Manual run is required to validate any startup claim because static contract mismatches indicate likely runtime breakage.

### 1.2 Material deviation from Prompt
**Conclusion: Fail**

**Rationale**
- Implementation includes many prompt-aligned modules, but core workflow cohesion is materially broken by API/data contract drift and incomplete implementations.

**Evidence**
- Prompt-critical roster import/export and scheduling workflow are not aligned end-to-end due API shape mismatch: `frontend/src/views/scheduling/SessionCreatePage.vue:96`, `src/main/java/com/exam/system/dto/session/SessionUpsertRequest.java:34`.
- Export explicitly placeholder: `src/main/java/com/exam/system/service/impl/ImportServiceImpl.java:208`.

---

## 2) Delivery Completeness

### 2.1 Coverage of explicitly stated core requirements
**Conclusion: Fail**

**Rationale**
- Core areas are present but several explicit requirements are only partial or missing in a production-ready sense.

**Evidence**
- Version history and restore are implemented: `src/main/java/com/exam/system/controller/VersionController.java:30`.
- Compliance queue and decisions exist: `src/main/java/com/exam/system/controller/ComplianceReviewController.java:47`.
- Request signing/replay/rate-limit filters exist: `src/main/java/com/exam/system/security/filter/ReplayGuardFilter.java:55`, `src/main/java/com/exam/system/security/filter/RateLimitFilter.java:59`.
- Sensitive field encryption at rest requirement not implemented in persistence paths (AES service exists but not wired to entities/repositories): `src/main/java/com/exam/system/security/crypto/AesCryptoService.java:17`, `src/main/java/com/exam/system/entity/SessionCandidate.java:19`.
- Bulk export/import real domain commit incomplete or mismatched: `src/main/java/com/exam/system/service/impl/ImportServiceImpl.java:208`, `frontend/src/views/rosters/RosterImportPage.vue:23`.

### 2.2 Basic end-to-end deliverable vs partial/demo
**Conclusion: Fail**

**Rationale**
- Repo is full-stack and non-trivial, but key paths are effectively non-functional statically because client/server contracts diverge.

**Evidence**
- Frontend calls nonexistent endpoints: `/auth/sessions`, `/auth/sessions/revoke-others`, `/inbox/{id}/delivery-status`, `/imports/rosters/commit`, `/notifications/deliveries/{id}/retry`, `/users/{id}/toggle-concurrent-sessions`: `frontend/src/stores/auth.ts:271`, `frontend/src/views/notifications/InboxPage.vue:31`, `frontend/src/views/rosters/RosterImportPage.vue:23`, `frontend/src/views/notifications/NotificationListPage.vue:120`, `frontend/src/views/admin/UserManagementPage.vue:185`.
- Backend exposes different endpoints only: `src/main/java/com/exam/system/controller/AuthController.java:31`, `src/main/java/com/exam/system/controller/ImportController.java:32`, `src/main/java/com/exam/system/controller/NotificationController.java:86`, `src/main/java/com/exam/system/controller/UserController.java:80`.

---

## 3) Engineering and Architecture Quality

### 3.1 Structure and module decomposition
**Conclusion: Pass**

**Rationale**
- Layered decomposition is generally sound (controller/service/repository/entity/security/job) with clear domain modules.

**Evidence**
- Backend modularization and dedicated security/job modules: `src/main/java/com/exam/system/config/SecurityConfig.java:17`, `src/main/java/com/exam/system/job/JobSchedulerService.java:16`.
- Frontend split into views/stores/composables/components/router: `frontend/src/router/routes.ts:13`, `frontend/src/stores/auth.ts:26`.

### 3.2 Maintainability and extensibility
**Conclusion: Partial Pass**

**Rationale**
- Architecture is extensible, but maintainability is reduced by duplicated authorization matrices and API contract drift.

**Evidence**
- Permission logic duplicated in multiple frontend places: `frontend/src/router/index.ts:7`, `frontend/src/composables/useRBAC.ts:10`.
- Data-scope aspect is intentionally foundational and incomplete for broader entities: `src/main/java/com/exam/system/security/rbac/DataScopeAspect.java:15`.

---

## 4) Engineering Details and Professionalism

### 4.1 Error handling, logging, validation, API design
**Conclusion: Partial Pass**

**Rationale**
- Basic validation/error handling/logging exist, but API design consistency is weak and several DTOs are inconsistent with frontend contracts.

**Evidence**
- Request-body sanitization and validation present: `src/main/java/com/exam/system/config/SanitizationRequestBodyAdvice.java:25`, `src/main/java/com/exam/system/dto/notification/NotificationCreateRequest.java:8`.
- API contract mismatch example (seat update payload naming/type): `frontend/src/views/rosters/RosterListPage.vue:54`, `src/main/java/com/exam/system/dto/session/CandidateSeatUpdateRequest.java:8`.
- Logging exists but mostly coarse app-level logging: `frontend/src/utils/logger.ts:21`, `src/main/resources/application.yml:44`.

### 4.2 Product/service-level organization vs demo
**Conclusion: Partial Pass**

**Rationale**
- Product-level breadth is substantial; however, placeholder export and broken contracts keep it below production-readiness.

**Evidence**
- Placeholder export path: `src/main/java/com/exam/system/service/impl/ImportServiceImpl.java:208`.
- Incomplete dashboard DTO vs frontend expectations: `src/main/java/com/exam/system/dto/dashboard/DashboardStatsResponse.java:5`, `frontend/src/views/dashboard/DashboardPage.vue:19`.

---

## 5) Prompt Understanding and Requirement Fit

### 5.1 Business goal and constraints fit
**Conclusion: Fail**

**Rationale**
- The repository shows understanding of many constraints (security filters, compliance review, role menus), but critical semantic requirements are not fully satisfied or not wired end-to-end.

**Evidence**
- Requirement intent present: lockout/session timeout/replay/rate limit: `src/main/java/com/exam/system/service/impl/AuthServiceImpl.java:248`, `src/main/java/com/exam/system/service/impl/SessionServiceImpl.java:74`, `src/main/java/com/exam/system/security/filter/ReplayGuardFilter.java:75`, `src/main/java/com/exam/system/security/filter/RateLimitFilter.java:59`.
- Constraint mismatch: frontend event types differ from backend supported enum set: `frontend/src/views/notifications/NotificationCreatePage.vue:39`, `src/main/java/com/exam/system/notification/NotificationEventType.java:13`.
- Prompt asks encryption-at-rest for sensitive fields; implementation stores student IDs plaintext in operational tables: `src/main/resources/db/migration/V1__init_schema.sql:113`, `src/main/java/com/exam/system/entity/SessionCandidate.java:19`.

---

## 6) Aesthetics (frontend/full-stack)

### 6.1 Visual/interaction quality
**Conclusion: Pass**

**Rationale**
- UI has coherent hierarchy, badges, modal feedback, responsive rules, and role-specific navigation.

**Evidence**
- Responsive layout and interaction states: `frontend/src/layouts/MainLayout.vue:607`, `frontend/src/layouts/MainLayout.vue:397`.
- User feedback patterns (loading/error/retry/modals): `frontend/src/views/student/NotificationPreferencesPage.vue:72`, `frontend/src/views/notifications/ComplianceReviewPage.vue:208`.

**Manual verification note**
- Final rendering and motion quality need browser-level manual check.

---

## Issues / Suggestions (Severity-Rated)

### 1) **Blocker** — Frontend/Backend API contract is materially inconsistent across critical workflows
**Conclusion**: Fail

**Evidence**
- Frontend calls missing auth session endpoints: `frontend/src/stores/auth.ts:271`, `frontend/src/stores/auth.ts:288`.
- Backend lacks these endpoints: `src/main/java/com/exam/system/controller/AuthController.java:31`.
- Frontend roster import commit endpoint mismatch: `frontend/src/views/rosters/RosterImportPage.vue:23` vs backend import paths `src/main/java/com/exam/system/controller/ImportController.java:32`.
- Delivery retry endpoint mismatch: `frontend/src/views/notifications/NotificationListPage.vue:120` vs backend `src/main/java/com/exam/system/controller/NotificationController.java:86`.
- Inbox delivery status endpoint missing: `frontend/src/views/notifications/InboxPage.vue:31`.

**Impact**
- Core flows (login session management, import commit, delivery retry, inbox detail) cannot be verified and are likely non-functional at runtime.

**Minimum actionable fix**
- Establish a single API contract source (OpenAPI/spec DTOs) and align frontend paths/methods/payloads with backend controller contracts.

**Minimal verification path**
- Static diff: generated API client vs controllers; then manual smoke: login, import preview+commit, notification delivery retry, inbox detail.

### 2) **Blocker** — Request/response schema mismatches break scheduling, roster, user admin, dashboard and notification flows
**Conclusion**: Fail

**Evidence**
- Session create payload mismatch (`roomIds/candidateIds/proctorIds/examDate`) vs backend expects `date + roomAssignments/candidates/proctors`: `frontend/src/views/scheduling/SessionCreatePage.vue:96`, `src/main/java/com/exam/system/dto/session/SessionUpsertRequest.java:34`.
- Seat update payload mismatch (`seatNo`) vs `seatNumber + roomId`: `frontend/src/views/rosters/RosterListPage.vue:54`, `src/main/java/com/exam/system/dto/session/CandidateSeatUpdateRequest.java:8`.
- User create/update mismatch (`role`, `displayName`, `status`) vs backend DTOs: `frontend/src/views/admin/UserManagementPage.vue:95`, `src/main/java/com/exam/system/dto/user/CreateUserRequest.java:16`, `src/main/java/com/exam/system/dto/user/UpdateUserRequest.java:7`.
- Dashboard frontend expects `upcomingSessions` and `recentActivity`; backend DTO does not expose these fields: `frontend/src/views/dashboard/DashboardPage.vue:19`, `src/main/java/com/exam/system/dto/dashboard/DashboardStatsResponse.java:5`.
- Session list/detail and student exam field naming mismatches: `frontend/src/views/scheduling/SessionListPage.vue:23`, `src/main/java/com/exam/system/dto/session/SessionSummaryResponse.java:9`; `frontend/src/views/student/MyExamsPage.vue:52`, `src/main/java/com/exam/system/dto/session/StudentExamResponse.java:9`.
- Notification event/type scope mismatch: `frontend/src/views/notifications/NotificationCreatePage.vue:39`, `src/main/java/com/exam/system/notification/NotificationEventType.java:13`, `src/main/java/com/exam/system/dto/notification/NotificationTargetScopeDto.java:5`.

**Impact**
- End-to-end core business workflows are blocked even where features exist in isolation.

**Minimum actionable fix**
- Normalize DTO contracts and field names end-to-end; add API compatibility tests that serialize/deserialize real payloads per endpoint.

**Minimal verification path**
- Manual API contract test for each critical endpoint with frontend payload fixtures.

### 3) **High** — Sensitive-data-at-rest requirement is not implemented for student identifiers
**Conclusion**: Fail

**Evidence**
- Student IDs stored directly in operational tables: `src/main/resources/db/migration/V1__init_schema.sql:113`, `src/main/java/com/exam/system/entity/SessionCandidate.java:19`.
- AES crypto service exists but is not integrated in persistence mappings/services for these fields: `src/main/java/com/exam/system/security/crypto/AesCryptoService.java:17`.

**Impact**
- Violates explicit prompt requirement for encryption at rest of sensitive identifiers.

**Minimum actionable fix**
- Use JPA `AttributeConverter` or service-layer encryption for sensitive fields (student IDs and any protected identifiers), with migration strategy.

**Minimal verification path**
- Static check of converter/entity usage + DB sample inspection in manual verification.

### 4) **High** — Bulk import/export implementation is partial and not domain-committing
**Conclusion**: Partial Pass / High Risk

**Evidence**
- Export explicitly placeholder workbook: `src/main/java/com/exam/system/service/impl/ImportServiceImpl.java:208`.
- Commit endpoint only flips batch status and does not apply data to domain tables: `src/main/java/com/exam/system/service/impl/ImportServiceImpl.java:165`.

**Impact**
- Core operational requirement (real roster/session import/export) not fulfilled.

**Minimum actionable fix**
- Implement entity-type-specific import transformers and transactional commits into target tables; implement real export query mappings.

**Minimal verification path**
- Manual verification with known CSV/XLSX fixture and DB row diffs before/after commit/export.

### 5) **High** — ABAC/object-level data-scope enforcement is incomplete beyond exam_session
**Conclusion**: Partial Pass / Suspected Risk

**Evidence**
- Scope aspect only configures `examSessionScope` filter and is marked foundational: `src/main/java/com/exam/system/security/rbac/DataScopeAspect.java:38`, `src/main/java/com/exam/system/security/rbac/DataScopeAspect.java:15`.
- Version scope check only validates term for non-privileged roles, not broader object-level constraints: `src/main/java/com/exam/system/service/impl/VersionServiceImpl.java:202`.

**Impact**
- Potential for cross-scope access in modules not protected by equivalent guards.

**Minimum actionable fix**
- Add entity-level scope filters or explicit guard checks per service method for all scoped resources (versions, imports, notifications where applicable).

**Minimal verification path**
- Add negative tests for cross-term/grade/class/course access attempts.

### 6) **Medium** — Job orchestration coverage does not fully match prompt scope
**Conclusion**: Partial Pass

**Evidence**
- Implemented handlers cover notification send/retry/DND release only: `src/main/java/com/exam/system/job/handler/NotificationSendJobHandler.java:19`, `src/main/java/com/exam/system/job/handler/RetryDeliveryJobHandler.java:19`, `src/main/java/com/exam/system/job/handler/DndReleaseJobHandler.java:20`.
- Declared job types include `BULK_IMPORT` and `DATA_CHECK`, but no corresponding handler usage observed: `src/main/java/com/exam/system/job/JobType.java:5`.

**Impact**
- Prompt-required distributed jobs for bulk imports/periodic data checks appear incomplete.

**Minimum actionable fix**
- Implement handlers and enqueue paths for `BULK_IMPORT` and `DATA_CHECK`; expose monitor metrics for them.

**Minimal verification path**
- Static: handler classes and enqueue calls for each type; manual: queue run + status transitions.

### 7) **Medium** — Frontend permission matrices are duplicated and can drift
**Conclusion**: Partial Pass

**Evidence**
- Role matrix duplicated in router and composable: `frontend/src/router/index.ts:7`, `frontend/src/composables/useRBAC.ts:10`.

**Impact**
- Inconsistent behavior risk across route guards and component controls.

**Minimum actionable fix**
- Centralize permission map/type source and consume from one module.

**Minimal verification path**
- Unit tests assert same matrix output from all call paths.

### 8) **Low** — README setup guidance is partially outdated/inconsistent
**Conclusion**: Partial Pass

**Evidence**
- Structure statements include potentially misleading paths/claims: `README.md:7`, `README.md:12`, `README.md:65`.

**Impact**
- Reviewer friction during setup and static verification.

**Minimum actionable fix**
- Refresh README to reflect current repo layout and real API/UI workflow prerequisites.

**Minimal verification path**
- Manual doc walk-through by a fresh reviewer.

---

## Security Review Summary

- **Authentication entry points**: **Partial Pass**  
  - Login/logout/me/password/role-switch exist: `src/main/java/com/exam/system/controller/AuthController.java:40`.  
  - Account lockout/session timeout present: `src/main/java/com/exam/system/service/impl/AuthServiceImpl.java:248`, `src/main/java/com/exam/system/service/impl/SessionServiceImpl.java:74`.

- **Route-level authorization**: **Partial Pass**  
  - Most controllers enforce role guard: `src/main/java/com/exam/system/controller/ExamSessionController.java:53`, `src/main/java/com/exam/system/controller/NotificationController.java:40`.

- **Object-level authorization**: **Partial Pass / Suspected Risk**  
  - Some explicit ownership checks exist (inbox mark-read): `src/main/java/com/exam/system/service/impl/InboxServiceImpl.java:67`.  
  - Not consistently evident across all modules; version scope check is term-only for non-admin roles: `src/main/java/com/exam/system/service/impl/VersionServiceImpl.java:202`.

- **Function-level authorization**: **Partial Pass**  
  - Active role guard widely applied in controllers: `src/main/java/com/exam/system/controller/ComplianceReviewController.java:37`.  
  - `PermissionEvaluatorService` exists but broad explicit usage is not evident in controllers/services: `src/main/java/com/exam/system/security/rbac/PermissionEvaluatorService.java:45`.

- **Tenant/user data isolation**: **Partial Pass**  
  - Session scope guard and exam session filter exist: `src/main/java/com/exam/system/security/rbac/SessionDataScopeGuard.java:13`, `src/main/java/com/exam/system/entity/ExamSession.java:23`.  
  - Scope enforcement not proven uniformly across all entities.

- **Admin/internal/debug endpoint protection**: **Pass (for identified endpoints)**  
  - Job monitor, users, audit endpoints protected by admin/admin+academic checks: `src/main/java/com/exam/system/controller/JobMonitorController.java:35`, `src/main/java/com/exam/system/controller/UserController.java:44`, `src/main/java/com/exam/system/controller/AuditLogController.java:35`.

---

## Tests and Logging Review

- **Unit tests**: **Partial Pass**  
  - Backend unit tests exist for selected services/entities/security helpers: `src/test/java/com/exam/system/service/AuthServiceTest.java:62`, `src/test/java/com/exam/system/security/ActiveRoleGuardTest.java:23`.

- **API/integration tests**: **Fail (static adequacy)**  
  - API shell script exists but is not authoritative and cannot prove contract correctness statically: `API_tests/run_api_tests.sh:1`.  
  - No strong controller/security integration test suite (e.g., MockMvc for 401/403/object-scope) found in reviewed test paths.

- **Logging categories / observability**: **Partial Pass**  
  - Basic backend and frontend logging exists: `src/main/resources/application.yml:44`, `frontend/src/utils/logger.ts:21`.  
  - Structured operational logging for queue/security events is limited.

- **Sensitive-data leakage risk in logs/responses**: **Partial Pass / Suspected Risk**  
  - Student IDs are masked in one session response path: `src/main/java/com/exam/system/service/impl/ExamSessionServiceImpl.java:767`.  
  - But raw `studentId` is still present in the same DTO and responses: `src/main/java/com/exam/system/dto/session/SessionCandidateResponse.java:5`.

---

## Test Coverage Assessment (Static Audit)

### 8.1 Test Overview

- Backend tests exist under JUnit5/Mockito (`src/test/java`): e.g., `src/test/java/com/exam/system/service/AuthServiceTest.java:36`.
- Frontend tests exist under Vitest (`frontend/src/__tests__`): e.g., `frontend/src/__tests__/integration/rbac-permissions.test.ts:58`.
- Test commands documented in scripts and package scripts: `run_tests.sh:8`, `frontend/package.json:10`.
- No strong static evidence of backend controller-level authz/scope integration tests (401/403/object-scope) in reviewed test files.

### 8.2 Coverage Mapping Table

| Requirement / Risk Point | Mapped Test Case(s) | Key Assertion / Fixture | Coverage Assessment | Gap | Minimum Test Addition |
|---|---|---|---|---|---|
| Password policy (>=12 + complexity) | `src/test/java/com/exam/system/service/PasswordPolicyValidatorTest.java` | Password validator branch assertions | basically covered | No endpoint-level validation test | Add controller/service integration for `/auth/password` failures |
| Auth login success/failure/lock semantics | `src/test/java/com/exam/system/service/AuthServiceTest.java:62` | invalid user/wrong password/locked user | basically covered | No filter-level 401 tests for missing Bearer/signature | Add MockMvc tests for `AuthFilter` + `ReplayGuardFilter` |
| Route RBAC in frontend navigation | `frontend/src/__tests__/integration/rbac-permissions.test.ts:65` | route guard role/action checks | sufficient (frontend-only) | No backend authorization integration | Add backend 403 tests per protected endpoint role matrix |
| Import file format validation UI | `frontend/src/__tests__/integration/import-validation.test.ts:33` | invalid formats/duplicates/missing fields | sufficient (component-level) | No backend commit/export business verification | Add backend import commit/export integration tests |
| Error handling UX for student pages | `frontend/src/__tests__/integration/error-handling.test.ts:43` | load failure + retry behaviors | basically covered | No API contract conformance tests | Add typed contract tests against backend DTOs |
| Draft autosave service logic | `src/test/java/com/exam/system/service/DraftServiceTest.java:34` | get/save/delete draft behavior | basically covered | No end-to-end autosave payload validation | Add request-body schema test for `/drafts/{formKey}` |
| Anti-cheat review service branch logic | `src/test/java/com/exam/system/service/AntiCheatServiceTest.java:48` | pending/reviewed transitions | basically covered | No role/scope access tests for anti-cheat endpoints | Add controller authz tests for `/anti-cheat/flags` |
| Object-level access isolation | none strong found | n/a | missing | Severe risk could pass tests undetected | Add negative tests for cross-user inbox access and cross-scope session/version access |
| API contract parity (frontend vs backend) | none strong found | n/a | missing | Blocker mismatches undetected by current tests | Add contract tests generated from shared OpenAPI/types |

### 8.3 Security Coverage Audit

- **Authentication**: **basically covered (service-level), insufficient (integration-level)**  
  - Service tests exist (`AuthServiceTest`), but no robust filter/endpoint auth integration coverage.
- **Route authorization**: **insufficient**  
  - Frontend guard tests exist; backend 403 authorization coverage is largely absent.
- **Object-level authorization**: **missing/insufficient**  
  - No static evidence of dedicated tests for cross-scope object access denial across modules.
- **Tenant/data isolation**: **insufficient**  
  - No strong tests for ABAC scope filtering boundaries and bypass attempts.
- **Admin/internal protection**: **insufficient**  
  - No backend integration tests proving non-admin rejection for all admin endpoints.

### 8.4 Final Coverage Judgment

**Fail**

- Major risks covered: some service-level logic and frontend route guard behavior.
- Major risks not covered: backend auth filter integration, endpoint authorization matrix, object-level and scope isolation, and API contract parity.
- Current tests could still pass while severe defects remain (including blocker-level contract mismatches and security authorization gaps).

---

## Final Notes

- This is a static-only assessment; runtime validity was not inferred from docs or scripts.
- Root-cause blockers are contract-level and should be resolved before further acceptance checks.
- After contract alignment, prioritize backend security integration tests (401/403/object-scope) and import/export real-data path tests.
