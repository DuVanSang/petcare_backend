package com.petcare.backend.dto.auth.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleLoginRequest {
    @NotBlank(message = "Google ID token không được để trống")
    private String idToken;

    @Valid
    private DeviceInfoRequest device;
}
