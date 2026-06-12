package com.petcare.backend.service;

import com.petcare.backend.model.User;

public interface EmailVerificationService {
    void createAndSendOtp(User user);

    void verify(User user, String otpCode);
}
