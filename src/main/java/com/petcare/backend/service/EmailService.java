package com.petcare.backend.service;

public interface EmailService {
    boolean sendVerificationOtp(String toEmail, String otpCode);

    boolean sendPasswordResetOtp(String toEmail, String otpCode);
}
