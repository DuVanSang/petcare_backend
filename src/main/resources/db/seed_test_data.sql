-- ============================================================
-- DỮ LIỆU TEST CHO SWAGGER / API
-- Mật khẩu chung tất cả tài khoản: Test@123
-- Chạy SAU schema.sql và seed_categories.sql
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- Dọn dữ liệu test cũ (nếu chạy lại)
DELETE FROM pet_co_parents;
DELETE FROM co_parent_invitations;
DELETE FROM pets;
DELETE FROM user_devices;
DELETE FROM refresh_tokens;
DELETE FROM password_reset_tokens;
DELETE FROM email_verification_tokens;
DELETE FROM users WHERE email LIKE '%@test.com';

SET FOREIGN_KEY_CHECKS = 1;

-- bcrypt hash của "Test@123"
SET @pwd = '$2b$10$amJX.EotpL2mrGbz2ocQkuohqSoBDsj3bFBrjFt1it3MV43uTYHVe';

INSERT INTO users (
  id, email, password_hash, full_name, phone_number, avatar_url,
  role, status, is_online, email_verified, email_verified_at,
  language_code, timezone, push_notification_enabled
) VALUES
(101, 'owner@test.com',   @pwd, 'Nguyễn Văn Owner',   '0901000001', NULL, 'user', 'active', 0, 1, NOW(6), 'vi', 'Asia/Ho_Chi_Minh', 1),
(102, 'editor@test.com',  @pwd, 'Trần Thị Editor',    '0901000002', NULL, 'user', 'active', 0, 1, NOW(6), 'vi', 'Asia/Ho_Chi_Minh', 1),
(103, 'viewer@test.com',  @pwd, 'Lê Văn Viewer',      '0901000003', NULL, 'user', 'active', 0, 1, NOW(6), 'vi', 'Asia/Ho_Chi_Minh', 1),
(104, 'banned@test.com',  @pwd, 'Phạm Bị Khóa',       '0901000004', NULL, 'user', 'banned', 0, 1, NOW(6), 'vi', 'Asia/Ho_Chi_Minh', 1),
(105, 'newuser@test.com', @pwd, 'Hoàng Chưa Verify',  '0901000005', NULL, 'user', 'active', 0, 0, NULL,   'vi', 'Asia/Ho_Chi_Minh', 1);

-- Thiết bị mẫu (để test GET/DELETE /users/me/devices)
INSERT INTO user_devices (
  user_id, device_id, device_name, device_type, device_token,
  notification_enabled, app_version, os_version,
  last_active_at, last_login_at, created_at, updated_at
) VALUES
(101, 'device-owner-ios',     'iPhone 15 Pro', 'ios',     'ExponentPushToken[owner-test]', 1, '1.0.0', 'iOS 18.0', NOW(6), NOW(6), NOW(6), NOW(6)),
(102, 'device-editor-android','Samsung S24',   'android', 'ExponentPushToken[editor-test]', 1, '1.0.0', 'Android 15', NOW(6), NOW(6), NOW(6), NOW(6));

-- Thú cưng mẫu (species_id 1=Chó, breed_id 1=Labrador; species_id 2=Mèo, breed_id 22=British Shorthair)
INSERT INTO pets (
  id, owner_id, name, species_id, breed_id, gender,
  date_of_birth, estimated_age_months, current_weight,
  color_features, spayed_status, status, notes
) VALUES
(201, 101, 'Milo', 1, 1, 'male',   '2022-03-15', NULL, 12.50, 'Vàng kem, đuôi cong', 'intact',  'active',   'Chó năng động, thích chơi bóng'),
(202, 101, 'Miu',  2, 22, 'female', '2021-08-20', NULL, 4.20,  'Trắng xám',           'spayed',  'active',   'Mèo hiền, thích ngủ'),
(203, 101, 'Coco', 1, 5,  'female', '2019-01-10', NULL, 8.00,  'Nâu đen',             'unknown', 'archived', 'Đã chuyển nhà nuôi');

-- Đồng nuôi: editor trên Milo, viewer trên Milo
INSERT INTO pet_co_parents (pet_id, user_id, role, invited_by) VALUES
(201, 102, 'editor', 101),
(201, 103, 'viewer', 101);

-- Lời mời pending (để test accept) — editor@test.com đã accept rồi, tạo invite cho viewer@test.com trên Miu
INSERT INTO co_parent_invitations (
  pet_id, inviter_id, invitee_email, invite_code, role, status, expires_at
) VALUES
(202, 101, 'viewer@test.com', 'INVITE-CODE-12', 'viewer', 'pending', DATE_ADD(NOW(), INTERVAL 7 DAY));
