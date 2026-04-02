INSERT INTO sys_role (name, description)
VALUES
    ('ADMIN', 'Full system administrator'),
    ('ACADEMIC_AFFAIRS', 'Academic affairs coordinator'),
    ('HOMEROOM_TEACHER', 'Homeroom teacher with class scope'),
    ('SUBJECT_TEACHER', 'Subject teacher with course scope'),
    ('STUDENT', 'Student role')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- ADMIN full permissions for foundational modules
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'USER_MANAGEMENT', 'CREATE' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'USER_MANAGEMENT', 'READ' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'USER_MANAGEMENT', 'UPDATE' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'USER_MANAGEMENT', 'DELETE' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'USER_MANAGEMENT', 'UNLOCK' FROM sys_role WHERE name = 'ADMIN';

INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'ROSTERS', 'CREATE' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'ROSTERS', 'READ' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'ROSTERS', 'UPDATE' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'ROSTERS', 'DELETE' FROM sys_role WHERE name = 'ADMIN';

INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'EXAM_SESSIONS', 'CREATE' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'EXAM_SESSIONS', 'READ' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'EXAM_SESSIONS', 'UPDATE' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'EXAM_SESSIONS', 'DELETE' FROM sys_role WHERE name = 'ADMIN';

INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'CAMPUS_ROOMS', 'CREATE' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'CAMPUS_ROOMS', 'READ' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'CAMPUS_ROOMS', 'UPDATE' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'CAMPUS_ROOMS', 'DELETE' FROM sys_role WHERE name = 'ADMIN';

INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'PROCTOR_ASSIGNMENTS', 'CREATE' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'PROCTOR_ASSIGNMENTS', 'READ' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'PROCTOR_ASSIGNMENTS', 'UPDATE' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'PROCTOR_ASSIGNMENTS', 'DELETE' FROM sys_role WHERE name = 'ADMIN';

INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'NOTIFICATIONS', 'PUBLISH' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'COMPLIANCE_REVIEW', 'APPROVE' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'COMPLIANCE_REVIEW', 'REJECT' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'ANTI_CHEAT_REVIEW', 'READ' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'AUDIT_LOGS', 'READ' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'BULK_IMPORT_EXPORT', 'EXECUTE' FROM sys_role WHERE name = 'ADMIN';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'VERSION_HISTORY', 'READ' FROM sys_role WHERE name = 'ADMIN';

-- Academic affairs: broad operational permissions + read user management
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'USER_MANAGEMENT', 'READ' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'ROSTERS', 'CREATE' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'ROSTERS', 'READ' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'ROSTERS', 'UPDATE' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'ROSTERS', 'DELETE' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'EXAM_SESSIONS', 'CREATE' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'EXAM_SESSIONS', 'READ' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'EXAM_SESSIONS', 'UPDATE' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'EXAM_SESSIONS', 'DELETE' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'CAMPUS_ROOMS', 'CREATE' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'CAMPUS_ROOMS', 'READ' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'CAMPUS_ROOMS', 'UPDATE' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'CAMPUS_ROOMS', 'DELETE' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'PROCTOR_ASSIGNMENTS', 'CREATE' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'PROCTOR_ASSIGNMENTS', 'READ' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'PROCTOR_ASSIGNMENTS', 'UPDATE' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'PROCTOR_ASSIGNMENTS', 'DELETE' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'NOTIFICATIONS', 'PUBLISH' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'COMPLIANCE_REVIEW', 'APPROVE' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'COMPLIANCE_REVIEW', 'REJECT' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'ANTI_CHEAT_REVIEW', 'READ' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'AUDIT_LOGS', 'READ' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'BULK_IMPORT_EXPORT', 'EXECUTE' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'VERSION_HISTORY', 'READ' FROM sys_role WHERE name = 'ACADEMIC_AFFAIRS';

-- Homeroom teacher
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'ROSTERS', 'READ' FROM sys_role WHERE name = 'HOMEROOM_TEACHER';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'ROSTERS', 'UPDATE' FROM sys_role WHERE name = 'HOMEROOM_TEACHER';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'EXAM_SESSIONS', 'READ' FROM sys_role WHERE name = 'HOMEROOM_TEACHER';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'CAMPUS_ROOMS', 'READ' FROM sys_role WHERE name = 'HOMEROOM_TEACHER';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'PROCTOR_ASSIGNMENTS', 'READ' FROM sys_role WHERE name = 'HOMEROOM_TEACHER';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'BULK_IMPORT_EXPORT', 'EXECUTE' FROM sys_role WHERE name = 'HOMEROOM_TEACHER';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'VERSION_HISTORY', 'READ' FROM sys_role WHERE name = 'HOMEROOM_TEACHER';

-- Subject teacher
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'ROSTERS', 'READ' FROM sys_role WHERE name = 'SUBJECT_TEACHER';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'ROSTERS', 'UPDATE' FROM sys_role WHERE name = 'SUBJECT_TEACHER';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'EXAM_SESSIONS', 'READ' FROM sys_role WHERE name = 'SUBJECT_TEACHER';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'CAMPUS_ROOMS', 'READ' FROM sys_role WHERE name = 'SUBJECT_TEACHER';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'PROCTOR_ASSIGNMENTS', 'READ' FROM sys_role WHERE name = 'SUBJECT_TEACHER';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'BULK_IMPORT_EXPORT', 'EXECUTE' FROM sys_role WHERE name = 'SUBJECT_TEACHER';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'VERSION_HISTORY', 'READ' FROM sys_role WHERE name = 'SUBJECT_TEACHER';

-- Student
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'ROSTERS', 'READ_SELF' FROM sys_role WHERE name = 'STUDENT';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'EXAM_SESSIONS', 'READ_SELF' FROM sys_role WHERE name = 'STUDENT';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'NOTIFICATION_PREFERENCE', 'READ_SELF' FROM sys_role WHERE name = 'STUDENT';
INSERT IGNORE INTO role_permission (role_id, resource, action)
SELECT id, 'NOTIFICATION_PREFERENCE', 'UPDATE_SELF' FROM sys_role WHERE name = 'STUDENT';
