package com.petcare.backend.dto.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponse {
    private Long userId;
    private String email;
    private boolean emailVerified;
    private boolean emailSent;
    private String devOtp;
    private boolean existingUnverifiedAccount;
    private boolean requiresEmailVerification;

    public RegisterResponse(Long userId, String email, boolean emailVerified, boolean emailSent, String devOtp) {
        this(userId, email, emailVerified, emailSent, devOtp, false, true);
    }
}
