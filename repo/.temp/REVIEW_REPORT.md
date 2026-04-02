# Delivery Acceptance / Project Architecture Inspection Report

**Project:** Secure Exam Scheduling & Notification Management System — Frontend  
**Review Date:** 2026-04-03  
**Reviewer:** Automated Architecture Inspector  

---

## 1. Verdict

**Pass**

The frontend is a credible, runnable, prompt-aligned, and professionally engineered Vue 3 application. It builds successfully, all 76 tests pass, comprehensive project documentation is provided, and the security model addresses all prompt-specified requirements. No Blocker or High-severity findings remain.

---

## 2. Scope and Verification Boundary

### What Was Reviewed
- All 60+ source files under `frontend/src/` — 22 Vue views, 4 reusable components, 4 Pinia stores, 4 composables, API layer, router, i18n, utilities, type definitions, logger
- 10 test files (76 test cases) under `src/__tests__/`
- Build configuration: `package.json`, `vite.config.ts`, `tsconfig.json`, `Dockerfile`, `nginx.conf`
- `README.md` (194 lines, project-specific)
- Pre-built `dist/` directory

### What Was Excluded
- `./.tmp/` directory — excluded per review rules
- `node_modules/` — third-party dependencies not audited
- Backend (`src/`, `pom.xml`, `target/`) — out of scope for frontend review

### Runtime Verification Performed
- **`vite build`**: Executed successfully — builds in ~2.4s with zero errors, produces 40+ chunked assets
- **`vitest run`**: Executed successfully — 10 test files, 76 tests, all passed (22.77s total)

### What Remains Unconfirmed
- Full runtime behavior of all 22 pages against a live backend API
- Server-side enforcement of RBAC, request signing validation, rate limiting, and replay rejection
- Docker-based runtime verification was **not required** and **not executed**; `Dockerfile` and `nginx.conf` are present for production deployment

---

## 3. Top Findings

### Finding 1 — `require()` Used in ES Module Context for Store Reset
- **Severity:** Medium
- **Conclusion:** `resetDependentStores()` in `stores/auth.ts:104-106` uses CommonJS `require()` to dynamically import dependent stores, which is inconsistent with the ESM project configuration (`"type": "module"`)
- **Evidence:** `stores/auth.ts:104` — `const { useNotificationsStore } = require("@/stores/notifications");`
- **Impact:** Works at runtime due to Vite's module transformation, but may cause issues in strict ESM environments or SSR. The try/catch silently swallows failures.
- **Fix:** Replace `require()` with dynamic `import()` or directly import the stores at the top of the file (circular dependency concern is mitigated since Pinia stores are lazily initialized)

### Finding 2 — Token and Session Secret Stored in Client-Side Storage
- **Severity:** Medium
- **Conclusion:** Authentication token and HMAC session secret are persisted to `localStorage` (remember-device) or `sessionStorage` (default)
- **Evidence:** `stores/auth.ts:200-208` — full `PersistedAuth` object written to browser storage
- **Impact:** Any XSS vulnerability on the same origin would expose both the bearer token and the HMAC signing secret. This is a common SPA trade-off; the prompt requires local-only auth, making httpOnly cookies an alternative but not strictly required. The 7-day TTL (`auth.ts:9`, enforced at `auth.ts:134-141`) limits exposure window for remembered sessions.
- **Fix:** Consider httpOnly cookie-based session transport if backend supports it; otherwise, accepted as inherent SPA limitation with proper XSS prevention

### Finding 3 — No E2E Test Coverage
- **Severity:** Medium
- **Conclusion:** The test suite covers unit, component, and router integration layers but lacks end-to-end tests (e.g., Cypress or Playwright)
- **Evidence:** No E2E framework in `package.json` devDependencies; no E2E config files found
- **Impact:** Multi-page user flows (login → dashboard → create session → verify) are not tested end-to-end. The existing 76 tests cover the critical paths at the unit/component level, which provides reasonable confidence but not full flow verification.
- **Fix:** Add a lightweight E2E smoke test covering login → dashboard → logout using Playwright or Cypress

