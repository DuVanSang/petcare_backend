package com.petcare.backend.service;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.notification.response.NotificationResponse;

public interface NotificationService {
    PageResponse<NotificationResponse> getMyNotifications(Long currentUserId, int page, int size);

    long countUnread(Long currentUserId);

    NotificationResponse markAsRead(Long currentUserId, Long notificationId);

    void markAllAsRead(Long currentUserId);
}
