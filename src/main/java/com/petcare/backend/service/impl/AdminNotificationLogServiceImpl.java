package com.petcare.backend.service.impl;

import com.petcare.backend.dto.admin.notification.response.AdminNotificationLogResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Notification;
import com.petcare.backend.repository.NotificationRepository;
import com.petcare.backend.service.AdminNotificationLogService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminNotificationLogServiceImpl implements AdminNotificationLogService {
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminNotificationLogResponse> getNotificationLogs(
            Long receiverId,
            String type,
            String status,
            Boolean unread,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    ) {
        validateTimeRange(from, to);
        Pageable pageable = PageRequest.of(
                validatePage(page),
                validateSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return PageResponse.from(notificationRepository
                .findAll(notificationSpecification(receiverId, type, status, unread, from, to), pageable)
                .map(AdminNotificationLogResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminNotificationLogResponse getNotificationLogDetail(Long notificationId) {
        return AdminNotificationLogResponse.from(notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo")));
    }

    private Specification<Notification> notificationSpecification(
            Long receiverId,
            String type,
            String status,
            Boolean unread,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (receiverId != null) {
                predicates.add(cb.equal(root.get("receiver").get("id"), receiverId));
            }
            if (StringUtils.hasText(type)) {
                predicates.add(cb.equal(cb.lower(root.get("type")), type.trim().toLowerCase()));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(cb.lower(root.get("status")), status.trim().toLowerCase()));
            }
            if (unread != null) {
                predicates.add(Boolean.TRUE.equals(unread)
                        ? cb.isFalse(root.get("isRead"))
                        : cb.isTrue(root.get("isRead")));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private void validateTimeRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Thời gian bắt đầu không được sau thời gian kết thúc");
        }
    }

    private int validatePage(int page) {
        if (page < 0) {
            throw new BadRequestException("Số trang không được âm");
        }
        return page;
    }

    private int validateSize(int size) {
        if (size <= 0) {
            throw new BadRequestException("Kích thước trang phải lớn hơn 0");
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
