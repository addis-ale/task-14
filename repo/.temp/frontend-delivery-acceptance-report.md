# Delivery Acceptance / Project Architecture Inspection Report

**Project:** Secure Exam Scheduling & Notification Management System — Frontend  
**Date:** 2026-04-03  
**Reviewer:** Automated Architecture Inspector (Re-inspection after changes)  

---

## 1. Verdict

**Pass**

The frontend is a credible, runnable, prompt-aligned, and professional deliverable. All 151 Vitest tests pass, TypeScript compiles cleanly with zero errors, and E2E test infrastructure (Playwright) is now configured with 20 test cases covering auth flows and role-based route protection. The project covers the prompt requirements comprehensively with real API integration, production-grade security patterns (request signing, memory-only tokens, RBAC, concurrent session detection), and complete business workflows across all 5 roles. Previous findings around account enumeration, anti-cheat fallback data, skeletal stores, and missing E2E tests have been addressed. Remaining issues are low severity and do not materially affect delivery credibility.

---

## 2. Scope and Verification Boundary

### What Was Reviewed
- All source files under `frontend/src/` (27 Vue components, ~42 TypeScript modules, 2 CSS files)
- All 15 Vitest test files containing 151 test cases
- 2 Playwright E2E test files containing 20 test cases (`e2e/auth-flow.spec.ts`, `e2e/role-journeys.spec.ts`)
- Playwright configuration (`playwright.config.ts`)
- Project configuration: `package.json`, `vite.config.ts`, `tsconfig.json`, `Dockerfile`, `nginx.conf`
- Both `README.md` files (root and frontend)
- Complete router definitions, route guards, RBAC composable, auth store, API client
- All 21 page views, 4 reusable components, 4 Pinia stores, 4 composables, 5 utility modules
- MainLayout.vue including concurrent session detection UI
- Diff of all 26 changed files (1,052 lines added)

### What Was Excluded
- `./.tmp/` directory and all its contents (per review rules)
- Prior report files (not treated as evidence source)
- Backend Java source code (out of scope for frontend review)
- `node_modules/`, `dist/`, `target/` directories

### What Was Executed
| Command | Result |
|---------|--------|
| `npm test` (vitest run) | **15 test files, 151 tests — all passed** (28.00s) |
| `vue-tsc -b --noEmit` | **TypeScript compilation — clean, zero errors** |

### What Was NOT Executed
- `npm run dev` / `npm run build` — requires backend API on localhost:8080
- `npx playwright test` — requires running dev server with backend; E2E tests reviewed statically
- Docker-based verification — **not executed per review rules**

### Docker Verification Boundary
Docker-based runtime verification is described in the root `README.md` but was not executed per review constraints. This is a **verification boundary**, not a project defect. The project's `Dockerfile` and `nginx.conf` were reviewed statically and appear correctly configured.

---

## 3. Top Findings

### Finding 1 — Unused Web Crypto API Call in sha256Hex
- **Severity:** Low
- **Conclusion:** `crypto.ts:58-60` calls `crypto.subtle.digest()` but discards the result with `void`, always falling through to synchronous CryptoJS.
- **Evidence:** `src/utils/crypto.ts` lines 58-60:
  ```typescript
  if (typeof crypto !== "undefined" && crypto.subtle) {
    void crypto.subtle.digest("SHA-256", encoder.encode(text));
  }
  return CryptoJS.SHA256(text).toString(CryptoJS.enc.Hex);
  ```
- **Impact:** No security vulnerability — CryptoJS provides correct SHA-256. Dead code creates confusion.
- **Minimum Fix:** Remove the unused `crypto.subtle` branch or properly implement async Web Crypto with fallback.

### Finding 2 — README Claims 76 Tests, Actual Count Is 151 + 20 E2E
- **Severity:** Low
- **Conclusion:** Frontend `README.md` line 179 states "Run all 76 tests" but actual `npm test` output shows 151 tests. Additionally, 20 Playwright E2E tests exist but are not documented.
- **Evidence:** `npm test` output: "Tests 151 passed (151)". README line 179: "npm test # Run all 76 tests via Vitest". E2E tests at `e2e/auth-flow.spec.ts` (7 tests) and `e2e/role-journeys.spec.ts` (13 tests) not mentioned.
- **Impact:** Minor documentation inaccuracy. No functional impact.
- **Minimum Fix:** Update README to reflect actual test counts and document E2E test execution.

