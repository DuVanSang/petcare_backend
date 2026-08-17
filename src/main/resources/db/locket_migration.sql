CREATE TABLE IF NOT EXISTS pet_moments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    media_url VARCHAR(1000) NOT NULL,
    caption VARCHAR(255) NULL,
    location_name VARCHAR(255) NULL,
    mood_tag VARCHAR(50) NULL DEFAULT 'PLAYFUL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pet_moments_pet_created (pet_id, created_at),
    INDEX idx_pet_moments_user_created (user_id, created_at),
    INDEX idx_pet_moments_created (created_at),
    CONSTRAINT fk_pet_moments_pet FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE,
    CONSTRAINT fk_pet_moments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pet_moment_reactions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    moment_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    emoji VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_moment_reactions_moment (moment_id),
    INDEX idx_moment_reactions_user (user_id),
    CONSTRAINT fk_moment_reactions_moment FOREIGN KEY (moment_id) REFERENCES pet_moments(id) ON DELETE CASCADE,
    CONSTRAINT fk_moment_reactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
