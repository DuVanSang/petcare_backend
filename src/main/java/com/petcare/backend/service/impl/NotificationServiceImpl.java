package com.petcare.backend.service.impl;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.notification.response.NotificationResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Notification;
import com.petcare.backend.repository.NotificationRepository;
import com.petcare.backend.service.NotificationService;
import com.petcare.backend.service.SocialPermissionService;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private static final int MAX_PAGE_SIZE = 50;
    private static final Pattern REFERENCE_ID_PATTERN = Pattern.compile("\"referenceId\"\\s*:\\s*(\\d+)");

    private final NotificationRepository notificationRepository;
    private final SocialPermissionService socialPermissionService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(Long currentUserId, int page, int size) {
        socialPermissionService.checkUserActive(currentUserId);
        Pageable pageable = buildPageable(page, size);
        Page<NotificationResponse> notifications = notificationRepository
                .findByReceiver_IdOrderByCreatedAtDesc(currentUserId, pageable)
                .map(this::toResponse);
        return PageResponse.from(notifications);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long currentUserId) {
        socialPermissionService.checkUserActive(currentUserId);
        return notificationRepository.countByReceiver_IdAndIsReadFalse(currentUserId);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long currentUserId, Long notificationId) {
        socialPermissionService.checkUserActive(currentUserId);
        if (notificationId == null || notificationId <= 0) {
            throw new BadRequestException("Notification id must be greater than 0");
        }
        Notification notification = notificationRepository.findByIdAndReceiver_Id(notificationId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead(Long currentUserId) {
        socialPermissionService.checkUserActive(currentUserId);
        LocalDateTime now = LocalDateTime.now();
        notificationRepository.findByReceiver_IdOrderByCreatedAtDesc(currentUserId).stream()
                .filter(notification -> !Boolean.TRUE.equals(notification.getIsRead()))
                .forEach(notification -> {
                    notification.setIsRead(true);
                    notification.setReadAt(now);
                });
    }

    private Pageable buildPageable(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page must not be negative");
        }
        if (size <= 0) {
            throw new BadRequestException("Size must be greater than 0");
        }
        return PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .senderId(notification.getSender() == null ? null : notification.getSender().getId())
                .receiverId(notification.getReceiver() == null ? null : notification.getReceiver().getId())
                .title(notification.getTitle())
                .content(notification.getBody())
                .type(notification.getType())
                .referenceId(extractReferenceId(notification.getData()))
                .isRead(Boolean.TRUE.equals(notification.getIsRead()))
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private Long extractReferenceId(String data) {
        if (data == null) {
            return null;
        }
        Matcher matcher = REFERENCE_ID_PATTERN.matcher(data);
        if (!matcher.find()) {
            return null;
        }
        return Long.valueOf(matcher.group(1));
    }
}