### Finding 3 — E2E Tests Require Backend for Full Value
- **Severity:** Low
- **Conclusion:** The 20 Playwright E2E tests are well-structured but most test unauthenticated behavior (route redirects, login page rendering). Tests that submit login credentials (`auth-flow.spec.ts` line 24-37) require a running backend to fully execute.
- **Evidence:** `playwright.config.ts` line 13: `webServer: { command: "npm run dev" }` starts the dev server but backend at localhost:8080 is needed for API calls. The login failure test expects a specific error message that depends on backend response handling.
- **Impact:** E2E tests provide value for route protection and UI rendering verification without backend. Full login flow testing requires backend integration.
- **Minimum Fix:** Document in README that E2E tests require backend or add mock API server configuration.

### Finding 4 — Rate-Limit 429 Handling in Login Only
- **Severity:** Low
- **Conclusion:** 429 rate-limit handling is implemented in `LoginPage.vue` (line 64: status 429 or `RATE_LIMITED` code) but is not implemented as a global API interceptor. Other API calls that receive 429 will show generic error toasts.
- **Evidence:** `src/api/index.ts` response interceptor handles 401 but has no 429 case. `LoginPage.vue` lines 64-65 handle rate limiting for login specifically.
- **Impact:** Login flow correctly handles rate limiting. Other endpoints that might return 429 show generic errors.
- **Minimum Fix:** Add global 429 handler in API response interceptor with `Retry-After` header parsing if backend applies rate limiting to non-login endpoints.

### Finding 5 — Concurrent Session Dismissal Without Re-check
- **Severity:** Low
- **Conclusion:** `MainLayout.vue` provides a "Dismiss" button for concurrent session warnings (`dismissConcurrentWarning()`), but dismissal only hides the modal until the next 60-second badge refresh cycle. There is no forced re-check.
- **Evidence:** `MainLayout.vue` line 111-113: `dismissConcurrentWarning()` hides the modal. Badge timer at line 147 refreshes every 60 seconds including concurrent session check.
- **Impact:** User can temporarily dismiss the warning but it will reappear on next cycle if concurrent sessions still exist. Acceptable UX for advisory notification.
- **Minimum Fix:** No action required — behavior is reasonable for a non-blocking advisory.

---

## 4. Security Summary

### Authentication / Login-State Handling
**Pass**

- Tokens and `sessionSecret` stored in memory only, never persisted to Web Storage (`auth.ts` lines 14-18, 27-29, 207-208: cleared on logout)
- Only non-sensitive profile data persisted via `persistToStorage()` (`auth.ts` lines 220-224: explicit comment "Token and sessionSecret are NEVER written to storage")
- Remember-device sessions expire after 7 days with TTL validation on restore
- Login error messages normalized to prevent account enumeration (`LoginPage.vue` lines 53-68: generic "用户名或密码错误 Invalid username or password" regardless of backend response)
- Account lockout detection (423 status / `ACCOUNT_LOCKED` code) with 5-minute countdown
- Rate limit detection (429 status / `RATE_LIMITED` code) with user-facing message
- Open redirect prevention validates paths (rejects `://`, `\\`, `//`, non-`/` paths)
- 401 API responses trigger automatic logout with redirect
- Session inactivity watchdog: warning at 25 min, auto-logout at 30 min
- Password strength indicator checks 5 dimensions (length >= 12, uppercase, lowercase, digit, special char)
- Logout clears all state and calls `resetDependentStores()` (`auth.ts` line 213)
- E2E test verifies generic error message on failed login (`auth-flow.spec.ts` lines 24-37)

### Frontend Route Protection / Route Guards
**Pass**

- Global `beforeEach` guard checks `requiresAuth`, `meta.roles`, and `meta.requiredAction`
- Action-level permissions enforced at router level (`router/index.ts` lines 15-19: `hasPermission()`)
- 4 routes have explicit `requiredAction` meta: session-create (`create`), roster-import (`import`), notification-create (`create`), compliance-reviews (`review`)
- All 21 page routes annotated with `requiresAuth` and `roles` metadata
- E2E tests verify 10 protected routes redirect unauthenticated users to `/login` (`role-journeys.spec.ts` lines 32-81)
- Redirect query parameter preserved for post-login navigation (`auth-flow.spec.ts` lines 72-77)

### Page-Level / Feature-Level Access Control
**Pass**

