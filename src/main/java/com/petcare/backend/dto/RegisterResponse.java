package com.petcare.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponse {
    private Long userId;
    private String email;
    private boolean emailVerified;
}
