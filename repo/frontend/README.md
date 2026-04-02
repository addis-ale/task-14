# Secure Exam Scheduling & Notification Management System — Frontend

## Project Purpose

A Vue 3 single-page application for a K-12 school exam scheduling and notification management platform. The system enables academic staff to plan exam sessions, assign rooms and proctors, manage student rosters via bulk import, send compliance-reviewed notifications through multiple channels, and review anti-cheat flags — all under strict role-based access control with request-level cryptographic signing.

## Feature Summary

- **Dashboard** — role-aware stats (sessions, students, reviews, jobs, anti-cheat flags) with term selector
- **Exam Scheduling** — create/edit sessions with conflict checking, version history, and diff comparison
- **Roster Management** — bulk CSV/XLSX import wizard with duplicate detection and row-level validation
- **Notification Workflow** — draft, compliance review, approval, publish, multi-channel delivery tracking
- **Compliance Review Queue** — tab-filtered (Pending/Approved/Rejected) with content preview and approval flow
- **Anti-Cheat Review** — flag visualizations (activity timeline, side-by-side submission diff, score distribution chart)
- **User Management** — CRUD with password strength meter, role/scope assignment, account lock/unlock
- **Job Monitor** — colored status badges, detail modal with payload JSON, retry for failed jobs
- **Audit Logs** — expandable detail rows, CSV export, date range and action type filters
- **Campus & Room Management** — expandable campus list with room capacity visualization
- **Version History** — side-by-side diff viewer with restore capability
- **Internationalization** — zh-CN (primary) and en (secondary) with runtime locale switching
- **Session Timeout** — inactivity warning at 25 min, auto-logout at 30 min
- **Auto-Save Drafts** — periodic draft persistence with recovery on next login

## Prerequisites

| Tool         | Version  |
|-------------|----------|
| Node.js     | >= 18.x  |
| npm          | >= 9.x   |
| Backend API  | Running on `localhost:8080` |

## Install & Run

```bash
# Install dependencies
npm install

# Start development server (with API proxy to localhost:8080)
npm run dev

# Type-check and build for production
npm run build

# Preview production build locally
npm run preview

# Run all tests
npm test
```

## Environment Configuration

The Vite dev server proxies `/api` requests to the Spring Boot backend:

```ts
// vite.config.ts
server: {
  proxy: {
    "/api": {
      target: "http://localhost:8080",
      changeOrigin: true,
    },
  },
}
```

For production, configure your reverse proxy (nginx) to forward `/api/` to the backend service.

## Architecture Overview

```
src/
├── api/                    # Axios client with HMAC request signing interceptors
│   └── index.ts
├── assets/                 # CSS theme variables, global styles, images
├── components/
│   ├── DataTable/          # Reusable paginated table with sort, search, PII masking, skeleton loading
│   ├── FormBuilder/        # Multi-step wizard form with auto-save and validation
│   ├── ImportWizard/       # Excel/CSV import with preview, duplicate detection, row validation
│   └── VersionDiff/        # Side-by-side version comparison with restore
├── composables/            # Vue 3 composition functions
│   ├── useAuth.ts          # Session watchdog (inactivity timer, activity listeners)
│   ├── useAutoSave.ts      # Form draft auto-save via API
│   ├── useNotifications.ts # Inbox + unread count wrapper
│   └── useRBAC.ts          # Role-based access control helper
├── i18n/                   # Internationalization
│   ├── zh-CN.ts            # Chinese locale (primary)
│   ├── en.ts               # English locale (secondary)
│   └── index.ts            # Locale state and t() function
├── layouts/
│   └── MainLayout.vue      # Sidebar navigation, topbar, toast container, session timeout modal
├── router/
│   ├── index.ts            # Route guards (auth, RBAC, redirect)
│   └── routes.ts           # All route definitions with role metadata
├── stores/                 # Pinia state management
│   ├── auth.ts             # Token, session, user profile, login/logout, redirect validation
│   ├── sessions.ts         # Exam session list data
│   ├── rosters.ts          # Student roster data
│   └── notifications.ts    # Inbox messages, unread count
├── types/
│   ├── api.ts              # ApiResponse, PageData, PaginationMeta
│   ├── auth.ts             # UserProfile, LoginPayload, ScopeDto
│   └── ui.ts               # TableColumn, SelectOption, RoleName
├── utils/
│   ├── crypto.ts           # HMAC-SHA256 signing, SHA-256 hashing, nonce generation
│   ├── date.ts             # dayjs formatting helpers
│   ├── logger.ts           # Structured logger (debug/info/warn/error), suppresses debug in prod
│   ├── pii.ts              # Student ID masking
│   └── toast.ts            # Toast notification system with API error code mapping
├── views/
│   ├── admin/              # UserManagement, JobMonitor, AuditLog, CampusRoom pages
│   ├── auth/               # Login, ChangePassword pages
│   ├── common/             # 403 Forbidden, 404 Not Found pages
│   ├── dashboard/          # Dashboard with term selector and stats cards
│   ├── notifications/      # NotificationList/Create, ComplianceReview, AntiCheat, Inbox
│   ├── rosters/            # RosterList, RosterImport pages
│   ├── scheduling/         # SessionList, SessionCreate, SessionDetail pages
│   └── student/            # MyExams, NotificationPreferences pages
├── __tests__/              # Vitest test suites
│   ├── unit/               # crypto, pii, date, toast utility tests
│   ├── components/         # DataTable, FormBuilder, ImportWizard, VersionDiff tests
│   ├── router/             # Route guard integration tests
│   └── auth/               # Auth store, redirect validation, TTL tests
├── App.vue                 # Root component
├── main.ts                 # App entry point (Pinia, router, API interceptor setup)
└── style.css               # Global styles with theme import
```