- `useRBAC()` composable provides `can(action)`, `canAll(actions)`, `canAny(roles)`, `canAnyAction(actions)`
- Default permission matrix: ADMIN (10 actions), ACADEMIC_AFFAIRS (9), HOMEROOM_TEACHER (3), SUBJECT_TEACHER (3), STUDENT (1)
- Server-provided permissions override role defaults
- All action buttons gated by `can()` checks across all page views
- Sidebar navigation filtered by active role (`MainLayout.vue` lines 34-72)
- Role switching and logout both reset dependent stores

### Sensitive Information Exposure
**Pass**

- No hardcoded credentials, API keys, or secrets in frontend source code
- Request signing: `X-Timestamp`, `X-Nonce` (UUID v4), `X-Signature` (HMAC-SHA256) on all authenticated requests
- Structured logger suppresses `debug` in production; accepts only string parameters
- Student IDs masked with `maskStudentId()` in DataTable columns
- Login error logging uses only status code, no raw error text (`LoginPage.vue` line 70: `status=${statusCode || "unknown"}`)
- Anti-cheat visualization functions return `null` when data absent — no synthetic data exposed

### Cache / State Isolation After Switching Users
**Pass**

- Logout clears: in-memory token/sessionSecret, all storage entries, calls `resetDependentStores()` (`auth.ts` line 213)
- Role switching also calls `resetDependentStores()` (`auth.ts` line 124)
- `resetDependentStores()` resets sessions, rosters, and notifications stores
- Session restore validates TTL and rejects expired sessions
- Auth tests verify logout clears dependent stores (`auth.test.ts` lines 139-151)

### Concurrent Session Detection
**Pass**

- `checkConcurrentSessions()` polls for active sessions (`auth.ts` lines 269-281)
- MainLayout displays concurrent session warning modal with device list and "Revoke Others" action
- `forceLogoutOtherSessions()` calls API to terminate competing sessions (`auth.ts` lines 286-299)
- Admin can toggle concurrent session policy per user (`UserManagementPage.vue`: `toggleConcurrentSessions`)
- E2E test verifies no false concurrent session warnings on login page (`role-journeys.spec.ts` lines 84-91)

---

## 5. Test Sufficiency Summary

### Test Overview
| Type | Exists | Files | Test Count | Entry Points |
|------|--------|-------|------------|-------------|
| Unit tests | Yes | 4 | ~29 | `crypto.test.ts`, `date.test.ts`, `pii.test.ts`, `toast.test.ts` |
| Component tests | Yes | 4 | ~28 | `DataTable.test.ts`, `FormBuilder.test.ts`, `ImportWizard.test.ts`, `VersionDiff.test.ts` |
| Integration tests | Yes | 7 | ~94 | `auth-security.test.ts`, `error-handling.test.ts`, `import-validation.test.ts`, `rbac-permissions.test.ts`, `useRBAC.test.ts`, `router.test.ts`, `auth.test.ts` |
| E2E tests | Yes | 2 | 20 | `e2e/auth-flow.spec.ts` (7), `e2e/role-journeys.spec.ts` (13) |
| **Total** | | **17** | **171** | |

**Vitest result:** 15 files, 151 tests, all passing (28.00s)  
**Playwright:** 2 files, 20 tests (not executed — requires backend; reviewed statically)

### Core Coverage
| Area | Status | Evidence |
|------|--------|---------|
| Happy path | **Covered** | Auth login/logout, RBAC permissions, import validation, data table rendering, version diff, form wizard, router navigation. E2E covers login form, route protection, role journeys |
| Key failure paths | **Covered** | Validation failures (import-validation: 9 tests), unauthenticated interception (router + E2E: 10 redirect tests), permission denied (rbac-permissions: 21 tests), error states with retry, expired session rejection, account lockout, rate limiting |
| Security-critical | **Covered** | Token storage isolation (14 tests), open redirect prevention (auth.test + E2E), PII masking regression (8 tests), RBAC matrix (21 tests), crypto/signing (10 tests), account enumeration prevention (E2E), logout store cleanup (auth.test), concurrent session UI (E2E) |

### Major Gaps
1. **E2E tests cannot run without backend** — Login flow, role-based navigation after auth, and CRUD operations require a running API server. Consider adding mock API server or MSW integration.
2. **No page-level tests for complex views** — `AntiCheatReviewPage` (635 lines), `UserManagementPage` (567 lines), `ComplianceReviewPage` (417 lines) have no dedicated component tests.
3. **Auto-save composable not tested** — `FormBuilder.test.ts` mocks `useAutoSave` entirely.

