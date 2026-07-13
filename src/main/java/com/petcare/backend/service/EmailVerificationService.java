package com.petcare.backend.service;

import com.petcare.backend.model.User;

public interface EmailVerificationService {
    String createOtp(User user);

    void verify(User user, String otpCode);
}
