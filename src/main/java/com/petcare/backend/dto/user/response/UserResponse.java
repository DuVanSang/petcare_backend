package com.petcare.backend.dto.user.response;

import com.petcare.backend.model.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
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
    private Boolean emailAlertsEnabled;
    private Boolean reminderAlertsEnabled;
    private Boolean publicProfileEnabled;
    private Boolean shareLocationEnabled;
    private String postDefaultPrivacy;
    private String role;
    private String status;
    private Boolean emailVerified;
    private Boolean isOnline;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
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
                .pushNotificationEnabled(user.getPushNotificationEnabled() != null ? user.getPushNotificationEnabled() : true)
                .emailAlertsEnabled(user.getEmailAlertsEnabled() != null ? user.getEmailAlertsEnabled() : true)
                .reminderAlertsEnabled(user.getReminderAlertsEnabled() != null ? user.getReminderAlertsEnabled() : true)
                .publicProfileEnabled(user.getPublicProfileEnabled() != null ? user.getPublicProfileEnabled() : true)
                .shareLocationEnabled(user.getShareLocationEnabled() != null ? user.getShareLocationEnabled() : true)
                .postDefaultPrivacy(user.getPostDefaultPrivacy() != null ? user.getPostDefaultPrivacy() : "friends")
                .role(user.getRole())
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .isOnline(user.getIsOnline())
                .lastActiveAt(user.getLastActiveAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
