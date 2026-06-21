package com.petcare.backend.service;

public interface EmailService {
    void sendVerificationOtp(String toEmail, String otpCode);

    void sendPasswordResetOtp(String toEmail, String otpCode);

    void sendCoParentInvitation(String toEmail, String inviterName, String petName, String inviteCode);
}
