# API Specification — Secure Exam Scheduling & Notification Management System

## 1. General Conventions

### 1.1 Base URL

```
https://{intranet-host}:{port}/api/v1
```

### 1.2 Authentication

All endpoints (except `POST /auth/login`) require a valid session token in the `Authorization` header:

```
Authorization: Bearer {session-token}
```

### 1.3 Request Signing

Every request must include the following headers for replay prevention:

| Header | Type | Description |
|---|---|---|
| `X-Timestamp` | `integer` | Unix epoch seconds |
| `X-Nonce` | `string` | UUID v4, single-use |
| `X-Signature` | `string` | HMAC-SHA256(method + path + timestamp + nonce + bodyHash, secret) |

Server rejects requests with timestamp older than **120 seconds** or with a previously seen nonce.

### 1.4 Rate Limiting

| Scope | Limit | Header |
|---|---|---|
| Per user | 60 req/min | `X-RateLimit-User-Remaining` |
| Per IP | 300 req/min | `X-RateLimit-IP-Remaining` |

When exceeded, the server returns `429 Too Many Requests` with a `Retry-After` header.

### 1.5 Standard Response Envelope

All responses follow this structure:

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1711929600
}
```

### 1.6 Error Response

```json
{
  "code": 40001,
  "message": "Validation failed: student ID is required",
  "errors": [
    { "field": "studentId", "message": "must not be blank" }
  ],
  "timestamp": 1711929600
}
```

### 1.7 Pagination

Paginated endpoints accept:

| Param | Type | Default | Description |
|---|---|---|---|
| `page` | integer | 1 | Page number (1-indexed) |
| `size` | integer | 20 | Items per page (max 100) |
| `sort` | string | — | Field name, e.g. `createdAt` |
| `order` | string | `asc` | `asc` or `desc` |

Response includes:

```json
{
  "data": {
    "items": [...],
    "pagination": {
      "page": 1,
      "size": 20,
      "totalItems": 150,
      "totalPages": 8
    }
  }
}
```

### 1.8 Common HTTP Status Codes

| Code | Meaning |
|---|---|
| 200 | Success |
| 201 | Created |
| 204 | No Content (successful delete) |
| 400 | Bad Request / Validation Error |
| 401 | Unauthorized (missing or invalid session) |
| 403 | Forbidden (insufficient permissions) |
| 404 | Resource Not Found |
| 409 | Conflict (duplicate, scheduling conflict) |
| 423 | Locked (account locked) |
| 429 | Rate Limit Exceeded |
| 500 | Internal Server Error |

---

## 2. Authentication & Session APIs

### 2.1 Login

```
POST /auth/login
```

**Request Body:**

```json
{
  "username": "string (required)",
  "password": "string (required, min 12 chars)",
  "rememberDevice": "boolean (optional, default false)"
}
```

**Success Response (200):**

```json
{
  "code": 200,
  "data": {
    "token": "uuid-session-token",
    "expiresIn": 1800,
    "user": {
      "id": 1,
      "username": "admin01",
      "roles": ["ADMIN", "ACADEMIC_AFFAIRS"],
      "activeRole": "ADMIN",
      "scopes": {
        "gradeIds": [1, 2, 3],
        "classIds": [],
        "courseIds": [],
        "termId": 5
      }
    }
  }
}
```

**Error Responses:**

| Code | Scenario |
|---|---|
| 401 | Invalid credentials |
| 423 | Account locked (includes `lockedUntil` timestamp) |

---

### 2.2 Logout

```
POST /auth/logout
```

**Response:** `204 No Content`

---

### 2.3 Switch Active Role

```
PUT /auth/active-role
```

**Request Body:**

```json
{
  "role": "HOMEROOM_TEACHER"
}
```

**Response (200):**

```json
{
  "code": 200,
  "data": {
    "activeRole": "HOMEROOM_TEACHER",
    "scopes": {
      "gradeIds": [2],
      "classIds": [4, 5],
      "courseIds": [],
      "termId": 5
    }
  }
}
```

---

### 2.4 Get Current Session

```
GET /auth/me
```

**Response (200):** Returns the same user object as login.

---

### 2.5 Change Password

```
PUT /auth/password
```

**Request Body:**

```json
{
  "currentPassword": "string (required)",
  "newPassword": "string (required, min 12 chars, complexity rules)"
}
```

**Error Responses:**

| Code | Scenario |
|---|---|
| 400 | New password does not meet complexity rules |
| 400 | New password matches one of last 5 passwords |
| 401 | Current password is incorrect |

---

## 3. User Management APIs

> **Roles Required:** `ADMIN`

### 3.1 List Users

```
GET /users?page=1&size=20&role=STUDENT&search=john
```

| Param | Type | Description |
|---|---|---|
| `role` | string | Filter by role |
| `status` | string | `ACTIVE`, `LOCKED`, `DISABLED` |
| `search` | string | Search by username or display name |

**Response (200):** Paginated list of user summaries (PII masked by default).

---

### 3.2 Get User Detail

```
GET /users/{userId}
```

**Response (200):**

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "student001",
    "displayName": "Zhang Wei",
    "studentId": "****5678",
    "roles": ["STUDENT"],
    "scopes": { ... },
    "status": "ACTIVE",
    "lockedUntil": null,
    "createdAt": "2025-09-01T08:00:00Z",
    "updatedAt": "2025-12-15T10:30:00Z"
  }
}
```

