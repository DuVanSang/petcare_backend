package com.petcare.backend.dto.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailVerificationRequiredResponse {
    private String email;
    private boolean requiresEmailVerification;
}
