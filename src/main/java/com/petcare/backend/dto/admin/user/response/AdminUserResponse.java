package com.petcare.backend.dto.admin.user.response;

import com.petcare.backend.model.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminUserResponse {
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
    private String role;
    private String status;
    private Boolean emailVerified;
    private Boolean isOnline;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long petCount;
    private Long postCount;

    public static AdminUserResponse from(User user) {
        return from(user, null, null);
    }

    public static AdminUserResponse from(User user, Long petCount, Long postCount) {
        return AdminUserResponse.builder()
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
                .role(user.getRole())
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .isOnline(user.getIsOnline())
                .lastActiveAt(user.getLastActiveAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .petCount(petCount)
                .postCount(postCount)
                .build();
    }
}