> Student ID is masked unless the caller has `VIEW_FULL_PII` permission.

---

### 3.3 Create User

```
POST /users
```

**Request Body:**

```json
{
  "username": "string (required, unique)",
  "password": "string (required, min 12 chars)",
  "displayName": "string (required)",
  "studentId": "string (optional, for students)",
  "roles": ["STUDENT"],
  "scopes": {
    "gradeIds": [2],
    "classIds": [4],
    "courseIds": [],
    "termId": 5
  }
}
```

**Response:** `201 Created` with user object.

---

### 3.4 Update User

```
PUT /users/{userId}
```

**Request Body:** Partial update, same fields as create (except `password`).

**Response:** `200` with updated user object.

---

### 3.5 Delete User

```
DELETE /users/{userId}
```

**Response:** `204 No Content`

> Soft delete — sets status to `DISABLED`.

---

### 3.6 Unlock User Account

```
POST /users/{userId}/unlock
```

**Response:** `200` with updated user object (lockedUntil cleared).

---

### 3.7 Toggle Concurrent Session Permission

```
PUT /users/{userId}/concurrent-sessions
```

**Request Body:**

```json
{
  "allowed": true
}
```

---

## 4. Academic Term APIs

> **Roles Required:** `ADMIN`, `ACADEMIC_AFFAIRS`

### 4.1 List Terms

```
GET /terms?page=1&size=10
```

**Response (200):** Paginated list of terms.

---

### 4.2 Get Term

```
GET /terms/{termId}
```

---

### 4.3 Create Term

```
POST /terms
```

**Request Body:**

```json
{
  "name": "2025-2026 Fall Semester",
  "startDate": "2025-09-01",
  "endDate": "2026-01-15",
  "isActive": true
}
```

> Setting `isActive: true` deactivates the currently active term.

---

### 4.4 Update Term

```
PUT /terms/{termId}
```

---

### 4.5 Delete Term

```
DELETE /terms/{termId}
```

> Only allowed if no exam sessions reference this term.

---

## 5. Campus & Exam Room APIs

> **Roles Required:** `ADMIN`, `ACADEMIC_AFFAIRS` for write; all staff for read.

### 5.1 List Campuses

```
GET /campuses?page=1&size=20
```

---

### 5.2 Create Campus

```
POST /campuses
```

**Request Body:**

