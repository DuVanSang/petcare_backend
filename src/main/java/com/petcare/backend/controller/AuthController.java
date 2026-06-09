package com.petcare.backend.controller;

import com.petcare.backend.dto.ApiResponse;
import com.petcare.backend.dto.AuthResponse;
import com.petcare.backend.dto.LoginRequest;
import com.petcare.backend.dto.RegisterRequest;
import com.petcare.backend.dto.RegisterResponse;
import com.petcare.backend.dto.ResendVerificationRequest;
import com.petcare.backend.dto.VerifyEmailRequest;
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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Dang ky thanh cong, vui long kiem tra email de lay ma OTP",
                        authService.register(request)));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Xac thuc email thanh cong", authService.verifyEmail(request)));
    }

    @PostMapping("/resend-verification-code")
    public ResponseEntity<ApiResponse<Void>> resendVerificationCode(
            @Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerificationCode(request);
        return ResponseEntity.ok(ApiResponse.success("Da gui lai ma OTP", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Dang nhap thanh cong", authService.login(request)));
    }
}