### Finding 4 — Badge Polling Continues in Background Tabs
- **Severity:** Low
- **Conclusion:** `MainLayout.vue:137` sets a 60-second `setInterval` for badge count fetching that continues when the browser tab is in the background
- **Evidence:** `layouts/MainLayout.vue:137` — `badgeTimer = window.setInterval(fetchBadges, 60_000);`
- **Impact:** Minor unnecessary network requests when the tab is inactive; browsers throttle background timers but do not fully stop them
- **Fix:** Use `document.visibilitychange` event to pause/resume polling

### Finding 5 — Rate-Limit Countdown Not Displayed in UI After Initial Toast
- **Severity:** Low
- **Conclusion:** The 429 handler creates a countdown variable (`remaining`) that decrements every second, but only the initial value is shown in the toast — subsequent decrements are not reflected in any visible UI
- **Evidence:** `api/index.ts:68-76` — `remaining` decrements but the toast content is static (set once at line 69)
- **Impact:** User sees "retry in 60s" but the toast does not update to show 59s, 58s, etc. Functional but could be more informative.
- **Fix:** Use a reactive toast or update the toast message on each tick

---

## 4. Security Summary

| Dimension | Verdict | Evidence |
|---|---|---|
| **Authentication / login-state handling** | **Pass** | Login via `stores/auth.ts:40-74` with Bearer token. Session timeout at 30min with 25min warning (`composables/useAuth.ts:5-6`). Account lockout UI with 300s countdown (`LoginPage.vue:52-60`). 7-day TTL enforced on remember-device (`auth.ts:9,134-141`). Logger used throughout — no sensitive data logged (`auth.ts:72,99,174`). |
| **Frontend route protection / route guards** | **Pass** | `router/index.ts:10-40` — `beforeEach` guard checks `isAuthenticated` and `meta.roles`. All 18 protected routes have `requiresAuth: true` and role arrays defined in `routes.ts`. Unauthenticated → `/login` with redirect; unauthorized role → `/403`. |
| **Page-level / feature-level access control** | **Pass** | Role-based menus in `MainLayout.vue:34-72` — 5 distinct menu sets per role. Action buttons conditionally rendered by role in views (e.g., session restore limited to ADMIN/ACADEMIC_AFFAIRS in `SessionDetailPage.vue`). `useRBAC` composable (`composables/useRBAC.ts`) provides `canAny(roles)` for in-component checks. |
| **Sensitive information exposure** | **Pass** | PII masking via `utils/pii.ts`. No `console.log` of sensitive data — structured logger (`utils/logger.ts`) suppresses debug in production (`import.meta.env.PROD`). No hardcoded secrets or API keys. No source maps in `dist/`. No `v-html` usage (XSS safe). Token stored in browser storage is an accepted SPA trade-off with TTL enforcement. |
| **Cache / state isolation after switching users** | **Pass** | Logout clears all auth state and both storage backends (`auth.ts:163-185`). Role switch triggers `resetDependentStores()` (`auth.ts:97-99,102-120`) which resets notification inbox, sessions, and roster stores to prevent stale cross-role data. |

---

## 5. Test Sufficiency Summary

### Test Overview
| Test Type | Status | Entry Point |
|---|---|---|
| Unit tests | **Exist** — 4 files | `src/__tests__/unit/crypto.test.ts`, `date.test.ts`, `pii.test.ts`, `toast.test.ts` |
| Component tests | **Exist** — 4 files | `src/__tests__/components/DataTable.test.ts`, `FormBuilder.test.ts`, `ImportWizard.test.ts`, `VersionDiff.test.ts` |
| Router integration tests | **Exist** — 1 file | `src/__tests__/router/router.test.ts` |
| Auth store tests | **Exist** — 1 file | `src/__tests__/auth/auth.test.ts` |
| E2E tests | **Missing** | No Cypress/Playwright configuration |

**Total:** 10 test files, 76 passing tests, Vitest 4.1.2 with jsdom environment.

### Core Coverage
| Area | Status | Evidence |
|---|---|---|
| Happy path | **Covered** | Auth test: login sets token/user/sessionSecret; Router test: authenticated user reaches dashboard; Component tests: DataTable renders data, FormBuilder submits, ImportWizard commits valid rows |
| Key failure paths | **Covered** | Auth test: redirect validation rejects external URLs; Toast test: handleApiError maps UNAUTHORIZED, FORBIDDEN, RATE_LIMITED, etc.; Router test: unauthenticated → login, wrong role → 403; FormBuilder: required field validation blocks next step |
| Security-critical coverage | **Covered** | Crypto test: signRequest produces correct HMAC-SHA256; Auth test: 7-day TTL enforced, validateRedirect blocks `://`, `\\`, `//`; Router test: role-based guard enforcement |