```json
{
  "name": "Main Campus",
  "address": "123 Education Road"
}
```

---

### 5.3 Update Campus

```
PUT /campuses/{campusId}
```

---

### 5.4 Delete Campus

```
DELETE /campuses/{campusId}
```

---

### 5.5 List Rooms for Campus

```
GET /campuses/{campusId}/rooms?page=1&size=20
```

**Response item:**

```json
{
  "id": 1,
  "name": "Room A-301",
  "capacity": 40,
  "campusId": 1,
  "campusName": "Main Campus"
}
```

---

### 5.6 Create Room

```
POST /campuses/{campusId}/rooms
```

**Request Body:**

```json
{
  "name": "Room A-301",
  "capacity": 40
}
```

---

### 5.7 Update Room

```
PUT /rooms/{roomId}
```

---

### 5.8 Delete Room

```
DELETE /rooms/{roomId}
```

---

## 6. Subject APIs

> **Roles Required:** `ADMIN`, `ACADEMIC_AFFAIRS` for write; all staff for read.

### 6.1 List Subjects

```
GET /subjects?gradeId=2&page=1&size=20
```

---

### 6.2 Create Subject

```
POST /subjects
```

**Request Body:**

```json
{
  "name": "Mathematics",
  "gradeId": 2
}
```

---

### 6.3 Update Subject

```
PUT /subjects/{subjectId}
```

---

### 6.4 Delete Subject

```
DELETE /subjects/{subjectId}
```

---

## 7. Exam Session APIs

> **Roles Required:** `ADMIN`, `ACADEMIC_AFFAIRS` for write; scoped read for teachers and students.

### 7.1 List Exam Sessions

```
GET /sessions?termId=5&gradeId=2&subjectId=3&date=2025-12-20&page=1&size=20
```

**Response item:**

```json
{
  "id": 1,
  "termId": 5,
  "termName": "2025-2026 Fall",
  "subjectId": 3,
  "subjectName": "Mathematics",
  "gradeId": 2,
  "date": "2025-12-20",
  "startTime": "09:00",
  "endTime": "11:00",
  "status": "SCHEDULED",
  "rooms": [
    { "roomId": 1, "roomName": "A-301", "assignedCount": 35, "capacity": 40 }
  ],
  "candidateCount": 70,
  "version": 3,
  "createdBy": "admin01",
  "createdAt": "2025-11-01T10:00:00Z"
}
```

---

### 7.2 Get Exam Session Detail

```
GET /sessions/{sessionId}
```

Returns full session data including candidate list and proctor assignments.

---

### 7.3 Create Exam Session

```
POST /sessions
```

**Request Body:**

```json
{
  "termId": 5,
  "subjectId": 3,
  "gradeId": 2,
  "date": "2025-12-20",
  "startTime": "09:00",
  "endTime": "11:00",
  "roomIds": [1, 2],
  "candidateStudentIds": [101, 102, 103]
}
```

**Conflict Response (409):**

```json
{
  "code": 40901,
  "message": "Scheduling conflicts detected",
  "data": {
    "studentConflicts": [
      {
        "studentId": 101,
        "conflictingSessionId": 5,
        "conflictingSubject": "English",
        "time": "09:00-10:30"
      }
    ],
    "roomConflicts": [
      {
        "roomId": 1,
        "conflictingSessionId": 5,
        "time": "08:30-10:00"
      }
    ],
    "proctorConflicts": []
  }
}
```

---

### 7.4 Update Exam Session

```
PUT /sessions/{sessionId}
```

Same body as create. Triggers version snapshot.

---

### 7.5 Delete Exam Session

```
DELETE /sessions/{sessionId}
```

**Response:** `204 No Content`

---

### 7.6 Conflict Check (Dry Run)

```
POST /sessions/conflict-check
```

Same body as create. Returns conflict details without saving.

---

## 8. Session Candidates (Roster) APIs

