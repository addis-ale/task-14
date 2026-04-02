ALTER TABLE notif_delivery
    ADD COLUMN read_at DATETIME NULL AFTER delivered_at;

CREATE INDEX idx_notif_delivery_recipient_status ON notif_delivery (recipient_id, status);
CREATE INDEX idx_notif_delivery_notification_status ON notif_delivery (notification_id, status);
CREATE INDEX idx_notif_delivery_read_at ON notif_delivery (recipient_id, read_at);

CREATE INDEX idx_notification_event_status ON notification (event_type, status);
CREATE INDEX idx_compliance_review_status ON compliance_review (status);

CREATE UNIQUE INDEX uk_notif_preference_user_event ON notif_preference (user_id, event_type);
