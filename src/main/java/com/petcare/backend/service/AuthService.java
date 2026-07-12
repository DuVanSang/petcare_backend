package com.petcare.backend.service;

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

public interface AuthService {
    RegisterResponse register(RegisterRequest request);

    AuthResponse verifyEmail(VerifyEmailRequest request);

    OtpDeliveryResponse resendVerificationCode(ResendVerificationRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse loginWithGoogle(GoogleLoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(LogoutRequest request);

    OtpDeliveryResponse forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
