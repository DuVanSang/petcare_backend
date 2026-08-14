package com.petcare.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_phone_number", columnNames = "phone_number"),
                @UniqueConstraint(name = "uk_users_username", columnNames = "username")
        },
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_username", columnList = "username"),
                @Index(name = "idx_users_phone_number", columnList = "phone_number"),
                @Index(name = "idx_users_role_status", columnList = "role,status"),
                @Index(name = "idx_users_status_deleted_at", columnList = "status,deleted_at"),
                @Index(name = "idx_users_last_active_at", columnList = "last_active_at")
        })
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(length = 50)
    private String username;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(length = 150)
    private String bio;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 150)
    private String location;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode = "vi";

    @Column(nullable = false, length = 50)
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "push_notification_enabled", nullable = false)
    private Boolean pushNotificationEnabled = true;

    @Column(name = "email_alerts_enabled", nullable = false)
    private Boolean emailAlertsEnabled = true;

    @Column(name = "reminder_alerts_enabled", nullable = false)
    private Boolean reminderAlertsEnabled = true;

    @Column(name = "public_profile_enabled", nullable = false)
    private Boolean publicProfileEnabled = true;

    @Column(name = "share_location_enabled", nullable = false)
    private Boolean shareLocationEnabled = true;

    @Column(name = "post_default_privacy", nullable = false, length = 20)
    private String postDefaultPrivacy = "friends";

    @Column(nullable = false)
    private String role = "user";

    @Column(nullable = false)
    private String status = "active";

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "is_online", nullable = false)
    private Boolean isOnline = false;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
