# Tài Liệu Mô Tả Chi Tiết Luồng Hoạt Động Xuyên Suốt Hệ Thống (End-to-End Flows)
*Dành Cho Dự Án Đồ Án Tốt Nghiệp: Ứng Dụng Pet Diary*

Tài liệu này đặc tả chi tiết **từng bước (Step-by-step)** luồng xử lý từ giao diện ứng dụng di động (Client App - Expo/React Native) qua cổng giao tiếp API (Backend NestJS) và xuống cơ sở dữ liệu (Database MySQL) dựa trên giao diện thiết kế đã cung cấp.

---

## PHÂN HỆ 1: CHÀO MỪNG, XÁC THỰC & QUẢN LÝ PHIÊN (AUTH & SESSIONS)

### 1.1. Luồng Onboarding & Splash Screens (Giới thiệu ứng dụng)
1. **Bước 1 (Client):** Người dùng mở app -> Màn hình Splash hiển thị logo `petdiary` và thanh loading chạy từ 0% đến 100%.
2. **Bước 2 (Client):** Điều hướng qua 3 trang Onboarding giới thiệu tính năng chính:
   - **Trang 1:** *Manage Pets Easily* (Quản lý hồ sơ, thông tin nhiều thú cưng tập trung).
   - **Trang 2:** *Vaccine & Care Alerts* (Cảnh báo lịch tiêm và nhắc nhở chăm sóc).
   - **Trang 3:** *Track Pet Health* (Theo dõi cân nặng, thói quen ăn uống, lịch sử y tế).
3. **Bước 3 (Client):** Tại Trang 3, người dùng chọn:
   - Bấm **"Get Started"** -> Điều hướng tới màn hình Đăng ký tài khoản (Create Account).
   - Bấm **"Login"** -> Điều hướng tới màn hình Đăng nhập (Welcome Back!).
   - Hoặc bấm **"Skip"** ở góc trên bên phải để vào thẳng màn hình Đăng ký.

---

### 1.2. Luồng Đăng Ký Tài Khoản Mới (Sign Up)
1. **Bước 1 (Client):** Người dùng điền: Họ tên (`full_name`), Email (`email`), Mật khẩu (`password`), Xác nhận mật khẩu (`confirm_password`), và tích chọn đồng ý điều khoản bảo mật.
2. **Bước 2 (Client - Validation):** 
   - Kiểm tra định dạng Email hợp lệ.
   - Kiểm tra `password` và `confirm_password` có khớp nhau không.
   - Nếu không tích chọn đồng ý điều khoản, nút "Sign Up" sẽ bị vô hiệu hóa.
3. **Bước 3 (Client -> API):** Bấm "Sign Up" -> Gửi request `POST /api/v1/auth/register` kèm JSON body:
   ```json
   {
     "email": "petlover@example.com",
     "password": "strongpassword123",
     "full_name": "Nguyen Van A"
   }
   ```
4. **Bước 4 (API):**
   - Kiểm tra Email đã tồn tại trong DB chưa.
   - Thực hiện băm (Hash) mật khẩu bằng thư viện `bcrypt` với Salt Round = 10.
5. **Bước 5 (API -> DB):** Xử lý giao dịch chèn dữ liệu:
   ```sql
   -- Kiểm tra email tồn tại
   SELECT id FROM users WHERE email = 'petlover@example.com';
   -- Chèn người dùng mới kèm cấu hình mặc định (đã gộp bảng user_settings)
   INSERT INTO users (email, password_hash, full_name, role, status, language, timezone, push_notifications_enabled)
   VALUES ('petlover@example.com', '$2b$10$...hashedpassword...', 'Nguyen Van A', 'user', 'active', 'vi', 'Asia/Ho_Chi_Minh', TRUE);
   ```
6. **Bước 6 (API -> Client):** Trả về HTTP 201 Created kèm thông tin user và thông báo "Create account successful!".
7. **Bước 7 (Client):** Hiển thị Popup Toast xanh lá cây **"Create account successful!"** ở góc trên màn hình và tự động điều hướng người dùng tới màn hình Đăng nhập (Login).

---

### 1.3. Luồng Đăng Nhập (Login) & Khởi Tạo Phiên (Session)
1. **Bước 1 (Client):** Người dùng nhập Email và Mật khẩu. Có thể tích chọn "Remember Me" để ghi nhớ trạng thái.
2. **Bước 2 (Client - Validation):** 
   - Nếu bấm "Log In" khi chưa điền Email hoặc Password, UI lập tức hiển thị dòng chữ đỏ báo lỗi: *"Email is required"* hoặc *"Password is required"* ngay dưới ô nhập tương ứng (Khớp thiết kế Frame 3 - Image 1).
3. **Bước 3 (Client -> API):** Gửi request `POST /api/v1/auth/login` chứa Email, Password và thông tin thiết bị (IP, User Agent, Device Token từ Expo Push API).
4. **Bước 4 (API):**
   - Truy vấn người dùng bằng email.
   - So sánh password đã băm bằng `bcrypt.compare()`.
   - Nếu tài khoản có trạng thái `users.status = 'banned'`, trả về lỗi HTTP 403 Forbidden.
   - Sinh chuỗi JWT `access_token` (hạn dùng ngắn, ví dụ 15 phút) và `refresh_token` (hạn dùng dài, ví dụ 30 ngày).
