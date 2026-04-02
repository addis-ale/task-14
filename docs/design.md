# Design Document — Secure Exam Scheduling & Notification Management System

## 1. Overview

A fully offline-capable web platform for K–12 and district testing offices to plan exam sessions, manage candidate rosters, and keep stakeholders informed. The system runs entirely within an organization's intranet and requires **no public internet access**.

| Layer | Technology |
|---|---|
| Frontend | Vue.js 3 (Composition API) + Vite + Pinia + Vue Router |s
| Backend | Spring Boot 3 (Java 17+) |
| Database | MySQL 8 |
| Messaging (optional) | WeChat subscription messages (intranet-only) |
| Job Scheduling | Spring Scheduler with sharded queues |

---

## 2. High-Level Architecture

```
┌──────────────────────────────────────────────────────────┐
│                     Vue.js SPA (Browser)                 │
│  ┌────────┐ ┌──────────┐ ┌───────────┐ ┌─────────────┐  │
│  │Auth    │ │Scheduling│ │Roster Mgmt│ │Notifications│  │
│  │Module  │ │Module    │ │Module     │ │Module       │  │
│  └────┬───┘ └─────┬────┘ └─────┬─────┘ └──────┬──────┘  │
│       └───────────┴────────────┴───────────────┘         │
│                         Axios + Request Signing           │
└──────────────────────────┬───────────────────────────────┘
                           │ HTTPS / Intranet
┌──────────────────────────▼───────────────────────────────┐
│                  Spring Boot API Gateway                  │
│  ┌────────────────────────────────────────────────────┐  │
│  │ Filters: Replay-Guard · Rate-Limiter · RBAC/ABAC  │  │
│  └────────────────────────────────────────────────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐   │
│  │Auth      │ │Schedule  │ │Roster    │ │Notification│  │
│  │Service   │ │Service   │ │Service   │ │Service     │  │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └─────┬─────┘  │
│       └─────────────┴────────────┴─────────────┘         │
│                     Service Layer                         │
│  ┌──────────────────────────────────────────────────────┐│
│  │ Spring Data JPA · Audit Interceptor · Version Store  ││
│  └──────────────────────────┬───────────────────────────┘│
└─────────────────────────────┤────────────────────────────┘
                              │
                    ┌─────────▼─────────┐
                    │     MySQL 8       │
                    │  (Master Data,    │
                    │   Audit Logs,     │
                    │   Outbox, Jobs)   │
                    └───────────────────┘
```

---

## 3. Roles & Permissions (RBAC / ABAC)

### 3.1 Roles

| Role | Key Responsibilities |
|---|---|
| **Administrator** | Full system access; user management; account lock/unlock; compliance review approval; anti-cheat review |
| **Academic Affairs Coordinator** | Manage all schedules, rosters, proctor assignments across all grades/campuses; publish notifications; review flagged activities |
| **Homeroom Teacher** | View/edit rosters for own assigned classes; view schedules for own grade |
| **Subject Teacher** | View/edit subject-specific sessions and rosters for assigned courses |
| **Student** | View personal schedule; receive notifications; manage subscription preferences |

### 3.2 Permission Matrix

| Resource | Admin | Acad. Affairs | Homeroom Teacher | Subject Teacher | Student |
|---|:---:|:---:|:---:|:---:|:---:|
| User Management | CRUD | R | — | — | — |
| Rosters | CRUD | CRUD | RU (own class) | RU (own course) | R (self) |
| Exam Sessions | CRUD | CRUD | R (own grade) | R (own course) | R (self) |
| Campus / Rooms | CRUD | CRUD | R | R | — |
| Proctor Assignments | CRUD | CRUD | R | R | — |
| Notifications Publish | ✓ | ✓ | — | — | — |
| Notification Preferences | — | — | — | — | RU (self) |
| Compliance Review Queue | Approve/Reject | Approve/Reject | — | — | — |
| Anti-Cheat Review Queue | ✓ | ✓ | — | — | — |
| Audit Logs | R | R | — | — | — |
| Bulk Import/Export | ✓ | ✓ | ✓ (own class) | ✓ (own course) | — |
| Version History | R | R | R (own scope) | R (own scope) | — |

### 3.3 Row-Level Data Scope (ABAC)

Every data query is filtered by the user's scope attributes:

