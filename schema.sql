-- ==========================================
-- HỆ THỐNG CƠ SỞ DỮ LIỆU PET CARE & SOCIAL NETWORK (PHIÊN BẢN MVP TỐI GIẢN TỐI ƯU HÓA)
-- CƠ SỞ DỮ LIỆU: MySQL (InnoDB, utf8mb4)
-- ==========================================

-- Tắt kiểm tra khóa ngoại tạm thời để tránh lỗi khi tạo lại bảng
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------
-- PHÂN HỆ 1: TÀI KHOẢN & XÁC THỰC
-- ------------------------------------------

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(255) NULL UNIQUE COMMENT 'Để NULL nếu chỉ login bằng Apple/Google không cung cấp email',
  `password_hash` VARCHAR(255) NULL COMMENT 'Mã hóa bcrypt, để NULL nếu login bằng mạng xã hội',
  `full_name` VARCHAR(255) NOT NULL,
  `phone_number` VARCHAR(20) NULL,
  `avatar_url` VARCHAR(255) NULL,
  `role` ENUM('user', 'admin', 'moderator') NOT NULL DEFAULT 'user',
  `status` ENUM('active', 'banned') NOT NULL DEFAULT 'active',
  `is_online` BOOLEAN NOT NULL DEFAULT FALSE,
  `last_active_at` TIMESTAMP NULL DEFAULT NULL,
  `language` VARCHAR(10) NOT NULL DEFAULT 'vi' COMMENT 'Ngôn ngữ cài đặt',
  `timezone` VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh' COMMENT 'Múi giờ người dùng',
  `push_notifications_enabled` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Bật/tắt thông báo đẩy',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_users_role_status` (`role`, `status`),
  INDEX `idx_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `user_social_accounts`;
CREATE TABLE `user_social_accounts` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `provider` ENUM('google', 'apple') NOT NULL,
  `provider_user_id` VARCHAR(255) NOT NULL COMMENT 'UID từ Google/Apple',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_provider_user` (`provider`, `provider_user_id`),
  CONSTRAINT `fk_social_accounts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `user_sessions`;
CREATE TABLE `user_sessions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT 'Liên kết tới tài khoản người dùng',
  `refresh_token` VARCHAR(255) NOT NULL UNIQUE COMMENT 'Refresh token để cấp lại access token',
  `device_token` VARCHAR(255) NULL COMMENT 'Expo Push Token phục vụ gửi thông báo',
  `device_type` ENUM('ios', 'android', 'web') NULL COMMENT 'Loại thiết bị đăng nhập',
  `ip_address` VARCHAR(45) NULL COMMENT 'Địa chỉ IP đăng nhập',
  `user_agent` VARCHAR(255) NULL COMMENT 'Thông tin trình duyệt/thiết bị',
  `is_revoked` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Trạng thái thu hồi/đăng xuất',
  `expires_at` TIMESTAMP NOT NULL COMMENT 'Thời gian hết hạn của token',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_sessions_user` (`user_id`),
  CONSTRAINT `fk_user_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ƯU HÓA 2: Bổ sung user_id để quản lý vòng đời OTP chính xác
DROP TABLE IF EXISTS `password_resets`;
CREATE TABLE `password_resets` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT 'Liên kết trực tiếp tới tài khoản cần reset',
  `email` VARCHAR(255) NOT NULL,
  `otp_code` VARCHAR(6) NOT NULL,
  `token` VARCHAR(255) NOT NULL UNIQUE,
  `expires_at` TIMESTAMP NOT NULL,
  `is_used` BOOLEAN NOT NULL DEFAULT FALSE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_resets_user_otp` (`user_id`, `otp_code`),
  INDEX `idx_resets_email` (`email`),
  CONSTRAINT `fk_password_resets_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------
-- PHÂN HỆ 8 (PHỤ TRỢ): DANH MỤC HỆ THỐNG
-- ------------------------------------------

DROP TABLE IF EXISTS `categories_species`;
CREATE TABLE `categories_species` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL UNIQUE,
  `icon_url` VARCHAR(255) NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `categories_breeds`;