5. **Bước 5 (API -> DB):** Ghi nhận phiên làm việc mới (Refresh Token) vào DB kèm thông tin thiết bị (đã gộp bảng `user_devices` cũ):
   ```sql
   -- 1. Ghi nhận phiên làm việc mới kèm token thiết bị và loại thiết bị
   INSERT INTO user_sessions (user_id, refresh_token, device_token, device_type, ip_address, user_agent, expires_at)
   VALUES (1, 'refresh_token_string', 'ExponentPushToken[xxx]', 'ios', '192.168.1.1', 'ExpoClient/Android', NOW() + INTERVAL 30 DAY);
   
   -- 2. Cập nhật trạng thái online của người dùng
   UPDATE users SET is_online = TRUE, last_active_at = CURRENT_TIMESTAMP WHERE id = 1;
   ```
6. **Bước 6 (API -> Client):** Trả về HTTP 200 OK kèm bộ Token và thông báo đăng nhập thành công.
7. **Bước 7 (Client):** Hiển thị Toast xanh lá cây **"Login successful!"**, lưu `access_token` vào bộ nhớ đệm ứng dụng (Memory) và `refresh_token` vào bộ nhớ an toàn của thiết bị (SecureStore), sau đó điều hướng người dùng vào màn hình Dashboard chính (Home).

---

### 1.4. Luồng Quên Mật Khẩu & Xác Thực OTP (Forgot Password & Verify OTP)
1. **Bước 1 (User -> Client):** Tại màn hình Đăng nhập (Welcome Back!), người dùng nhấn vào nút **"Forgot Password?"** -> Mở màn hình nhập Email.
2. **Bước 2 (User -> Client):** Nhập Email và bấm nút **"Send OTP"**.
3. **Bước 3 (Client -> API):** Gửi request `POST /api/v1/auth/forgot-password` với JSON body:
   ```json
   {
     "email": "petlover@example.com"
   }
   ```
4. **Bước 4 (API):**
   - Truy vấn kiểm tra Email trong hệ thống (bảng `users`).
   - Sinh mã xác thực OTP ngẫu nhiên gồm 6 chữ số (ví dụ: `123456`).
   - Sinh chuỗi token ngẫu nhiên bảo mật đại diện cho phiên reset mật khẩu (ví dụ: `secure_reset_token_xxx`).
   - Thiết lập thời hạn của mã OTP là 15 phút.
5. **Bước 5 (API -> DB):** Lưu thông tin mã reset mật khẩu vào DB:
   ```sql
   -- 1. Vô hiệu hóa toàn bộ các OTP cũ chưa sử dụng của tài khoản này trước đó để bảo mật
   UPDATE password_resets 
   SET is_used = TRUE 
   WHERE user_id = :user_id AND is_used = FALSE;
   
   -- 2. Ghi nhận bản ghi OTP mới và token phiên
   INSERT INTO password_resets (user_id, email, otp_code, token, expires_at, is_used)
   VALUES (:user_id, 'petlover@example.com', '123456', 'secure_reset_token_xxx', NOW() + INTERVAL 15 MINUTE, FALSE);
   ```
6. **Bước 6 (API -> Mail Server):** Gọi dịch vụ SMTP gửi Email chứa mã OTP `123456` tới hòm thư người dùng.
7. **Bước 7 (API -> Client):** Trả về HTTP 200 OK kèm theo `token` phiên (không trả về mã OTP trong phản hồi API).
8. **Bước 8 (User -> Client):** Người dùng nhận mail, lấy mã OTP `123456` điền vào ô xác thực trên giao diện, đồng thời nhập Mật khẩu mới và Xác nhận mật khẩu mới.
9. **Bước 9 (Client -> API):** Gửi request `POST /api/v1/auth/reset-password` chứa JSON payload:
   ```json
   {
     "token": "secure_reset_token_xxx",
     "otp_code": "123456",
     "new_password": "newstrongpassword456"
   }
   ```
10. **Bước 10 (API):**
    - Kiểm tra xem token phiên và mã OTP có khớp trong DB, trạng thái chưa sử dụng và chưa hết hạn.
    - Thực hiện băm (Hash) mật khẩu mới sử dụng `bcrypt`.
11. **Bước 11 (API -> DB):** Tiến hành cập nhật mật khẩu và thu hồi các phiên đăng nhập cũ để bảo vệ tài khoản (Transaction):
    ```sql
    START TRANSACTION;
    
    -- 1. Xác thực tính hợp lệ của mã OTP
    SELECT user_id FROM password_resets 
    WHERE token = 'secure_reset_token_xxx' AND otp_code = '123456' AND is_used = FALSE AND expires_at > NOW();
    
    -- 2. Cập nhật hash mật khẩu mới vào bảng users
    UPDATE users SET password_hash = '$2b$10$...newhashedpassword...' WHERE id = :user_id;
    
    -- 3. Đánh dấu mã OTP đã được sử dụng
    UPDATE password_resets SET is_used = TRUE WHERE token = 'secure_reset_token_xxx';
    
    -- 4. Thu hồi (đăng xuất) toàn bộ các phiên làm việc của tài khoản này trên các thiết bị khác để bảo mật
    UPDATE user_sessions SET is_revoked = TRUE WHERE user_id = :user_id;
    
    COMMIT;
    ```
12. **Bước 12 (API -> Client):** Trả về HTTP 200 OK thông báo cập nhật mật khẩu thành công.
13. **Bước 13 (Client):** Hiển thị Popup Toast xanh lá thông báo đổi mật khẩu thành công và điều hướng người dùng quay trở lại màn hình Đăng nhập để sử dụng mật khẩu mới.