- **grade_id** — restricts Homeroom Teachers to their grade.
- **class_id** — restricts Homeroom Teachers to their classes.
- **course_id** — restricts Subject Teachers to their courses.
- **term_id** — restricts all role queries to the active academic term (with read-only access to prior terms).

Scope attributes are stored in a `user_scope` join table and injected into every repository query via a Spring AOP aspect.

### 3.4 Multi-Role Support

A single user account may hold multiple roles (e.g., a teacher who is also an Admin). The system supports:

- A `user_roles` many-to-many table.
- An **active role selector** in the UI header so the user can switch context per session.
- Permission checks are evaluated against the **currently active role only** to prevent privilege confusion.

---

## 4. Data Model (Entity-Relationship)

```
┌──────────────┐     ┌──────────────┐     ┌─────────────────┐
│   sys_user    │────<│  user_roles  │>────│    sys_role      │
│──────────────│     └──────────────┘     │─────────────────│
│ id           │                          │ id              │
│ username     │     ┌──────────────┐     │ name            │
│ password_hash│────<│  user_scope  │     │ description     │
│ salt         │     │──────────────│     └─────────────────┘
│ status       │     │ user_id      │
│ locked_until │     │ grade_id     │     ┌─────────────────┐
│ device_token │     │ class_id     │     │ role_permission  │
│ created_at   │     │ course_id    │     │─────────────────│
│ updated_at   │     │ term_id      │     │ role_id         │
└──────────────┘     └──────────────┘     │ resource        │
                                          │ action          │
                                          └─────────────────┘

┌──────────────┐     ┌──────────────┐     ┌─────────────────┐
│   campus     │────<│  exam_room   │>────│ room_assignment  │
│──────────────│     │──────────────│     │─────────────────│
│ id           │     │ id           │     │ session_id      │
│ name         │     │ campus_id    │     │ room_id         │
│ address      │     │ name         │     │ assigned_count  │
└──────────────┘     │ capacity     │     └─────────────────┘
                     └──────────────┘

┌──────────────────┐     ┌───────────────────┐
│  academic_term   │     │   exam_session     │
│──────────────────│     │───────────────────│
│ id               │────<│ id                │
│ name             │     │ term_id           │
│ start_date       │     │ subject_id        │
│ end_date         │     │ grade_id          │
│ is_active        │     │ date              │
└──────────────────┘     │ start_time        │
                         │ end_time          │
┌──────────────┐         │ status            │
│   subject    │────────>│ created_by        │
│──────────────│         │ version           │
│ id           │         └───────┬───────────┘
│ name         │                 │
│ grade_id     │     ┌───────────▼───────────┐
└──────────────┘     │  session_candidate    │
                     │───────────────────────│
                     │ session_id            │
                     │ student_id            │
                     │ seat_number           │
                     └───────────────────────┘

┌──────────────────┐
│ proctor_assign   │
│──────────────────│
│ id               │
│ session_id       │
│ room_id          │
│ proctor_user_id  │
│ time_slot_start  │
│ time_slot_end    │
└──────────────────┘

┌────────────────────┐    ┌───────────────────────┐
│   notification     │    │  notif_preference     │
│────────────────────│    │───────────────────────│
│ id                 │    │ id                    │
│ event_type         │    │ user_id               │
│ title              │    │ event_type            │
│ body               │    │ enabled               │
│ priority           │    │ dnd_start (TIME)      │
│ target_scope       │    │ dnd_end   (TIME)      │
│ status             │    └───────────────────────┘
│ published_by       │
│ compliance_status  │    ┌───────────────────────┐
│ created_at         │    │  notif_delivery       │
└────────┬───────────┘    │───────────────────────│
         │                │ id                    │
         └───────────────>│ notification_id       │
                          │ recipient_id          │
                          │ channel (app|wechat)  │
                          │ status                │
                          │ attempts              │
                          │ last_attempt_at       │
                          │ delivered_at          │
                          └───────────────────────┘

┌──────────────────────┐    ┌─────────────────────┐
│   entity_version     │    │     audit_log       │
│──────────────────────│    │─────────────────────│
│ id                   │    │ id                  │
│ entity_type          │    │ user_id             │
│ entity_id            │    │ action              │
│ term_id              │    │ resource            │
│ version_number       │    │ resource_id         │
│ snapshot_json        │    │ ip_address          │
│ changed_by           │    │ timestamp           │
│ changed_at           │    │ details_json        │
└──────────────────────┘    └─────────────────────┘

┌─────────────────────┐    ┌─────────────────────┐
│   job_record        │    │  login_attempt      │
│─────────────────────│    │─────────────────────│
│ id                  │    │ id                  │
│ job_type            │    │ user_id             │
│ dedup_key           │    │ ip_address          │
│ status              │    │ device_fingerprint  │
│ payload_json        │    │ success             │
│ attempts            │    │ attempted_at        │
│ next_retry_at       │    └─────────────────────┘
│ created_at          │
│ completed_at        │    ┌─────────────────────┐
│ error_message       │    │ anti_cheat_flag     │
└─────────────────────┘    │─────────────────────│
                           │ id                  │
┌─────────────────────┐    │ user_id             │
│  import_batch       │    │ flag_type           │
│─────────────────────│    │ details_json        │
│ id                  │    │ review_status       │
│ file_name           │    │ reviewer_id         │
│ entity_type         │    │ decision            │
│ total_rows          │    │ decided_at          │
│ valid_rows          │    └─────────────────────┘
│ invalid_rows        │
│ status              │    ┌─────────────────────┐
│ error_report_json   │    │ compliance_review   │
│ uploaded_by         │    │─────────────────────│
│ created_at          │    │ id                  │
└─────────────────────┘    │ content_type        │
                           │ content_id          │
                           │ status              │
                           │ reviewer_id         │
                           │ comments            │
                           │ decided_at          │
                           └─────────────────────┘

┌─────────────────────┐
│   auto_save_draft   │
│─────────────────────│
│ id                  │
│ user_id             │
│ form_key            │
│ draft_json          │
│ saved_at            │
└─────────────────────┘
```

