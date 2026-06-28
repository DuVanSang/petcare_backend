# Hướng dẫn đăng nhập Google (dev)

## 1. Cấu hình Google Cloud (làm 1 lần)

1. Vào https://console.cloud.google.com/apis/credentials
2. Tạo **OAuth client ID** → loại **Web application**
3. Thêm **Authorized redirect URI**:
   ```
   http://localhost:9090
   ```
4. Copy **Client ID** (dạng `xxx.apps.googleusercontent.com`)
5. Vào **OAuth consent screen** → thêm email test vào **Test users**

## 2. Cấu hình project

Copy `.env.example` thành `.env`, thêm:

```properties
GOOGLE_CLIENT_ID=xxx.apps.googleusercontent.com
```

Chạy backend:

```bash
mvn spring-boot:run
```

## 3. Lấy idToken để test

Mở link sau trên trình duyệt (thay `CLIENT_ID`):

```
https://accounts.google.com/o/oauth2/v2/auth?client_id=CLIENT_ID&redirect_uri=http://localhost:9090&response_type=id_token&scope=openid%20email%20profile&nonce=abc123
```

Đăng nhập Google → copy phần `id_token` trên thanh địa chỉ (sau `id_token=`, trước `&authuser`).

## 4. Gọi API

**Swagger:** http://localhost:9090/swagger-ui.html

**Endpoint:** `POST /api/v1/auth/google`

```json
{
  "idToken": "dán_token_vào_đây"
}
```

Thành công → nhận `accessToken`, `refreshToken`, thông tin `user`.

## 5. Lưu ý

- File `.env` **không** push lên Git — mỗi người tự tạo
- Mỗi dev có thể dùng OAuth client riêng, hoặc team chia sẻ 1 Client ID
- `idToken` hết hạn sau ~1 giờ → lấy token mới khi test lại
