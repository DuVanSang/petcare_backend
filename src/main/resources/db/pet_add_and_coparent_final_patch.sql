-- Apply once to the current database after the latest dev schema.
ALTER TABLE co_parent_invitations
    ADD COLUMN invitee_user_id BIGINT UNSIGNED NULL AFTER invitee_email,
    ADD COLUMN accepted_at TIMESTAMP NULL DEFAULT NULL AFTER expires_at,
    ADD COLUMN declined_at TIMESTAMP NULL DEFAULT NULL AFTER accepted_at,
    ADD COLUMN revoked_at TIMESTAMP NULL DEFAULT NULL AFTER declined_at,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

ALTER TABLE co_parent_invitations
    MODIFY COLUMN status ENUM('pending','accepted','expired','revoked','declined') NOT NULL DEFAULT 'pending';

CREATE INDEX idx_coparent_inv_invitee_status
    ON co_parent_invitations (invitee_user_id, status);

ALTER TABLE co_parent_invitations
    ADD CONSTRAINT fk_coparent_inv_invitee_user
        FOREIGN KEY (invitee_user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Defensive default: every notification starts unread even for legacy inserts.
ALTER TABLE notifications
    DROP COLUMN user_id,
    MODIFY COLUMN is_read BIT(1) NOT NULL DEFAULT b'0';

-- Align authentication token foreign-key columns with users.id (BIGINT UNSIGNED).
ALTER TABLE refresh_tokens
    MODIFY COLUMN user_id BIGINT UNSIGNED NOT NULL,
    ADD CONSTRAINT FK1lih5y2npsf8u5o3vhdb9y0os
        FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE email_verification_tokens
    MODIFY COLUMN user_id BIGINT UNSIGNED NOT NULL,
    ADD CONSTRAINT FKi1c4mmamlb8keqt74k4lrtwhc
        FOREIGN KEY (user_id) REFERENCES users(id);
