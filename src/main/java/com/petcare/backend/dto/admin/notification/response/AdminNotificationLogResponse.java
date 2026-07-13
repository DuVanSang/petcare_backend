package com.petcare.backend.dto.admin.notification.response;

import com.petcare.backend.model.Notification;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminNotificationLogResponse {
    private Long id;
    private Long receiverId;
    private String receiverName;
    private String receiverEmail;
    private Long senderId;
    private String senderName;
    private String title;
    private String body;
    private String type;
    private String data;
    private String status;
    private Boolean read;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminNotificationLogResponse from(Notification notification) {
        return AdminNotificationLogResponse.builder()
                .id(notification.getId())
                .receiverId(notification.getReceiver() == null ? null : notification.getReceiver().getId())
                .receiverName(notification.getReceiver() == null ? null : notification.getReceiver().getFullName())
                .receiverEmail(notification.getReceiver() == null ? null : notification.getReceiver().getEmail())
                .senderId(notification.getSender() == null ? null : notification.getSender().getId())
                .senderName(notification.getSender() == null ? null : notification.getSender().getFullName())
                .title(notification.getTitle())
                .body(notification.getBody())
                .type(notification.getType())
                .data(notification.getData())
                .status(notification.getStatus())
                .read(Boolean.TRUE.equals(notification.getIsRead()))
                .scheduledAt(notification.getScheduledAt())
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}
