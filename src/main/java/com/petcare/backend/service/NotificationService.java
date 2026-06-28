package com.petcare.backend.service;

import com.petcare.backend.dto.notification.response.NotificationResponse;
import com.petcare.backend.security.UserPrincipal;
import java.util.List;

public interface NotificationService {
    List<NotificationResponse> getMyNotifications(UserPrincipal principal, boolean unread);

    NotificationResponse markAsRead(UserPrincipal principal, Long notificationId);

    void markAllAsRead(UserPrincipal principal);
}