### 8.1 List Candidates for Session

```
GET /sessions/{sessionId}/candidates?page=1&size=50
```

**Response item:**

```json
{
  "studentId": 101,
  "studentName": "Li Ming",
  "maskedStudentNumber": "****5678",
  "classId": 4,
  "className": "Class 2-1",
  "seatNumber": 15,
  "roomId": 1,
  "roomName": "A-301"
}
```

---

### 8.2 Add Candidates to Session

```
POST /sessions/{sessionId}/candidates
```

**Request Body:**

```json
{
  "studentIds": [101, 102, 103],
  "roomId": 1
}
```

**Error (409):** Room capacity exceeded.

---

### 8.3 Remove Candidate from Session

```
DELETE /sessions/{sessionId}/candidates/{studentId}
```

---

### 8.4 Update Seat Assignment

```
PUT /sessions/{sessionId}/candidates/{studentId}/seat
```

**Request Body:**

```json
{
  "seatNumber": 22,
  "roomId": 1
}
```

---

## 9. Proctor Assignment APIs

### 9.1 List Proctors for Session

```
GET /sessions/{sessionId}/proctors
```

**Response item:**

```json
{
  "id": 1,
  "proctorUserId": 50,
  "proctorName": "Wang Jing",
  "roomId": 1,
  "roomName": "A-301",
  "timeSlotStart": "09:00",
  "timeSlotEnd": "11:00"
}
```

---

### 9.2 Assign Proctor

```
POST /sessions/{sessionId}/proctors
```

**Request Body:**

```json
{
  "proctorUserId": 50,
  "roomId": 1,
  "timeSlotStart": "09:00",
  "timeSlotEnd": "11:00"
}
```

**Error (409):** Proctor already assigned to another room in the same time slot.

---

### 9.3 Remove Proctor Assignment

```
DELETE /sessions/{sessionId}/proctors/{assignmentId}
```

---

### 9.4 Get Proctor Schedule

```
GET /proctors/{userId}/schedule?termId=5&date=2025-12-20
```

Returns all proctor assignments for the given user, useful for conflict visualization.

---

## 10. Bulk Import / Export APIs

### 10.1 Upload Import File

```
POST /import/upload
Content-Type: multipart/form-data
```

| Field | Type | Description |
|---|---|---|
| `file` | file | CSV or XLSX file (max 50 MB) |
| `entityType` | string | `STUDENT_ROSTER`, `SESSION_CANDIDATE`, `PROCTOR_ASSIGNMENT` |

**Response (200):**

```json
{
  "code": 200,
  "data": {
    "batchId": "uuid",
    "totalRows": 150,
    "validRows": 142,
    "invalidRows": 8,
    "preview": [
      {
        "rowNumber": 1,
        "data": { "name": "Li Ming", "studentId": "S12345" },
        "status": "VALID",
        "errors": []
      },
      {
        "rowNumber": 5,
        "data": { "name": "", "studentId": "INVALID" },
        "status": "INVALID",
        "errors": [
          { "field": "name", "message": "must not be blank" },
          { "field": "studentId", "message": "invalid format" }
        ]
      }
    ],
    "duplicates": [
      { "rowNumber": 12, "existingRecordId": 45, "field": "studentId" }
    ]
  }
}
```

---

### 10.2 Commit Import

```
POST /import/{batchId}/commit
```

**Request Body:**

```json
{
  "skipInvalidRows": true
}
```

**Response (200):**

```json
{
  "code": 200,
  "data": {
    "batchId": "uuid",
    "committedRows": 142,
    "skippedRows": 8,
    "status": "COMMITTED"
  }
}
```

---

### 10.3 Cancel Import

```
POST /import/{batchId}/cancel
```

---

### 10.4 Get Import Batch Status

```
GET /import/{batchId}
```

---

### 10.5 List Import History