## Roles & Permitted Pages

| Role | Pages |
|------|-------|
| **ADMIN** | Dashboard, User Management, Scheduling, Rosters, Campus & Rooms, Notifications, Compliance Reviews, Anti-Cheat Reviews, Job Monitor, Audit Logs |
| **ACADEMIC_AFFAIRS** | Dashboard, Scheduling, Rosters, Campus & Rooms, Notifications, Compliance Reviews, Anti-Cheat Reviews, Audit Logs |
| **HOMEROOM_TEACHER** | Dashboard, Scheduling (read-only), Rosters |
| **SUBJECT_TEACHER** | Dashboard, Scheduling (read-only), Course Rosters |
| **STUDENT** | My Exams, Inbox, Notification Preferences |

All roles can access the Change Password page. Unauthorized access redirects to `/403`.

## Security Design

### Request Signing (Anti-Replay)
Every authenticated API request includes three headers:
- `X-Timestamp` — Unix epoch seconds
- `X-Nonce` — UUID v4 (one-time use)
- `X-Signature` — HMAC-SHA256(METHOD + PATH + timestamp + nonce + SHA256(body), sessionSecret)

The backend verifies the timestamp is within a 120-second window, the nonce has not been used before, and the signature matches. Login requests are exempt from signing.

### Session Management
- JWT-style bearer tokens stored in `sessionStorage` (default) or `localStorage` (remember-device)
- Remember-device sessions expire after **7 days** — TTL checked on restore
- Inactivity watchdog: warning modal at 25 min, auto-logout at 30 min
- 401 API responses trigger automatic logout

### RBAC
- Route guards check `meta.roles` against the user's `activeRole`
- Multi-role users can switch roles at runtime; switching resets all data stores
- Sidebar navigation is role-filtered

### Login Security
- Open-redirect prevention: redirect query parameter validated (must start with `/`, no `://`)
- Password strength indicator checks 5 dimensions: length >= 12, uppercase, lowercase, digit, special char
- Account lockout with countdown timer on repeated failures

### Rate Limiting
- HTTP 429 responses are intercepted and displayed as countdown toast notifications
- `Retry-After` header is parsed when available

### Logging
- Structured logger (`utils/logger.ts`) with severity levels (debug, info, warn, error)
- Debug output suppressed in production builds
- No tokens, passwords, PII, or session secrets are logged

## Testing

```bash
npm test           # Run all 76 tests via Vitest
```

| Layer | File | Coverage |
|-------|------|----------|
| Unit | `crypto.test.ts` | hashBody, createNonce, signRequest, sha256Hex |
| Unit | `pii.test.ts` | maskStudentId with various lengths and showRaw toggle |
| Unit | `toast.test.ts` | showToast, showError, handleApiError error-code mapping |
| Unit | `date.test.ts` | formatDateTime, formatDate, fromNow |
| Component | `DataTable.test.ts` | Columns, pagination, rowClick, skeleton, empty state, PII mask |
| Component | `FormBuilder.test.ts` | Multi-step nav, validation, submit emit |
| Component | `ImportWizard.test.ts` | File parsing, duplicate detection, invalid flagging, select filtering |
| Component | `VersionDiff.test.ts` | Dropdown selection, diff display, restore visibility |
| Router | `router.test.ts` | Auth redirect, wrong-role 403, login redirect, homePath resolution |
| Auth | `auth.test.ts` | Login/logout state, 7-day TTL, open-redirect validation |
