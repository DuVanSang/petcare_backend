package com.petcare.backend.dto.notification.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class NotificationResponse {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String title;
    private String content;
    private String type;
    private Long referenceId;
    private Boolean isRead;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