---

## 5. Authentication & Session Management

### 5.1 Login Flow

1. User submits `username` + `password`.
2. Server verifies against salted hash (bcrypt, cost factor 12).
3. On success → issue a session token (opaque, stored server-side in `sys_session` table).
4. On failure → increment `login_attempt` counter for that user + IP.

### 5.2 Password Policy

| Rule | Value |
|---|---|
| Minimum length | 12 characters |
| Complexity | ≥ 1 uppercase, ≥ 1 lowercase, ≥ 1 digit, ≥ 1 special character |
| Storage | bcrypt with random salt |
| History | Last 5 passwords cannot be reused |

### 5.3 Session Rules

| Rule | Value |
|---|---|
| Idle timeout | 30 minutes |
| "Remember device" max | 7 days (managed machines only) |
| Concurrent sessions | Blocked by default; Admin can allow per-user |
| Device fingerprinting | Browser + OS hash stored in `login_attempt` |

### 5.4 Account Lockout

- **Trigger**: 5 consecutive failed login attempts within 30 minutes.
- **Duration**: 15-minute automatic lockout.
- **Admin override**: Administrator can manually unlock via user management.
- **Notification**: Locked accounts generate an audit log event.

### 5.5 Auto-Save Drafts

To prevent data loss on session expiry:

- Critical forms (roster editing, session creation, proctor assignment) auto-save drafts every 30 seconds to `auto_save_draft`.
- On next login, the user is prompted to resume or discard the draft.
- Drafts expire after 24 hours.

---

## 6. Security Controls

### 6.1 Request Signing & Replay Prevention

Every API request includes:

| Header | Purpose |
|---|---|
| `X-Timestamp` | Unix epoch seconds |
| `X-Nonce` | UUID v4, single-use |
| `X-Signature` | HMAC-SHA256(method + path + timestamp + nonce + body hash, secret) |

- Server rejects requests older than **120 seconds**.
- Nonce is stored in a MySQL table with TTL cleanup; duplicate nonces are rejected.

### 6.2 Rate Limiting

| Scope | Limit |
|---|---|
| Per user | 60 requests/minute |
| Per IP | 300 requests/minute |

Implemented via a Spring filter backed by a sliding-window counter in MySQL (or in-memory cache if available).

### 6.3 Input Sanitization

- All string inputs are sanitized server-side (strip HTML tags, SQL special characters).
- Parameterized queries only (no string concatenation in SQL).
- Request body size limited to 10 MB (for file uploads, 50 MB with multipart).

### 6.4 Sensitive Data Protection

