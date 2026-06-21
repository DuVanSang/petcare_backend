package com.petcare.backend.dto.user.response;

import com.petcare.backend.model.UserDevice;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDeviceResponse {
    private Long id;
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String deviceToken;
    private Boolean notificationEnabled;
    private String appVersion;
    private String osVersion;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserDeviceResponse from(UserDevice device) {
        return UserDeviceResponse.builder()
                .id(device.getId())
                .deviceId(device.getDeviceId())
                .deviceName(device.getDeviceName())
                .deviceType(device.getDeviceType())
                .deviceToken(device.getDeviceToken())
                .notificationEnabled(device.getNotificationEnabled())
                .appVersion(device.getAppVersion())
                .osVersion(device.getOsVersion())
                .lastActiveAt(device.getLastActiveAt())
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }
}
