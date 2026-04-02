ALTER TABLE session_candidate
    ADD COLUMN room_id BIGINT NULL AFTER student_id,
    ADD CONSTRAINT fk_session_candidate_room FOREIGN KEY (room_id) REFERENCES exam_room(id);

CREATE INDEX idx_session_candidate_session_room ON session_candidate (session_id, room_id);
CREATE INDEX idx_session_candidate_student ON session_candidate (student_id);

CREATE INDEX idx_exam_session_term ON exam_session (term_id);
CREATE INDEX idx_exam_session_subject ON exam_session (subject_id);
CREATE INDEX idx_exam_session_date_time ON exam_session (date, start_time, end_time);

CREATE INDEX idx_room_assignment_room ON room_assignment (room_id);

CREATE UNIQUE INDEX uk_proctor_exact_slot ON proctor_assign (proctor_user_id, time_slot_start, time_slot_end);
CREATE INDEX idx_proctor_slot_lookup ON proctor_assign (proctor_user_id, time_slot_start, time_slot_end);