| Measure | Details |
|---|---|
| **UI masking** | Student IDs display as `****1234` by default |
| **Unmasking** | Only roles with `VIEW_FULL_PII` permission see full IDs |
| **Encryption at rest** | Student ID and other PII columns encrypted with AES-256 |
| **Audit trail** | Every PII access logged |

### 6.5 Content Compliance Review

- Any notification or content targeted at students is routed to a **compliance review queue** before publishing.
- Admin or Academic Affairs must approve; until approved, content is not visible to students.
- Includes sensitive-content and minor-protection checks (health-related disclaimers, appropriate language).

---

## 7. Exam Scheduling Module

### 7.1 Session Creation Flow

1. User selects term → grade → subject → date/time → room(s).
2. **Conflict detection** runs before save:
   - **Student conflict**: checks if any student in the candidate list already has a session overlapping the same time slot.
   - **Room conflict**: checks if the room is already booked at that time.
   - **Proctor conflict**: checks if the assigned proctor is already assigned elsewhere at that time.
3. If conflicts exist → display detailed conflict report (which students/rooms/proctors overlap) and **block save**.
4. If clean → persist session + room assignments + candidate list.

### 7.2 Room Capacity Enforcement

- When assigning students to a room, `assigned_count` must not exceed `exam_room.capacity`.
- The UI shows remaining capacity in real-time.
- Bulk assignment stops and reports if capacity would be exceeded.

### 7.3 Proctor Assignment Constraints

- A proctor can only be assigned to **one room per time slot**.
- Constraint enforced at the database level (unique index on `proctor_user_id + time_slot range overlap`) and in the service layer.
- The UI shows a proctor's existing assignments when selecting time slots.

---

## 8. Roster Management Module

### 8.1 CRUD Operations

- Guided multi-step forms with inline validation.
- Immediate, readable error messages (not generic HTTP codes).
- Row-level access enforced: Homeroom Teachers see only their class; Subject Teachers see only their course.

### 8.2 Bulk Import / Export

| Feature | Details |
|---|---|
| Formats | CSV, XLSX |
| Preview step | Upload → parse → show preview table with row-level validation status |
| Duplicate detection | Highlight rows matching existing records (by student ID) |
| Invalid format detection | Flag malformed fields (dates, IDs, required fields) |
| Partial commit | User can deselect invalid rows and commit valid rows only |
| Error threshold | No hard fail; all rows validated and reported individually |
| Export | Filtered by current role's data scope |

### 8.3 Import Batch Tracking

Every import creates an `import_batch` record tracking:
- Total rows, valid rows, invalid rows.
- Error report JSON (row number + field + error message).
- Status: `PENDING → PREVIEWING → COMMITTED / CANCELLED`.

---

## 9. Version History & Auditing

### 9.1 Versioned Entities

Version history is enabled for the following critical entities only:

- `exam_session`
- `session_candidate` (roster)
- `proctor_assign`
- `room_assignment`

### 9.2 Snapshot Storage

- On every update, a JSON snapshot of the record's previous state is stored in `entity_version`.
- Snapshots are scoped to `term_id` for "as of" comparisons across terms.
- Users can view a diff between any two versions.
- **Restore**: Authorized users (Admin, Academic Affairs) can restore a previous version, which creates a new version entry.

### 9.3 Audit Logs

- Every write operation (create, update, delete) generates an `audit_log` entry.
- Logs include: user, action, resource type, resource ID, IP, timestamp, and a details JSON blob.
- **Retention**: 5 years, with automatic archival to a compressed archive table after 1 year.

---

## 10. Notification System

### 10.1 Event Types

| Event Type | Priority | Example |
|---|---|---|
| `SCHEDULE_CHANGE` | HIGH | "Your Math exam moved to Room 201" |
| `REVIEW_OUTCOME` | HIGH | "Your exam results are published" |
| `CHECKIN_REMINDER` | MEDIUM | "Exam starts in 30 minutes" |
| `GENERAL_ANNOUNCEMENT` | LOW | "Campus closed on Friday" |

### 10.2 Delivery Channels

1. **In-App Inbox** — Always available, works offline. Primary channel.
2. **WeChat Subscription Messages** — Optional, intranet-only. Used when the organization has deployed local WeChat integration.

If WeChat is unavailable, notifications automatically fall back to the in-app inbox.

### 10.3 Student Subscription Preferences

