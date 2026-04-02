Business Logic Questions Log
1. Exam Session Conflict Handling

Question: How should the system handle overlapping exam sessions for the same student or room?
My Understanding: A student or room should not have overlapping sessions within the same time slot.
Solution: Add conflict detection during session creation. Block save if overlap exists. Provide conflict details.

2. Roster Ownership and Editing Rights

Question: Who can edit candidate rosters after creation?
My Understanding: Admin and Academic Affairs can edit all. Teachers can edit only their assigned classes.
Solution: Enforce row-level RBAC based on class/grade ownership.

3. Version History Scope

Question: Does version history apply to all entities or only key records?
My Understanding: Applies to critical entities like rosters, schedules, and assignments.
Solution: Enable versioning only for selected tables with snapshot storage and restore capability.

4. Bulk Import Error Threshold

Question: Should bulk import fail entirely if some rows are invalid?
My Understanding: Allow partial success but highlight invalid rows before commit.
Solution: Add preview step with validation report. Allow user to fix or proceed with valid rows only.

5. Notification Priority Rules

Question: How are conflicting notifications handled during Do Not Disturb (DND)?
My Understanding: Non-critical notifications are delayed. Critical ones may override DND.
Solution: Add priority levels. Allow only high-priority events to bypass DND.

6. Student Subscription Defaults

Question: What are default notification settings for new students?
My Understanding: All essential notifications enabled by default.
Solution: Initialize default preferences. Allow users to modify later.

7. Offline Sync Strategy

Question: How does the system sync data across nodes without internet?
My Understanding: Uses local network sync or periodic batch sync between nodes.
Solution: Implement local replication or scheduled sync jobs within intranet.

8. Proctor Assignment Constraints

Question: Can a proctor be assigned to multiple rooms at the same time?
My Understanding: No. One proctor per time slot.
Solution: Add constraint validation during assignment creation.

9. Account Lock Recovery

Question: How does a user unlock their account after failed attempts?
My Understanding: Auto unlock after 15 minutes or manual admin unlock.
Solution: Implement both automatic timer and admin override.

10. Session Expiry Behavior

Question: What happens to unsaved work when session expires?
My Understanding: Unsaved data is lost unless auto-save exists.
Solution: Add auto-save drafts for critical forms.

11. Data Retention Policy

Question: How long should audit logs and historical data be stored?
My Understanding: Long-term storage required for compliance.
Solution: Define retention policy (e.g., 5 years) with archive mechanism.

12. Role Switching for Users

Question: Can a user have multiple roles (e.g., Teacher and Admin)?
My Understanding: Yes, but access should be context-based.
Solution: Support multi-role accounts with active role selection per session.

13. Exam Room Capacity Limits

Question: How is room capacity enforced during scheduling?
My Understanding: Student count must not exceed room capacity.
Solution: Validate capacity before assignment.

14. Notification Delivery Failure Handling

Question: What happens if notification delivery fails after retries?
My Understanding: Mark as failed and allow manual retry.
Solution: Add failure status and admin retry option.

15. Sensitive Data Masking Scope

Question: Which roles can view full student IDs without masking?
My Understanding: Only Admin and authorized staff.
Solution: Apply masking by default. Allow full access via permission flag.

16. Anti-Cheat Flag Review Workflow

Question: Who reviews flagged suspicious activities?
My Understanding: Academic Affairs or designated reviewers.
Solution: Create review queue with assignable reviewers and decision tracking.