---

## PHÂN HỆ 2: QUẢN LÝ HỒ SƠ THÚ CƯNG (PET MANAGEMENT)

### 2.1. Luồng Thêm Mới Thú Cưng 3 Bước (Add New Pet Flow)
Màn hình này được thiết kế theo quy trình Multi-step chia làm 3 bước rõ ràng (Khớp thiết kế Image 3):

```
[Step 1: Basic Info] ──> [Step 2: Details] ──> [Step 3: Medical] ──> Hoàn thành
```

#### Bước 1: Basic Info (Thông tin cơ bản)
- **UI:** Người dùng tải lên ảnh đại diện thú cưng, nhập Tên thú cưng (`name`), Chọn loài (`species_id` - Dog, Cat, Other), nhập Giống thú cưng (`breed_id` - ví dụ: Golden Retriever), chọn Giới tính (`gender` - Male/Female/Unknown), và nhập Ngày sinh (`date_of_birth`).
- **Client xử lý:** Lưu tạm thời dữ liệu Step 1 vào State cục bộ của màn hình, bấm "Next" để chuyển qua Step 2.

#### Bước 2: Details (Chi tiết sinh học)
- **UI:** Người dùng nhập Cân nặng ban đầu (`weight`), Màu sắc/Vết bớt đặc trưng (`color_features`), Chọn trạng thái triệt sản (`spayed_status` - Spayed/Intact/Unknown) và mã số thẻ căn cước thú cưng (`microchip_number`).
- **Client xử lý:** Lưu tạm thời dữ liệu Step 2 vào State, bấm "Next" để chuyển sang Step 3.

#### Bước 3: Medical & Status (Bệnh án & Trạng thái hoạt động)
- **UI:** Nhập các dị ứng (`allergies`), Bệnh lý hiện tại/mãn tính (`medical_conditions`), ghi chú bổ sung (`notes`) và thiết lập trạng thái ban đầu của thú cưng (`status` - Active hoặc Archived).
- **Client -> API:** Người dùng nhấn **"Save Pet"** -> App đóng gói dữ liệu của cả 3 bước gửi request `POST /api/v1/pets` lên Backend.

#### Xử lý tại Backend & Database:
Backend API nhận JSON gói dữ liệu tổng hợp và thực thi một transaction để lưu trữ đồng bộ:
```sql
-- 1. Thêm thú cưng vào bảng pets
INSERT INTO pets (owner_id, name, avatar_url, species_id, breed_id, gender, date_of_birth, current_weight, color_features, spayed_status, microchip_number, status, notes)
VALUES (1, 'Luna', 'https://avatar-url.com', 1, 12, 'female', '2024-07-18', 4.20, 'Golden, White chest', 'spayed', '123456789', 'active', 'Luna loves playing fetch');

SET @new_pet_id = LAST_INSERT_ID();

-- 2. Thêm lịch sử cân nặng ban đầu vào weight_logs để vẽ biểu đồ
INSERT INTO weight_logs (pet_id, weight, logged_date, logged_by)
VALUES (@new_pet_id, 4.20, CURRENT_DATE(), 1);

-- 3. Lưu thông tin dị ứng/bệnh lý mãn tính vào health_conditions (nếu có)
INSERT INTO health_conditions (pet_id, type, title, is_active)
VALUES (@new_pet_id, 'allergy', 'Allergic to chicken', TRUE);

-- 4. Tự động sinh danh sách tiêm chủng mẫu dựa trên Loài của Pet
INSERT INTO pet_vaccinations (pet_id, vaccine_template_id, vaccine_name, dose_number, status, scheduled_date)
SELECT @new_pet_id, id, vaccine_name, dose_number, 'scheduled', DATE_ADD('2024-07-18', INTERVAL recommended_age_weeks WEEK)
FROM vaccine_templates
WHERE species_id = 1; -- 1 đại diện cho loài Chó

-- 5. Tạo sự kiện khởi tạo thú cưng trên Timeline
INSERT INTO pet_timeline_events (pet_id, event_type, reference_id, event_date, summary)
VALUES (@new_pet_id, 'profile_created', @new_pet_id, CURRENT_DATE(), 'Hồ sơ bé Luna đã được khởi tạo thành công.');
```
- **API -> Client:** Trả về HTTP 201 Created chứa thông tin chi tiết của thú cưng mới tạo.
- **Client:** Điều hướng về màn hình danh sách thú cưng (Pets), cập nhật danh sách hiển thị (thêm bé Luna vào danh sách hiển thị dạng ngang ở Dashboard đầu trang).

---

### 2.2. Luồng Mời Đồng Chăm Sóc (Co-Parenting Link)
1. **Bước 1 (Chủ nuôi chính):** Tại trang quản lý thú cưng, chọn "Add Co-Parent" -> Nhập email người nhận và quyền hạn (`role` - editor / viewer) -> Nhấn "Generate Code".
2. **Bước 2 (API -> DB):** API sinh mã mời ngẫu nhiên gồm 8 ký tự duy nhất, thiết lập thời gian hết hạn là 24 giờ:
   ```sql
   INSERT INTO co_parent_invitations (pet_id, inviter_id, invitee_email, invite_code, role, status, expires_at)
   VALUES (1, 1, 'family_member@example.com', 'PET-X9A2F4', 'editor', 'pending', NOW() + INTERVAL 24 HOUR);
   ```
