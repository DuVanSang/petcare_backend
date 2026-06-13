package com.petcare.backend.dto.user.response;

import com.petcare.backend.model.RefreshToken;
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

    public static UserDeviceResponse from(RefreshToken session) {
        return UserDeviceResponse.builder()
                .id(session.getId())
                .deviceId(session.getIpAddress() != null ? session.getIpAddress() : "Session-" + session.getId())
                .deviceName(session.getUserAgent() != null ? session.getUserAgent() : "Thiết bị di động")
                .deviceType(session.getDeviceType())
                .deviceToken(session.getDeviceToken())
                .notificationEnabled(session.getDeviceToken() != null)
                .appVersion("1.0.0")
                .osVersion(session.getUserAgent() != null ? session.getUserAgent() : "Unknown OS")
                .lastActiveAt(session.getUpdatedAt())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
