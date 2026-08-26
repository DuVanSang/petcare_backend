package com.petcare.backend.controller;

import com.petcare.backend.dto.admin.reminder.response.AdminReminderLogResponse;
import com.petcare.backend.dto.admin.reminder.response.AdminVaccinationReminderLogResponse;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.model.CareReminder;
import com.petcare.backend.model.CareReminderLog;
import com.petcare.backend.model.VaccinationReminderLog;
import com.petcare.backend.service.AdminReminderLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
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
@RequestMapping("/api/v1/admin/reminder-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Reminder Logs", description = "Giám sát log lịch nhắc")
@SecurityRequirement(name = "bearerAuth")
public class AdminReminderLogController {
    private final AdminReminderLogService adminReminderLogService;

    @GetMapping("/custom")
    @Operation(summary = "Lấy log lịch nhắc cá nhân")
    public ResponseEntity<ApiResponse<PageResponse<AdminReminderLogResponse>>> getCustomReminderLogs(
            @RequestParam(required = false) CareReminderLog.ReminderLogStatus status,
            @RequestParam(required = false) CareReminder.ReminderCategory category,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy log lịch nhắc cá nhân thành công",
                adminReminderLogService.getCustomReminderLogs(status, category, petId, userId, from, to, page, size)
        ));
    }

    @GetMapping("/custom/{logId}")
    @Operation(summary = "Xem chi tiết log lịch nhắc cá nhân")
    public ResponseEntity<ApiResponse<AdminReminderLogResponse>> getCustomReminderLogDetail(
            @PathVariable Long logId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết log lịch nhắc cá nhân thành công",
                adminReminderLogService.getCustomReminderLogDetail(logId)
        ));
    }

    @GetMapping("/vaccination")
    @Operation(summary = "Lấy log nhắc tiêm hệ thống")
    public ResponseEntity<ApiResponse<PageResponse<AdminVaccinationReminderLogResponse>>> getVaccinationReminderLogs(
            @RequestParam(required = false) VaccinationReminderLog.VaccinationReminderStatus status,
            @RequestParam(required = false) VaccinationReminderLog.VaccinationReminderStage stage,
            @RequestParam(required = false) Long vaccinationId,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy log nhắc tiêm hệ thống thành công",
                adminReminderLogService.getVaccinationReminderLogs(
                        status,
                        stage,
                        vaccinationId,
                        petId,
                        userId,
                        from,
                        to,
                        page,
                        size
                )
        ));
    }

    @GetMapping("/vaccination/{logId}")
    @Operation(summary = "Xem chi tiết log nhắc tiêm hệ thống")
    public ResponseEntity<ApiResponse<AdminVaccinationReminderLogResponse>> getVaccinationReminderLogDetail(
            @PathVariable Long logId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết log nhắc tiêm hệ thống thành công",
                adminReminderLogService.getVaccinationReminderLogDetail(logId)
        ));
    }
}