```
GET /import?entityType=STUDENT_ROSTER&page=1&size=20
```

---

### 10.6 Export Data

```
GET /export/{entityType}?format=csv&termId=5&gradeId=2
```

| Param | Type | Values |
|---|---|---|
| `entityType` | string | `STUDENT_ROSTER`, `SESSION_CANDIDATE`, `PROCTOR_ASSIGNMENT`, `EXAM_SESSION` |
| `format` | string | `csv`, `xlsx` |
| Additional filters | varies | `termId`, `gradeId`, `classId`, `subjectId` |

**Response:** File download with appropriate `Content-Type` and `Content-Disposition` headers.

> Export data is scoped to the caller's RBAC permissions.

---

## 11. Version History APIs

### 11.1 List Versions for Entity

```
GET /versions?entityType=EXAM_SESSION&entityId=1&page=1&size=20
```

**Response item:**

```json
{
  "id": 1,
  "entityType": "EXAM_SESSION",
  "entityId": 1,
  "versionNumber": 3,
  "termId": 5,
  "changedBy": "admin01",
  "changedAt": "2025-11-15T14:30:00Z",
  "snapshotPreview": {
    "date": "2025-12-20",
    "startTime": "09:00",
    "status": "SCHEDULED"
  }
}
```

---

### 11.2 Get Version Snapshot

```
GET /versions/{versionId}
```

Returns full `snapshot_json` for that version.

---

### 11.3 Compare Versions

```
GET /versions/compare?versionA=2&versionB=3
```

**Response (200):**

```json
{
  "code": 200,
  "data": {
    "entityType": "EXAM_SESSION",
    "entityId": 1,
    "versionA": 2,
    "versionB": 3,
    "diff": [
      {
        "field": "startTime",
        "oldValue": "08:30",
        "newValue": "09:00"
      },
      {
        "field": "rooms",
        "oldValue": [1],
        "newValue": [1, 2]
      }
    ]
  }
}
```

---

### 11.4 Restore Version

```
POST /versions/{versionId}/restore
```

> **Roles Required:** `ADMIN`, `ACADEMIC_AFFAIRS`

Restores the entity to the state captured in the snapshot. Creates a new version entry for audit trail.

**Response (200):** Updated entity object with incremented version number.

---

## 12. Notification APIs

### 12.1 List Notifications (Admin/Publisher View)

```
GET /notifications?eventType=SCHEDULE_CHANGE&status=APPROVED&page=1&size=20
```

---

### 12.2 Get Notification Detail

```
GET /notifications/{notificationId}
```