3. **Bước 3 (Chủ nuôi chính):** Bấm nút Share trên UI để gửi mã `PET-X9A2F4` cho thành viên gia đình qua ứng dụng nhắn tin.
4. **Bước 4 (Người nhận lời mời):** Mở ứng dụng -> Chọn "Enter Invite Code" -> Nhập mã và xác nhận.
5. **Bước 5 (API -> DB):** API kiểm tra tính hợp lệ của mã mời:
   ```sql
   -- Kiểm tra mã mời tồn tại, trạng thái pending và chưa hết hạn
   SELECT * FROM co_parent_invitations 
   WHERE invite_code = 'PET-X9A2F4' AND status = 'pending' AND expires_at > NOW();
   
   -- Thực hiện liên kết quyền đồng chăm sóc
   INSERT INTO pet_co_parents (pet_id, user_id, role, invited_by)
   VALUES (1, 2, 'editor', 1);
   
   -- Cập nhật trạng thái mã mời đã được chấp nhận
   UPDATE co_parent_invitations SET status = 'accepted' WHERE invite_code = 'PET-X9A2F4';
   ```
6. **Bước 6 (Client):** Ứng dụng của người nhận hiển thị thông báo liên kết thành công. Màn hình Dashboard lập tức xuất hiện thêm thú cưng được chia sẻ quyền nuôi.

---

## PHÂN HỆ 3: NHẮC NHỞ CHĂM SÓC & LỊCH TIÊM PHÒNG (REMINDERS & VACCINES)

### 3.1. Luồng Tạo Nhắc Nhở Chăm Sóc Thủ Công
1. **Bước 1 (Client):** Người dùng vào tab Reminders -> Bấm nút **"Add Reminder"** -> Mở Form nhập:
   - Chọn Pet nhận thông báo (Dropdown lấy từ danh sách Pet).
   - Chọn loại hoạt động (Vaccination, Bath, Nail Trim, Deworming, Medication, Checkup, Other).
   - Thiết lập ngày đến hạn (`date`) và giờ cụ thể (`time`).
   - Thiết lập tần suất lặp lại (`repeat` - Daily, Weekly, Monthly, Quarterly, Never).
   - Nhập ghi chú bổ sung (`notes`).
2. **Bước 2 (Client -> API):** Nhấn **"Save"** -> Gửi request `POST /api/v1/reminders`.
3. **Bước 3 (API -> DB):** API tính toán thời điểm nhắc nhở và lưu vào DB:
   ```sql
   INSERT INTO care_reminders (pet_id, category, title, reminder_time, frequency, start_date, next_due_date, is_active)
   VALUES (1, 'bathing', 'Bath Time for Luna', '17:00:00', 'weekly', '2026-06-14', '2026-06-14', TRUE);
   
   -- Khởi tạo bản ghi logs trạng thái chờ đầu tiên
   INSERT INTO care_reminder_logs (reminder_id, due_date, status)
   VALUES (LAST_INSERT_ID(), '2026-06-14', 'pending');
   ```

---

### 3.2. Luồng Xử Lý Nhắc Nhở Đến Hạn (Động Cơ Đẩy Thông Báo - Push Notification Engine)
Đây là quy trình tự động chạy ngầm trên máy chủ (Background Job):
1. **Quét lịch hẹn:** Backend chạy một cron job tự động định kỳ (ví dụ: mỗi 5 phút) để tìm các nhắc nhở đến hạn thông báo trong vòng 5 phút tới:
   ```sql
   SELECT cr.id, cr.title, cr.category, p.name AS pet_name, p.owner_id
   FROM care_reminders cr
   JOIN pets p ON cr.pet_id = p.id
   WHERE cr.is_active = TRUE 
     AND cr.next_due_date = CURRENT_DATE()
     AND HOUR(cr.reminder_time) = HOUR(CURRENT_TIME())
     AND MINUTE(cr.reminder_time) BETWEEN MINUTE(CURRENT_TIME()) AND (MINUTE(CURRENT_TIME()) + 5);
   ```
2. **Tìm người nhận:** Với mỗi nhắc nhở đến hạn, hệ thống truy vấn danh sách các token thiết bị từ các phiên làm việc đang hoạt động của chủ sở hữu chính và toàn bộ đồng chăm sóc (Co-parents) (đã gộp bảng `user_devices` vào `user_sessions`):
   ```sql
   SELECT us.user_id, us.device_token
   FROM user_sessions us
   WHERE (us.user_id = :owner_id OR us.user_id IN (
       SELECT user_id FROM pet_co_parents WHERE pet_id = :pet_id
   ))
     AND us.is_revoked = FALSE 
     AND us.expires_at > NOW()
     AND us.device_token IS NOT NULL;
   ```
3. **Ghi log & Gửi tin:** Hệ thống chèn lịch sử thông báo đã gửi vào bảng `notifications` và gửi payload tin nhắn đẩy tới cổng dịch vụ của Expo:
   ```sql
   INSERT INTO notifications (user_id, title, content, type, reference_id)
   VALUES (:receiver_id, 'Nhắc nhở chăm sóc', 'Đến giờ tắm cho bé Luna rồi!', 'vaccine_reminder', :reminder_id);
   ```
4. **Hiển thị trên điện thoại:** Máy chủ Expo chuyển tiếp thông báo. Điện thoại của tất cả các thành viên trong gia đình thú cưng cùng đổ chuông và hiện thông báo: *"Đến giờ tắm cho bé Luna rồi!"*.