CREATE TABLE `categories_breeds` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `species_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_species_breed` (`species_id`, `name`),
  CONSTRAINT `fk_breeds_species` FOREIGN KEY (`species_id`) REFERENCES `categories_species` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------
-- PHÂN HỆ 2: QUẢN LÝ HỒ SƠ THÚ CƯNG
-- ------------------------------------------

-- TỐI ƯU 1: Đổi ON DELETE RESTRICT thành ON DELETE CASCADE cho owner_id
DROP TABLE IF EXISTS `pets`;
CREATE TABLE `pets` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `owner_id` BIGINT UNSIGNED NOT NULL COMMENT 'Chủ sở hữu chính tạo hồ sơ',
  `name` VARCHAR(100) NOT NULL,
  `avatar_url` VARCHAR(255) NULL,
  `species_id` BIGINT UNSIGNED NOT NULL,
  `breed_id` BIGINT UNSIGNED NOT NULL,
  `gender` ENUM('male', 'female', 'unknown') NOT NULL DEFAULT 'unknown',
  `date_of_birth` DATE NULL COMMENT 'Ngày sinh chính xác',
  `estimated_age_months` INT NULL COMMENT 'Dùng khi không rõ ngày sinh chính xác',
  `current_weight` DECIMAL(5,2) NULL COMMENT 'Cân nặng hiện tại (kg)',
  `color_features` TEXT NULL COMMENT 'Màu sắc, đặc điểm nhận dạng',
  `spayed_status` ENUM('spayed', 'intact', 'unknown') NOT NULL DEFAULT 'unknown' COMMENT 'Đã triệt sản / Chưa triệt sản',
  `microchip_number` VARCHAR(100) NULL COMMENT 'Mã số thẻ căn cước thú cưng',
  `status` ENUM('active', 'archived', 'deceased') NOT NULL DEFAULT 'active' COMMENT 'Đang nuôi, Lưu trữ, Đã mất',
  `notes` TEXT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_pets_owner` (`owner_id`),
  CONSTRAINT `fk_pets_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pets_species` FOREIGN KEY (`species_id`) REFERENCES `categories_species` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_pets_breed` FOREIGN KEY (`breed_id`) REFERENCES `categories_breeds` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `pet_co_parents`;
CREATE TABLE `pet_co_parents` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `pet_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `role` ENUM('editor', 'viewer') NOT NULL DEFAULT 'viewer',
  `joined_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `invited_by` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_pet_user` (`pet_id`, `user_id`),
  CONSTRAINT `fk_co_parents_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_co_parents_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_co_parents_inviter` FOREIGN KEY (`invited_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `co_parent_invitations`;
