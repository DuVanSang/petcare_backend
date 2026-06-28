package com.petcare.backend.controller;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.notification.response.NotificationResponse;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Quản lý thông báo in-app")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Lấy danh sách thông báo")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "false") boolean unread) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách thông báo thành công",
                notificationService.getMyNotifications(principal, unread)
        ));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Đánh dấu thông báo đã đọc")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long notificationId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đánh dấu thông báo đã đọc",
                notificationService.markAsRead(principal, notificationId)
        ));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Đánh dấu tất cả thông báo đã đọc")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllAsRead(principal);
        return ResponseEntity.ok(ApiResponse.success("Đã đọc tất cả thông báo", null));
    }
}