---

### 3.3. Luồng Tương Tác với Thông Báo Nhắc Nhở (Đọc / Hoàn Thành / Đặt lại lịch)
Khi người dùng nhận được thông báo đẩy trên điện thoại hoặc mở tab Thông báo:
1. **Bước 1 (User -> Client):** Chạm vào thông báo -> Ứng dụng tự động kích hoạt chế độ **Deep Linking** mở trang chi tiết nhắc nhở (`Reminder Detail Dialog` - Khớp thiết kế góc dưới bên phải Image 4).
2. **Bước 2 (User -> Client):** Chọn một trong ba nút hành động:
   * **Lựa chọn A: Bấm "Mark Complete" (Hoàn thành công việc):**
     1. App gửi request `PATCH /api/v1/reminders/logs/{log_id}/complete`.
     2. Backend API cập nhật trạng thái lịch sử và tự động tính toán lịch tiếp theo:
        ```sql
        -- Cập nhật log hiện tại thành đã hoàn thành
        UPDATE care_reminder_logs 
        SET status = 'completed', completed_at = CURRENT_TIMESTAMP, completed_by = :user_id 
        WHERE id = :log_id;
        
        -- Tính toán ngày lặp lại tiếp theo của lịch gốc
        UPDATE care_reminders 
        SET next_due_date = DATE_ADD(next_due_date, INTERVAL 7 DAY) -- Ví dụ tần suất weekly
        WHERE id = :reminder_id;
        
        -- Tạo log rỗng trạng thái pending cho lịch tiếp theo
        INSERT INTO care_reminder_logs (reminder_id, due_date, status)
        VALUES (:reminder_id, DATE_ADD(CURRENT_DATE(), INTERVAL 7 DAY), 'pending');
        
        -- Thêm một dấu mốc hoàn thành chăm sóc vào Timeline của Pet
        INSERT INTO pet_timeline_events (pet_id, event_type, reference_id, event_date, summary)
        VALUES (:pet_id, 'reminder_completed', :log_id, CURRENT_DATE(), 'Đã hoàn thành tắm rửa cho bé Luna.');
        ```
   * **Lựa chọn B: Bấm "Reschedule" (Đặt lại lịch mới):**
     1. Hộp thoại lịch hiện lên -> Người dùng chọn thời điểm mới (Ví dụ: báo lại sau 1 tiếng hoặc dời sang ngày mai).
     2. Gửi request cập nhật. API thực hiện chỉnh sửa cột `snoozed_until` hoặc thay đổi trực tiếp ngày đến hạn:
        ```sql
        UPDATE care_reminder_logs 
        SET status = 'snoozed', snoozed_until = DATE_ADD(NOW(), INTERVAL 1 HOUR)
        WHERE id = :log_id;
        ```
   * **Lựa chọn C: Bấm "Delete" (Xóa bỏ):**
     1. Gửi lệnh xóa lên API -> API thu hồi nhắc nhở hiện tại và tắt lịch nhắc nhở:
        ```sql
        UPDATE care_reminders SET is_active = FALSE WHERE id = :reminder_id;
        DELETE FROM care_reminder_logs WHERE id = :log_id AND status = 'pending';
        ```

---

## PHÂN HỆ 4: THEO DÕI SỨC KHỎE CƠ BẢN (HEALTH TRACKING)

### 4.1. Luồng Ghi Nhật Ký Sức Khỏe & Cân Nặng Hàng Ngày
1. **Bước 1 (Client):** Tại chi tiết thú cưng, người dùng chọn tab **"Health Logs"** -> Nhấn **"Add Log"** -> Mở Form nhập:
   - Cân nặng hiện tại (`weight` - ví dụ: 4.2 kg).
   - Mức độ ăn uống (`appetite` - chọn 1 trong: Good, Normal, Poor).
   - Ghi chú các biểu hiện bất thường hoặc thức ăn sử dụng (`notes`).
2. **Bước 2 (Client -> API):** Bấm nút **"Save"** -> Gửi request `POST /api/v1/health-logs`.
3. **Bước 3 (API -> DB):** Backend xử lý trong 1 transaction khép kín để đảm bảo tính đồng bộ dữ liệu:
   ```sql
   START TRANSACTION;
   
   -- 1. Lưu log sức khỏe hàng ngày vào bảng health_logs
   INSERT INTO health_logs (pet_id, logged_date, appetite, treatment_notes, logged_by)
   VALUES (:pet_id, CURRENT_DATE(), :appetite, :notes, :user_id)
   ON DUPLICATE KEY UPDATE appetite = :appetite, treatment_notes = :notes;
   
   -- 2. Thêm một bản ghi vào weight_logs để lưu lịch sử cân nặng phục vụ vẽ biểu đồ
   INSERT INTO weight_logs (pet_id, weight, logged_date, logged_by)
   VALUES (:pet_id, :weight, CURRENT_DATE(), :user_id);
   
   -- 3. Cập nhật cân nặng tức thời tại bảng chính pets
   UPDATE pets SET current_weight = :weight WHERE id = :pet_id;
   
   -- 4. Ghi nhận sự kiện cập nhật cân nặng lên dòng thời gian Timeline của Pet
   INSERT INTO pet_timeline_events (pet_id, event_type, reference_id, event_date, summary)
   VALUES (:pet_id, 'weight_updated', LAST_INSERT_ID(), CURRENT_DATE(), CONCAT('Cân nặng được cập nhật mới: ', :weight, ' kg.'));
   
   COMMIT;
   ```
