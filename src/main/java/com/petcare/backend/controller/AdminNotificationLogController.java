package com.petcare.backend.controller;

import com.petcare.backend.dto.admin.notification.response.AdminNotificationLogResponse;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.service.AdminNotificationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/notification-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
@Tag(name = "Admin - Notification Logs", description = "Giám sát thông báo in-app")
@SecurityRequirement(name = "bearerAuth")
public class AdminNotificationLogController {
    private final AdminNotificationLogService adminNotificationLogService;

    @GetMapping
    @Operation(summary = "Lấy log thông báo")
    public ResponseEntity<ApiResponse<PageResponse<AdminNotificationLogResponse>>> getNotificationLogs(
            @RequestParam(required = false) Long receiverId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean unread,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy log thông báo thành công",
                adminNotificationLogService.getNotificationLogs(receiverId, type, status, unread, from, to, page, size)
        ));
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Xem chi tiết log thông báo")
    public ResponseEntity<ApiResponse<AdminNotificationLogResponse>> getNotificationLogDetail(
            @PathVariable Long notificationId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết log thông báo thành công",
                adminNotificationLogService.getNotificationLogDetail(notificationId)
        ));
    }
}
