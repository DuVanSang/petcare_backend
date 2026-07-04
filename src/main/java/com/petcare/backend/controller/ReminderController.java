package com.petcare.backend.controller;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.reminder.request.CreateReminderRequest;
import com.petcare.backend.dto.reminder.request.RescheduleReminderRequest;
import com.petcare.backend.dto.reminder.request.SnoozeReminderRequest;
import com.petcare.backend.dto.reminder.request.UpdateReminderRequest;
import com.petcare.backend.dto.reminder.response.ReminderCategoryResponse;
import com.petcare.backend.dto.reminder.response.ReminderLogResponse;
import com.petcare.backend.dto.reminder.response.ReminderResponse;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
@Tag(name = "Reminders", description = "Quản lý lịch nhắc chăm sóc thú cưng")
@SecurityRequirement(name = "bearerAuth")
public class ReminderController {
    private final ReminderService reminderService;

    @GetMapping("/categories")
    @Operation(summary = "Lấy danh sách loại lịch nhắc")
    public ResponseEntity<ApiResponse<List<ReminderCategoryResponse>>> getReminderCategories() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách loại lịch nhắc thành công",
                reminderService.getReminderCategories()
        ));
    }

    @PostMapping
    @Operation(summary = "Tạo lịch nhắc")
    public ResponseEntity<ApiResponse<ReminderResponse>> createReminder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReminderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo lịch nhắc thành công",
                reminderService.createReminder(principal, request)
        ));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách lịch nhắc của tôi")
    public ResponseEntity<ApiResponse<List<ReminderResponse>>> getMyReminders(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách lịch nhắc thành công",
                reminderService.getMyReminders(principal)
        ));
    }

    @GetMapping("/{reminderId}")
    @Operation(summary = "Lấy chi tiết lịch nhắc")
    public ResponseEntity<ApiResponse<ReminderResponse>> getReminder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reminderId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy thông tin lịch nhắc thành công",
                reminderService.getReminder(principal, reminderId)
        ));
    }

    @PatchMapping("/{reminderId}")
    @Operation(summary = "Cập nhật lịch nhắc")
    public ResponseEntity<ApiResponse<ReminderResponse>> updateReminder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reminderId,
            @Valid @RequestBody UpdateReminderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật lịch nhắc thành công",
                reminderService.updateReminder(principal, reminderId, request)
        ));
    }

    @PatchMapping("/{reminderId}/reschedule")
    @Operation(summary = "Dời lịch nhắc")
    public ResponseEntity<ApiResponse<ReminderResponse>> rescheduleReminder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reminderId,
            @Valid @RequestBody RescheduleReminderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Dời lịch nhắc thành công",
                reminderService.rescheduleReminder(principal, reminderId, request)
        ));
    }

    @DeleteMapping("/{reminderId}")
    @Operation(summary = "Xóa lịch nhắc")
    public ResponseEntity<ApiResponse<Void>> deleteReminder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reminderId) {
        reminderService.deleteReminder(principal, reminderId);
        return ResponseEntity.ok(ApiResponse.success("Xóa lịch nhắc thành công", null));
    }

    @PostMapping("/{reminderId}/complete")
    @Operation(summary = "Đánh dấu lần nhắc đã hoàn thành")
    public ResponseEntity<ApiResponse<ReminderLogResponse>> completeReminder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reminderId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đánh dấu hoàn thành thành công",
                reminderService.completeReminder(principal, reminderId)
        ));
    }

    @PostMapping("/{reminderId}/snooze")
    @Operation(summary = "Báo lại lịch nhắc")
    public ResponseEntity<ApiResponse<ReminderLogResponse>> snoozeReminder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reminderId,
            @Valid @RequestBody SnoozeReminderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật thời gian báo lại thành công",
                reminderService.snoozeReminder(principal, reminderId, request)
        ));
    }

    @GetMapping("/{reminderId}/logs")
    @Operation(summary = "Lấy lịch sử các lần nhắc")
    public ResponseEntity<ApiResponse<List<ReminderLogResponse>>> getReminderLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reminderId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy lịch sử nhắc thành công",
                reminderService.getReminderLogs(principal, reminderId)
        ));
    }
}
