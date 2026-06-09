package com.petcare.backend.dto;

import com.petcare.backend.model.User;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
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
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .isOnline(user.getIsOnline())
                .lastActiveAt(user.getLastActiveAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