**Response (200):**

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "eventType": "SCHEDULE_CHANGE",
    "title": "Math Exam Rescheduled",
    "body": "Your Mathematics exam has been moved to Room A-301 on Dec 21.",
    "priority": "HIGH",
    "targetScope": { "gradeId": 2, "subjectId": 3 },
    "complianceStatus": "APPROVED",
    "status": "SENT",
    "publishedBy": "coordinator01",
    "createdAt": "2025-12-10T08:00:00Z",
    "deliveryStats": {
      "total": 70,
      "delivered": 68,
      "pending": 1,
      "failed": 1
    }
  }
}
```

---

### 12.3 Create Notification (Draft)

```
POST /notifications
```

**Request Body:**

```json
{
  "eventType": "SCHEDULE_CHANGE",
  "title": "Math Exam Rescheduled",
  "body": "Your Mathematics exam has been moved to Room A-301 on Dec 21.",
  "priority": "HIGH",
  "targetScope": {
    "gradeId": 2,
    "subjectId": 3
  }
}
```

**Response:** `201 Created` — notification in `DRAFT` status.

---

### 12.4 Submit for Compliance Review

```
POST /notifications/{notificationId}/submit-review
```

Transitions status from `DRAFT` → `PENDING_REVIEW`.

---

### 12.5 Publish Notification (After Approval)

```
POST /notifications/{notificationId}/publish
```

> Only allowed when `complianceStatus == APPROVED`.

Triggers the notification send job.

---

### 12.6 Delete Notification

```
DELETE /notifications/{notificationId}
```

> Only allowed for `DRAFT` or `REJECTED` status.

---

## 13. In-App Inbox APIs (Student View)

### 13.1 List Inbox Messages

```
GET /inbox?page=1&size=20&read=false
```

**Response item:**

```json
{
  "id": 1,
  "notificationId": 5,
  "eventType": "SCHEDULE_CHANGE",
  "title": "Math Exam Rescheduled",
  "body": "Your Mathematics exam has been moved...",
  "priority": "HIGH",
  "read": false,
  "deliveredAt": "2025-12-10T08:05:00Z"
}
```

---

### 13.2 Mark as Read

```
PUT /inbox/{deliveryId}/read
```

---

### 13.3 Mark All as Read

```
PUT /inbox/read-all
```

---

### 13.4 Get Unread Count

```
GET /inbox/unread-count
```

**Response:**

```json
{
  "code": 200,
  "data": { "count": 3 }
}
```

---

## 14. Notification Preference APIs

### 14.1 Get My Preferences

```
GET /notification-preferences
```

**Response (200):**

```json
{
  "code": 200,
  "data": {
    "preferences": [
      {
        "eventType": "SCHEDULE_CHANGE",
        "enabled": true
      },
      {
        "eventType": "REVIEW_OUTCOME",
        "enabled": true
      },
      {
        "eventType": "CHECKIN_REMINDER",
        "enabled": true
      },
      {
        "eventType": "GENERAL_ANNOUNCEMENT",
        "enabled": false
      }
    ],
    "dndStart": "21:00",
    "dndEnd": "07:00"
  }
}
```

---

### 14.2 Update Preferences

```
PUT /notification-preferences
```

**Request Body:**

```json
{
  "preferences": [
    { "eventType": "GENERAL_ANNOUNCEMENT", "enabled": true }
  ],
  "dndStart": "22:00",
  "dndEnd": "06:30"
}
```

---

## 15. Compliance Review APIs

> **Roles Required:** `ADMIN`, `ACADEMIC_AFFAIRS`

### 15.1 List Pending Reviews

```
GET /compliance-reviews?status=PENDING&page=1&size=20
```

---

### 15.2 Get Review Detail

```
GET /compliance-reviews/{reviewId}
```

**Response (200):**

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "contentType": "NOTIFICATION",
    "contentId": 5,
    "contentPreview": {
      "title": "Math Exam Rescheduled",
      "body": "Your Mathematics exam has been moved..."
    },
    "status": "PENDING",
    "reviewerId": null,
    "comments": null,
    "createdAt": "2025-12-10T08:00:00Z"
  }
}
```

---

### 15.3 Approve Review

```
POST /compliance-reviews/{reviewId}/approve
```

**Request Body:**

```json
{
  "comments": "Content verified. Approved for student visibility."
}
```

---

### 15.4 Reject Review

```
POST /compliance-reviews/{reviewId}/reject
```

**Request Body:**

```json
{
  "comments": "Contains inappropriate language in line 3. Please revise."
}
```

---

## 16. Anti-Cheat Review APIs

> **Roles Required:** `ADMIN`, `ACADEMIC_AFFAIRS`

### 16.1 List Flagged Activities

```
GET /anti-cheat/flags?reviewStatus=PENDING&page=1&size=20
```

**Response item:**

```json
{
  "id": 1,
  "userId": 101,
  "userName": "Li Ming",
  "flagType": "ACTIVITY_BURST",
  "details": {
    "actionCount": 25,
    "timeWindowSeconds": 60,
    "actions": ["submit_answer", "submit_answer", "..."]
  },
  "reviewStatus": "PENDING",
  "reviewerId": null,
  "decision": null,
  "createdAt": "2025-12-20T09:15:00Z"
}
```

