package com.petcare.backend.dto.auth.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceInfoRequest {
    private String deviceId;

    @Size(max = 255, message = "Tên thiết bị không được vượt quá 255 ký tự")
    private String deviceName;

    @Pattern(regexp = "^$|ios|android|web", message = "Loại thiết bị phải là ios, android hoặc web")
    private String deviceType;

    private String deviceToken;

    private Boolean notificationEnabled;

    @Size(max = 50, message = "Phiên bản ứng dụng không được vượt quá 50 ký tự")
    private String appVersion;

    @Size(max = 100, message = "Phiên bản hệ điều hành không được vượt quá 100 ký tự")
    private String osVersion;
}
