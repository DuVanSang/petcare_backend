package com.petcare.backend.dto.admin.user.response;

import com.petcare.backend.dto.user.response.UserDeviceResponse;
import com.petcare.backend.model.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminUserDetailResponse {
    private Long id;
    private String email;
    private String fullName;
    private String username;
    private String phoneNumber;
    private String avatarUrl;
    private String coverImageUrl;
    private String bio;
    private LocalDate dateOfBirth;
    private String location;
    private String languageCode;
    private String timezone;
    private Boolean pushNotificationEnabled;
    private String role;
    private String status;
    private Boolean emailVerified;
    private LocalDateTime emailVerifiedAt;
    private Boolean isOnline;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private List<UserDeviceResponse> devices;

    public static AdminUserDetailResponse from(User user, List<UserDeviceResponse> devices) {
        return AdminUserDetailResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .coverImageUrl(user.getCoverImageUrl())
                .bio(user.getBio())
                .dateOfBirth(user.getDateOfBirth())
                .location(user.getLocation())
                .languageCode(user.getLanguageCode())
                .timezone(user.getTimezone())
                .pushNotificationEnabled(user.getPushNotificationEnabled())
                .role(user.getRole())
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .isOnline(user.getIsOnline())
                .lastActiveAt(user.getLastActiveAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .devices(devices)
                .build();
    }
}
