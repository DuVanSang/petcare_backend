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

    public static UserDeviceResponse from(UserDevice userDevice) {
        return UserDeviceResponse.builder()
                .id(userDevice.getId())
                .deviceId(userDevice.getDeviceId())
                .deviceName(userDevice.getDeviceName())
                .deviceType(userDevice.getDeviceType())
                .deviceToken(userDevice.getDeviceToken())
                .notificationEnabled(userDevice.getNotificationEnabled())
                .appVersion(userDevice.getAppVersion())
                .osVersion(userDevice.getOsVersion())
                .lastActiveAt(userDevice.getLastActiveAt())
                .createdAt(userDevice.getCreatedAt())
                .updatedAt(userDevice.getUpdatedAt())
                .build();
    }
}
