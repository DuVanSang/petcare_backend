package com.petcare.backend.dto.auth.request;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceInfoRequest {
    private String deviceId;

    @Pattern(regexp = "^$|ios|android|web", message = "Loại thiết bị phải là ios, android hoặc web")
    private String deviceType;

    private String deviceToken;
}