4. **Bước 4 (API -> Client):** Trả về HTTP 200 OK kèm thông tin log vừa lưu.
5. **Bước 5 (Client):** Tab **Health** lập tức tải lại dữ liệu mới, vẽ điểm cân nặng mới tăng/giảm trên Biểu đồ đường (Line Chart) và tab **Timeline** hiển thị một sự kiện mới: *"Cân nặng được cập nhật mới: 4.2 kg."* (Khớp UI thiết kế Image 3).

---

## PHÂN HỆ 5: PHÂN HỆ MẠNG XÃ HỘI (SOCIAL LAYER)

### 5.1. Luồng Đăng Tải Khoảnh Khắc (Create Post)
1. **Bước 1 (Client):** Người dùng nhập nội dung chia sẻ (caption), chọn 1 hoặc nhiều ảnh từ điện thoại, gắn thẻ thú cưng liên quan (`pet_id`), và chọn chế độ riêng tư (Chỉ mình tôi - `private` hoặc Chia sẻ cho bạn bè - `friends` kiểu Zalo/Locket).
2. **Bước 2 (Client -> Cloud):** Ứng dụng upload các tệp ảnh trực tiếp lên Cloud Storage (Amazon S3 hoặc Cloudinary) -> Nhận về mảng chứa danh sách các đường dẫn URL ảnh dạng chuỗi (ví dụ: `["https://s3.com/image1.jpg", "https://s3.com/image2.jpg"]`).
3. **Bước 3 (Client -> API):** Gửi request `POST /api/v1/posts` chứa payload JSON:
   ```json
   {
     "caption": "Luna's first time at the beach! 🏖️ She loved the waves",
     "images": "[\"https://s3.com/image1.jpg\", \"https://s3.com/image2.jpg\"]",
     "pet_id": 1,
     "privacy": "friends"
   }
   ```
4. **Bước 4 (API -> DB):** Lưu trữ bài viết và tự động đồng bộ dòng thời gian của thú cưng:
   ```sql
   START TRANSACTION;
   
   -- 1. Lưu bài viết vào bảng posts (Trường images lưu mảng JSON dạng TEXT để tránh JOIN bảng phụ)
   INSERT INTO posts (user_id, pet_id, caption, images, privacy, status)
   VALUES (:user_id, :pet_id, :caption, :images, :privacy, 'published');
   
   SET @new_post_id = LAST_INSERT_ID();
   
   -- 2. Tự động thêm sự kiện đăng bài MXH lên dòng thời gian sự kiện của Pet
   INSERT INTO pet_timeline_events (pet_id, event_type, reference_id, event_date, summary)
   VALUES (:pet_id, 'social_post', @new_post_id, CURRENT_DATE(), 'Chủ nuôi đã đăng một bài viết mới về bé Luna lên mạng xã hội.');
   
   COMMIT;
   ```
5. **Bước 5 (API -> Client):** Trả về HTTP 201 Created.
6. **Bước 6 (Client):** Điều hướng về màn hình Bảng tin chung hoặc màn hình chi tiết thú cưng. 
   - **Tự động gom Album:** Khi người dùng mở tab **"Album"** của thú cưng Luna (Image 3), app sẽ chỉ cần gọi API lấy tất cả các ảnh mà thú cưng được gắn thẻ:
     ```sql
     SELECT images FROM posts WHERE pet_id = 1 AND status = 'published' ORDER BY created_at DESC;
     ```
     Sau đó parse chuỗi JSON của cột `images` để kết xuất lưới ảnh (Photo Grid View) mượt mà mà không cần join nhiều bảng phức tạp.

---

### 5.2. Luồng Kết Bạn & Thiết Lập Quan Hệ (Friendship Connection)
Hệ thống kết bạn hai chiều đồng thuận (Zalo / Locket style) yêu cầu cả 2 người dùng kết bạn với nhau để xem được khoảnh khắc của nhau trên Bảng tin:
1. **Bước 1 (User A -> Client):** Người dùng A truy cập hồ sơ của người dùng B trên app -> Nhấn nút **"Add Friend"** (Kết bạn).
2. **Bước 2 (Client -> API):** Gửi yêu cầu kết bạn `POST /api/v1/friends/requests` với `receiver_id` là ID của B.
3. **Bước 3 (API -> DB):** Xử lý chèn lời mời kết bạn ở trạng thái chờ:
   ```sql
   INSERT INTO friend_requests (sender_id, receiver_id, status)
   VALUES (:sender_id, :receiver_id, 'pending');
   ```
4. **Bước 4 (API - Notification Engine):** Tạo thông báo đẩy và lưu thông tin thông báo cho User B:
   ```sql
   INSERT INTO notifications (user_id, title, content, type, reference_id)
   VALUES (:receiver_id, 'Lời mời kết bạn', 'Nguyen Van A đã gửi cho bạn lời mời kết bạn.', 'system', LAST_INSERT_ID());
   ```
