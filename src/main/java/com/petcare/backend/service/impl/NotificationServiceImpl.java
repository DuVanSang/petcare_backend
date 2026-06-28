package com.petcare.backend.service.impl;

import com.petcare.backend.dto.notification.response.NotificationResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.Notification;
import com.petcare.backend.repository.NotificationRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.NotificationService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(UserPrincipal principal, boolean unread) {
        List<Notification> notifications = unread
                ? notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(principal.getId())
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(principal.getId());
        return notifications.stream().map(NotificationResponse::from).toList();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UserPrincipal principal, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, principal.getId())
                .orElseThrow(() -> new BadRequestException("Thông báo không tồn tại"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return NotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UserPrincipal principal) {
        LocalDateTime now = LocalDateTime.now();
        notificationRepository.findByUserIdAndReadAtIsNull(principal.getId()).forEach(notification -> {
            notification.setReadAt(now);
            notificationRepository.save(notification);
        });
    }
}