- **Defaults**: All essential event types (`SCHEDULE_CHANGE`, `REVIEW_OUTCOME`, `CHECKIN_REMINDER`) are enabled for new students.
- **Customization**: Students can opt in/out per event type via a settings page.
- **Do Not Disturb (DND)**: Students can set a DND window (e.g., 9:00 PM – 7:00 AM).
  - During DND, non-critical notifications are **held and delivered after DND ends**.
  - **HIGH priority** notifications bypass DND.

### 10.4 Delivery Failure Handling

- Delivery attempts stored in `notif_delivery`.
- **Retry policy**: Up to 3 attempts with exponential backoff (1 min → 5 min → 15 min).
- After 3 failures → status set to `FAILED`.
- Admin can manually retry via the notification management dashboard.

### 10.5 Compliance Gate

All notifications targeting students pass through the compliance review queue before delivery. Status flow:

```
DRAFT → PENDING_REVIEW → APPROVED → SENDING → SENT
                       → REJECTED (with comments)
```

---

## 11. Distributed Job Scheduling

### 11.1 Job Types

| Job Type | Description |
|---|---|
| `NOTIFICATION_SEND` | Send batch notifications to recipients |
| `BULK_IMPORT` | Process uploaded CSV/XLSX files |
| `DATA_CHECK` | Periodic validation of data integrity |
| `ARCHIVE` | Move old audit logs to archive table |
| `DND_RELEASE` | Deliver held notifications after DND window |

### 11.2 Execution Model

- Multiple Spring Boot nodes can run concurrently.
- Jobs are distributed via **sharded queues** (shard key = hash of job type + target entity).
- **Idempotency**: Every job carries a `dedup_key`; duplicate keys are skipped.
- **Retry**: Up to 3 attempts with exponential backoff (30 s → 2 min → 8 min).
- **Dead letter**: After 3 failures, job is marked `FAILED` and visible in the admin monitor.

### 11.3 Admin Job Monitor

- Visual dashboard showing: job type, status, created time, attempts, error message.
- Filter by status (running / completed / failed / pending).
- Manual re-run button for failed jobs.

---

## 12. Anti-Cheat & Fraud Detection

### 12.1 Detection Rules

Applicable to any ranking-style or score-based views:

| Rule | Trigger |
|---|---|
| Activity burst | > 20 actions in 1 minute from same user |
| Identical submissions | Same answer pattern submitted 3+ times |
| Abnormal score delta | Score change > 2 standard deviations from class mean |

### 12.2 Review Workflow

1. System flags suspicious activity → creates `anti_cheat_flag` record.
2. Flag appears in the **Anti-Cheat Review Queue** (visible to Admin and Academic Affairs).
3. Reviewer examines details, assigns decision: `CLEARED` or `CONFIRMED_FRAUD`.
4. **No automated punitive action** — all decisions require human review.
5. Decision and reviewer ID are recorded for audit.

---

## 13. Offline & Intranet Strategy

### 13.1 Architecture Principles

- The entire stack runs on the organization's **local network**. No external API calls required.
- Vue.js SPA is served from a local web server; API calls go to local Spring Boot instances.
- MySQL runs on a local database server.

### 13.2 Multi-Node Sync

For organizations with multiple campuses/nodes on the same intranet:

- **Primary-replica MySQL** topology for read scaling.
- **Scheduled sync jobs** run periodically (configurable interval, default 15 minutes) to batch-sync data between nodes.
- Conflict resolution: last-write-wins with version number tie-breaking. Conflicts are logged for admin review.

### 13.3 WeChat Fallback

If the intranet WeChat relay is down or not configured:
- All notifications are delivered to the in-app inbox.
- No feature degradation — the system remains fully functional.

---

## 14. Frontend Architecture

### 14.1 Project Structure

```
src/
├── api/              # Axios instances with request signing interceptor
├── assets/           # Static assets, fonts, icons
├── components/       # Shared UI components
│   ├── DataTable/    # Configurable table with sort, filter, pagination
│   ├── FormBuilder/  # Dynamic form renderer with validation
│   ├── ImportWizard/ # Bulk import preview & commit flow
│   └── VersionDiff/  # Side-by-side version comparison
├── composables/      # Shared composition functions (useAuth, useRBAC, etc.)
├── layouts/          # MainLayout with role-based sidebar
├── router/           # Vue Router with per-route RBAC guards
├── stores/           # Pinia stores (auth, session, notifications, etc.)
├── views/            # Page-level components by module
│   ├── auth/
│   ├── dashboard/
│   ├── scheduling/
│   ├── rosters/
│   ├── notifications/
│   ├── admin/
│   └── student/
└── utils/            # Helpers: crypto (signing), date, masking
```