CREATE TABLE `co_parent_invitations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `pet_id` BIGINT UNSIGNED NOT NULL,
  `inviter_id` BIGINT UNSIGNED NOT NULL,
  `invitee_email` VARCHAR(255) NULL,
  `invite_code` VARCHAR(20) NOT NULL UNIQUE,
  `role` ENUM('editor', 'viewer') NOT NULL DEFAULT 'viewer',
  `status` ENUM('pending', 'accepted', 'expired', 'revoked') NOT NULL DEFAULT 'pending',
  `expires_at` TIMESTAMP NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_invitations_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_invitations_inviter` FOREIGN KEY (`inviter_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `pet_shares`;
CREATE TABLE `pet_shares` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `pet_id` BIGINT UNSIGNED NOT NULL,
  `share_token` VARCHAR(100) NOT NULL UNIQUE,
  `permissions` ENUM('public', 'restricted_medical') NOT NULL DEFAULT 'public',
  `created_by` BIGINT UNSIGNED NOT NULL COMMENT 'Người tạo link chia sẻ',
  `expires_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_pet_shares_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pet_shares_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------
-- PHÂN HỆ 3: LỊCH TIÊM PHÒNG
-- ------------------------------------------

DROP TABLE IF EXISTS `vaccine_templates`;
CREATE TABLE `vaccine_templates` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `species_id` BIGINT UNSIGNED NOT NULL,
  `vaccine_name` VARCHAR(150) NOT NULL,
  `dose_number` INT NOT NULL DEFAULT 1,
  `recommended_age_weeks` INT NOT NULL,
  `description` TEXT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_templates_species` FOREIGN KEY (`species_id`) REFERENCES `categories_species` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `pet_vaccinations`;
CREATE TABLE `pet_vaccinations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `pet_id` BIGINT UNSIGNED NOT NULL,
  `vaccine_template_id` BIGINT UNSIGNED NULL,
  `vaccine_name` VARCHAR(150) NOT NULL,
  `dose_number` INT NOT NULL DEFAULT 1,
  `status` ENUM('scheduled', 'completed', 'skipped', 'overdue') NOT NULL DEFAULT 'scheduled',
  `scheduled_date` DATE NOT NULL,
  `actual_date` DATE NULL,
  `administered_by` VARCHAR(150) NULL,
  `clinic_name` VARCHAR(150) NULL,
  `cost` DECIMAL(10,2) NULL,
  `notes` TEXT NULL,
  `medical_proof_url` VARCHAR(255) NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_vaccinations_pet_status` (`pet_id`, `status`),
  CONSTRAINT `fk_vaccinations_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_vaccinations_template` FOREIGN KEY (`vaccine_template_id`) REFERENCES `vaccine_templates` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------
-- PHÂN HỆ 4: NHẮC NHỞ CHĂM SÓC
-- ------------------------------------------

DROP TABLE IF EXISTS `care_reminders`;
CREATE TABLE `care_reminders` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `pet_id` BIGINT UNSIGNED NOT NULL,
  `category` ENUM('vaccination', 'medical_checkup', 'deworming', 'bathing', 'nail_clipping', 'medication', 'other') NOT NULL,
  `title` VARCHAR(150) NOT NULL,
  `description` TEXT NULL,
  `reminder_time` TIME NOT NULL,
  `frequency` ENUM('once', 'daily', 'weekly', 'monthly', 'yearly') NOT NULL,
  `interval_value` INT NOT NULL DEFAULT 1,
  `start_date` DATE NOT NULL,
  `end_date` DATE NULL,
  `next_due_date` DATE NOT NULL,
  `before_duration_minutes` INT NOT NULL DEFAULT 0,
  `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_reminders_next_due` (`is_active`, `next_due_date`),
  CONSTRAINT `fk_reminders_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- TỐI ƯU 6: Khóa ngoại SET NULL khi User bị xóa là chính xác, giúp giữ lại lịch sử chăm sóc y tế của Pet
DROP TABLE IF EXISTS `care_reminder_logs`;
CREATE TABLE `care_reminder_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `reminder_id` BIGINT UNSIGNED NOT NULL,
  `due_date` DATE NOT NULL,
  `status` ENUM('pending', 'completed', 'snoozed', 'overdue') NOT NULL DEFAULT 'pending',
  `completed_at` TIMESTAMP NULL DEFAULT NULL,
  `completed_by` BIGINT UNSIGNED NULL COMMENT 'Khóa ngoại SET NULL khi User bị xóa để giữ lại vết y tế lịch sử của Pet',
  `snoozed_until` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_reminder_logs_reminder` FOREIGN KEY (`reminder_id`) REFERENCES `care_reminders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reminder_logs_completed_by` FOREIGN KEY (`completed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------
-- PHÂN HỆ 5: THEO DÕI SỨC KHỎE CƠ BẢN
-- ------------------------------------------

DROP TABLE IF EXISTS `weight_logs`;
CREATE TABLE `weight_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `pet_id` BIGINT UNSIGNED NOT NULL,
  `weight` DECIMAL(5,2) NOT NULL,
  `logged_date` DATE NOT NULL,
  `logged_by` BIGINT UNSIGNED NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_weight_pet_date` (`pet_id`, `logged_date`),
  CONSTRAINT `fk_weight_logs_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_weight_logs_user` FOREIGN KEY (`logged_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `health_conditions`;
CREATE TABLE `health_conditions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `pet_id` BIGINT UNSIGNED NOT NULL,
  `type` ENUM('allergy', 'chronic_disease', 'current_medication', 'other') NOT NULL,
  `title` VARCHAR(150) NOT NULL,
  `description` TEXT NULL,
  `started_date` DATE NULL,
  `ended_date` DATE NULL,
  `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_conditions_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `health_logs`;
CREATE TABLE `health_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `pet_id` BIGINT UNSIGNED NOT NULL,
  `logged_date` DATE NOT NULL,
  `appetite` ENUM('normal', 'poor', 'no_food', 'excessive') NOT NULL DEFAULT 'normal',
  `activity_level` ENUM('low', 'moderate', 'high', 'lethargic') NOT NULL DEFAULT 'moderate',
  `abnormal_event` TEXT NULL,
  `treatment_notes` TEXT NULL,
  `logged_by` BIGINT UNSIGNED NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_pet_logged_date` (`pet_id`, `logged_date`),
  CONSTRAINT `fk_health_logs_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_health_logs_user` FOREIGN KEY (`logged_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------
-- PHÂN HỆ 6: HỒ SƠ Y TẾ ĐIỆN TỬ (EMR)
-- ------------------------------------------

DROP TABLE IF EXISTS `emr_records`;
CREATE TABLE `emr_records` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `pet_id` BIGINT UNSIGNED NOT NULL,
  `record_type` ENUM('visit', 'prescription', 'lab_result', 'surgery', 'other') NOT NULL,
  `visit_date` DATE NOT NULL,
  `clinic_name` VARCHAR(150) NULL,
  `vet_name` VARCHAR(150) NULL,
  `vet_contact` VARCHAR(50) NULL,
  `diagnosis` TEXT NOT NULL,
  `prescription_details` TEXT NULL,
  `notes` TEXT NULL,
  `attachments` TEXT NULL COMMENT 'Mảng JSON tệp đính kèm: [{"file_name": "xray.jpg", "file_url": "url", "file_type": "image/jpeg"}]',
  `created_by` BIGINT UNSIGNED NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_emr_records_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_emr_records_user` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------
-- PHÂN HỆ 7: MẠNG XÃ HỘI (TỐI GIẢN TỐI ƯU CHO MVP)
-- ------------------------------------------

-- TỐI ƯU 5: Tích hợp trường images dạng TEXT (chứa chuỗi JSON URL) để bỏ bảng post_images, tăng tốc độ API load feed
DROP TABLE IF EXISTS `posts`;
CREATE TABLE `posts` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT 'Người đăng bài',
  `pet_id` BIGINT UNSIGNED NULL COMMENT 'Bài đăng thuộc về bé thú cưng cụ thể (Phục vụ gom Album và lọc timeline)',
  `caption` TEXT NULL,
  `images` TEXT NULL COMMENT 'Mảng JSON lưu URLs ảnh: ["url1", "url2"] để loại bỏ JOIN bảng phụ',
  `privacy` ENUM('private', 'friends') NOT NULL DEFAULT 'friends',
  `status` ENUM('published', 'hidden', 'deleted') NOT NULL DEFAULT 'published',
  `comments_locked` BOOLEAN NOT NULL DEFAULT FALSE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_posts_user_status` (`user_id`, `status`),
  INDEX `idx_posts_pet` (`pet_id`),
  CONSTRAINT `fk_posts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_posts_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `post_likes`;
CREATE TABLE `post_likes` (
  `post_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`post_id`, `user_id`),
  CONSTRAINT `fk_likes_post` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `post_comments`;
CREATE TABLE `post_comments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `post_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `comment_text` TEXT NOT NULL,
  `status` ENUM('visible', 'deleted') NOT NULL DEFAULT 'visible',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_comments_post` (`post_id`),
  CONSTRAINT `fk_comments_post` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bạn bè đồng thuận 2 chiều (Zalo / Locket style)
DROP TABLE IF EXISTS `friendships`;
CREATE TABLE `friendships` (
  `user_id1` BIGINT UNSIGNED NOT NULL COMMENT 'ID người dùng 1 (Luôn nhỏ hơn user_id2)',
  `user_id2` BIGINT UNSIGNED NOT NULL COMMENT 'ID người dùng 2 (Luôn lớn hơn user_id1)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id1`, `user_id2`),
  INDEX `idx_friendships_user2` (`user_id2`),
  CONSTRAINT `chk_user_order` CHECK (`user_id1` < `user_id2`),
  CONSTRAINT `fk_friendships_user1` FOREIGN KEY (`user_id1`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_friendships_user2` FOREIGN KEY (`user_id2`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `friend_requests`;
CREATE TABLE `friend_requests` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `sender_id` BIGINT UNSIGNED NOT NULL COMMENT 'Người gửi lời mời',
  `receiver_id` BIGINT UNSIGNED NOT NULL COMMENT 'Người nhận lời mời',
  `status` ENUM('pending', 'accepted', 'declined') NOT NULL DEFAULT 'pending',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_sender_receiver` (`sender_id`, `receiver_id`),
  INDEX `idx_friend_requests_receiver_status` (`receiver_id`, `status`),
  CONSTRAINT `fk_friend_requests_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_friend_requests_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



-- ------------------------------------------
-- PHÂN HỆ 8 (PHỤ TRỢ): KIỂM DUYỆT & REPORT
-- ------------------------------------------

DROP TABLE IF EXISTS `reports`;
CREATE TABLE `reports` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `reporter_user_id` BIGINT UNSIGNED NOT NULL,
  `target_type` ENUM('post', 'comment', 'user') NOT NULL,
  `target_id` BIGINT UNSIGNED NOT NULL,
  `reason` TEXT NOT NULL,
  `status` ENUM('pending', 'reviewed', 'ignored', 'resolved') NOT NULL DEFAULT 'pending',
  `moderator_notes` TEXT NULL,
  `resolved_by` BIGINT UNSIGNED NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_reports_reporter` FOREIGN KEY (`reporter_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reports_resolver` FOREIGN KEY (`resolved_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `admin_audits`;
CREATE TABLE `admin_audits` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `admin_id` BIGINT UNSIGNED NOT NULL,
  `action` VARCHAR(100) NOT NULL,
  `target_id` BIGINT UNSIGNED NULL,
  `details` TEXT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_audits_admin` FOREIGN KEY (`admin_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------
-- PHÂN HỆ 9: HỆ THỐNG THÔNG BÁO (NOTIFICATIONS)
-- ------------------------------------------

DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT 'Người nhận thông báo',
  `title` VARCHAR(255) NOT NULL,
  `content` TEXT NOT NULL,
  `type` VARCHAR(50) NOT NULL COMMENT 'Loại thông báo: vaccine_reminder, medical_checkup, new_follower, post_like, post_comment, co_parent_invite, co_parent_accepted, pet_shared, system',
  `reference_id` BIGINT UNSIGNED NULL COMMENT 'ID của đối tượng liên quan (post_id, pet_id, reminder_log_id, etc.)',
  `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
  `read_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_notifications_user_read` (`user_id`, `is_read`),
  CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------
-- PHÂN HỆ ĐẶC BIỆT: TỐI ƯU TIMELINE VÀ REPORT
-- ------------------------------------------

DROP TABLE IF EXISTS `pet_timeline_events`;
CREATE TABLE `pet_timeline_events` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `pet_id` BIGINT UNSIGNED NOT NULL,
  `event_type` ENUM('profile_created', 'vaccinated', 'medical_visit', 'weight_updated', 'reminder_completed', 'social_post') NOT NULL,
  `reference_id` BIGINT UNSIGNED NOT NULL,
  `event_date` DATE NOT NULL,
  `summary` VARCHAR(255) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_timeline_pet_date` (`pet_id`, `event_date` DESC, `event_type`),
  CONSTRAINT `fk_timeline_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `admin_analytics_daily`;
CREATE TABLE `admin_analytics_daily` (
  `stat_date` DATE NOT NULL,
  `total_users` INT UNSIGNED NOT NULL DEFAULT 0,
  `total_pets` INT UNSIGNED NOT NULL DEFAULT 0,
  `dau` INT UNSIGNED NOT NULL DEFAULT 0,
  `mau` INT UNSIGNED NOT NULL DEFAULT 0,
  `completed_reminders` INT UNSIGNED NOT NULL DEFAULT 0,
  `total_posts` INT UNSIGNED NOT NULL DEFAULT 0,
  `total_interactions` INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bật lại kiểm tra khóa ngoại
SET FOREIGN_KEY_CHECKS = 1;
