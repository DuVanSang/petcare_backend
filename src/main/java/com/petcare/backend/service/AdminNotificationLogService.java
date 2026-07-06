package com.petcare.backend.service;

import com.petcare.backend.dto.admin.notification.response.AdminNotificationLogResponse;
import com.petcare.backend.dto.common.PageResponse;
import java.time.LocalDateTime;

public interface AdminNotificationLogService {
    PageResponse<AdminNotificationLogResponse> getNotificationLogs(
            Long receiverId,
            String type,
            String status,
            Boolean unread,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    );

    AdminNotificationLogResponse getNotificationLogDetail(Long notificationId);
}