### Major Gaps
1. **No E2E tests** — multi-page flows not verified end-to-end
2. **No composable-level tests** — `useAutoSave`, `useNotifications` only tested indirectly through component tests
3. **No view-level tests** — individual page components not directly tested (tested indirectly via router and component tests)

### Final Test Verdict
**Partial Pass** — Solid unit, component, and integration test coverage for the core business logic and security paths. The absence of E2E tests is a gap but does not block acceptance given the breadth of the existing suite (76 tests across 10 files covering all 4 utility modules, all 4 shared components, router guards, and auth store).

---

## 6. Engineering Quality Summary

### Strengths
- **Clean separation of concerns**: Views, components, composables, stores, API layer, utilities, types, i18n, and router are well-organized in purpose-specific directories
- **TypeScript strict mode**: All source files use TypeScript with strict compilation; typed interfaces for API responses (`types/api.ts`), auth payloads (`types/auth.ts`), and UI constructs (`types/ui.ts`)
- **Vue 3 Composition API**: Consistent use of `<script setup>` across all 22 views and 4 components
- **Lazy-loaded routes**: All view components use dynamic `() => import()` for code splitting (verified in `routes.ts`)
- **Reusable component design**: `DataTable`, `FormBuilder`, `ImportWizard`, `VersionDiff` are prop-driven with slot customization
- **Structured logging**: `utils/logger.ts` provides severity-level logging with production-aware suppression
- **Request signing**: HMAC-SHA256 anti-replay implementation in `utils/crypto.ts` and `api/index.ts`
- **Bilingual i18n**: Full zh-CN/en translation files with runtime locale switching
- **Production deployment**: `Dockerfile` (multi-stage Node/Nginx) and `nginx.conf` included

### Issues
- **`require()` in ESM context** (`auth.ts:104-106`): Should use `import()` or static imports
- **No error boundary component**: Runtime JS errors in any view would crash the SPA with no recovery UI

### Overall
Engineering quality is **good** — the codebase demonstrates professional Vue 3/TypeScript practices with clean architecture, consistent patterns, and thoughtful security design.

---

## 7. Visual and Interaction Summary

### Strengths
- **Consistent design system**: CSS custom properties in `assets/theme.css` for colors, fonts, shadows, border-radius used throughout all views
- **Card-based layout**: All content areas use consistent `.card` styling with soft shadows
- **Responsive design**: Grid-based layout with sidebar collapse; mobile breakpoint at 980px converts sidebar to horizontal grid
- **Toast notification system**: Animated slide-in toasts with 4 color-coded types (success/error/warning/info)
- **Modal dialogs**: Session timeout warning and draft resume with backdrop overlay
- **Password strength meter**: Visual bar with color gradient (red → yellow → green) based on 5 complexity dimensions
- **Skeleton loaders**: Loading states in dashboard stats and data tables
- **Badge counts**: Real-time unread/pending counts on sidebar navigation items
- **Accessibility**: ARIA labels on landmarks (`aria-label`, `aria-live="polite"`, `aria-modal="true"`), proper form labeling
- **Anti-cheat visualizations**: Bar charts for activity bursts, side-by-side panels for identical submissions, score distribution charts

### Issues
- **No dark mode**: Only light theme available (not required by prompt)
- **Limited button hover states**: Buttons rely on browser defaults for hover transitions

### Overall
Visual quality is **good** — the UI is coherent, professionally themed, responsive, and functionally complete. It clearly resembles a real administrative product, not a demo or tutorial artifact.

---

## 8. Next Actions

| Priority | Action | Severity | Rationale |
|---|---|---|---|
| 1 | Replace `require()` with ESM imports in `resetDependentStores()` | Medium | Ensures module consistency and avoids potential SSR/test issues |
| 2 | Add E2E smoke test (login → dashboard → logout) | Medium | Closes the remaining test layer gap |
| 3 | Add `document.visibilitychange` listener to pause badge polling | Low | Reduces unnecessary background network traffic |
| 4 | Make rate-limit countdown toast reactive | Low | Improves UX for rate-limited users |
| 5 | Add a Vue error boundary component at the app root | Low | Prevents full-page crashes from unhandled runtime errors |
