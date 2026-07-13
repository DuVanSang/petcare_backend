-- Reminder Engine migration for existing MySQL databases.
-- Back up the database before running this script.
-- Set REMINDER_SCHEDULER_ENABLED=false while applying this migration,
-- then restart the backend with REMINDER_SCHEDULER_ENABLED=true.

SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

ALTER TABLE care_reminders
    ADD COLUMN created_by BIGINT UNSIGNED NULL,
    ADD COLUMN vaccination_id BIGINT UNSIGNED NULL,
    ADD COLUMN notes TEXT NULL,
    ADD COLUMN timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    ADD COLUMN next_due_at DATETIME(6) NULL,
    ADD COLUMN vaccination_offset_minutes BIGINT NULL;

UPDATE care_reminders
SET created_by = (
    SELECT owner_id FROM pets WHERE pets.id = care_reminders.pet_id
)
WHERE created_by IS NULL;

UPDATE care_reminders
SET notes = description
WHERE notes IS NULL AND description IS NOT NULL;

UPDATE care_reminders
SET next_due_at = TIMESTAMP(next_due_date, reminder_time)
WHERE next_due_at IS NULL;

ALTER TABLE care_reminders
    MODIFY created_by BIGINT UNSIGNED NOT NULL,
    MODIFY category ENUM(
        'vaccination','bathing','nail_clipping','deworming',
        'medication','medical_checkup','other'
    ) NOT NULL,
    MODIFY frequency ENUM('once','daily','weekly','monthly','quarterly') NOT NULL,
    ADD INDEX idx_care_reminders_creator_due (created_by, is_active, next_due_at),
    ADD INDEX idx_care_reminders_pet_active (pet_id, is_active),
    ADD INDEX idx_care_reminders_vaccination_active (vaccination_id, is_active),
    ADD CONSTRAINT fk_care_reminders_created_by
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_care_reminders_vaccination
        FOREIGN KEY (vaccination_id) REFERENCES pet_vaccinations(id) ON DELETE CASCADE;

ALTER TABLE care_reminder_logs
    ADD COLUMN due_at DATETIME(6) NULL,
    ADD COLUMN notified_at DATETIME(6) NULL;

UPDATE care_reminder_logs logs
JOIN care_reminders reminders ON reminders.id = logs.reminder_id
SET logs.due_at = TIMESTAMP(logs.due_date, reminders.reminder_time)
WHERE logs.due_at IS NULL;

UPDATE care_reminder_logs
SET status = 'pending'
WHERE status = 'overdue';

ALTER TABLE care_reminder_logs
    MODIFY due_at DATETIME(6) NOT NULL,
    MODIFY status ENUM('pending','notified','completed','snoozed','cancelled') NOT NULL DEFAULT 'pending',
    ADD UNIQUE KEY uk_care_reminder_log_due (reminder_id, due_at),
    ADD INDEX idx_care_reminder_logs_due (status, due_at);

CREATE TABLE IF NOT EXISTS vaccination_reminder_logs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    vaccination_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    stage ENUM(
        'BEFORE_7_DAYS','BEFORE_1_DAY','DUE_TODAY',
        'OVERDUE_1_DAY','OVERDUE_3_DAYS','OVERDUE_7_DAYS','OVERDUE_14_DAYS'
    ) NOT NULL,
    scheduled_at DATETIME(6) NOT NULL,
    notified_at DATETIME(6) NULL,
    status ENUM('pending','notified','cancelled') NOT NULL DEFAULT 'pending',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vaccination_reminder_recipient_stage (vaccination_id, user_id, stage),
    INDEX idx_vaccination_reminder_status_time (status, scheduled_at),
    CONSTRAINT fk_vaccination_reminder_vaccination
        FOREIGN KEY (vaccination_id) REFERENCES pet_vaccinations(id) ON DELETE CASCADE,
    CONSTRAINT fk_vaccination_reminder_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;