---

### 16.2 Get Flag Detail

```
GET /anti-cheat/flags/{flagId}
```

---

### 16.3 Review Flag

```
POST /anti-cheat/flags/{flagId}/review
```

**Request Body:**

```json
{
  "decision": "CLEARED",
  "comments": "Activity spike was due to network reconnection causing batch submission."
}
```

| Decision Values | Meaning |
|---|---|
| `CLEARED` | No fraud detected, flag dismissed |
| `CONFIRMED_FRAUD` | Suspicious activity confirmed, flagged for record |

---

## 17. Audit Log APIs

> **Roles Required:** `ADMIN`, `ACADEMIC_AFFAIRS` (read-only)

### 17.1 List Audit Logs

```
GET /audit-logs?userId=1&resource=EXAM_SESSION&action=UPDATE&from=2025-12-01&to=2025-12-31&page=1&size=50
```

| Param | Type | Description |
|---|---|---|
| `userId` | integer | Filter by user |
| `resource` | string | Entity type filter |
| `action` | string | `CREATE`, `UPDATE`, `DELETE`, `LOGIN`, `EXPORT` |
| `from` | date | Start date |
| `to` | date | End date |

**Response item:**

```json
{
  "id": 1,
  "userId": 1,
  "userName": "admin01",
  "action": "UPDATE",
  "resource": "EXAM_SESSION",
  "resourceId": 5,
  "ipAddress": "192.168.1.100",
  "timestamp": "2025-12-15T10:30:00Z",
  "details": {
    "changedFields": ["startTime", "rooms"],
    "previousVersion": 2,
    "newVersion": 3
  }
}
```

---

## 18. Job Monitor APIs

> **Roles Required:** `ADMIN`

### 18.1 List Jobs

```
GET /jobs?jobType=NOTIFICATION_SEND&status=FAILED&page=1&size=20
```

**Response item:**

```json
{
  "id": 1,
  "jobType": "NOTIFICATION_SEND",
  "dedupKey": "notif-5-batch-1",
  "status": "FAILED",
  "attempts": 3,
  "nextRetryAt": null,
  "errorMessage": "WeChat relay unreachable after 3 attempts",
  "createdAt": "2025-12-10T08:05:00Z",
  "completedAt": null
}
```

---

### 18.2 Get Job Detail

```
GET /jobs/{jobId}
```

Returns full `payload_json` and execution history.

---

### 18.3 Retry Failed Job

```
POST /jobs/{jobId}/retry
```

Resets attempts to 0 and re-queues the job.

---

## 19. Auto-Save Draft APIs

### 19.1 Save Draft

```
PUT /drafts/{formKey}
```

**Request Body:**

```json
{
  "data": {
    "termId": 5,
    "subjectId": 3,
    "startTime": "09:00",
    "partialFields": "..."
  }
}
```

| `formKey` examples | Description |
|---|---|
| `session-create` | Exam session creation form |
| `roster-edit-{sessionId}` | Roster editing for a specific session |
| `proctor-assign-{sessionId}` | Proctor assignment form |

---

### 19.2 Get Draft

```
GET /drafts/{formKey}
```

**Response (200):**

```json
{
  "code": 200,
  "data": {
    "formKey": "session-create",
    "data": { ... },
    "savedAt": "2025-12-15T10:28:00Z"
  }
}
```

**Response (404):** No draft exists for this form.

---

### 19.3 Delete Draft

```
DELETE /drafts/{formKey}
```

---

## 20. Dashboard / Student Schedule API

### 20.1 My Upcoming Exams (Student)

```
GET /my/exams?termId=5
```

**Response (200):**

```json
{
  "code": 200,
  "data": {
    "items": [
      {
        "sessionId": 1,
        "subject": "Mathematics",
        "date": "2025-12-20",
        "startTime": "09:00",
        "endTime": "11:00",
        "room": "A-301",
        "seatNumber": 15,
        "campus": "Main Campus"
      }
    ]
  }
}
```

