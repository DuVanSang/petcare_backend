package com.petcare.backend.dto.user.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PasswordStatusResponse {
    private String passwordStatus;
}
