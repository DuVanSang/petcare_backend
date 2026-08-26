package com.petcare.backend.controller;

import com.petcare.backend.dto.auth.request.FirebaseLoginRequest;
import com.petcare.backend.dto.auth.request.ForgotPasswordRequest;
import com.petcare.backend.dto.auth.request.GoogleLoginRequest;
import com.petcare.backend.dto.auth.request.LoginRequest;
import com.petcare.backend.dto.auth.request.LogoutRequest;
import com.petcare.backend.dto.auth.request.RefreshTokenRequest;
import com.petcare.backend.dto.auth.request.RegisterRequest;
import com.petcare.backend.dto.auth.request.ResendVerificationRequest;
import com.petcare.backend.dto.auth.request.ResetPasswordRequest;
import com.petcare.backend.dto.auth.request.VerifyEmailRequest;
import com.petcare.backend.dto.auth.response.AuthResponse;
import com.petcare.backend.dto.auth.response.OtpDeliveryResponse;
import com.petcare.backend.dto.auth.response.RegisterResponse;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse result = authService.register(request);
        if (result.isExistingUnverifiedAccount()) {
            String pendingMessage = result.getDevOtp() != null
                    ? "Tài khoản chưa xác thực. Không gửi được email, vui lòng dùng mã OTP hiển thị trên màn hình"
                    : "Tài khoản chưa xác thực. Mã OTP mới đã được gửi đến email của bạn";
            return ResponseEntity.ok(ApiResponse.success(pendingMessage, result));
        }

        String message = result.getDevOtp() != null
                ? "Đăng ký thành công. Không gửi được email, vui lòng dùng mã OTP hiển thị trên màn hình"
                : "Đăng ký thành công, vui lòng kiểm tra email để lấy mã OTP";
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(message, result));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Xác thực email thành công", authService.verifyEmail(request)));
    }

    @PostMapping("/resend-verification-code")
    public ResponseEntity<ApiResponse<OtpDeliveryResponse>> resendVerificationCode(
            @Valid @RequestBody ResendVerificationRequest request) {
        OtpDeliveryResponse delivery = authService.resendVerificationCode(request);
        String message = delivery.isEmailSent()
                ? "Đã gửi lại mã OTP"
                : "Không gửi được email, vui lòng dùng mã OTP hiển thị trên màn hình";
        return ResponseEntity.ok(ApiResponse.success(message, delivery));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", authService.login(request)));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập Google thành công", authService.loginWithGoogle(request)));
    }

    @PostMapping("/firebase")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithFirebase(@Valid @RequestBody FirebaseLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập Firebase thành công", authService.loginWithFirebase(request)));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<OtpDeliveryResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        OtpDeliveryResponse delivery = authService.forgotPassword(request);
        String message = delivery.getDevOtp() != null
                ? "Không gửi được email, vui lòng dùng mã OTP hiển thị trên màn hình"
                : "Nếu email tồn tại và tài khoản đang hoạt động, mã OTP đặt lại mật khẩu sẽ được gửi đến email của bạn";
        return ResponseEntity.ok(ApiResponse.success(message, delivery));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công", null));
    }
}
