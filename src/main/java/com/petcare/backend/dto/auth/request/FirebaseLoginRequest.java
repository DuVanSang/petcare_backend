package com.petcare.backend.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FirebaseLoginRequest {
    @NotBlank(message = "Firebase ID token là bắt buộc")
    private String idToken;
}