5. **Bước 5 (User B -> Client):** Người dùng B nhận thông báo đẩy -> Click vào thông báo điều hướng tới danh sách lời mời -> Nhấn nút **"Accept"** (Đồng ý).
6. **Bước 6 (Client -> API):** Gửi yêu cầu chấp nhận `PATCH /api/v1/friends/requests/{request_id}/accept`.
7. **Bước 7 (API -> DB):** Chấp nhận lời mời kết bạn và lưu thông tin quan hệ bạn bè 2 chiều (transaction):
   ```sql
   START TRANSACTION;
   
   -- 1. Cập nhật trạng thái lời mời kết bạn
   UPDATE friend_requests SET status = 'accepted' WHERE id = :request_id;
   
   -- 2. Thiết lập bạn bè chính thức (LEAST và GREATEST để bảo đảm user_id1 < user_id2 tránh trùng lặp)
   INSERT INTO friendships (user_id1, user_id2)
   VALUES (LEAST(:sender_id, :receiver_id), GREATEST(:sender_id, :receiver_id));
   
   COMMIT;
   ```
8. **Bước 8 (Unfriend - Hủy kết bạn nếu muốn):** Nếu một trong hai người hủy kết bạn -> Gọi API `DELETE /api/v1/friends/{friend_id}` -> Backend thực thi:
   ```sql
   DELETE FROM friendships 
   WHERE user_id1 = LEAST(:user_id, :friend_id) AND user_id2 = GREATEST(:user_id, :friend_id);
   ```

---

### 5.3. Luồng Tương Tác: Thích Bài Viết (Like)
1. **Bước 1 (User -> Client):** Người dùng chạm vào biểu tượng trái tim của bài viết trên Bảng tin.
2. **Bước 2 (Client -> API):** Gửi yêu cầu `POST /api/v1/posts/{post_id}/like`.
3. **Bước 3 (API -> DB):** Lưu lượt thích (Mỗi tài khoản chỉ được thích một lần duy nhất trên mỗi bài viết nhờ khóa chính phức hợp):
   ```sql
   -- Ghi nhận lượt thích
   INSERT INTO post_likes (post_id, user_id) VALUES (:post_id, :user_id);
   ```
4. **Bước 4 (API - Notification Trigger):** Nếu người bấm Thích không phải là tác giả bài viết, hệ thống sẽ chèn thông báo tương tác vào DB để gửi thông báo cho tác giả:
   ```sql
   -- Tìm tác giả bài viết
   SELECT user_id INTO @author_id FROM posts WHERE id = :post_id;
   
   -- Tạo thông báo tương tác
   INSERT INTO notifications (user_id, title, content, type, reference_id)
   VALUES (@author_id, 'Tương tác mới', 'Một người dùng đã thích bài viết của bạn.', 'post_like', :post_id);
   ```
5. **Bước 5 (API -> Client):** Trả về trạng thái đã thích (HTTP 200 OK). Icon trái tim chuyển màu đỏ và số lượt thích tăng thêm 1.

---

### 5.3. Luồng Tương Tác: Bình Luận Phẳng (Flat Comments)
1. **Bước 1 (User -> Client):** Nhập nội dung bình luận vào ô văn bản dưới bài viết -> Nhấn nút Gửi.
2. **Bước 2 (Client -> API):** Gửi request `POST /api/v1/posts/{post_id}/comments` kèm body dạng text.
3. **Bước 3 (API -> DB):** Lưu trữ bình luận:
   ```sql
   INSERT INTO post_comments (post_id, user_id, comment_text, status)
   VALUES (:post_id, :user_id, :comment_text, 'visible');
   ```
4. **Bước 4 (API - Notification Trigger):** Tự động gửi thông báo cho chủ sở hữu bài viết:
   ```sql
   SELECT user_id INTO @author_id FROM posts WHERE id = :post_id;
   
   INSERT INTO notifications (user_id, title, content, type, reference_id)
   VALUES (@author_id, 'Bình luận mới', 'Ai đó đã bình luận về bài viết của bạn.', 'post_comment', LAST_INSERT_ID());
   ```
5. **Bước 5 (API -> Client):** Trả về bản ghi bình luận mới tạo để hiển thị lên màn hình.
6. **Bước 6 (Luồng Xóa Bình Luận):**
   - Người viết bình luận hoặc chủ nhân bài viết có quyền xóa.
   - Khi chọn xóa, gửi request `DELETE /api/v1/comments/{comment_id}`.
   - Để tránh phá vỡ giao diện hiển thị, Backend không xóa vật lý mà chỉ cập nhật ẩn nội dung:
     ```sql
     UPDATE post_comments 
     SET status = 'deleted', comment_text = 'Bình luận này đã bị xóa' 
     WHERE id = :comment_id;
     ```

---

## PHÂN HỆ 6: QUẢN TRỊ ADMIN (ADMIN MODERATION & ANALYTICS)

### 6.1. Luồng Báo Cáo Vi Phạm (Report)
1. **Bước 1 (User -> Client):** Người dùng phát hiện bài viết có nội dung xấu -> Bấm "Báo cáo" -> Chọn lý do vi phạm và xác nhận.
2. **Bước 2 (Client -> API):** Gửi request `POST /api/v1/reports` chứa `target_type` ('post', 'comment', 'user'), `target_id` và lý do.
3. **Bước 3 (API -> DB):** Ghi nhận báo cáo vào DB ở trạng thái chờ:
   ```sql
   INSERT INTO reports (reporter_user_id, target_type, target_id, reason, status)
   VALUES (:user_id, 'post', :post_id, 'Nội dung phản cảm, spam', 'pending');
   ```

---