---

### 20.2 Dashboard Stats (Admin / Academic Affairs)

```
GET /dashboard/stats?termId=5
```

**Response (200):**

```json
{
  "code": 200,
  "data": {
    "totalSessions": 45,
    "upcomingSessions": 12,
    "totalStudents": 850,
    "pendingReviews": 3,
    "failedJobs": 1,
    "pendingAntiCheatFlags": 2,
    "recentActivity": [
      {
        "action": "Session Created",
        "user": "coordinator01",
        "timestamp": "2025-12-15T10:30:00Z"
      }
    ]
  }
}
```

---

## 21. Notification Delivery Status API

> **Roles Required:** `ADMIN`, `ACADEMIC_AFFAIRS`

### 21.1 List Deliveries for Notification

```
GET /notifications/{notificationId}/deliveries?status=FAILED&page=1&size=50
```

**Response item:**

```json
{
  "id": 1,
  "recipientId": 101,
  "recipientName": "Li Ming",
  "channel": "APP",
  "status": "DELIVERED",
  "attempts": 1,
  "lastAttemptAt": "2025-12-10T08:05:00Z",
  "deliveredAt": "2025-12-10T08:05:00Z"
}
```

---

### 21.2 Retry Failed Delivery

```
POST /notifications/{notificationId}/deliveries/{deliveryId}/retry
```

---

## Appendix A: Enum Values

### Event Types

| Value | Description |
|---|---|
| `SCHEDULE_CHANGE` | Exam schedule modifications |
| `REVIEW_OUTCOME` | Exam results published |
| `CHECKIN_REMINDER` | Pre-exam reminder |
| `GENERAL_ANNOUNCEMENT` | General announcements |

### Notification Priority

| Value | DND Behavior |
|---|---|
| `HIGH` | Bypasses DND |
| `MEDIUM` | Held during DND |
| `LOW` | Held during DND |

### Notification Status Flow

```
DRAFT → PENDING_REVIEW → APPROVED → SENDING → SENT
                       → REJECTED
```

### Delivery Status

| Value | Description |
|---|---|
| `PENDING` | Queued for delivery |
| `SENDING` | Currently being sent |
| `DELIVERED` | Successfully delivered |
| `FAILED` | All retry attempts exhausted |
| `HELD_DND` | Held due to Do Not Disturb window |

### Job Status

| Value | Description |
|---|---|
| `PENDING` | Queued, not yet picked up |
| `RUNNING` | Currently executing |
| `COMPLETED` | Successfully finished |
| `FAILED` | All retries exhausted |
| `CANCELLED` | Manually cancelled by admin |

### Import Batch Status

| Value | Description |
|---|---|
| `PENDING` | File uploaded, not yet parsed |
| `PREVIEWING` | Parsed and preview available |
| `COMMITTED` | Valid rows imported |
| `CANCELLED` | Import cancelled by user |

### Anti-Cheat Flag Types

| Value | Description |
|---|---|
| `ACTIVITY_BURST` | > 20 actions in 1 minute |
| `IDENTICAL_SUBMISSIONS` | Same answer pattern 3+ times |
| `ABNORMAL_SCORE_DELTA` | Score change > 2 std deviations |

### User Status

| Value | Description |
|---|---|
| `ACTIVE` | Normal active account |
| `LOCKED` | Temporarily locked (auto or manual) |
| `DISABLED` | Soft-deleted / deactivated |

### Roles

| Value | Description |
|---|---|
| `ADMIN` | Administrator |
| `ACADEMIC_AFFAIRS` | Academic Affairs Coordinator |
| `HOMEROOM_TEACHER` | Homeroom Teacher |
| `SUBJECT_TEACHER` | Subject Teacher |
| `STUDENT` | Student |
