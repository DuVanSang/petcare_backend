package com.petcare.backend.service;

import com.petcare.backend.dto.AuthResponse;
import com.petcare.backend.dto.ForgotPasswordRequest;
import com.petcare.backend.dto.LoginRequest;
import com.petcare.backend.dto.LogoutRequest;
import com.petcare.backend.dto.RefreshTokenRequest;
import com.petcare.backend.dto.RegisterRequest;
import com.petcare.backend.dto.RegisterResponse;
import com.petcare.backend.dto.ResendVerificationRequest;
import com.petcare.backend.dto.ResetPasswordRequest;
import com.petcare.backend.dto.VerifyEmailRequest;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);

    AuthResponse verifyEmail(VerifyEmailRequest request);

    void resendVerificationCode(ResendVerificationRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(LogoutRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