### 14.2 Role-Based Navigation

- The sidebar menu is dynamically generated from the active role's permissions.
- Routes are protected with a `beforeEach` guard that checks the user's active role against the route's required permissions.
- Unauthorized access attempts redirect to a "403 Forbidden" page.

### 14.3 Request Signing (Client-Side)

An Axios request interceptor:

1. Reads the current timestamp (epoch seconds).
2. Generates a UUID v4 nonce.
3. Computes HMAC-SHA256 over `method + path + timestamp + nonce + bodyHash` using a session-derived secret.
4. Attaches `X-Timestamp`, `X-Nonce`, `X-Signature` headers.

---

## 15. Backend Architecture

### 15.1 Package Structure

```
com.exam.system
├── config/              # Security, CORS, rate-limiter, job scheduler configs
├── controller/          # REST controllers per module
├── service/             # Business logic
│   ├── impl/
├── repository/          # Spring Data JPA repositories
├── entity/              # JPA entities
├── dto/                 # Request/Response DTOs
├── security/
│   ├── filter/          # ReplayGuardFilter, RateLimitFilter, AuthFilter
│   ├── rbac/            # RBAC/ABAC evaluator, DataScopeAspect
│   └── crypto/          # Password hashing, AES encryption, HMAC verification
├── job/                 # Scheduled job implementations
├── audit/               # Audit log interceptor
├── version/             # Entity versioning service
├── notification/        # Notification send, delivery, preference services
├── import_export/       # Bulk import/export processing
└── exception/           # Global exception handler, error codes
```

### 15.2 Key Design Patterns

| Pattern | Usage |
|---|---|
| **AOP Aspects** | Data scope injection, audit logging, version snapshotting |
| **Strategy Pattern** | Notification channel selection (in-app vs. WeChat) |
| **Template Method** | Bulk import processing (parse → validate → preview → commit) |
| **Observer / Event** | Spring ApplicationEvents for audit log, notification triggers |
| **Outbox Pattern** | Notification outbox table ensures reliable delivery even if the send service is temporarily down |

---

## 16. Business Logic Decisions (from Questions)

| # | Topic | Decision |
|---|---|---|
| 1 | Exam session conflicts | Block save if student, room, or proctor overlap detected. Show conflict details. |
| 2 | Roster editing rights | Admin + Academic Affairs: all. Teachers: own class/course only (row-level RBAC). |
| 3 | Version history scope | Enabled for rosters, schedules, proctor assignments, room assignments only. |
| 4 | Bulk import error threshold | No hard fail. Preview all rows; user commits valid rows and skips invalid ones. |
| 5 | DND notification priority | HIGH priority bypasses DND. Others are held and delivered after DND ends. |
| 6 | Student subscription defaults | All essential types enabled by default. Users can customize later. |
| 7 | Offline sync | Primary-replica MySQL + scheduled sync jobs within intranet. |
| 8 | Proctor double-booking | One proctor per time slot enforced at DB + service layer. |
| 9 | Account lock recovery | Auto-unlock after 15 min + admin manual unlock. |
| 10 | Session expiry + unsaved work | Auto-save drafts every 30 s for critical forms. Resume prompt on next login. |
| 11 | Data retention | 5-year retention. Archive after 1 year. |
| 12 | Multi-role users | Supported. Active role selector per session. |
| 13 | Room capacity | Enforced before assignment. UI shows remaining capacity. |
| 14 | Notification delivery failure | 3 retries with exponential backoff → FAILED status → admin manual retry. |
| 15 | Sensitive data masking | Masked by default. Full access via `VIEW_FULL_PII` permission flag. |
| 16 | Anti-cheat review | Academic Affairs / Admin review queue. No automated punishment. |

---

## 17. Non-Functional Requirements

| Requirement | Target |
|---|---|
| Response time (API) | < 500 ms p95 |
| Concurrent users | 500+ per node |
| Database size | Up to 10 GB per academic year |
| Availability | 99.5% during exam periods |
| Browser support | Chrome 90+, Edge 90+, Firefox 90+ |
| Accessibility | WCAG 2.1 AA |
| Localization | Chinese (Simplified) primary; English secondary |