### Final Test Verdict
**Pass**

171 total tests across 17 files provide strong coverage across unit, component, integration, and E2E layers. The test suite is meaningful with security regression tests, real business validation logic, and Playwright E2E tests covering route protection and login security properties. The gaps (untested complex views, mock-dependent E2E) are reasonable for the project scope.

---

## 6. Engineering Quality Summary

### Architecture Strengths
- **Clean separation:** Well-organized directory structure with api, components, composables, i18n, layouts, router, stores, types, utils, views
- **Complete stores:** All 4 Pinia stores now have full CRUD operations, error states, loading management, and pagination (auth: 330 lines/8+ actions, sessions: 188 lines/6 actions, rosters: 150 lines/4 actions, notifications: 206 lines/7 actions)
- **Security infrastructure:** Request signing (HMAC-SHA256), memory-only tokens, concurrent session detection, open redirect prevention, PII masking, account lockout/rate-limit handling
- **Reusable components:** DataTable (paginated with PII masking), FormBuilder (multi-step wizard), ImportWizard (XLSX/CSV with validation), VersionDiff (side-by-side comparison)
- **Type safety:** TypeScript strict mode with dedicated type files, typed action permissions, scope DTOs
- **Internationalization:** zh-CN primary and en secondary with runtime locale switching
- **E2E testing:** Playwright configured with Chromium, screenshot-on-failure, trace retention

### Architecture Weaknesses
- **No shared form validation library** — Validation logic exists in multiple places (ImportWizard column validators, UserManagement password strength, FormBuilder required fields)
- **Silent error handling in auth store** — `checkConcurrentSessions()`, `resetDependentStores()` silently catch errors, which could hide issues in development

### Overall Assessment
The project demonstrates professional frontend engineering practices. The stores have been expanded from skeletal to production-complete, security implementation is comprehensive, and E2E testing infrastructure addresses the previous gap. The architecture supports the application's complexity without unnecessary abstraction.

---

## 7. Visual and Interaction Summary

### Layout and Structure
- Sidebar + content area with collapsible navigation (`MainLayout.vue`, ~620 lines)
- Responsive: horizontal nav grid below 980px
- Consistent card-based containers with CSS custom properties
- Bilingual labels throughout (Chinese primary, English secondary)

### Interaction Quality
- **Loading states:** Skeleton loaders, loading spinners across all pages
- **Error states:** Retry buttons, user-friendly error messages, bilingual error text
- **Empty states:** "No data" messages in all list views
- **Toast notifications:** Animated slide-in with success/error/warning/info types
- **Modal dialogs:** Session timeout (25 min), concurrent session warning with device list, draft resume, delete confirmations
- **Form feedback:** Inline validation, password strength meter, disabled states during submission
- **Navigation feedback:** Active route highlighting, badge counts (unread, pending reviews, anti-cheat flags), collapsible sidebar
- **Data visualization:** Anti-cheat timeline charts, side-by-side submission diffs, score distributions, room capacity bars — all with explicit "Data unavailable" fallbacks when backend data is absent

### Assessment
Visual and interaction quality is appropriate for an internal K-12 administrative tool. Design is clean, consistent, and functional with comprehensive state feedback. The concurrent session modal with device list and "Revoke Others" action is a notable UX addition for the security-critical use case.

---

## 8. Next Actions

1. **Update README test count** (Low priority) — Change "76 tests" to "151 unit/integration tests + 20 E2E tests" and document E2E test execution requirements.

2. **Remove dead crypto.subtle code** (Low priority) — Delete the unused `void crypto.subtle.digest()` call in `sha256Hex()` at `crypto.ts:58-60`.

3. **Add global 429 handler** (Low priority) — Implement `Retry-After` parsing in API response interceptor for non-login endpoints.

4. **Add mock API for E2E tests** (Low priority) — Configure MSW or similar to enable E2E tests to run without a live backend.

5. **Add component tests for complex views** (Low priority) — `AntiCheatReviewPage`, `UserManagementPage`, and `ComplianceReviewPage` would benefit from dedicated tests.

---

*Report generated via static code review, local test execution (151/151 Vitest tests passing, TypeScript zero errors), and static review of 20 Playwright E2E tests. Docker-based runtime verification was not performed per review constraints. Playwright E2E tests were not executed (requires backend).*
