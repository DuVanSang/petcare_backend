package com.petcare.backend.dto.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponse {
    private Long userId;
    private String email;
    private boolean emailVerified;
}
