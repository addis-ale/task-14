-- Add encrypted PII column for sensitive identifiers (student card number, national ID)
-- The column stores AES-256-GCM encrypted values via EncryptedLongConverter
ALTER TABLE sys_user ADD COLUMN encrypted_student_no VARCHAR(512) NULL AFTER device_token;