### 6.2. Luồng Kiểm Duyệt Của Admin (Admin Resolution)
1. **Bước 1 (Admin -> Web):** Admin đăng nhập vào trang quản trị (Admin Dashboard) -> Xem danh sách báo cáo ở trạng thái `pending`.
2. **Bước 2 (Admin):** Xem chi tiết nội dung bị báo cáo, bấm chọn "Approve Report & Hide Post" (Đồng ý báo cáo và ẩn bài viết).
3. **Bước 3 (API -> DB):** API thực thi cập nhật trạng thái bài viết, cập nhật báo cáo và ghi nhận lịch sử kiểm duyệt (Audit Log):
   ```sql
   START TRANSACTION;
   
   -- 1. Ẩn bài viết khỏi Bảng tin cộng đồng
   UPDATE posts SET status = 'hidden' WHERE id = :post_id;
   
   -- 2. Cập nhật báo cáo đã được giải quyết
   UPDATE reports SET status = 'resolved', resolved_by = :admin_id, moderator_notes = 'Đã ẩn bài viết vi phạm tiêu chuẩn.' WHERE id = :report_id;
   
   -- 3. Lưu lịch sử hành động kiểm duyệt của Admin
   INSERT INTO admin_audits (admin_id, action, target_id, details)
   VALUES (:admin_id, 'hide_post', :post_id, 'Ẩn bài viết do vi phạm tiêu chuẩn cộng đồng.');
   
   COMMIT;
   ```
4. **Bước 4 (API -> Client):** Trả về trạng thái xử lý thành công. Bài viết lập tức biến mất khỏi bảng tin của tất cả người dùng ứng dụng.

---

## PHÂN HỆ 7: HỒ SƠ Y TẾ ĐIỆN TỬ (ELECTRONIC MEDICAL RECORD - EMR)

### 7.1. Luồng Tạo Mới Hồ Sơ EMR & Đính Kèm Tài Liệu
1. **Bước 1 (User -> Client):** Tại màn hình chi tiết thú cưng, người dùng chọn tab **"EMR"** -> Nhấn **"Add EMR Record"** -> Mở Form nhập:
   - Chọn loại hồ sơ (`record_type` - khám bệnh: 'visit', đơn thuốc: 'prescription', xét nghiệm: 'lab_result', phẫu thuật: 'surgery', khác: 'other').
   - Chọn ngày khám bệnh (`visit_date`).
   - Nhập tên phòng khám (`clinic_name`), tên bác sĩ thú y (`vet_name`) và thông tin liên hệ (`vet_contact`).
   - Nhập chẩn đoán bệnh (`diagnosis`) và chi tiết đơn thuốc (`prescription_details`).
   - Nhập ghi chú bổ sung (`notes`).
   - Chọn tải lên các tài liệu đính kèm (phim chụp X-Quang, kết quả xét nghiệm...).
2. **Bước 2 (Client -> Cloud):** Ứng dụng upload các tệp đính kèm lên Cloud Storage -> Nhận về danh sách thông tin tệp (gồm tên tệp, URL và loại tệp).
3. **Bước 3 (Client -> API):** Gửi request `POST /api/v1/emr-records` kèm JSON body:
   ```json
   {
     "pet_id": 1,
     "record_type": "visit",
     "visit_date": "2026-06-13",
     "clinic_name": "PetCare Clinic",
     "vet_name": "Dr. John Doe",
     "vet_contact": "0901234567",
     "diagnosis": "Viêm tai ngoài dị ứng",
     "prescription_details": "Thuốc nhỏ tai Dexoryl 10ml, ngày 2 lần",
     "notes": "Tái khám sau 7 ngày",
     "attachments": [
       {
         "file_name": "tai_viem.jpg",
         "file_url": "https://s3.com/tai_viem.jpg",
         "file_type": "image/jpeg"
       }
     ]
   }
   ```
4. **Bước 4 (API -> DB):** Backend lưu trữ hồ sơ EMR trực tiếp kèm cột `attachments` dạng JSON array (gộp bảng `emr_attachments` cũ) và đồng bộ lên dòng thời gian Timeline của Pet:
   ```sql
   START TRANSACTION;

   -- 1. Lưu hồ sơ EMR (cột attachments chứa chuỗi JSON đại diện cho mảng các tệp đính kèm)
   INSERT INTO emr_records (pet_id, record_type, visit_date, clinic_name, vet_name, vet_contact, diagnosis, prescription_details, notes, attachments, created_by)
   VALUES (1, 'visit', '2026-06-13', 'PetCare Clinic', 'Dr. John Doe', '0901234567', 'Viêm tai ngoài dị ứng', 'Thuốc nhỏ tai Dexoryl 10ml, ngày 2 lần', 'Tái khám sau 7 ngày', '[{"file_name": "tai_viem.jpg", "file_url": "https://s3.com/tai_viem.jpg", "file_type": "image/jpeg"}]', 1);

   SET @new_emr_id = LAST_INSERT_ID();

   -- 2. Đồng bộ sự kiện khám bệnh y tế lên dòng thời gian sự kiện của Pet
   INSERT INTO pet_timeline_events (pet_id, event_type, reference_id, event_date, summary)
   VALUES (1, 'medical_visit', @new_emr_id, '2026-06-13', 'Bé Luna được khám bệnh tại PetCare Clinic: Viêm tai ngoài dị ứng.');

   COMMIT;
   ```
5. **Bước 5 (API -> Client):** Trả về HTTP 201 Created.
6. **Bước 6 (Client):** Điều hướng về danh sách EMR của Pet, hiển thị thông tin bệnh án và các tài liệu đính kèm từ cột `attachments` đã phân tích cú pháp.
