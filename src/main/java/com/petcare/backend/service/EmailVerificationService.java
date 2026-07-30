package com.petcare.backend.service;

import com.petcare.backend.model.User;

public interface EmailVerificationService {
    String createOtp(User user);

    String createOtpForExistingUser(Long userId);

    void verify(User user, String otpCode);
}
